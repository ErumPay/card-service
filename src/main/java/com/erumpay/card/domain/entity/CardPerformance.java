package com.erumpay.card.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_performance")
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
}
