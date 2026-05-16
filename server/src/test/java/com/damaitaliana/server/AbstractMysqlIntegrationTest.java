package com.damaitaliana.server;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for full integration tests that need a real MySQL 8 instance.
 *
 * <p>Connects to the locally installed MySQL server ({@code localhost:3306}, database {@code
 * dama_italiana_it}) — see ARCHITECTURE.md ADR-049. The connection details come from {@code
 * application-it.yml} (overridable via {@code SPRING_DATASOURCE_*} env vars).
 *
 * <p>Schema isolation between test classes is enforced by running {@link Flyway#clean()} + {@link
 * Flyway#migrate()} before every test method. Each test method therefore starts with an empty
 * schema reset to Flyway V1 state.
 *
 * <p>Tagged {@code slow}: excluded from the fast loop {@code mvn -pl server verify
 * -DexcludedGroups=slow}; included in {@code mvn -pl server verify} and {@code mvn clean verify}.
 * Requires the local MySQL server running with the schemas + app user created by {@code
 * scripts/db/setup-local-mysql.sql}.
 *
 * <p>Subclasses inherit the active {@code it} profile and the schema reset; they only declare their
 * own test methods.
 */
@SpringBootTest
@ActiveProfiles("it")
@Tag("slow")
public abstract class AbstractMysqlIntegrationTest {

  @Autowired private Flyway flyway;

  @BeforeEach
  void resetSchema() {
    flyway.clean();
    flyway.migrate();
  }
}
