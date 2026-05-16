package com.damaitaliana.server.config;

import com.damaitaliana.server.auth.JwtAuthenticationEntryPoint;
import com.damaitaliana.server.auth.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless Spring Security configuration for the REST API.
 *
 * <p>CSRF is disabled (no browser-session cookies are issued — every request must carry its own JWT
 * Bearer token). Session creation is forbidden: each request is authenticated solely from the
 * {@code Authorization} header by {@link JwtAuthenticationFilter}, which runs in front of the
 * standard {@link UsernamePasswordAuthenticationFilter} slot. Authentication failures are written
 * out as RFC 7807 {@code application/problem+json} by {@link JwtAuthenticationEntryPoint}.
 *
 * <p>Whitelist: {@code /v1/auth/**} (register/login/refresh/logout — credentials are exchanged
 * here, no token yet); {@code /v3/api-docs/**} and {@code /swagger-ui/**} (API documentation is
 * itself a public artefact); {@code /actuator/health} and {@code /actuator/prometheus} (probe
 * targets for orchestration and observability).
 *
 * <p>CORS: origins driven by {@code dama.cors.allowed-origins} (default {@code
 * http://localhost:8080}); in {@code prod} this MUST be overridden via env var to the actual client
 * origin(s) — see ADR-044 §CORS.
 *
 * <p>The {@code PasswordEncoder} bean lives in its own {@link PasswordEncoderConfig} (introduced in
 * Task 5.4 before {@code SecurityConfig} existed); leaving it separate keeps the BCrypt strength
 * decision in a place that's easy to find without having to grep through the HTTP wiring.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
    "/v1/auth/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/actuator/health",
    "/actuator/prometheus",
    "/favicon.ico"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final List<String> allowedOrigins;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      @Value("${dama.cors.allowed-origins:http://localhost:8080}") String allowedOriginsCsv) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.allowedOrigins = Arrays.stream(allowedOriginsCsv.split(",")).map(String::trim).toList();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(eh -> eh.authenticationEntryPoint(jwtAuthenticationEntryPoint))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(allowedOrigins);
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
    cors.setExposedHeaders(List.of("X-Correlation-Id"));
    cors.setAllowCredentials(true);
    cors.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }
}
