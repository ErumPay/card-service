package com.erumpay.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardRegisterRequest {

	@NotNull(message = "회원 식별자는 필수입니다.")
	private Long userId;

	@NotBlank(message = "Mock BIN은 필수입니다.")
	@Pattern(regexp = "\\d{6}", message = "Mock BIN 형식 오류")
	private String mockBin;

	@ToString.Exclude
	@NotBlank(message = "카드번호는 필수입니다.")
	@Pattern(regexp = "\\d{16}", message = "카드번호 형식 오류")
	private String cardNumber;

	@NotBlank(message = "유효기간은 필수입니다.")
	@Pattern(regexp = "\\d{6}", message = "유효기간 형식 오류")
	private String expiryYm;

	@ToString.Exclude
	@NotBlank(message = "CVC는 필수입니다.")
	@Pattern(regexp = "\\d{3}", message = "CVC 형식 오류")
	private String cvc;

	@ToString.Exclude
	@NotBlank(message = "카드 비밀번호 앞 두 자리는 필수입니다.")
	@Pattern(regexp = "\\d{2}", message = "카드 비밀번호 형식 오류")
	private String cardPassword2;

	@Size(max = 10, message = "카드 별칭은 10자 이하여야 합니다.")
	private String cardAlias;

	private Boolean isDefault;

	public String normalizedCardAlias() {
		if (cardAlias == null) {
			return null;
		}
		String trimmedAlias = cardAlias.trim();
		return trimmedAlias.isEmpty() ? null : trimmedAlias;
	}

	public boolean defaultRequested() {
		return Boolean.TRUE.equals(isDefault);
	}
}
