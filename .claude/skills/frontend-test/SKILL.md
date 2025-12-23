---
name: frontend-test
description: Run frontend e2e system tests with Playwright to verify web application functionality. Use when testing Angular UI, running end-to-end tests, testing user workflows, or verifying cross-browser compatibility.
allowed-tools: Bash
---

# Frontend Tests

This Skill runs end-to-end system tests using Playwright to verify the Angular web application.

## Instructions

1. Build the frontend:
   - From the `/webapp` folder in the project root, run `npm build`
   - Fix any build issues

2. Run tests from project root:
   - `./gradlew systemTestsChromium` - Run tests in Chromium
   - `./gradlew systemTestsCrossBrowser` - Run cross-browser tests
   - Fix any failing tests

3. NEVER run `./gradlew dockerUp` or `./gradlew dockerDown`. This is already built into the specific tasks!

## Guidelines

You never have to compose up by yourself! Compose up/down is always done automatically via the gradle tasks!
Whenever you need to run the tests, focus first on running the chromium ones, to save time. Only once these pass,
should you run the cross browser ones.

## When to use

Use this Skill when:

- Testing Angular component changes
- Verifying user workflows end-to-end
- Checking browser compatibility
- Running Playwright tests
- If prompted to run all tests at once, run the following command in the project root:
   - `./gradlew systemTests`

## Possible issues

Sometimes the flyway migration can fail due to local changes. To fix this, reset the db.
In the project root, run:

- `./gradlew dockerResetDb` - deletes the postgres docker volume
