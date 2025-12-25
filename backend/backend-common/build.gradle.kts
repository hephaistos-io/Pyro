plugins {
    java
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    }
    dependencies {
        dependency("io.swagger.core.v3:swagger-annotations:2.2.22")
    }
}

dependencies {
    // JPA & Hibernate (annotations only - no Spring Boot app)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Jackson for JSON types (version managed by Spring Boot BOM)
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    // Swagger for enum documentation
    compileOnly("io.swagger.core.v3:swagger-annotations")

    // For TraceIdFilter (optional - only used when services include this)
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework:spring-web")

    // Validation (version managed by Spring Boot BOM)
    implementation("jakarta.validation:jakarta.validation-api")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Important: NOT a Spring Boot application - just a library
