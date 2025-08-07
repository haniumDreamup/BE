# Spring Boot 3.x JPA Entity Scan 문제 최종 분석 및 해결

## 문제 상황
"Not a managed type: class com.bifai.reminder.bifai_backend.entity.User" 오류가 지속적으로 발생

## 시도한 해결 방법들
1. ✅ @EntityScan 제거 (Spring Boot 자동 구성 활용) - 실패
2. ✅ @EntityScan을 메인 클래스에 추가 - 실패
3. ✅ JPA 설정 클래스 분리 - 실패
4. ✅ EntityListener 제거 - 실패
5. ✅ 빌드 캐시 정리 - 실패

## 근본 원인 분석

### 가능한 원인들
1. **클래스 로딩 문제**: DevTools가 엔티티 클래스를 다른 ClassLoader로 로드
2. **순환 참조**: User 엔티티의 복잡한 관계 매핑
3. **Spring Boot 3.x 버전 호환성**: 3.5.3 버전의 특정 이슈
4. **멀티 모듈 문제**: 프로젝트 구조상의 문제

## 최종 해결 방안

### 방안 1: DevTools 비활성화
```yaml
spring:
  devtools:
    restart:
      enabled: false
```

### 방안 2: 명시적 LocalContainerEntityManagerFactoryBean 설정
```java
@Configuration
public class JpaConfig {
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, 
            JpaVendorAdapter jpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bifai.reminder.bifai_backend.entity");
        em.setJpaVendorAdapter(jpaVendorAdapter);
        return em;
    }
    
    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(false);
        adapter.setGenerateDdl(false);
        adapter.setDatabase(Database.H2);
        return adapter;
    }
}
```

### 방안 3: Spring Boot 버전 다운그레이드
- 3.5.3 → 3.2.x LTS 버전으로 변경

### 방안 4: 프로젝트 구조 단순화
- bifai-backend 하위 디렉토리 구조를 프로젝트 루트로 이동

## 권장 조치

1. **즉시 조치**: DevTools 비활성화로 문제 해결 시도
2. **중기 조치**: 명시적 EntityManagerFactory 설정
3. **장기 조치**: 프로젝트 구조 개선

## 임시 해결책

테스트와 개발을 계속하기 위해:
1. JPA 관련 테스트는 일단 스킵
2. 핵심 비즈니스 로직 개발에 집중
3. 별도의 최소 프로젝트에서 JPA 설정 테스트

## 추가 디버깅 방법

```java
@Component
public class EntityScanDebugger {
    
    @Autowired
    private EntityManager entityManager;
    
    @PostConstruct
    public void debugEntities() {
        Metamodel metamodel = entityManager.getMetamodel();
        Set<EntityType<?>> entities = metamodel.getEntities();
        
        System.out.println("=== Managed Entities ===");
        entities.forEach(entity -> {
            System.out.println("Entity: " + entity.getName() + " - " + entity.getJavaType());
        });
    }
}
```