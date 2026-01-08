# Pyro (FlagForge) - Project Guide

> Feature flag management platform with Angular frontend and Spring Boot backend.

## Project Structure

```
pyro/
├── backend/                 # Spring Boot services
│   ├── webapp-api/          # Main API (port 8080) - auth, companies, apps, flags
│   ├── customer-api/        # SDK API (port 8081) - read-only access
│   └── CLAUDE.md            # Backend coding guidelines
├── webapp/                  # Angular 21 frontend (port 4200)
│   └── CLAUDE.md            # Frontend coding guidelines
├── system-tests/            # Playwright E2E tests
├── contracts/               # OpenAPI specs (generated)
├── deployment/local/        # Docker & nginx config
├── doc/                     # ARC42 documentation (asciidoc)
└── build.gradle.kts         # Root build file with Docker tasks
```

## Component Documentation

| Component | Guidelines                                                       | Description                            |
|-----------|------------------------------------------------------------------|----------------------------------------|
| Backend   | [backend/CLAUDE.md](backend/CLAUDE.md)                           | Service patterns, security, migrations |
| Webapp    | [webapp/CLAUDE.md](webapp/CLAUDE.md)                             | Angular patterns, SCSS, components     |
| Setup     | [doc/src/subchapters/setup.adoc](doc/src/subchapters/setup.adoc) | Full setup, Docker, testing guide      |

## Quick Commands

```bash
# Start everything (recommended)
./gradlew dockerUp

# Run all tests
./gradlew allTests

# Generate TypeScript client after backend changes
./gradlew webapp:generateTypeScriptClient

# Run E2E tests
./gradlew systemTests
```

## Key Patterns

### Backend (Java 21, Spring Boot)

- Interface + implementation pattern for services
- `@Transactional` on service classes
- Use `findByIdFiltered()` for multi-tenant queries
- Flyway migrations in `webapp-api/src/main/resources/db/migration/`

### Frontend (Angular 21)

- Standalone components with `inject()` for DI
- Signals for state (`signal()`, `computed()`)
- Modern template syntax (`@if`, `@for` with `track`)
- SCSS with `@use '../../../styles/index' as *`

### System Tests (Playwright)

- Located in `system-tests/tests/`
- Run with `./gradlew systemTests` or `cd system-tests && npm test`
- Use strict Playwright locators (`getByRole`, `getByLabel`)

## Testing Philosophy

**Failing tests must be fixed, not blamed.** If a test fails, fix it regardless of what caused the issue. Do not waste time investigating whether it's "your fault" or a pre-existing problem—just fix it and move on.

## Architecture Documentation

Full ARC42 documentation in `doc/`:

- [Introduction & Goals](doc/src/01_introduction_and_goals.adoc)
- [Architecture Decisions](doc/src/09_architecture_decisions.adoc)
- [Building Block View](doc/src/05_building_block_view.adoc)

## Important Notes

- Run Gradle commands from project root (`./gradlew`), not from subdirectories
- Backend uses PostgreSQL with two users: `webapp-flagforge` (full access), `customer-flagforge` (read-only)
- Frontend API client is auto-generated from OpenAPI specs in `contracts/`
- Design inspiration: www.enode.com
