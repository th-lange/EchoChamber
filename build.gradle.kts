import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
}

group = "com.echochamber"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

extra["resilience4jVersion"] = "2.2.0"

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Bean Validation (request DTO validation)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Data JPA + REST + HAL Explorer
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.data:spring-data-rest-hal-explorer")

    // Flyway
    implementation("org.flywaydb:flyway-core")

    // PostgreSQL driver
    runtimeOnly("org.postgresql:postgresql")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-kotlin:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")

    // GraalVM SDK (for ScriptMutationHandler)
    compileOnly("org.graalvm.sdk:graal-sdk:23.0.3")

    // Kotlin reflection (required by Spring)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.19.7")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Forward `-Drun.integration=true` from the Gradle invocation to the test JVM so
    // Testcontainers-driven integration tests gated by `@EnabledIfSystemProperty` opt
    // in. CI sets this; local dev runs without it so missing/old Docker is a skip,
    // not a failure.
    systemProperty(
        "run.integration",
        System.getProperty("run.integration", "false")
    )
}
