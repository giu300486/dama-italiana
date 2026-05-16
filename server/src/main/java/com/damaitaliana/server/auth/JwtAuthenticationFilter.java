package com.damaitaliana.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>} on each request, verifies the token via {@link
 * JwtService}, and populates the {@link SecurityContextHolder} with a {@link
 * UsernamePasswordAuthenticationToken} whose principal is the {@code userId} (Long) and whose
 * single authority comes from {@code claims.role().toAuthority()}.
 *
 * <p>No header → pass through (the security chain decides whether the endpoint allows anonymous
 * access). Bearer token present but invalid → the filter short-circuits with a 401 via {@link
 * JwtAuthenticationEntryPoint}, even if the target endpoint would otherwise allow anonymous. The
 * rationale: a client that bothers to send a token is asserting an identity; if that assertion is
 * not verifiable, the safest response is to reject the request rather than silently downgrade to
 * "anonymous".
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final JwtAuthenticationEntryPoint entryPoint;

  public JwtAuthenticationFilter(JwtService jwtService, JwtAuthenticationEntryPoint entryPoint) {
    this.jwtService = jwtService;
    this.entryPoint = entryPoint;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    String token = header.substring(BEARER_PREFIX.length()).trim();
    JwtClaims claims;
    try {
      claims = jwtService.parse(token);
    } catch (JwtVerificationException e) {
      SecurityContextHolder.clearContext();
      entryPoint.commence(request, response, new BearerTokenInvalid(e));
      return;
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            claims.userId(), null, List.of(claims.role().toAuthority()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    chain.doFilter(request, response);
  }

  /**
   * The entry point's contract takes an {@link AuthenticationException}; none of the framework
   * subclasses (BadCredentials, AccountExpired, ...) fit the "JWT failed verification" case
   * cleanly, so we publish a minimal local subclass instead of misappropriating one.
   */
  private static final class BearerTokenInvalid extends AuthenticationException {
    BearerTokenInvalid(Throwable cause) {
      super("Bearer token verification failed", cause);
    }
  }
}
