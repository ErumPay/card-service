package com.erumpay.card.dto;

import com.erumpay.card.domain.enums.CardStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardResponse {

	private Long cardId;
	private Long cardProductId;
	private String cardCompany;
	private String cardName;
	private String maskedNumber;
	private String cardAlias;
	private String expiryYm;
	private Boolean isDefault;
	private CardStatus status;
}
