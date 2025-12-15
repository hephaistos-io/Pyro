# Backend - Coding Guidelines

> For setup, running, testing, and Docker commands, see `doc/src/subchapters/setup.adoc`

## Philosophy

Prefer **readability and simplicity over cleverness**. Always ask before making big architectural changes.

**Important:** Run all Gradle commands from the project root (`./gradlew`), not from the backend directory.

---

## Package Structure

```
io.hephaistos.flagforge/
├── FlagForgeApplication.java      # Spring Boot entry point
├── configuration/                 # Spring configuration classes
├── controller/                    # REST controllers
│   └── dto/                       # Request/Response records
├── data/                          # Entities & repositories
├── exception/                     # Custom exceptions
├── security/                      # Security context utilities
└── service/                       # Business logic (interface + impl)
```

---

## Service Pattern

All services follow interface + implementation for testability:

```java
public interface MyService {
    void doSomething(UUID id);
}

@Service
@Transactional
public class DefaultMyService implements MyService {
    private final MyRepository repository;

    public DefaultMyService(MyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void doSomething(UUID id) { ... }
}
```

---

## Security Context

Use `FlagForgeSecurityContext` to get authenticated user info:

```java
@Autowired
private FlagForgeSecurityContext securityContext;

UUID userId = securityContext.getUserId();
UUID companyId = securityContext.getCompanyId();
```

### Multi-tenancy

Repositories extending filtered base classes automatically filter by company. Use `findByIdFiltered()` instead of
`findById()` to ensure filters are applied.

---

## Database Migrations

Location: `webapp-api/src/main/resources/db/migration/`

Naming: `V{version}__{description}.sql`

```
V1.0.1__create_user_table.sql
V1.0.2__create_company_table.sql
```

**Rules:**

- Never modify an applied migration - create a new version
- Each migration should be atomic (one logical change)
- Use `gen_random_uuid()` for UUIDs (PostgreSQL 13+)
- Avoid reserved keywords as table names (`user`, `order`, etc.)

---

## DTOs

Use Java records for request/response objects:

```java
public record MyRequest(String name, @NotBlank String email) {}
public record MyResponse(UUID id, String name) {
    public static MyResponse fromEntity(MyEntity e) {
        return new MyResponse(e.getId(), e.getName());
    }
}
```

---

## Custom Exceptions

- `NotFoundException` - Resource not found (404)
- `EmailAlreadyExistsException` - Duplicate email (409)
- `InvalidCredentialsException` - Auth failure (401)
- `ApplicationAlreadyExistsException` - Duplicate app name

All handled by `GlobalExceptionHandler`.

---

## Do's and Don'ts

**Do:**

- Define dependency versions in root `build.gradle.kts`
- Create new migration files for schema changes
- Use interface + implementation pattern for services
- Use `@Transactional` on service classes

**Don't:**

- Modify applied Flyway migrations
- Use Hibernate auto-DDL (Flyway manages schema)
- Use `findById()` on filtered repositories (use `findByIdFiltered()`)
