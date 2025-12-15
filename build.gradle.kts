// Root build file for FlagForge
// Contains Docker Compose convenience tasks

// Docker Compose tasks for development environment
// Note: Docker must be in the system PATH for these tasks to work
// Uses shell wrapper to inherit PATH correctly at execution time

tasks.register<Exec>("dockerUp") {
    description = "Start all containers with Docker Compose"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose up -d")
}

tasks.register<Exec>("dockerDown") {
    description = "Stop all containers"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose down")
}

tasks.register<Exec>("dockerBuild") {
    description = "Build all Docker images"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose build")
}

tasks.register<Exec>("dockerLogs") {
    description = "Show container logs (follow mode)"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose logs -f")
}

tasks.register<Exec>("dockerClean") {
    description = "Remove containers, networks, and volumes"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose down -v --rmi local")
}

tasks.register("dockerRestart") {
    description = "Restart all containers"
    group = "docker"
    dependsOn("dockerDown")
    finalizedBy("dockerUp")
}

tasks.register<Exec>("dockerPs") {
    description = "Show running containers status"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose ps")
}

tasks.register<Exec>("dockerResetDb") {
    description = "Reset the PostgreSQL database volume (stops containers and deletes volume)"
    group = "docker"
    workingDir = projectDir
    commandLine(
        "sh",
        "-c",
        "docker compose down && docker volume rm flagforge-pgdata 2>/dev/null || true"
    )
}

tasks.register<Exec>("dockerStartPostgres") {
    description = "Start only the PostgreSQL container and wait for it to be healthy"
    group = "docker"
    workingDir = projectDir
    commandLine("sh", "-c", "docker compose up -d --wait postgres")
}

// Make generateOpenApiDocs depend on PostgreSQL being available
gradle.projectsEvaluated {
    tasks.findByPath(":backend:webapp-api:generateOpenApiDocs")?.dependsOn(":dockerStartPostgres")
    tasks.findByPath(":backend:customer-api:generateOpenApiDocs")?.dependsOn(":dockerStartPostgres")
}

// System tests (E2E tests with Playwright)
tasks.register<Exec>("systemTests") {
    description = "Run Playwright E2E tests (starts Docker before, stops after)"
    group = "verification"
    workingDir = file("system-tests")
    // Wait for services to be fully ready after dockerUp, then run tests
    commandLine("sh", "-c", "sleep 5 && npm test")
    dependsOn("dockerUp")
    finalizedBy("dockerDown")
}

// Frontend tests (Vitest)
tasks.register<Exec>("frontendTests") {
    description = "Run frontend unit tests with Vitest"
    group = "verification"
    workingDir = file("webapp")
    commandLine("sh", "-c", "npm test")
}

// Run all tests in order: architecture -> JUnit -> Vitest -> Playwright
tasks.register("allTests") {
    description = "Run all tests: architecture, backend (JUnit), frontend (Vitest), and system (Playwright)"
    group = "verification"

    // Depend on all test tasks
    dependsOn(
        ":backend:webapp-api:architectureTest",
        ":backend:webapp-api:test",
        ":backend:customer-api:test",
        "frontendTests",
        "systemTests"
    )

    doLast {
        println("\n✓ All tests completed successfully!")
    }
}

// Configure test execution order after all projects are evaluated
gradle.projectsEvaluated {
    val archTest = tasks.findByPath(":backend:webapp-api:architectureTest")
    val webappApiTest = tasks.findByPath(":backend:webapp-api:test")
    val customerApiTest = tasks.findByPath(":backend:customer-api:test")
    val frontendTest = tasks.findByName("frontendTests")
    val systemTest = tasks.findByName("systemTests")

    // Enforce execution order: arch -> junit -> vitest -> playwright
    if (archTest != null) {
        webappApiTest?.mustRunAfter(archTest)
        customerApiTest?.mustRunAfter(archTest)
    }
    if (webappApiTest != null && customerApiTest != null) {
        frontendTest?.mustRunAfter(webappApiTest, customerApiTest)
    }
    if (frontendTest != null) {
        systemTest?.mustRunAfter(frontendTest)
    }
}
