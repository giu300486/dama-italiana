package com.damaitaliana.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for the core-server module build pipeline.
 *
 * <p>Verifies that JUnit 5 and AssertJ are wired correctly. Will be removed in Fase 4 once real
 * tournament/match-manager tests exist.
 */
class CoreServerSmokeTest {

  @Test
  void buildPipelineIsAlive() {
    assertThat("core-server").isEqualTo("core-server");
  }
}
