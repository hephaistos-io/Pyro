# Pyro Webapp - Project Guidelines

## Project Overview

**Pyro** is a feature flag management platform designed for engineers. In this folder, we have
a backend service that deals with the API for the frontend application.

**Tech Stack:**

- Java 21
- Spring Boot 4.0.0
- Gradle Kotlin DSL for build management
- HSQLDB (in-memory, development) - *Note: Should migrate to PostgreSQL for production*
- Flyway 10.20.1 for database migrations

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
# From the project root
./gradlew backend:webapp-api:bootRun
```

**Important:** Always run gradle commands from the project root, not from the backend directory.

---

## Database & Migrations

### Current Setup

- **Database:** HSQLDB in-memory (`jdbc:hsqldb:mem:pyro`)
- **Migration Tool:** Flyway
- **Schema Management:** Flyway manages all schema changes (Hibernate is set to `validate` mode)

### Working with Migrations

**Migration files location:** `webapp-api/src/main/resources/db/migration/`

**Naming convention:**

```
V{version}__{description}.sql

Examples:
- V1.0.1__create_user_table.sql
- V1.0.2__Add_role_column.sql
- V1.1.0__Create_feature_flag_table.sql
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

### HSQLDB-Specific Notes

- **UUID generation:** Use `RANDOM_UUID()` not `gen_random_uuid()`
- **Data persistence:** Current in-memory setup loses data on restart
- **Migration to PostgreSQL:** Will require reviewing SQL syntax in all migrations

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

**Output location:** `webapp-api/src/main/resources/static/openapi.json`

**Swagger UI (when app is running):** `http://localhost:8080/api/swagger-ui.html`

---

## Best Practices & Gotchas

### Do's

- ✅ Always ask before making big architectural changes
- ✅ Keep components focused and simple - split large components
- ✅ Define interfaces close to where they're used
- ✅ Define dependencies, and their versions, at the root level build.gradle.kts file
- ✅ Add dependencies to the project without version at the project level build.gradle.kts file
- ✅ Create a new migration file for any schema change
- ✅ Test migrations locally before committing

### Don'ts

- ❌ Don't create "clever" solutions - prefer readable, simple code
- ❌ Don't modify applied Flyway migrations - create new versions instead
- ❌ Don't use Hibernate auto-DDL features - Flyway manages the schema
