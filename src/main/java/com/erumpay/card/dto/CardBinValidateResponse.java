package com.erumpay.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBinValidateResponse {

	private boolean supported;
	private CardProductSummary cardProduct;

	public static CardBinValidateResponse supported(
		Long cardProductId,
		String cardCompany,
		String cardName,
		String cardType,
		String imageUrl
	) {
		return CardBinValidateResponse.builder()
			.supported(true)
			.cardProduct(CardProductSummary.builder()
				.cardProductId(cardProductId)
				.cardCompany(cardCompany)
				.cardName(cardName)
				.cardType(cardType)
				.imageUrl(imageUrl)
				.build())
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
		private String cardType;
		private String imageUrl;
	}
}
