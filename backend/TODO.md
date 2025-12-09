# Pyro Backend - TODO List

**Last Updated:** 2025-12-07

This document contains prioritized action items based on a comprehensive codebase analysis covering architecture,
dependencies, code quality, and security.

---

## 🔴 HIGH PRIORITY

### 7. Add Comprehensive Test Coverage

**Current State:**

- Only 1 test file: `UserServiceTest.java` (61 lines)
- Test coverage: ~10-15%
- No controller tests
- No security/authentication tests
- No JWT service tests
- No integration tests for authentication flow

**Missing Tests:**

1. **Controller Tests:**
    - `AuthorizationControllerTest` - registration and authentication endpoints
    - `UserControllerTest` - profile endpoint

2. **Security Tests:**
    - `JwtOncePerRequestFilterTest` - token validation logic
    - `ApiSecurityConfigurationTest` - security configuration

3. **Service Tests:**
    - `JwtServiceTest` - token generation and validation
    - `AuthenticationServiceTest` - registration and login flows

4. **Integration Tests:**
    - End-to-end authentication flow
    - Token refresh scenarios

**Suggested Approach:**
Start with critical path tests:

1. AuthorizationController tests (register/authenticate)
2. JwtService tests (token generation/validation)
3. Security filter tests

**Effort:** 2-3 days
**Priority:** HIGH

---

## 🟡 MEDIUM PRIORITY

### 9. Upgrade Flyway

**Current:** `10.20.1`
**Latest:** `11.18.0` (Released Nov 27, 2025)

**File:** `backend/build.gradle.kts:34`

**Impact:** Not critical, but missing newer features and bug fixes. Version 11.x adds improvements to migration
handling.

**Suggested Fix:**

```kotlin
implementation("org.flywaydb:flyway-core:11.18.0")
```

Test migrations after upgrade to ensure compatibility.

**Effort:** 30 minutes (including testing)
**Priority:** MEDIUM

---

### 10. Remove Hard-coded JWT Secret Default

**File:** `webapp-api/src/main/resources/application.yml:4`

**Issue:**

```yaml
secret: ${PYRO_JWT_SECRET:4407a837731d92425e627e49632afbabd538a25e080fafed65a9ff7e71a9f5d1}
```

**Problems:**

- Default secret is hard-coded in version control
- If environment variable not set, uses this weak default
- Security risk for production deployments

**Suggested Fix:**

Option 1: Fail fast if not provided

```yaml
secret: ${PYRO_JWT_SECRET}  # No default - requires env var
```

Option 2: Add validation in JwtConfiguration:

```java

@PostConstruct
public void validate() {
    if (secret.equals("4407a837...")) {
        throw new IllegalStateException("JWT secret must be set via PYRO_JWT_SECRET environment variable");
    }
}
```

**Effort:** 15 minutes
**Priority:** MEDIUM

---

### 11. Add Rate Limiting on Authentication Endpoints

**Issue:** No protection against:

- Brute force password attacks
- Account enumeration via registration endpoint
- JWT token generation abuse
- DoS attacks on authentication endpoints

**Impact:** System vulnerable to automated attacks.

**Suggested Fix:**

Add Bucket4j dependency:

```kotlin
implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:8.10.1")
```

Create rate limiting filter:

```java

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String ip = request.getRemoteAddr();
        Bucket bucket = cache.computeIfAbsent(ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        }
        else {
            response.setStatus(429);  // Too Many Requests
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
        }
    }

    private Bucket createBucket() {
        return Bucket.builder().addLimit(Bandwidth.simple(10, Duration.ofMinutes(1))).build();
    }
}
```

**Effort:** 2-3 hours
**Priority:** MEDIUM

---

### 12. Remove Redundant StorageConfiguration

**File:** `webapp-api/src/main/java/io/hephaistos/pyro/configuration/StorageConfiguration.java`

**Issue:**

