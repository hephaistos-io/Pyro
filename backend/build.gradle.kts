plugins {
    java apply true
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.springframework.boot") version "4.0.0" apply false
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    group = "io.hephaistos.pyro"

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

            runtimeOnly("org.hsqldb:hsqldb:2.7.4")

            testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test:4.0.0")
            testImplementation("org.springframework.boot:spring-boot-starter-security-test:4.0.0")
            testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test:4.0.0")
            testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
