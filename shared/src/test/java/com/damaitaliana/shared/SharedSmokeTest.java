package com.damaitaliana.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for the shared module build pipeline.
 *
 * <p>Verifies that JUnit 5 and AssertJ are wired correctly and that JaCoCo produces a coverage
 * report. Will be removed in Fase 1 once real domain tests exist.
 */
class SharedSmokeTest {

  @Test
  void buildPipelineIsAlive() {
    assertThat("shared").isEqualTo("shared");
  }
}
