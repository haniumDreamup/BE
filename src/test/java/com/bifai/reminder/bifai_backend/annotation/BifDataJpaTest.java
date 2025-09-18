package com.bifai.reminder.bifai_backend.annotation;

import com.bifai.reminder.bifai_backend.config.JpaConfig;
import com.bifai.reminder.bifai_backend.config.RepositoryTestConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.*;

/**
 * BIF DataJpaTest Annotation
 * DataJpaTest를 위한 커스텀 애노테이션
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest(includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
    type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
    classes = {
        com.bifai.reminder.bifai_backend.entity.User.class,
        com.bifai.reminder.bifai_backend.entity.Role.class,
        com.bifai.reminder.bifai_backend.entity.Device.class,
        com.bifai.reminder.bifai_backend.entity.LocationHistory.class,
        com.bifai.reminder.bifai_backend.entity.ActivityLog.class,
        com.bifai.reminder.bifai_backend.entity.Medication.class,
        com.bifai.reminder.bifai_backend.entity.HealthMetric.class,
        com.bifai.reminder.bifai_backend.entity.UserPreference.class,
        com.bifai.reminder.bifai_backend.entity.BaseEntity.class,
        com.bifai.reminder.bifai_backend.entity.BaseTimeEntity.class
    }))
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.data.jpa.repositories.enabled=true",
    "spring.jpa.defer-datasource-initialization=false",
    "spring.flyway.enabled=false",
    "fcm.enabled=false",
    "logging.level.root=WARN",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl"
})
@Import({RepositoryTestConfig.class})
public @interface BifDataJpaTest {
}