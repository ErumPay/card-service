package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRegisteredRepository extends JpaRepository<CardRegistered, Long> {

	// 중복 카드 등록 차단용
	boolean existsByUserIdAndCardProductIdAndStatusIn(
			Long userId,
			Long cardProductId,
			Collection<CardStatus> statuses);
}
