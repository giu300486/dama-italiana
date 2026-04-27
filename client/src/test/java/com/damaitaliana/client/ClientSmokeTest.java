package com.damaitaliana.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for the client module build pipeline.
 *
 * <p>Verifies that JUnit 5, AssertJ and the Spring Boot test starter resolve correctly. Will be
 * removed in Fase 3 once real UI/controller tests exist.
 */
class ClientSmokeTest {

  @Test
  void buildPipelineIsAlive() {
    assertThat("client").isEqualTo("client");
  }
}
