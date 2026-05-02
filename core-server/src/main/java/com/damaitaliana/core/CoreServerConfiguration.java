package com.damaitaliana.core;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Java config root for the core-server module.
 *
 * <p>Imported by other modules ({@code client} for LAN host, {@code server} for Internet) via
 * {@code @Import(CoreServerConfiguration.class)} or via a parent {@code @ComponentScan} that
 * already covers the {@code com.damaitaliana.core} base package.
 *
 * <p>Constraint (CLAUDE.md §8.8): this configuration MUST NOT pull a transport. It only enables
 * component scanning over the core-server sub-packages (match, tournament, repository, eventbus,
 * stomp). Transport adapters (Tomcat, Jetty, JPA) live in the importing modules.
 */
@Configuration
@ComponentScan(basePackages = "com.damaitaliana.core")
public class CoreServerConfiguration {}
