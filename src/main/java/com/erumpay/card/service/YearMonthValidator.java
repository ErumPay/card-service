package com.erumpay.card.service;

import com.erumpay.card.exception.InvalidYearMonthException;
import org.springframework.stereotype.Component;

@Component
public class YearMonthValidator {

	public void validate(String yearMonth) {
		if (yearMonth == null || !yearMonth.matches("\\d{6}")) {
			throw new InvalidYearMonthException();
		}

		int month = Integer.parseInt(yearMonth.substring(4, 6));
		if (month < 1 || month > 12) {
			throw new InvalidYearMonthException();
		}
	}
}
