package com.erumpay.card.service;

import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.exception.BillingKeyNotIntegratedException;
import com.erumpay.card.exception.BinMismatchException;
import com.erumpay.card.exception.CardProductNotFoundException;
import com.erumpay.card.exception.DuplicateCardRegistrationException;
import com.erumpay.card.exception.InvalidExpiryYmException;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardRegistrationService {

	private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
	private static final List<CardStatus> DUPLICATE_CHECK_STATUSES = List.of(
			CardStatus.ACTIVE,
			CardStatus.PAUSED,
			CardStatus.EXPIRED);

	private final CardProductRepository cardProductRepository;
	private final CardRegisteredRepository cardRegisteredRepository;
	private final Clock clock;

	// [be] 이준혁 260521 1602 | 카드 등록 요청의 기본 검증, 카드 상품 조회, 중복 등록 여부를 순서대로 확인한다.
	// [be] 이준혁 260521 1602 | billing-key-service 연동 전이므로 카드 원본정보를 저장하지 않고 501로 중단한다.
	@Transactional(readOnly = true)
	public void register(CardRegisterRequest request) {
		validateMockBinMatchesCardNumber(request.getMockBin(), request.getCardNumber());
		validateExpiryYm(request.getExpiryYm());

		CardProduct cardProduct = cardProductRepository.findByMockBin(request.getMockBin())
				.orElseThrow(CardProductNotFoundException::new);

		validateDuplicateRegistration(request.getUserId(), cardProduct.getCardProductId());

		throw new BillingKeyNotIntegratedException();
	}

	// [be] 이준혁 260521 1602 | 사용자가 보낸 mockBin이 실제 카드번호 앞 6자리와 같은지 확인한다.
	private void validateMockBinMatchesCardNumber(String mockBin, String cardNumber) {
		if (!cardNumber.startsWith(mockBin)) {
			throw new BinMismatchException();
		}
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
				DUPLICATE_CHECK_STATUSES);

		if (alreadyRegistered) {
			throw new DuplicateCardRegistrationException();
		}
	}
}
