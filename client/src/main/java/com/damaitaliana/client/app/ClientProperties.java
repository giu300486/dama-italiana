package com.damaitaliana.client.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized client configuration.
 *
 * <p>All persistent client state lives under {@code ~/.dama-italiana/} (SPEC §14.1). The default
 * values mirror that layout; tests and packaging may override individual paths via standard Spring
 * Boot mechanisms (env vars, system properties, profile-specific {@code application.yml}).
 *
 * @param savesDir directory containing single-player save slots (FR-SP-07/08).
 * @param configFile path to the user-preferences JSON file (Task 3.4).
 */
@ConfigurationProperties(prefix = "dama.client")
public record ClientProperties(String savesDir, String configFile) {}
