package com.erumpay.card.client;

import com.erumpay.card.dto.client.AuthUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.base-url}")
public interface AuthServiceClient {

	// auth-service 현재 구현은 프로젝트 표준 /internal/v1이 아니라 /api/v1/internal 경로를 사용한다.
	@GetMapping("/api/v1/internal/users/{userId}")
	AuthUserInfoResponse getUserInfo(@PathVariable("userId") Long userId);
}
