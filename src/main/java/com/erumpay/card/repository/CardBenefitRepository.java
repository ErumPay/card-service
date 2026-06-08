package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardBenefit;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBenefitRepository extends JpaRepository<CardBenefit, Long> {

	boolean existsByBenefitIdAndCardProductId(Long benefitId, Long cardProductId);

	List<CardBenefit> findByCardProductIdOrderByPriorityDescBenefitIdAsc(Long cardProductId);

	List<CardBenefit> findByCardProductIdInOrderByCardProductIdAscPriorityDescBenefitIdAsc(
		Collection<Long> cardProductIds
	);
}
