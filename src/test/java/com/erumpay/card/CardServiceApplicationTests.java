package com.erumpay.card;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 운영 스키마 정합은 Docker MySQL에서 확인하고, 이 테스트는 로컬 MySQL 없이 컨텍스트 로드만 검증한다.
@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:card_service_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"card.billing-key.aes-key=0123456789abcdef"
})
class CardServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
