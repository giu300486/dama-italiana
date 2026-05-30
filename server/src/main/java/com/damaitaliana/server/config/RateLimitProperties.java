package com.damaitaliana.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound to the {@code dama.ratelimit.*} configuration keys (NFR-S-04 part 1, ADR-047).
 *
 * <p>Two independent fixed windows guard the unauthenticated auth endpoints: {@code POST
 * /v1/auth/login} (default 5 attempts / 60 s per client IP) and {@code POST /v1/auth/register}
 * (default 3 attempts / 3600 s per client IP). Exceeding either yields HTTP 429 with a {@code
 * Retry-After} header.
 *
 * <p>{@link #enabled} defaults to {@code true} so {@code dev}, {@code it}, and {@code prod} are
 * rate-limited out of the box. The {@code test} profile sets it to {@code false} (see {@code
 * application-test.yml}) so the existing auth integration tests — which fire many requests from the
 * same loopback address across their methods — are not throttled; {@code RateLimitIntegrationTest}
 * re-enables it explicitly with short windows to exercise the filter in isolation.
 */
@ConfigurationProperties(prefix = "dama.ratelimit")
public class RateLimitProperties {

  private boolean enabled = true;
  private int loginMaxAttempts = 5;
  private int loginWindowSeconds = 60;
  private int registerMaxAttempts = 3;
  private int registerWindowSeconds = 3600;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getLoginMaxAttempts() {
    return loginMaxAttempts;
  }

  public void setLoginMaxAttempts(int loginMaxAttempts) {
    this.loginMaxAttempts = loginMaxAttempts;
  }

  public int getLoginWindowSeconds() {
    return loginWindowSeconds;
  }

  public void setLoginWindowSeconds(int loginWindowSeconds) {
    this.loginWindowSeconds = loginWindowSeconds;
  }

  public int getRegisterMaxAttempts() {
    return registerMaxAttempts;
  }

  public void setRegisterMaxAttempts(int registerMaxAttempts) {
    this.registerMaxAttempts = registerMaxAttempts;
  }

  public int getRegisterWindowSeconds() {
    return registerWindowSeconds;
  }

  public void setRegisterWindowSeconds(int registerWindowSeconds) {
    this.registerWindowSeconds = registerWindowSeconds;
  }
}
