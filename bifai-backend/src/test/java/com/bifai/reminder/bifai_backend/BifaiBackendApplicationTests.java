package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * BifaiBackendApplication 기본 테스트
 * 애플리케이션 컨텍스트가 제대로 로드되는지 확인
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"app.jwt.secret=testSecretKeyForJWTTokenGenerationAndValidation1234567890",
	"app.jwt.access-token-expiration-ms=900000",
	"app.jwt.refresh-token-expiration-ms=604800000"
})
class BifaiBackendApplicationTests {

	@Test
	void contextLoads() {
		// 컨텍스트 로드 테스트
	}

}
