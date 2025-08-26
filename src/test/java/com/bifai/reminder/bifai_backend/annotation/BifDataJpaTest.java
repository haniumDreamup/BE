package com.bifai.reminder.bifai_backend.annotation;

import com.bifai.reminder.bifai_backend.config.JpaConfig;
import com.bifai.reminder.bifai_backend.config.RepositoryTestConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * BIF DataJpaTest Annotation
 * DataJpaTest를 위한 커스텀 애노테이션
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({RepositoryTestConfig.class})
public @interface BifDataJpaTest {
}