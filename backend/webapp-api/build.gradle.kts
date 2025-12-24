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
    implementation("io.jsonwebtoken:jjwt-api")
    implementation("io.jsonwebtoken:jjwt-impl")
    implementation("io.jsonwebtoken:jjwt-jackson")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.lettuce:lettuce-core")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<Test>("architectureTest") {
    description = "Run architecture tests only (fast, no containers needed)"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("architecture")
    }
}

openApi {
    apiDocsUrl.set("http://localhost:8080/api/v3/api-docs")
    outputDir.set(file("$projectDir/../../contracts"))
    outputFileName.set("webapp_api.json")
    waitTimeInSeconds.set(60)
    customBootRun {
        // Use testcontainers for database when generating OpenAPI docs
        args.set(listOf("--spring.profiles.active=openapi"))
    }
}
