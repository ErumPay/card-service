package com.erumpay.card.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_benefit_brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardBenefitBrand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "brand_id")
	private Long brandId;

	@Column(name = "benefit_id", nullable = false)
	private Long benefitId;

	@Column(name = "brand_name", nullable = false, length = 100)
	private String brandName;
}
