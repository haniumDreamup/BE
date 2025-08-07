package com.bifai.reminder.bifai_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * BIF-AI 백엔드 애플리케이션 메인 클래스
 */
@SpringBootApplication
@EntityScan(basePackages = {"com.bifai.reminder.bifai_backend.entity"})
@EnableJpaRepositories(basePackages = {"com.bifai.reminder.bifai_backend.repository"})
public class BifaiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BifaiBackendApplication.class, args);
	}

}
