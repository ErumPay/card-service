package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardBenefitTier;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBenefitTierRepository extends JpaRepository<CardBenefitTier, Long> {

	List<CardBenefitTier> findByBenefitIdInOrderByMinPrevMonthUsageAsc(Collection<Long> benefitIds);
}
