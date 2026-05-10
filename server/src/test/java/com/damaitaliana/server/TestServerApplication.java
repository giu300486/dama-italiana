package com.damaitaliana.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * Bootstrap entry point for local development via {@code mvn spring-boot:test-run}.
 *
 * <p>Configures an ephemeral MySQL 8 container (Testcontainers + {@link ServiceConnection}) so the
 * developer can run the full server locally without a native MySQL installation. The container
 * lifecycle matches the JVM lifecycle: started at boot, stopped on shutdown.
 *
 * <p>Sets the {@code dev} Spring profile programmatically so the server's profile fail-fast ({@link
 * ServerApplication#requireActiveProfile()}) is satisfied.
 *
 * <p>To inspect the ephemeral DB in MySQL Workbench: the container exposes a random port logged at
 * boot (look for {@code MySQL container ... port}). Default credentials are {@code test} / {@code
 * test} for both the user and the {@code test} database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestServerApplication {

  @Bean
  @ServiceConnection
  public MySQLContainer<?> mysqlContainer() {
    return new MySQLContainer<>("mysql:8.0");
  }

  public static void main(String[] args) {
    System.setProperty("spring.profiles.active", "dev");
    SpringApplication.from(ServerApplication::main).with(TestServerApplication.class).run(args);
  }
}
