package com.damaitaliana.client.app;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Static bridge between the Spring Boot bootstrap (main thread) and the JavaFX application
 * lifecycle (JavaFX thread).
 *
 * <p>{@link javafx.application.Application#launch} instantiates the {@link JavaFxApp} reflectively
 * and offers no constructor injection hook. The Spring context is therefore set on this holder
 * before {@code launch} is invoked and read back from inside {@link JavaFxApp#start}.
 *
 * <p>The holder is package-thread-safe via a {@code volatile} reference. Callers that depend on the
 * context (i.e. JavaFX controllers) must use {@link #requireContext()} so that an early null fails
 * fast with a clear message rather than producing a {@link NullPointerException}.
 */
public final class JavaFxAppContextHolder {

  private static volatile ConfigurableApplicationContext context;

  private JavaFxAppContextHolder() {}

  /** Publishes the Spring context. Called once from {@link ClientApplication#main}. */
  public static void setContext(ConfigurableApplicationContext ctx) {
    context = ctx;
  }

  /**
   * @return the published Spring context.
   * @throws IllegalStateException if no context has been published yet.
   */
  public static ConfigurableApplicationContext requireContext() {
    ConfigurableApplicationContext ctx = context;
    if (ctx == null) {
      throw new IllegalStateException(
          "ApplicationContext not initialized: ClientApplication.main must run before JavaFX");
    }
    return ctx;
  }

  /** Closes and clears the held context. Idempotent. */
  public static void closeContext() {
    ConfigurableApplicationContext ctx = context;
    if (ctx != null) {
      context = null;
      ctx.close();
    }
  }
}
