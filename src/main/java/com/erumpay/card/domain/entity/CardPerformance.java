package com.erumpay.card.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "card_performance",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_card_performance_user_card_month",
			columnNames = {"user_id", "card_id", "`year_month`"}
		)
	},
	indexes = {
		@Index(name = "idx_card_performance_card", columnList = "card_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardPerformance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "perf_id")
	private Long perfId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "card_id", nullable = false)
	private Long cardId;

	@Column(name = "`year_month`", nullable = false, length = 6, columnDefinition = "CHAR(6)")
	private String yearMonth;

	@Column(name = "amount", nullable = false)
	private Long amount;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	public static CardPerformance create(Long userId, Long cardId, String yearMonth, Long amount) {
		CardPerformance performance = new CardPerformance();
		performance.userId = userId;
		performance.cardId = cardId;
		performance.yearMonth = yearMonth;
		performance.amount = amount;
		return performance;
	}

	public void increase(Long amount) {
		this.amount += amount;
	}

	public void overwrite(Long amount) {
		this.amount = amount;
	}

	public boolean decreaseWithFloor(Long amount) {
		boolean clamped = this.amount < amount;
		this.amount = clamped ? 0L : this.amount - amount;
		return clamped;
	}
}
