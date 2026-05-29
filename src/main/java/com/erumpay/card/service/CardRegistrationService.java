package com.erumpay.card.service;

import com.erumpay.card.client.AuthServiceClient;
import com.erumpay.card.client.BillingKeyServiceClient;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.dto.CardRegisterResponse;
import com.erumpay.card.dto.client.AuthUserInfoResponse;
import com.erumpay.card.dto.client.BillingKeyDeleteRequest;
import com.erumpay.card.dto.client.BillingKeyDeleteResponse;
import com.erumpay.card.dto.client.BillingKeyIssueRequest;
import com.erumpay.card.dto.client.BillingKeyIssueResponse;
import com.erumpay.card.exception.AuthServiceUnavailableException;
import com.erumpay.card.exception.BillingKeyIssueFailedException;
import com.erumpay.card.exception.BillingKeyIssuePendingException;
import com.erumpay.card.exception.BillingKeyIssueUnknownException;
import com.erumpay.card.exception.CardProductNotFoundException;
import com.erumpay.card.exception.CardRegistrationFailedException;
import com.erumpay.card.exception.DuplicateCardRegistrationException;
import com.erumpay.card.exception.InvalidExpiryYmException;
import com.erumpay.card.exception.InvalidUserBirthDateException;
import com.erumpay.card.exception.UserNotActiveException;
import com.erumpay.card.exception.UserNotFoundException;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import feign.FeignException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardRegistrationService {

	private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
	private static final String USER_STATUS_ACTIVE = "ACTIVE";
	private static final String BILLING_KEY_ISSUE_SUCCESS = "100";
	private static final String BILLING_KEY_ISSUE_ACTIVE_ECHO = "102";
	private static final String BILLING_KEY_ISSUE_PENDING = "105";
	private static final String BILLING_KEY_DELETE_SUCCESS = "100";
	private static final List<CardStatus> DUPLICATE_CHECK_STATUSES = List.of(
		CardStatus.REGISTERING,
		CardStatus.ACTIVE,
		CardStatus.PAUSED,
		CardStatus.EXPIRED
	);

	private final CardProductRepository cardProductRepository;
	private final CardRegisteredRepository cardRegisteredRepository;
	private final AuthServiceClient authServiceClient;
	private final BillingKeyServiceClient billingKeyServiceClient;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	// [be] 이준혁 260521 1602 | 카드 등록 요청의 기본 검증, 상품 조회, 중복 등록 여부를 먼저 확인한다.
	public CardRegisterResponse register(CardRegisterRequest request) {
		validateExpiryYm(request.getExpiryYm());

		CardProduct cardProduct = cardProductRepository.findByMockBin(request.mockBin())
			.orElseThrow(CardProductNotFoundException::new);

		validateDuplicateRegistration(request.getUserId(), cardProduct.getCardProductId());

		String billingBirthDate = findBillingBirthDate(request.getUserId());
		CardRegistered registeringCard = createRegisteringCard(request, cardProduct.getCardProductId());
		BillingKeyIssueResponse issueResponse;
		try {
			issueResponse = issueBillingKey(registeringCard.getCardId(), request, billingBirthDate);
		} catch (BillingKeyIssueFailedException exception) {
			softDeleteRegisteringCard(registeringCard);
			throw exception;
		}

		if (isPendingIssue(issueResponse)) {
			softDeleteRegisteringCard(registeringCard);
			throw new BillingKeyIssuePendingException();
		}
		if (!isSuccessfulIssue(issueResponse)) {
			softDeleteRegisteringCard(registeringCard);
			throw new BillingKeyIssueFailedException();
		}

		return activateRegisteredCard(request, cardProduct, registeringCard, issueResponse);
	}

	// [be] 이준혁 260521 1602 | 유효기간이 yyyyMM 형식이고 현재 월보다 과거가 아닌지 확인한다.
	private void validateExpiryYm(String expiryYm) {
		YearMonth parsedExpiryYm;
		try {
			parsedExpiryYm = YearMonth.parse(expiryYm, EXPIRY_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new InvalidExpiryYmException(exception);
		}

		if (parsedExpiryYm.isBefore(YearMonth.now(clock))) {
			throw new InvalidExpiryYmException();
		}
	}

	// [be] 이준혁 260521 1602 | 같은 사용자가 같은 카드 상품을 이미 등록했는지 확인한다. DELETED 상태는 재등록 가능하므로 제외한다.
	private void validateDuplicateRegistration(Long userId, Long cardProductId) {
		boolean alreadyRegistered = cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(
			userId,
			cardProductId,
			DUPLICATE_CHECK_STATUSES
		);

		if (alreadyRegistered) {
			throw new DuplicateCardRegistrationException();
		}
	}

	private String findBillingBirthDate(Long userId) {
		AuthUserInfoResponse response;
		try {
			response = authServiceClient.getUserInfo(userId);
		} catch (FeignException.NotFound exception) {
			throw new UserNotFoundException();
		} catch (FeignException exception) {
			throw new AuthServiceUnavailableException(exception);
		}

		if (response == null) {
			throw new AuthServiceUnavailableException(
				new IllegalStateException("auth-service user info response is required")
			);
		}
		if (!USER_STATUS_ACTIVE.equals(response.status())) {
			throw new UserNotActiveException();
		}
		return normalizeBirthDate(response.birthDate());
	}

	private String normalizeBirthDate(String birthDate) {
		if (birthDate == null || birthDate.isBlank()) {
			throw new InvalidUserBirthDateException();
		}

		String normalized = birthDate.trim();
		if (normalized.matches("\\d{8}")) {
			return normalized.substring(2);
		}
		if (normalized.matches("\\d{6}")) {
			return normalized;
		}
		throw new InvalidUserBirthDateException();
	}

	private CardRegistered createRegisteringCard(CardRegisterRequest request, Long cardProductId) {
		return Objects.requireNonNull(transactionTemplate.execute(status -> cardRegisteredRepository.save(
			CardRegistered.registering(
				request.getUserId(),
				cardProductId,
				request.normalizedCardAlias(),
				request.getExpiryYm()
			)
		)));
	}

	// [be] 이준혁 260528 1910 | 빌링키 발급 결과가 불명확하면 같은 pay_card_id로 1회 재확인한다.
	private BillingKeyIssueResponse issueBillingKey(Long payCardId, CardRegisterRequest request, String birthDate) {
		BillingKeyIssueRequest issueRequest = new BillingKeyIssueRequest(
			payCardId,
			request.getCardNumber(),
			toBillingExpiryDate(request.getExpiryYm()),
			request.getCvc(),
			request.getCardPassword2(),
			birthDate
		);

		try {
			return billingKeyServiceClient.issue(issueRequest);
		} catch (FeignException exception) {
			if (!isUnknownIssueResult(exception.status())) {
				throw new BillingKeyIssueFailedException(exception);
			}
			return retryIssueBillingKeyOnce(issueRequest);
		} catch (RuntimeException exception) {
			return retryIssueBillingKeyOnce(issueRequest);
		}
	}

	private BillingKeyIssueResponse retryIssueBillingKeyOnce(BillingKeyIssueRequest issueRequest) {
		try {
			return billingKeyServiceClient.issue(issueRequest);
		} catch (FeignException retryException) {
			if (isUnknownIssueResult(retryException.status())) {
				throw new BillingKeyIssueUnknownException(retryException);
			}
			throw new BillingKeyIssueFailedException(retryException);
		} catch (RuntimeException retryException) {
			throw new BillingKeyIssueUnknownException(retryException);
		}
	}

	private boolean isUnknownIssueResult(int status) {
		return status <= 0 || status >= 500;
	}

	private String toBillingExpiryDate(String expiryYm) {
		return expiryYm.substring(2);
	}

	private boolean isPendingIssue(BillingKeyIssueResponse response) {
		return response != null && BILLING_KEY_ISSUE_PENDING.equals(response.responseCode());
	}

	private boolean isSuccessfulIssue(BillingKeyIssueResponse response) {
		return response != null
			&& !isBlank(response.billingKey())
			&& !isBlank(response.maskedNumber())
			&& (
				BILLING_KEY_ISSUE_SUCCESS.equals(response.responseCode())
					|| BILLING_KEY_ISSUE_ACTIVE_ECHO.equals(response.responseCode())
			);
	}

	private CardRegisterResponse activateRegisteredCard(
		CardRegisterRequest request,
		CardProduct cardProduct,
		CardRegistered registeringCard,
		BillingKeyIssueResponse issueResponse
	) {
		try {
			CardRegistered activeCard = Objects.requireNonNull(transactionTemplate.execute(status -> {
				Optional<CardRegistered> currentDefault = cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(
					request.getUserId(),
					CardStatus.ACTIVE
				);
				boolean shouldBeDefault = request.defaultRequested() || currentDefault.isEmpty();
				if (shouldBeDefault) {
					currentDefault.ifPresent(card -> {
						card.unsetDefault();
						cardRegisteredRepository.save(card);
						cardRegisteredRepository.flush();
					});
				}
				registeringCard.activate(issueResponse.billingKey(), issueResponse.maskedNumber(), shouldBeDefault);
				return cardRegisteredRepository.save(registeringCard);
			}));
			return toRegisterResponse(activeCard, cardProduct);
		} catch (RuntimeException exception) {
			compensateBillingKey(registeringCard.getCardId(), issueResponse.billingKey(), exception);
			trySoftDeleteAfterActivationFailure(registeringCard);
			throw new CardRegistrationFailedException(exception);
		}
	}

	private void compensateBillingKey(Long payCardId, String billingKey, RuntimeException originalException) {
		try {
			BillingKeyDeleteResponse response = billingKeyServiceClient.delete(
				new BillingKeyDeleteRequest(payCardId, billingKey)
			);
			if (response == null || !BILLING_KEY_DELETE_SUCCESS.equals(response.responseCode())) {
				log.error("Billing key compensation returned non-success response. payCardId={}", payCardId);
			}
		} catch (RuntimeException compensationException) {
			log.error("Billing key compensation failed. payCardId={}", payCardId, compensationException);
			originalException.addSuppressed(compensationException);
		}
	}

	private void softDeleteRegisteringCard(CardRegistered registeringCard) {
		transactionTemplate.executeWithoutResult(status -> {
			registeringCard.delete(LocalDateTime.now(clock));
			cardRegisteredRepository.save(registeringCard);
		});
	}

	private void trySoftDeleteAfterActivationFailure(CardRegistered registeringCard) {
		try {
			softDeleteRegisteringCard(registeringCard);
		} catch (RuntimeException exception) {
			log.error(
				"Failed to soft delete registering card after activation failure. cardId={}",
				registeringCard.getCardId(),
				exception
			);
		}
	}

	private CardRegisterResponse toRegisterResponse(CardRegistered card, CardProduct product) {
		return CardRegisterResponse.builder()
			.cardId(card.getCardId())
			.cardProductId(card.getCardProductId())
			.cardCompany(product.getCardCompany())
			.cardName(product.getCardName())
			.maskedNumber(card.getMaskedNumber())
			.cardAlias(card.getCardAlias())
			.expiryYm(card.getExpiryYm())
			.isDefault(card.isDefaultCard())
			.status(card.getStatus())
			.build();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
