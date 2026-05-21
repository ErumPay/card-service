package com.erumpay.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CardBinValidateRequest {

	@ToString.Exclude
	@NotBlank(message = "카드번호는 필수입니다.")
	@Pattern(regexp = "\\d{16}", message = "카드번호 형식 오류")
	private String cardNumber;

	public String mockBin() {
		return cardNumber.substring(0, 6);
	}
}
