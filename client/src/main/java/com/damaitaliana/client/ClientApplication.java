package com.damaitaliana.client;

import com.damaitaliana.client.app.JavaFxApp;
import com.damaitaliana.client.app.JavaFxAppContextHolder;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Desktop client entry point.
 *
 * <p>Boot order: Spring Boot non-web container first ({@code WebApplicationType.NONE}), then the
 * resulting {@link ConfigurableApplicationContext} is published on {@link JavaFxAppContextHolder}
 * so that {@link JavaFxApp#start} can reach Spring beans, then {@link Application#launch} hands
 * control to the JavaFX runtime. {@code Application.launch} blocks until the UI exits.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ClientApplication {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(ClientApplication.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    ConfigurableApplicationContext context = app.run(args);
    JavaFxAppContextHolder.setContext(context);
    Application.launch(JavaFxApp.class, args);
  }
}
