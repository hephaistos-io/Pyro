---
name: api-generation
description: Generate OpenAPI specification and TypeScript client code from backend API. Used when the backend API changes.
allowed-tools: Bash
---

# API Generation

This Skill provides commands to generate OpenAPI specs and TypeScript client code from your backend API.
You NEVER have to compose up/down with this, the task contains it all!
If there is an issue with docker, it might not be building correctly!

## Instructions

1. From the project root, run these commands in order:
   - `./gradlew cleanGenerated` - Clean previous generated files
   - `./gradlew generateOpenApiDocs` - Generate OpenAPI specification
   - `./gradlew generateTypeScriptClient` - Generate TypeScript client from spec

## When to use

Use this Skill when:

- Backend API endpoints have changed
- You need to sync frontend types with backend changes
- API contracts need to be updated
