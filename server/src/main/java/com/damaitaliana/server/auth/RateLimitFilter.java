package com.damaitaliana.server.auth;

import com.damaitaliana.server.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP fixed-window rate limiter for the two unauthenticated auth endpoints (NFR-S-04 part 1,
 * ADR-047). Auto-registered as a servlet filter (it is a {@link Component}); it runs ahead of the
 * controller for every request and acts only on {@code POST /v1/auth/login} and {@code POST
 * /v1/auth/register}, letting everything else through untouched.
 *
 * <p>State is two in-memory Caffeine caches keyed by client IP, each holding an {@link
 * AtomicInteger} attempt counter that {@code expireAfterWrite} evicts one window after the first
 * request — a fixed window, not a sliding one. Single-instance only; a restart resets all counters,
 * which is acceptable for v1 (a multi-instance deployment would move this to Redis — ADR-047).
 *
 * <p>On the (window+1)-th attempt the filter answers 429 with an {@code application/problem+json}
 * body and a {@code Retry-After} header, written directly here rather than thrown — a filter sits
 * outside the {@code @RestControllerAdvice} dispatch, so it shapes its own RFC 7807 response, the
 * same way {@link JwtAuthenticationEntryPoint} does for 401. {@code Retry-After} is reported as the
 * full window length (a conservative upper bound on the wait), not the exact residual TTL.
 *
 * <p>Client IP honours {@code X-Forwarded-For} (first hop) when present so the limiter keys on the
 * real client behind a reverse proxy, falling back to {@link HttpServletRequest#getRemoteAddr()}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/v1/auth/login";
  private static final String REGISTER_PATH = "/v1/auth/register";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private final RateLimitProperties properties;
  private final ObjectMapper objectMapper;
  private final Cache<String, AtomicInteger> loginBuckets;
  private final Cache<String, AtomicInteger> registerBuckets;

  public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.loginBuckets =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(properties.getLoginWindowSeconds()))
            .build();
    this.registerBuckets =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(properties.getRegisterWindowSeconds()))
            .build();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!properties.isEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    String path = request.getRequestURI();
    Cache<String, AtomicInteger> bucket;
    int maxAttempts;
    int windowSeconds;
    if (LOGIN_PATH.equals(path)) {
      bucket = loginBuckets;
      maxAttempts = properties.getLoginMaxAttempts();
      windowSeconds = properties.getLoginWindowSeconds();
    } else if (REGISTER_PATH.equals(path)) {
      bucket = registerBuckets;
      maxAttempts = properties.getRegisterMaxAttempts();
      windowSeconds = properties.getRegisterWindowSeconds();
    } else {
      chain.doFilter(request, response);
      return;
    }

    String clientIp = clientIp(request);
    int attempts = bucket.get(clientIp, key -> new AtomicInteger()).incrementAndGet();
    if (attempts > maxAttempts) {
      writeTooManyRequests(request, response, windowSeconds);
      return;
    }
    chain.doFilter(request, response);
  }

  private static String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader(X_FORWARDED_FOR);
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private void writeTooManyRequests(
      HttpServletRequest request, HttpServletResponse response, int retryAfterSeconds)
      throws IOException {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
    problem.setTitle("Too Many Requests");
    problem.setInstance(URI.create(request.getRequestURI()));

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.getWriter().write(objectMapper.writeValueAsString(problem));
  }
}