- Manually configures `dataSource`, `entityManagerFactory`, and `transactionManager` beans
- Spring Boot auto-configuration already handles this based on `application.yml`
- Line 38: `vendorAdapter.setGenerateDdl(true)` conflicts with Flyway approach
- Unnecessary code duplication

**Suggested Fix:**
Delete the entire `StorageConfiguration.java` file. Spring Boot will auto-configure based on:

```yaml
spring:
  datasource:
    url: jdbc:hsqldb:mem:pyro
    driver-class-name: org.hsqldb.jdbc.JDBCDriver
```

**Effort:** 5 minutes
**Priority:** MEDIUM

---

### 13. Add Spring Boot Actuator

**Missing Dependency:** `spring-boot-starter-actuator`

**Why It's Important:**

- Provides health checks, metrics, and monitoring endpoints
- Essential for production deployments
- Integrates with monitoring tools (Prometheus, Grafana, Datadog)
- Currently no way to monitor application health

**Suggested Fix:**

In `backend/build.gradle.kts`:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator:4.0.0")
```

In `backend/webapp-api/build.gradle.kts`:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
```

Configure in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

**Effort:** 30 minutes
**Priority:** MEDIUM

---

### 14. Apply io.spring.dependency-management Plugin

**File:** `build.gradle.kts:3`

**Issue:**
The plugin is declared but never applied to subprojects:

```kotlin
id("io.spring.dependency-management") version "1.1.7" apply false
```

**Impact:** Dependency management may not work as expected in subprojects.

**Suggested Fix:**

In `backend/build.gradle.kts`, add to `subprojects` block:

```kotlin
subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")  // ADD THIS

    // ... rest of config
}
```

**Effort:** 5 minutes
**Priority:** MEDIUM

---

### 15. Consolidate Configuration Files

**Issue:**
Configuration is split between two files:

- `application.yml` (most configuration)
- `application.properties` (only contains `spring.application.name=pyro`)

**Impact:** Inconsistent configuration format, harder to maintain.

**Suggested Fix:**

Move `spring.application.name` to `application.yml`:

```yaml
spring:
  application:
    name: pyro
```

Delete `application.properties`.

**Effort:** 5 minutes
**Priority:** MEDIUM

---

### 16. Adjust Logging Levels

**File:** `application.yml:36-37`

**Issue:**

```yaml
logging:
  level:
    root: DEBUG
```

**Problem:**

- Root logging set to DEBUG in configuration (will apply to production if not overridden)
- Generates excessive logs in production
- Performance impact

**Suggested Fix:**

Use profile-specific logging:

`application.yml`:

```yaml
logging:
  level:
    root: INFO
    io.hephaistos.pyro: INFO
```

`application-dev.yml`:

```yaml
logging:
  level:
    root: DEBUG
    io.hephaistos.pyro: DEBUG
```

**Effort:** 15 minutes
**Priority:** MEDIUM

---

## 🟢 LOW PRIORITY

### 17. Add PostgreSQL Driver

**Context:** CLAUDE.md mentions migration to PostgreSQL for production.

**Current:** HSQLDB in-memory database (data lost on restart)

**Suggested Addition:**

In `backend/build.gradle.kts`:

```kotlin
runtimeOnly("org.postgresql:postgresql:42.7.4")
```

In `backend/webapp-api/build.gradle.kts`:

```kotlin
// Uncomment when migrating to PostgreSQL
// runtimeOnly("org.postgresql:postgresql")
```

**Effort:** 5 minutes
**Priority:** LOW (until migration needed)

---

### 18. Add TestContainers

**Why:** Essential for integration tests with real database (especially for PostgreSQL migration).

**Suggested Addition:**

In `backend/build.gradle.kts`:

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers:4.0.0")
testImplementation("org.testcontainers:postgresql:1.20.4")
```

Example test:

```java

