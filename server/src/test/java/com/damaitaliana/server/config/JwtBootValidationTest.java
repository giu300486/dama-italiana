package com.damaitaliana.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Startup validation of {@link JwtProperties} (ADR-045): the secret rules are checked through the
 * real Spring binding ({@code @ConfigurationProperties}, {@code EnvironmentAware}, {@code
 * PostConstruct}) by starting a minimal context, without web server or database.
 *
 * <p>Not covered here: the primary {@code prod} fail-fast, the {@code ${JWT_SECRET}} placeholder
 * without default in {@code application-prod.yml}. Booting the whole {@code prod} profile would
 * also need a MySQL datasource; these tests guard the second line of defence.
 */
class JwtBootValidationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(JwtPropertiesConfig.class);

  @Test
  void failsAtStartup_whenProdProfileHasNoSecret() {
    runner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .rootCause()
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("JWT secret missing in prod profile");
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"prod", "dev"})
  void failsAtStartup_whenSecretIsShorterThan32Bytes(String profile) {
    // 31 bytes: one short of the HS256 minimum.
    String shortSecret = "a".repeat(31);

    runner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
        .withPropertyValues("dama.jwt.secret=" + shortSecret)
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .rootCause()
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("at least 32 bytes");
            });
  }

  @Test
  void startsWithTheDevOnlySecret_whenDevProfileHasNoSecret() {
    runner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("dev"))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(JwtProperties.class).getSecret())
                  .isEqualTo("dev-only-secret-change-me-32bytes-min-padding-xxxxxx");
            });
  }

  @Test
  void startsWithTheConfiguredSecret_whenProdProfileHasA32ByteSecret() {
    // Exactly the HS256 minimum: the boundary value must be accepted.
    String secret = "p".repeat(32);

    runner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
        .withPropertyValues("dama.jwt.secret=" + secret)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(JwtProperties.class).getSecret()).isEqualTo(secret);
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JwtProperties.class)
  static class JwtPropertiesConfig {}
}
