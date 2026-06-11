package com.erumpay.card.service;

import com.erumpay.card.client.AuthServiceClient;
import com.erumpay.card.client.CardSimulatorServiceClient;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.client.AuthUserInfoResponse;
import com.erumpay.card.dto.client.PerformanceInquireRequest;
import com.erumpay.card.dto.client.PerformanceInquireResponse;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardPerformanceSyncService {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
	private static final String USER_STATUS_ACTIVE = "ACTIVE";
	private static final String PERFORMANCE_SUCCESS_CODE = "SIM-CARD-200";

	private final AuthServiceClient authServiceClient;
	private final CardSimulatorServiceClient cardSimulatorServiceClient;
	private final CardRegisteredRepository cardRegisteredRepository;
	private final CardProductRepository cardProductRepository;
	private final CardPerformanceRepository cardPerformanceRepository;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public void syncAfterRegistration(AuthUserInfoResponse userInfo, Long cardId, CardProduct cardProduct) {
		syncCardPerformance(userInfo, cardId, cardProduct, previousYearMonth());
	}

	public void syncPreviousMonthForActiveCards() {
		String yearMonth = previousYearMonth();
		for (CardRegistered card : cardRegisteredRepository.findByStatus(CardStatus.ACTIVE)) {
			CardProduct cardProduct = cardProductRepository.findById(card.getCardProductId()).orElse(null);
			if (cardProduct == null) {
				log.warn(
					"Card performance sync skipped because card product was not found. userId={}, cardId={}, cardProductId={}, yearMonth={}",
					card.getUserId(),
					card.getCardId(),
					card.getCardProductId(),
					yearMonth
				);
				continue;
			}
			syncCardPerformance(findUserInfo(card.getUserId()), card.getCardId(), cardProduct, yearMonth);
		}
	}

	private void syncCardPerformance(
		AuthUserInfoResponse userInfo,
		Long cardId,
		CardProduct cardProduct,
		String yearMonth
	) {
		if (!hasRequiredUserInfo(userInfo)) {
			log.warn(
				"Card performance sync skipped because user info was missing. userId={}, cardId={}, yearMonth={}",
				userInfo != null ? userInfo.userId() : null,
				cardId,
				yearMonth
			);
			return;
		}
		if (!USER_STATUS_ACTIVE.equals(userInfo.status())) {
			log.warn(
				"Card performance sync skipped because user is not active. userId={}, cardId={}, status={}, yearMonth={}",
				userInfo.userId(),
				cardId,
				userInfo.status(),
				yearMonth
			);
			return;
		}

		try {
			PerformanceInquireResponse response = cardSimulatorServiceClient.inquirePerformance(
				new PerformanceInquireRequest(
					userInfo.name().trim(),
					userInfo.phoneNumber().trim(),
					cardProduct.getCardCompany(),
					cardProduct.getCardName(),
					yearMonth
				)
			);
			if (!isSuccessful(response)) {
				log.warn(
					"Card simulator performance sync returned non-success response. userId={}, cardId={}, yearMonth={}, responseCode={}, responseMessage={}",
					userInfo.userId(),
					cardId,
					yearMonth,
					response != null ? response.responseCode() : null,
					response != null ? response.responseMessage() : null
				);
				return;
			}
			upsertPerformance(userInfo.userId(), cardId, yearMonth, response.currentAmount());
		} catch (RuntimeException exception) {
			log.warn(
				"Card simulator performance sync failed. userId={}, cardId={}, yearMonth={}",
				userInfo.userId(),
				cardId,
				yearMonth,
				exception
			);
		}
	}

	private AuthUserInfoResponse findUserInfo(Long userId) {
		try {
			return authServiceClient.getUserInfo(userId);
		} catch (RuntimeException exception) {
			log.warn("Card performance sync skipped because auth user info lookup failed. userId={}", userId, exception);
			return null;
		}
	}

	private boolean hasRequiredUserInfo(AuthUserInfoResponse userInfo) {
		return userInfo != null
			&& !isBlank(userInfo.name())
			&& !isBlank(userInfo.phoneNumber());
	}

	private boolean isSuccessful(PerformanceInquireResponse response) {
		return response != null
			&& PERFORMANCE_SUCCESS_CODE.equals(response.responseCode())
			&& response.currentAmount() != null;
	}

	private void upsertPerformance(Long userId, Long cardId, String yearMonth, Long amount) {
		transactionTemplate.executeWithoutResult(status ->
			cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(cardId, userId, yearMonth)
				.ifPresentOrElse(
					performance -> performance.overwrite(amount),
					() -> cardPerformanceRepository.save(CardPerformance.create(userId, cardId, yearMonth, amount))
				)
		);
	}

	private String previousYearMonth() {
		return YearMonth.now(clock).minusMonths(1).format(YEAR_MONTH_FORMATTER);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
