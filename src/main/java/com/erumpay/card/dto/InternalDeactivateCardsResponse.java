package com.erumpay.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalDeactivateCardsResponse {

	private Long userId;
	private Integer deactivatedCount;
}
