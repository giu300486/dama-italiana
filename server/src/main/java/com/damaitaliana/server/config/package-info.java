/**
 * Spring configuration and typed configuration properties.
 *
 * <ul>
 *   <li>{@link com.damaitaliana.server.config.SecurityConfig}: stateless security filter chain,
 *       public-path whitelist and environment-driven CORS.
 *   <li>{@link com.damaitaliana.server.config.PasswordEncoderConfig}: BCrypt with cost 12 in every
 *       profile.
 *   <li>{@link com.damaitaliana.server.config.OpenApiConfig}: OpenAPI 3.1 metadata and the {@code
 *       bearerAuth} security scheme (NFR-M-03).
 *   <li>{@link com.damaitaliana.server.config.JwtProperties} ({@code dama.jwt.*}) and {@link
 *       com.damaitaliana.server.config.RateLimitProperties} ({@code dama.ratelimit.*}).
 * </ul>
 */
package com.damaitaliana.server.config;
