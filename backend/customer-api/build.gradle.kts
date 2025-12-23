plugins {
    id("org.springframework.boot")
    id("org.springdoc.openapi-gradle-plugin")
}

dependencies {
    implementation(project(":backend:backend-common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    // Rate limiting with Redis
    implementation("com.bucket4j:bucket4j-core")
    implementation("com.bucket4j:bucket4j-redis")
    implementation("io.lettuce:lettuce-core")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("com.redis:testcontainers-redis")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Flyway for test database migrations (migrations come from webapp-api)
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testRuntimeOnly("org.flywaydb:flyway-database-postgresql")
    // Only include webapp-api when available (not in Docker build context)
    findProject(":backend:webapp-api")?.let { testRuntimeOnly(it) }
}

openApi {
    apiDocsUrl.set("http://localhost:8081/v3/api-docs")
    outputDir.set(file("$projectDir/../../contracts"))
    outputFileName.set("customer_api.json")
    waitTimeInSeconds.set(60)
    customBootRun {
        args.set(listOf("--spring.profiles.active=openapi"))
    }
}
