package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRegisteredRepository extends JpaRepository<CardRegistered, Long> {

	// [be] 이준혁 260521 1602 | 중복 카드 등록 차단용
	boolean existsByUserIdAndCardProductIdAndStatusIn(
			Long userId,
			Long cardProductId,
			Collection<CardStatus> statuses);

	List<CardRegistered> findByUserIdAndStatusInOrderByDefaultCardDescCreatedAtDesc(
			Long userId,
			Collection<CardStatus> statuses);

	List<CardRegistered> findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(Long userId, CardStatus status);

	Optional<CardRegistered> findByCardIdAndUserId(Long cardId, Long userId);

	Optional<CardRegistered> findByCardIdAndUserIdAndStatusIn(
			Long cardId,
			Long userId,
			Collection<CardStatus> statuses);

	Optional<CardRegistered> findByCardIdAndUserIdAndStatusNot(Long cardId, Long userId, CardStatus status);

	List<CardRegistered> findByUserIdAndCardIdIn(Long userId, Collection<Long> cardIds);

	List<CardRegistered> findByUserIdAndCardIdInAndStatusNot(Long userId, Collection<Long> cardIds, CardStatus status);

	Optional<CardRegistered> findByUserIdAndDefaultCardTrueAndStatus(Long userId, CardStatus status);

	Optional<CardRegistered> findFirstByUserIdAndStatusAndCardIdNotOrderByCreatedAtAscCardIdAsc(
			Long userId,
			CardStatus status,
			Long cardId);

	boolean existsByUserIdAndStatusIn(Long userId, Collection<CardStatus> statuses);

	boolean existsByUserIdAndStatus(Long userId, CardStatus status);

	@Query("""
		select count(card) > 0
		from CardRegistered card
		where card.userId = :userId
			and card.status = :status
			and card.encryptedBillingKey is not null
			and length(trim(card.encryptedBillingKey)) > 0
		""")
	boolean existsPaymentAvailableCard(@Param("userId") Long userId, @Param("status") CardStatus status);

	@Query("""
		select card
		from CardRegistered card
		where card.userId = :userId
			and card.status = :status
			and card.encryptedBillingKey is not null
			and length(trim(card.encryptedBillingKey)) > 0
		order by card.defaultCard desc, card.createdAt desc
		""")
	List<CardRegistered> findPaymentAvailableCards(@Param("userId") Long userId, @Param("status") CardStatus status);
}
