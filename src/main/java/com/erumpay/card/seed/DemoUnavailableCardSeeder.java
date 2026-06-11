package com.erumpay.card.seed;

import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DemoUnavailableCardSeeder implements ApplicationRunner {

	private static final Long DEMO_USER_ID = 2L;
	private static final String LOCA_LIKIT_EAT_BIN = "840002";
	private static final String LOCA_LIKIT_EAT_MASKED_NUMBER = "8400-****-****-7890";
	private static final String LOCA_LIKIT_EAT_EXPIRY_YM = "202912";
	private static final List<CardStatus> VISIBLE_STATUSES = List.of(
		CardStatus.ACTIVE,
		CardStatus.PAUSED,
		CardStatus.EXPIRED
	);

	private final CardProductRepository cardProductRepository;
	private final CardRegisteredRepository cardRegisteredRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		CardProduct product = cardProductRepository.findByMockBin(LOCA_LIKIT_EAT_BIN)
			.orElse(null);

		if (product == null) {
			return;
		}

		boolean alreadySeeded = cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(
			DEMO_USER_ID,
			product.getCardProductId(),
			VISIBLE_STATUSES
		);

		if (alreadySeeded) {
			return;
		}

		CardRegistered card = CardRegistered.registering(
			DEMO_USER_ID,
			product.getCardProductId(),
			null,
			LOCA_LIKIT_EAT_EXPIRY_YM
		);
		card.markUnavailable(LOCA_LIKIT_EAT_MASKED_NUMBER, CardStatus.EXPIRED);
		cardRegisteredRepository.save(card);
	}
}
