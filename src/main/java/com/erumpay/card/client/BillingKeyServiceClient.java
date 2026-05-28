package com.erumpay.card.client;

import com.erumpay.card.dto.client.BillingKeyDeleteRequest;
import com.erumpay.card.dto.client.BillingKeyDeleteResponse;
import com.erumpay.card.dto.client.BillingKeyIssueRequest;
import com.erumpay.card.dto.client.BillingKeyIssueResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "billing-key-service", url = "${billing-key.base-url}")
public interface BillingKeyServiceClient {

	@PostMapping("/api/v1/billing-key/issue")
	BillingKeyIssueResponse issue(@RequestBody BillingKeyIssueRequest request);

	@PostMapping("/api/v1/billing-key/delete")
	BillingKeyDeleteResponse delete(@RequestBody BillingKeyDeleteRequest request);
}
