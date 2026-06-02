package com.erumpay.card.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InternalBillingKeysRequest(
	@NotEmpty(message = "cardIds는 비어 있을 수 없습니다.")
	List<Long> cardIds
) {
}
