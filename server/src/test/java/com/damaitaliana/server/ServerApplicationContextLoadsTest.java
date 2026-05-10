package com.damaitaliana.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: asserts that the Spring context bootstraps under the {@code test} profile.
 *
 * <p>Replaces the original {@code ServerSmokeTest} (Fase 0 placeholder). With H2 in-memory
 * datasource (configured by {@code application-test.yml}) and Flyway running migrations at startup,
 * a successful context load proves: properties bind cleanly, JPA / security / web
 * auto-configuration wires up, and Hibernate validates against the Flyway-managed schema.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServerApplicationContextLoadsTest {

  @Test
  void contextLoads() {
    // Assertion-free: success = no exception thrown by Spring context bootstrap.
  }
}
