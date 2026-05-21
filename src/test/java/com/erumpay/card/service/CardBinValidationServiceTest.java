package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.enums.CardType;
import com.erumpay.card.dto.CardBinValidateRequest;
import com.erumpay.card.dto.CardBinValidateResponse;
import com.erumpay.card.repository.CardProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardBinValidationServiceTest {

	private CardProductRepository cardProductRepository;
	private CardBinValidationService cardBinValidationService;

	@BeforeEach
	void setUp() {
		cardProductRepository = mock(CardProductRepository.class);
		cardBinValidationService = new CardBinValidationService(cardProductRepository);
	}

	@Test
	void validateReturnsSupportedWhenMockBinMatchesCardProduct() {
		CardProduct cardProduct = cardProduct();
		when(cardProductRepository.findByMockBin("800012")).thenReturn(Optional.of(cardProduct));

		CardBinValidateResponse response = cardBinValidationService.validate(
			new CardBinValidateRequest("8000123456781234")
		);

		assertThat(response.isSupported()).isTrue();
		assertThat(response.getCardProduct()).isNotNull();
		assertThat(response.getCardProduct().getCardProductId()).isEqualTo(10L);
		assertThat(response.getCardProduct().getCardCompany()).isEqualTo("롯데카드");
		assertThat(response.getCardProduct().getCardName()).isEqualTo("LOCA 365 카드");
		assertThat(response.getCardProduct().getCardType()).isEqualTo("CREDIT");
		assertThat(response.getCardProduct().getImageUrl()).isEqualTo("https://example.com/card.png");
		verify(cardProductRepository).findByMockBin("800012");
	}

	@Test
	void validateReturnsUnsupportedWhenMockBinDoesNotMatchCardProduct() {
		when(cardProductRepository.findByMockBin("800012")).thenReturn(Optional.empty());

		CardBinValidateResponse response = cardBinValidationService.validate(
			new CardBinValidateRequest("8000123456781234")
		);

		assertThat(response.isSupported()).isFalse();
		assertThat(response.getCardProduct()).isNull();
		verify(cardProductRepository).findByMockBin("800012");
	}

	private CardProduct cardProduct() {
		CardProduct cardProduct = mock(CardProduct.class);
		when(cardProduct.getCardProductId()).thenReturn(10L);
		when(cardProduct.getCardCompany()).thenReturn("롯데카드");
		when(cardProduct.getCardName()).thenReturn("LOCA 365 카드");
		when(cardProduct.getCardType()).thenReturn(CardType.CREDIT);
		when(cardProduct.getImageUrl()).thenReturn("https://example.com/card.png");
		return cardProduct;
	}
}
