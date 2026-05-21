package com.erumpay.card.domain.entity;

import com.erumpay.card.domain.enums.CardStatus;
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
@Table(name = "card_registered")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardRegistered {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "card_id")
	private Long cardId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "card_product_id", nullable = false)
	private Long cardProductId;

	@Column(name = "encrypted_billing_key", nullable = false, length = 255)
	private String encryptedBillingKey;

	@Column(name = "masked_number", nullable = false, length = 20)
	private String maskedNumber;

	@Column(name = "card_alias", length = 50)
	private String cardAlias;

	@Column(name = "expiry_ym", nullable = false, length = 6, columnDefinition = "CHAR(6)")
	private String expiryYm;

	@Column(name = "is_default", nullable = false)
	private boolean defaultCard;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CardStatus status;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public void updateAlias(String cardAlias) {
		this.cardAlias = cardAlias;
	}

	public void markDefault() {
		this.defaultCard = true;
	}

	public void unsetDefault() {
		this.defaultCard = false;
	}

	public void delete(LocalDateTime deletedAt) {
		this.status = CardStatus.DELETED;
		this.deletedAt = deletedAt;
		this.defaultCard = false;
	}

	public boolean isDeleted() {
		return status == CardStatus.DELETED;
	}

	public boolean isActive() {
		return status == CardStatus.ACTIVE;
	}

	public boolean hasBillingKey() {
		return encryptedBillingKey != null && !encryptedBillingKey.isBlank();
	}
}
