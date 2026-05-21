package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardBenefitBrand;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardBenefitBrandRepository extends JpaRepository<CardBenefitBrand, Long> {

	List<CardBenefitBrand> findByBenefitIdInOrderByBrandNameAsc(Collection<Long> benefitIds);
}
