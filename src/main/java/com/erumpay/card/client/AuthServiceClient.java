package com.erumpay.card.client;

import com.erumpay.card.dto.client.AuthUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.base-url}")
public interface AuthServiceClient {

	@GetMapping("/internal/v1/users/{userId}")
	AuthUserInfoResponse getUserInfo(@PathVariable("userId") Long userId);
}