@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }

    @Test
    void testUserPersistence() {
        // Test with real PostgreSQL database
    }
}
```

**Effort:** 1-2 hours
**Priority:** LOW (becomes HIGH when migrating to PostgreSQL)

---

### 19. Add MapStruct for DTO Mapping

**Why:** Clean, type-safe DTO/Entity mapping without boilerplate.

**Current Approach:**

- Static factory methods (e.g., `UserResponse.fromEntity()`)
- Manual field mapping
- Coupling between DTOs and entities

**Suggested Addition:**

In `backend/build.gradle.kts`:

```kotlin
implementation("org.mapstruct:mapstruct:1.7.1")
annotationProcessor("org.mapstruct:mapstruct-processor:1.7.1")
```

Example mapper:

```java

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(UserEntity entity);

    UserEntity toEntity(UserRegistrationRequest request);
}
```

**Effort:** 2-3 hours (includes refactoring)
**Priority:** LOW

---

### 20. Implement Audit Trail Logging

**Why:** Track important user actions for security and compliance.

**Suggested Approach:**

Add JPA entity listeners:

```java

@EntityListeners(AuditingEntityListener.class)
@Entity
public class UserEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```

Enable auditing in configuration:

```java

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName);
    }
}
```

**Effort:** 1-2 hours
**Priority:** LOW

---

### 21. Add CORS Configuration

**Current:** Using Spring Security defaults.

**Suggested:**

Create explicit CORS configuration:

```java

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    @Value("${pyro.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

**Effort:** 30 minutes
**Priority:** LOW

---

### 22. Add Request/Response Logging Filter

**Why:** Debug issues, monitor API usage, audit trails.

**Suggested Implementation:**

```java

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        long startTime = System.currentTimeMillis();

        LOGGER.info("Request: {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Response: {} {} - Status: {} - Duration: {}ms", request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration);
    }
}
```

**Effort:** 1 hour
**Priority:** LOW

---

### 23. Add Correlation IDs for Request Tracing

**Why:** Track requests across distributed systems and logs.

**Suggested Implementation:**

```java

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

Update logback pattern:

```xml

<pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
```

**Effort:** 1 hour
**Priority:** LOW

---

### 24. Add JavaDoc Documentation

**Current State:** No JavaDoc documentation found in the codebase.

**Suggested:**

- Add JavaDoc to public APIs
- Document complex business logic
- Document security-sensitive code

**Effort:** Ongoing
**Priority:** LOW

---

### 25. Add Metrics/Monitoring Endpoints

**Why:** Monitor application performance, track business metrics.

**Suggested Approach:**

Use Micrometer (already in dependencies) with custom metrics:

```java

@Service
public class MetricsService {

    private final Counter registrationCounter;
    private final Counter loginCounter;
    private final Timer authenticationTimer;

    public MetricsService(MeterRegistry registry) {
        this.registrationCounter =
                Counter.builder("pyro.user.registrations").description("Total user registrations").register(registry);

        this.loginCounter = Counter.builder("pyro.user.logins").description("Total user logins").register(registry);

        this.authenticationTimer = Timer.builder("pyro.authentication.duration")
                .description("Authentication processing time")
                .register(registry);
    }

    public void recordRegistration() {
        registrationCounter.increment();
    }

    public void recordLogin() {
        loginCounter.increment();
    }
}
```

**Effort:** 2-3 hours
**Priority:** LOW

---

### 26. Add Caffeine Cache for Performance

**Why:** Cache frequently accessed data (e.g., JWT validation, user lookups).

**Suggested Addition:**

In `backend/build.gradle.kts`:

```kotlin
implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
implementation("org.springframework.boot:spring-boot-starter-cache:4.0.0")
```

Enable caching:

```java

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        return new CaffeineCacheManager("users", "jwtTokens");
    }
}
```

Use in service:

```java

@Service
public class DefaultUserService {

