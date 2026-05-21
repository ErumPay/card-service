package com.erumpay.card.domain.entity;

import com.erumpay.card.domain.enums.CardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "card_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "card_product_id")
	private Long cardProductId;

	@Column(name = "mock_bin", nullable = false, unique = true, length = 6)
	private String mockBin;

	@Column(name = "card_company", nullable = false, length = 50)
	private String cardCompany;

	@Column(name = "card_name", nullable = false, length = 100)
	private String cardName;

	@Enumerated(EnumType.STRING)
	@Column(name = "card_type", nullable = false)
	private CardType cardType;

	@Column(name = "annual_fee")
	private Long annualFee;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "source_card_id", nullable = false, length = 50)
	private String sourceCardId;

	@Column(name = "source_url", length = 500)
	private String sourceUrl;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;
}
