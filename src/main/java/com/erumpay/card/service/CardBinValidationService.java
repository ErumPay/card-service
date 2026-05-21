package com.erumpay.card.service;

import com.erumpay.card.dto.CardBinValidateRequest;
import com.erumpay.card.dto.CardBinValidateResponse;
import com.erumpay.card.repository.CardProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardBinValidationService {

	private final CardProductRepository cardProductRepository;

	// [be] 이준혁 260521 1706 | 카드번호 앞 6자리로 지원 카드 상품 여부를 조회한다.
	@Transactional(readOnly = true)
	public CardBinValidateResponse validate(CardBinValidateRequest request) {
		return cardProductRepository.findByMockBin(request.mockBin())
			.map(CardBinValidateResponse::supported)
			.orElseGet(CardBinValidateResponse::unsupported);
	}
}
