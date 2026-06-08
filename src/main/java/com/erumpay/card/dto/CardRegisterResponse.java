package com.erumpay.card.dto;

import com.erumpay.card.domain.enums.CardStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardRegisterResponse {

	private Long cardId;
	private Long cardProductId;
	private String cardCompany;
	private String cardName;
	private String imageUrl;
	private String maskedNumber;
	private String cardAlias;
	private String expiryYm;
	private Boolean isDefault;
	private CardStatus status;
}
