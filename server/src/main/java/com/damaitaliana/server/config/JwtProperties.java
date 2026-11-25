package com.damaitaliana.server.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Bound to the {@code dama.jwt.*} configuration keys.
 *
 * <p>Per-profile secret strategy (PLAN-fase-5 §9.1, ADR-045):
 *
 * <ul>
 *   <li>{@code prod}: secret comes from the {@code JWT_SECRET} env var via the {@code
 *       application-prod.yml} placeholder {@code ${JWT_SECRET}}. If the env var is missing Spring
 *       fails to resolve the placeholder at bootstrap; the validator below is a defensive second
 *       line in case the YAML is ever changed to provide a default in prod.
 *   <li>{@code dev}: {@code application-dev.yml} defaults to a well-known fallback when {@code
 *       JWT_SECRET} is not set. The validator emits a {@code WARN} log so the operator notices the
 *       dev-only secret is in use.
 *   <li>{@code test} / {@code it}: the secret is hardcoded in the respective YAML — predictable, no
 *       external setup required for the test suite.
 * </ul>
 *
 * <p>Length invariant: HS256 mandates a 256-bit (32-byte) key minimum. The validator rejects
 * shorter secrets unconditionally, in every profile, so a typo in YAML cannot leave the system with
 * a key {@link io.jsonwebtoken.security.Keys#hmacShaKeyFor(byte[])} would later reject at runtime.
 */
@ConfigurationProperties(prefix = "dama.jwt")
public class JwtProperties implements EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(JwtProperties.class);

  /** HS256 minimum: 256 bits = 32 bytes. */
  static final int MIN_SECRET_BYTES = 32;

  /**
   * Fallback used when the {@code dev} profile boots without {@code JWT_SECRET} set. Length is
   * exactly the HS256 minimum (32 bytes UTF-8). Used to detect "is the operator still on the
   * default?" so the warning fires even when the secret arrives from the YAML default.
   */
  static final String DEV_FALLBACK_SECRET = "dev-only-secret-change-me-32bytes-min-padding-xxxxxx";

  private static final String PROD_PROFILE = "prod";

  private Environment environment;

  private String secret;
  private int accessTtlSeconds = 900;
  private int refreshTtlSeconds = 604800;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void validate() {
    boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);

    if (secret == null || secret.isBlank()) {
      if (isProd) {
        throw new IllegalStateException(
            "JWT secret missing in prod profile. Set env var JWT_SECRET (>= 32 bytes).");
      }
      secret = DEV_FALLBACK_SECRET;
      log.warn("Using DEV-ONLY JWT secret -- NEVER use in production");
    } else if (!isProd && DEV_FALLBACK_SECRET.equals(secret)) {
      log.warn("Using DEV-ONLY JWT secret -- NEVER use in production");
    }

    int actualBytes = secret.getBytes(StandardCharsets.UTF_8).length;
    if (actualBytes < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT secret must be at least "
              + MIN_SECRET_BYTES
              + " bytes for HS256 (got "
              + actualBytes
              + ").");
    }
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public int getAccessTtlSeconds() {
    return accessTtlSeconds;
  }

  public void setAccessTtlSeconds(int accessTtlSeconds) {
    this.accessTtlSeconds = accessTtlSeconds;
  }

  public int getRefreshTtlSeconds() {
    return refreshTtlSeconds;
  }

  public void setRefreshTtlSeconds(int refreshTtlSeconds) {
    this.refreshTtlSeconds = refreshTtlSeconds;
  }
}
