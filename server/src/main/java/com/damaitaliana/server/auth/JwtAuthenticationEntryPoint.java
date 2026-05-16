package com.damaitaliana.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Writes HTTP 401 with an {@code application/problem+json} body (RFC 7807) when the security chain
 * rejects a request — both for "no credentials at all" (Spring's default {@code
 * AuthenticationCredentialsNotFoundException}) and for the targeted invocation from {@link
 * JwtAuthenticationFilter} after a {@link JwtVerificationException}.
 *
 * <p>The body is intentionally minimal: a single generic {@code detail} of "Authentication
 * required" with no hint about whether the token was missing, malformed, expired, signed with the
 * wrong secret, or rejected for a stateful reason. Leaking the failure mode helps attackers probe
 * the auth pipeline; omitting it costs nothing for legitimate clients (the WWW-Authenticate header
 * already signals that a Bearer token is expected).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
    problem.setTitle("Unauthorized");
    problem.setInstance(java.net.URI.create(request.getRequestURI()));

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("WWW-Authenticate", "Bearer");
    response.getWriter().write(objectMapper.writeValueAsString(problem));
  }
}
