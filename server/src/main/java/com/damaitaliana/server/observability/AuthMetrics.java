package com.damaitaliana.server.observability;

import com.damaitaliana.server.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom auth metrics exposed on {@code /actuator/prometheus} (A5.18, NFR-O-01). Owns the meter
 * names so callers only say what happened.
 *
 * <p>Meters are registered eagerly, so every series appears in the scrape at 0 from startup instead
 * of only after its first event.
 */
@Component
public class AuthMetrics {

  static final String LOGIN_ATTEMPTS = "auth.login.attempts";
  static final String REGISTER_ATTEMPTS = "auth.register.attempts";

  /**
   * Not {@code users.total}: Prometheus client 1.x reserves the {@code _total} suffix for counters
   * and strips it from gauge names, so the series would surface as plain {@code users}.
   */
  static final String USERS_REGISTERED = "users.registered";

  private final Counter loginSuccess;
  private final Counter loginFailure;
  private final Counter registerAttempts;

  public AuthMetrics(MeterRegistry registry, UserRepository userRepository) {
    this.loginSuccess = loginCounter(registry, "success");
    this.loginFailure = loginCounter(registry, "failure");
    this.registerAttempts =
        Counter.builder(REGISTER_ATTEMPTS)
            .description(
                "Registration attempts that passed validation, created or rejected as duplicate")
            .register(registry);
    Gauge.builder(USERS_REGISTERED, userRepository, UserRepository::count)
        .description("Accounts currently stored in the users table")
        .strongReference(true)
        .register(registry);
  }

  /** A registration request that passed validation, whatever its outcome. */
  public void registerAttempted() {
    registerAttempts.increment();
  }

  /** A login that issued tokens. */
  public void loginSucceeded() {
    loginSuccess.increment();
  }

  /** A login rejected for unknown username or wrong password. */
  public void loginFailed() {
    loginFailure.increment();
  }

  private static Counter loginCounter(MeterRegistry registry, String result) {
    return Counter.builder(LOGIN_ATTEMPTS)
        .description("Login attempts that reached the login endpoint, by outcome")
        .tag("result", result)
        .register(registry);
  }
}