    @Cacheable(value = "users", key = "#email")
    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
```

**Effort:** 1-2 hours
**Priority:** LOW

---

## 📊 Analysis Summary

### Codebase Statistics

- **Total Java Files:** 22 source files
- **Lines of Code:** ~756 lines (main source)
- **Test Coverage:** ~10-15% (1 test file, 61 lines)
- **Spring Boot Version:** 4.0.0 (latest, released Nov 2025)
- **Java Version:** 21 (current LTS)
- **Build Tool:** Gradle 9.2.1 (latest)

### Architecture Assessment

**Strengths:**

- ✅ Clean 3-tier layered architecture (Controller → Service → Repository)
- ✅ Interface-driven design with proper dependency injection
- ✅ Modern Java features (records, Optional, streams)
- ✅ Good package structure and naming conventions
- ✅ Proper use of Spring Security with JWT
- ✅ Flyway for database migrations

**Weaknesses:**

- ❌ Minimal test coverage
- ❌ Missing input validation
- ❌ No rate limiting or brute force protection

### Dependency Assessment

**Current Status:** ✅ Generally excellent

- All major dependencies are current (Spring Boot 4.0.0, Java 21, HSQLDB 2.7.4)
- No known security vulnerabilities in dependencies
- JJWT 0.13.0 is latest and secure
- Flyway slightly outdated but not critical (10.20.1 → 11.18.0)

**Missing Dependencies:**

- Spring Boot Actuator (production monitoring)
- PostgreSQL driver (for future migration)
- TestContainers (for integration tests)
- MapStruct (optional, for DTO mapping)

### Security Assessment

**Critical Issues:** 0

**High Priority Issues:** 2

- No password strength requirements
- Insufficient test coverage

**Overall Security Grade:** ⚠️ A- (critical issues resolved, race condition handled, test dependencies configured
correctly, needs password validation)

### Code Quality Assessment

**Positive:**

- Code is clean, readable, and follows conventions
- Small, focused classes (SRP followed)
- Good use of Spring Boot patterns
- Proper separation of concerns

**Areas for Improvement:**

- Test coverage is critically low
- Some configuration redundancy
- Missing validation in multiple places
- Security issues need immediate fixes

**Overall Code Quality Grade:** 📊 B- (good foundation, needs hardening)

---

## 📚 References

### Documentation

- [Spring Boot 4.0.0 Documentation](https://docs.spring.io/spring-boot/docs/4.0.0/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Flyway Documentation](https://documentation.red-gate.com/flyway/)

### Security Best Practices

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)

### Tools

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [TestContainers](https://testcontainers.com/)
- [MapStruct](https://mapstruct.org/)
- [Bucket4j (Rate Limiting)](https://bucket4j.com/)

---

## 🎯 Recommended Action Plan

### Week 1: Critical Security Fixes

1. ✅ Fix password logging vulnerability - COMPLETED
2. ✅ Fix JWT token extraction - COMPLETED
3. ✅ Add global exception handler - COMPLETED

### Week 2: Validation & Testing

4. ✅ Add email validation - COMPLETED
5. ✅ Fix race condition in registration - COMPLETED
6. ✅ Fix test dependencies - COMPLETED (already configured correctly)
7. ✅ Implement password strength requirements - COMPLETED
8. Start adding controller tests (Item #7)

### Week 3: Dependencies & Configuration

9. Upgrade Flyway (Item #9)
10. Fix JWT secret handling (Item #10)
11. Add Spring Boot Actuator (Item #13)
12. Remove StorageConfiguration (Item #12)
13. Consolidate configuration (Item #15)

### Week 4+: Enhancements

14. Add rate limiting (Item #11)
15. Continue test coverage expansion
16. Plan PostgreSQL migration
17. Add monitoring and observability

---

**Total Issues Identified:** 20
**Critical:** 0 | **High:** 1 | **Medium:** 8 | **Low:** 10

**Next Steps:** All critical security issues resolved! Email validation, race condition handling, test dependencies, and
password strength validation complete. Focus on remaining high-priority item: test coverage expansion.
