package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardBenefitUsage;
import com.erumpay.card.domain.enums.CardBenefitUsageStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBenefitUsageRepository extends JpaRepository<CardBenefitUsage, Long> {

	List<CardBenefitUsage> findByUserIdAndCardIdInAndBenefitIdInAndStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
		Long userId,
		Collection<Long> cardIds,
		Collection<Long> benefitIds,
		CardBenefitUsageStatus status,
		LocalDateTime approvedAtFrom,
		LocalDateTime approvedAtTo
	);
}
