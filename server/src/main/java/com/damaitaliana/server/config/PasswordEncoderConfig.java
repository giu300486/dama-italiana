package com.damaitaliana.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wires the {@link PasswordEncoder} bean used by the service layer to hash and verify user
 * passwords.
 *
 * <p>BCrypt strength is fixed at 12 across all profiles (PLAN-fase-5 §9.2 — NFR-S-01 + decisione
 * utente 2026-05-10). A cost of 12 means ~250ms per hash on commodity hardware — slow enough to
 * blunt offline brute-force, fast enough to keep the test suite under the agreed 60s budget.
 *
 * <p>This is a deliberately minimal {@code @Configuration} — it exists in isolation in Task 5.4 to
 * be consumable by {@code UserService} before the full {@code SecurityConfig} arrives in Task 5.7.
 * The {@code SecurityFilterChain}, CORS, and stateless session policy will land later and may share
 * or absorb this configuration class; for now keeping it small avoids depending on security
 * infrastructure not yet built.
 */
@Configuration
public class PasswordEncoderConfig {

  private static final int BCRYPT_STRENGTH = 12;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
  }
}
