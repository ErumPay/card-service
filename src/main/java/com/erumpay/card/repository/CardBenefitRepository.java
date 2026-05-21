package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardBenefit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBenefitRepository extends JpaRepository<CardBenefit, Long> {

	List<CardBenefit> findByCardProductIdOrderByPriorityDescBenefitIdAsc(Long cardProductId);
}
