package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardBenefitUsage;
import com.erumpay.card.domain.enums.CardBenefitUsageStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardBenefitUsageRepository extends JpaRepository<CardBenefitUsage, Long> {

	List<CardBenefitUsage> findByPaymentIdAndCardId(
		Long paymentId,
		Long cardId
	);

	List<CardBenefitUsage> findByUserIdAndCardIdInAndBenefitIdInAndStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
		Long userId,
		Collection<Long> cardIds,
		Collection<Long> benefitIds,
		CardBenefitUsageStatus status,
		LocalDateTime approvedAtFrom,
		LocalDateTime approvedAtTo
	);

	@Query("""
		select coalesce(sum(usage.benefitAmount), 0)
		from CardBenefitUsage usage
		where usage.userId = :userId
			and usage.cardId = :cardId
			and usage.status = :status
			and usage.approvedAt >= :approvedAtFrom
			and usage.approvedAt < :approvedAtTo
		""")
	Long sumBenefitAmount(
		@Param("userId") Long userId,
		@Param("cardId") Long cardId,
		@Param("status") CardBenefitUsageStatus status,
		@Param("approvedAtFrom") LocalDateTime approvedAtFrom,
		@Param("approvedAtTo") LocalDateTime approvedAtTo
	);
}
