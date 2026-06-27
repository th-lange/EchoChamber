package com.echochamber.engine

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.boot.test.context.SpringBootTest

/**
 * Full-context smoke test. Loads the entire Spring context against the Testcontainers
 * PostgreSQL datasource (`jdbc:tc:postgresql:...` in `src/test/resources/application.yml`),
 * so it requires a running Docker daemon. Gated behind `-Drun.integration=true` like the
 * other Testcontainers-driven tests so local runs without Docker skip rather than fail.
 */
@SpringBootTest
@Tag("integration")
@EnabledIfSystemProperty(named = "run.integration", matches = "true")
class ApplicationContextTest {

    @Test
    fun contextLoads() {
        // Verifies that the Spring application context starts without errors
    }
}
