package com.erumpay.card.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class CardAliasUpdateRequest {

	@Size(max = 10, message = "카드 별칭은 10자 이하여야 합니다.")
	private String cardAlias;

	public CardAliasUpdateRequest(String cardAlias) {
		setCardAlias(cardAlias);
	}

	public void setCardAlias(String cardAlias) {
		if (cardAlias == null) {
			this.cardAlias = null;
			return;
		}
		String trimmedAlias = cardAlias.trim();
		this.cardAlias = trimmedAlias.isEmpty() ? null : trimmedAlias;
	}

	public String normalizedCardAlias() {
		return cardAlias;
	}
}
