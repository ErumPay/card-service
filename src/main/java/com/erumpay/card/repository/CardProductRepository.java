package com.erumpay.card.repository;

import com.erumpay.card.domain.entity.CardProduct;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardProductRepository extends JpaRepository<CardProduct, Long> {

	Optional<CardProduct> findByMockBin(String mockBin);
}
