package com.erumpay.card.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class InternalBillingKeysResponse {

	@ToString.Include
	private Long userId;

	private List<InternalBillingKeyResponse> billingKeys;
}
