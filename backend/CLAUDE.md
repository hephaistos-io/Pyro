# FlagForge Backend - Project Guidelines

## Project Overview

**FlagForge** is a feature flag management platform designed for engineers. This folder contains
the backend Spring Boot API that serves the frontend application.

**Tech Stack:**

- Java 21
- Spring Boot 4.0.0
- Gradle Kotlin DSL for build management
- PostgreSQL (requires running instance for bootRun)
- Flyway 10.20.1 for database migrations
- Testcontainers for integration tests (requires Docker)

**Philosophy:** When working on this project, we prefer **readability and simplicity over cleverness**. Always ask for
input before proceeding with big, impactful changes.

---

## Project Structure

**Root Directory:** `/Users/risi/Documents/Pyro/`

- Gradle wrapper (`./gradlew`) is at the root level
- Backend services are in `backend/` subdirectory
- You MUST execute gradlew commands in the root of the project, as the wrapper is located there.

**Running the application:**

```bash
# From the project root (requires PostgreSQL running on localhost:5432)
./gradlew backend:webapp-api:bootRun
```

**Important:**

- Always run gradle commands from the project root, not from the backend directory.
- `bootRun` requires a PostgreSQL instance running on `localhost:5432` with database `flagforge` and credentials
  `flagforge/flagforge`.

**Running tests:**

```bash
# From the project root (requires Docker for Testcontainers)
./gradlew backend:webapp-api:test
```

Tests use Testcontainers to spin up a PostgreSQL container automatically.

---

## Database & Migrations

### Current Setup

- **Database:** PostgreSQL (`jdbc:postgresql://localhost:5432/flagforge`)
- **Migration Tool:** Flyway
- **Schema Management:** Flyway manages all schema changes (Hibernate is set to `validate` mode)
- **Tests:** Use Testcontainers with PostgreSQL (requires Docker)

### Working with Migrations

**Migration files location:** `webapp-api/src/main/resources/db/migration/`

**Naming convention:**

```
V{version}__{description}.sql

Examples:
- V1.0.1__create_user_table.sql
- V1.0.2__create_company_table.sql
- V1.0.3__create_application_table.sql
```

**Rules for migrations:**

- Version numbers must be sequential (V1, V2, V3...)
- Use double underscore `__` after version number
- Use underscores `_` for spaces in description
- **Never modify an applied migration** - always create a new version
- Each migration should be atomic (one logical change)

**Useful Flyway commands:**

```bash
# Check migration status
./gradlew backend:webapp-api:flywayInfo

# Validate migrations
./gradlew backend:webapp-api:flywayValidate

# Manually trigger migration (usually auto-runs on startup)
./gradlew backend:webapp-api:flywayMigrate
```

### PostgreSQL-Specific Notes

- **UUID generation:** Use `gen_random_uuid()` (built-in PostgreSQL 13+)
- **Reserved keywords:** Avoid PostgreSQL reserved keywords (e.g., `user`, `order`) as table names
- **Default credentials:** username `flagforge`, password `flagforge`, database `flagforge`

---

## API Documentation

### OpenAPI Spec Generation

The project uses springdoc-openapi to automatically generate OpenAPI documentation.

**Generate and save OpenAPI spec:**

```bash
./gradlew backend:webapp-api:generateOpenApiDocs
```

This command will:

1. Start the Spring Boot application
2. Wait for initialization (up to 30 seconds)
3. Fetch the OpenAPI spec from `http://localhost:8080/api/v3/api-docs`
4. Save to the `contracts` folder in the root of the project
5. Shut down the application

**Output location:** `contracts/webapp_api.json` (at project root level)

**Swagger UI (when app is running):** `http://localhost:8080/api/swagger-ui.html`

---

## Best Practices & Gotchas

### Do's

- Always ask before making big architectural changes
- Keep components focused and simple - split large components
- Define interfaces close to where they're used
- Define dependencies, and their versions, at the root level build.gradle.kts file
- Add dependencies to the project without version at the project level build.gradle.kts file
- Create a new migration file for any schema change
- Test migrations locally before committing

### Don'ts

- Don't create "clever" solutions - prefer readable, simple code
- Don't modify applied Flyway migrations - create new versions instead
- Don't use Hibernate auto-DDL features - Flyway manages the schema

---

## Application Architecture

### Package Structure

```
io.hephaistos.flagforge/
├── FlagForgeApplication.java      # Spring Boot entry point
├── configuration/                 # Spring configuration classes
├── controller/                    # REST controllers & DTOs
├── data/                          # Entities & repositories
├── exception/                     # Custom exceptions
├── security/                      # Security context utilities
└── service/                       # Business logic (interface + impl)
```

### Controllers

| Controller                | Base Path          | Endpoints                              |
|---------------------------|--------------------|----------------------------------------|
| `AuthorizationController` | `/v1/auth`         | `POST /register`, `POST /authenticate` |
| `UserController`          | `/v1/user`         | `GET /profile`                         |
| `CompanyController`       | `/v1/company`      | `GET /`, `POST /`, `GET /{id}`         |
| `ApplicationController`   | `/v1/applications` | `GET /`, `POST /`                      |
| `GlobalExceptionHandler`  | -                  | Centralized error handling             |

### Services

All services follow the interface + implementation pattern for testability:

- `AuthenticationService` / `DefaultAuthenticationService` - User registration & login
- `UserService` / `DefaultUserService` - User profile management
- `CompanyService` / `DefaultCompanyService` - Company CRUD operations
- `ApplicationService` / `DefaultApplicationService` - Application management
- `JwtService` / `DefaultJwtService` - JWT token generation & validation
- `BreachedPasswordService` - Integrates with Have I Been Pwned API for password validation

### Entities

| Entity              | Table         | Key Fields                                                          | Relationships                          |
|---------------------|---------------|---------------------------------------------------------------------|----------------------------------------|
| `CustomerEntity`    | `customer`    | id (UUID), email (unique), firstName, lastName, password, companyId | ManyToOne -> Company                   |
| `CompanyEntity`     | `company`     | id (UUID), name                                                     | OneToMany -> Customers                 |
| `ApplicationEntity` | `application` | id (UUID), name, companyId                                          | Unique constraint on (name, companyId) |

### DTOs (Request/Response Records)

- `UserRegistrationRequest`, `UserAuthenticationRequest`, `UserResponse`
- `AuthenticationResponse` (contains JWT token)
- `CompanyCreationRequest`, `CompanyResponse`
- `ApplicationCreationRequest`, `ApplicationResponse`

---

## Security Configuration

### JWT Authentication

- **Stateless sessions** - No server-side session storage
- **Token expiration:** 7200 seconds (2 hours)
- **Secret override:** Set `FLAGFORGE_JWT_SECRET` environment variable

### Endpoint Security

**Whitelisted (no auth required):**

- `POST /v1/auth/register`
- `POST /v1/auth/authenticate`
- `GET /v3/api-docs` (OpenAPI spec)

**All other endpoints require valid JWT Bearer token.**

### Security Context

Use `FlagForgeSecurityContext` to extract authenticated user info:

```java

@Autowired
private FlagForgeSecurityContext securityContext;

// In controller/service
UUID userId = securityContext.getUserId();
UUID companyId = securityContext.getCompanyId();
```

### Custom Exceptions

- `EmailAlreadyExistsException` - Registration with existing email
- `InvalidCredentialsException` - Login failure
- `UserNotFoundException` - User lookup failure
- `CompanyNotFoundException` - Company lookup failure
- `ApplicationAlreadyExistsException` - Duplicate app name per company
