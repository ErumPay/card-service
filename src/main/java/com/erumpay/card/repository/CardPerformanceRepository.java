package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardPerformance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardPerformanceRepository extends JpaRepository<CardPerformance, Long> {

	Optional<CardPerformance> findByCardIdAndUserIdAndYearMonth(Long cardId, Long userId, String yearMonth);
}
