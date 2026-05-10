package com.damaitaliana.server;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for full integration tests that need a real MySQL 8 instance.
 *
 * <p>Spins up a {@link MySQLContainer} via Testcontainers and wires it into the Spring context via
 * {@link ServiceConnection} (no manual {@code @DynamicPropertySource} needed). The container is
 * started on test class init and stopped after all methods.
 *
 * <p>Tagged {@code slow}: excluded from the fast loop {@code mvn -pl server verify
 * -DexcludedGroups=slow}; included in {@code mvn clean verify} root regression. Requires Docker to
 * be running locally.
 *
 * <p>Subclasses inherit the active {@code it} profile (see {@code application-it.yml}) and the
 * container; they only need to declare their own test methods.
 */
@SpringBootTest
@ActiveProfiles("it")
@Testcontainers
@Tag("slow")
public abstract class AbstractMysqlIntegrationTest {

  @Container @ServiceConnection
  protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");
}
