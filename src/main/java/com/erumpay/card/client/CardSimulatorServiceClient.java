package com.erumpay.card.client;

import com.erumpay.card.dto.client.PerformanceInquireRequest;
import com.erumpay.card.dto.client.PerformanceInquireResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "card-simulator-service", url = "${card-simulator.base-url}")
public interface CardSimulatorServiceClient {

	@PostMapping("/api/v1/card-simulator/performance/inquire")
	PerformanceInquireResponse inquirePerformance(@RequestBody PerformanceInquireRequest request);
}
