package com.damaitaliana.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * Verifies what {@code logback-spring.xml} writes to stdout for each Spring profile (A5.17,
 * NFR-O-02/03): JSON lines in {@code prod}/{@code it}, plain console lines in {@code dev}/{@code
 * test}.
 *
 * <p>The configuration is loaded through Spring Boot's {@link LoggingSystem}, the same path Boot
 * uses at startup, so {@code <springProfile>} blocks are honoured without a full application
 * context.
 */
@ExtendWith(OutputCaptureExtension.class)
class LogbackSpringConfigurationTest {

  private static final String CONFIG_LOCATION = "classpath:logback-spring.xml";
  private static final ObjectMapper JSON = new ObjectMapper();

  private final Logger log = LoggerFactory.getLogger(LogbackSpringConfigurationTest.class);

  @AfterEach
  void restoreTestProfileLogging() {
    initializeLogging("test");
  }

  @Test
  void writesJsonLine_whenProfileProd(CapturedOutput output) throws Exception {
    initializeLogging("prod");

    log.warn("json-probe-prod");

    JsonNode event = JSON.readTree(lastLineContaining(output, "json-probe-prod"));
    assertThat(event.path("message").asText()).isEqualTo("json-probe-prod");
    assertThat(event.path("level").asText()).isEqualTo("WARN");
    assertThat(event.path("logger_name").asText())
        .isEqualTo(LogbackSpringConfigurationTest.class.getName());
    assertThat(event.path("@timestamp").asText()).isNotBlank();
  }

  @Test
  void includesCorrelationId_whenMdcSet(CapturedOutput output) throws Exception {
    initializeLogging("prod");

    MDC.put("correlationId", "corr-123");
    try {
      log.warn("json-probe-mdc");
    } finally {
      MDC.remove("correlationId");
    }

    JsonNode event = JSON.readTree(lastLineContaining(output, "json-probe-mdc"));
    assertThat(event.path("correlationId").asText()).isEqualTo("corr-123");
  }

  @Test
  void includesStackTrace_whenExceptionLogged(CapturedOutput output) throws Exception {
    initializeLogging("prod");

    log.error("json-probe-error", new IllegalStateException("boom"));

    JsonNode event = JSON.readTree(lastLineContaining(output, "json-probe-error"));
    assertThat(event.path("stack_trace").asText())
        .contains("java.lang.IllegalStateException: boom");
  }

  @Test
  void writesJsonLine_whenProfileIt(CapturedOutput output) throws Exception {
    initializeLogging("it");

    log.warn("json-probe-it");

    JsonNode event = JSON.readTree(lastLineContaining(output, "json-probe-it"));
    assertThat(event.path("message").asText()).isEqualTo("json-probe-it");
  }

  @Test
  void writesPlainLine_whenProfileTest(CapturedOutput output) {
    initializeLogging("test");

    log.warn("plain-probe-test");

    String line = lastLineContaining(output, "plain-probe-test");
    assertThat(line.strip()).doesNotStartWith("{");
    assertThat(line).contains("WARN");
  }

  @Test
  void writesPlainLine_whenProfileDev(CapturedOutput output) {
    initializeLogging("dev");

    log.warn("plain-probe-dev");

    String line = lastLineContaining(output, "plain-probe-dev");
    assertThat(line.strip()).doesNotStartWith("{");
    assertThat(line).contains("WARN");
  }

  private static void initializeLogging(String profile) {
    LoggingSystem loggingSystem =
        LoggingSystem.get(LogbackSpringConfigurationTest.class.getClassLoader());
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    loggingSystem.cleanUp();
    loggingSystem.beforeInitialize();
    loggingSystem.initialize(new LoggingInitializationContext(environment), CONFIG_LOCATION, null);
  }

  private static String lastLineContaining(CapturedOutput output, String marker) {
    return Arrays.stream(output.getOut().split("\\R"))
        .filter(line -> line.contains(marker))
        .reduce((first, second) -> second)
        .orElseThrow(() -> new AssertionError("no log line contains " + marker));
  }
}
