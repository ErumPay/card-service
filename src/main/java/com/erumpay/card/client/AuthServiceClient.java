package com.erumpay.card.client;

import com.erumpay.card.dto.client.AuthUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.base-url}")
public interface AuthServiceClient {

	// auth-service 내부 사용자 조회 API에서 카드 등록용 생년월일과 사용자 상태를 조회한다.
	@GetMapping("/internal/v1/users/{userId}")
	AuthUserInfoResponse getUserInfo(@PathVariable("userId") Long userId);
}
