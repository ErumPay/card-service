package com.erumpay.card.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPerformanceSyncScheduler {

	private final CardPerformanceSyncService cardPerformanceSyncService;

	@Scheduled(cron = "0 0 1 1 * *", zone = "Asia/Seoul")
	public void syncPreviousMonthForActiveCards() {
		cardPerformanceSyncService.syncPreviousMonthForActiveCards();
	}
}
