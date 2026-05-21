package com.erumpay.card.dto;

import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.enums.CardType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBinValidateResponse {

	private boolean supported;
	private CardProductSummary cardProduct;

	public static CardBinValidateResponse supported(CardProduct cardProduct) {
		return CardBinValidateResponse.builder()
			.supported(true)
			.cardProduct(CardProductSummary.from(cardProduct))
			.build();
	}

	public static CardBinValidateResponse unsupported() {
		return CardBinValidateResponse.builder()
			.supported(false)
			.cardProduct(null)
			.build();
	}

	@Getter
	@Builder
	public static class CardProductSummary {

		private Long cardProductId;
		private String cardCompany;
		private String cardName;
		private CardType cardType;
		private String imageUrl;

		private static CardProductSummary from(CardProduct cardProduct) {
			return CardProductSummary.builder()
				.cardProductId(cardProduct.getCardProductId())
				.cardCompany(cardProduct.getCardCompany())
				.cardName(cardProduct.getCardName())
				.cardType(cardProduct.getCardType())
				.imageUrl(cardProduct.getImageUrl())
				.build();
		}
	}
}
