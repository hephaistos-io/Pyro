plugins {
    java apply true
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.springframework.boot") version "4.0.0" apply false
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0" apply false
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    group = "io.hephaistos.flagforge"

    repositories {
        mavenCentral()
    }

    dependencies {
        constraints {
            implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.0.0")
            implementation("org.springframework.boot:spring-boot-starter-security:4.0.0")
            implementation("org.springframework.boot:spring-boot-starter-webmvc:4.0.0")
            implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")

            implementation("io.jsonwebtoken:jjwt-api:0.13.0")
            implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
            implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

            // Rate limiting for customer-api
            implementation("com.bucket4j:bucket4j-core:8.10.1")
            implementation("com.bucket4j:bucket4j-redis:8.10.1")
            implementation("io.lettuce:lettuce-core:6.5.2.RELEASE")

            implementation("org.springframework.boot:spring-boot-starter-flyway:4.0.0")
            implementation("org.flywaydb:flyway-database-postgresql:10.20.1")
            runtimeOnly("org.postgresql:postgresql:42.7.4")

            testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.0")
            testImplementation("org.springframework.boot:spring-boot-testcontainers:4.0.0")
            testImplementation("org.testcontainers:testcontainers:2.0.2")
            testImplementation("org.testcontainers:testcontainers-postgresql:2.0.2")
            testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.2")
            testImplementation("com.redis:testcontainers-redis:2.2.2")
            testImplementation("org.springframework.boot:spring-boot-resttestclient:4.0.0")
            testImplementation("org.springframework.security:spring-security-test:7.0.0")
            testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.named<Test>("test") {
        useJUnitPlatform {
            excludeTags("architecture")
        }
    }
}
