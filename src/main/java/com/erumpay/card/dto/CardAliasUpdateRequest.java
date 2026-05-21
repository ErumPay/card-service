package com.erumpay.card.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardAliasUpdateRequest {

	@Size(max = 10, message = "카드 별칭은 10자 이하여야 합니다.")
	private String cardAlias;

	public String normalizedCardAlias() {
		if (cardAlias == null) {
			return null;
		}
		String trimmedAlias = cardAlias.trim();
		return trimmedAlias.isEmpty() ? null : trimmedAlias;
	}
}
