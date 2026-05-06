package com.damaitaliana.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Smoke test for the {@link CoreServerConfiguration} root: verifies that the Spring context
 * bootstraps cleanly with only the core-server config registered. Replaces the Fase-0 {@code
 * CoreServerSmokeTest}.
 *
 * <p>In Fase 4 the {@code @ComponentScan} finds no concrete components yet (the in-memory adapters
 * and the {@code MatchManager} bean arrive in subsequent tasks). The test still asserts that the
 * configuration class itself is registered as a bean and that the context is non-empty.
 */
class CoreServerConfigurationTest {

  @Test
  void shouldBootstrapEmptyContextWithoutErrors() {
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(CoreServerConfiguration.class)) {
      assertThat(context.getBean(CoreServerConfiguration.class)).isNotNull();
      assertThat(context.getBeanDefinitionCount()).isPositive();
    }
  }
}
