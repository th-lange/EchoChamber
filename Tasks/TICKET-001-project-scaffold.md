# TICKET-001 — Project Scaffold & Build Configuration

## Summary
Initialise the Gradle Kotlin DSL project with all required dependencies and Spring Boot configuration. No business logic — foundation only.

## Acceptance Criteria
- [ ] `build.gradle.kts` compiles with all required dependencies declared
- [ ] Spring Boot application starts and reaches a healthy state with no errors
- [ ] Flyway is on the classpath and configured to run on startup
- [ ] Package root `com.echochamber.engine` exists with an empty `Application.kt`
- [ ] All folder structure from the implementation plan is scaffolded (empty packages are fine)

## Dependencies
None — this is the root ticket.

## Technical Notes

### Required dependencies
- `org.springframework.boot:spring-boot-starter-web` (or `webflux`)
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-data-rest`
- `org.springframework.data:spring-data-rest-hal-explorer`
- `org.flywaydb:flyway-core`
- `org.postgresql:postgresql`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`
- `org.jetbrains.kotlinx:kotlinx-coroutines-reactor` (if WebFlux)
- `org.graalvm.sdk:graal-sdk` (for ScriptMutationHandler)
- `io.github.resilience4j:resilience4j-kotlin` (rate limiting)
- `org.testcontainers:postgresql` (test scope)

### application.yml baseline
```yaml
spring:
  data:
    rest:
      base-path: /api
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## Tests Required
- Smoke test: application context loads without errors (`@SpringBootTest`)
