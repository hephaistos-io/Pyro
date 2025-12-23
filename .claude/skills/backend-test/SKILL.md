---
name: backend-test
description: Run backend unit and architecture tests to verify functionality. Use when testing backend Java code, checking architecture compliance, running unit tests, or verifying Spring Boot services.
allowed-tools: Bash
---

# Backend Tests

This Skill runs backend tests to verify functionality and architecture compliance.

## Instructions

1. From the project root, run these commands:
   - `./gradlew backend:build` - verify everything builds
   - `./gradlew backend:architectureTest` - Verify architecture rules
   - `./gradlew backend:test` - Run all backend unit tests

## When to use

Use this Skill when:

- Making backend changes
- Verifying Java code functionality
- Checking architecture compliance
- Running Spring Boot service tests
