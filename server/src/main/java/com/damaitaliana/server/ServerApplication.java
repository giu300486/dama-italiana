package com.damaitaliana.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap entry point for the central Internet-facing server.
 *
 * <p>Refuses to start if no Spring profile is active. Supported profiles: {@code dev}, {@code
 * test}, {@code it}, {@code prod} (see ARCHITECTURE.md ADR-044).
 *
 * <ul>
 *   <li>Production: {@code java -jar server.jar} with env {@code SPRING_PROFILES_ACTIVE=prod} (also
 *       requires {@code JWT_SECRET}, {@code SPRING_DATASOURCE_URL/USERNAME/PASSWORD}).
 *   <li>Local development: {@code mvn -pl server spring-boot:test-run} which delegates to {@code
 *       TestServerApplication} and auto-sets the {@code dev} profile + Testcontainers MySQL.
 * </ul>
 */
@SpringBootApplication
public class ServerApplication {

  public static void main(String[] args) {
    requireActiveProfile();
    SpringApplication.run(ServerApplication.class, args);
  }

  // Visible for testing.
  static void requireActiveProfile() {
    String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
    String sysProfile = System.getProperty("spring.profiles.active");
    boolean envSet = envProfile != null && !envProfile.isBlank();
    boolean sysSet = sysProfile != null && !sysProfile.isBlank();
    if (!envSet && !sysSet) {
      System.err.println(
          "FATAL: SPRING_PROFILES_ACTIVE not set. Specify one of: dev, test, it, prod.");
      System.err.println("Examples:");
      System.err.println(
          "  Production: java -jar server.jar  (with env SPRING_PROFILES_ACTIVE=prod)");
      System.err.println(
          "  Local dev:  mvn -pl server spring-boot:test-run  (auto-sets dev profile)");
      System.exit(1);
    }
  }
}
