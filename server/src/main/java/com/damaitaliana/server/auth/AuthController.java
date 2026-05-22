package com.damaitaliana.server.auth;

import com.damaitaliana.server.auth.RefreshTokenService.RotatedTokens;
import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.LoginRequest;
import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.mapper.UserMapper;
import com.damaitaliana.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth endpoints. Whitelisted under {@code /v1/auth/**} so they bypass {@link
 * com.damaitaliana.server.auth.JwtAuthenticationFilter}'s "no token = 401" semantics — the whole
 * point of these endpoints is to issue or exchange tokens, so requiring one upfront would be a
 * chicken-and-egg.
 *
 * <p>The same generic 401 message is returned for "username does not exist" and "password does not
 * match". Differentiating would let an attacker enumerate registered usernames just by reading
 * which error variant comes back.
 *
 * <p>{@code /refresh} and {@code /logout} both carry the refresh token in the {@code Authorization:
 * Bearer} header rather than the request body — the established convention for this API. Structural
 * token failures (bad signature, expired, malformed) never reach these methods: {@link
 * JwtAuthenticationFilter} intercepts them first and answers 401 via the entry point. What does
 * reach here is a structurally valid token that fails for stateful reasons (wrong {@code type},
 * already rotated/revoked, user gone), surfaced as {@link JwtVerificationException} / {@link
 * RefreshTokenInvalidException} and mapped to 401 by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

  private static final String INVALID_CREDENTIALS = "Invalid username or password";
  private static final String BEARER_PREFIX = "Bearer ";

  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final long accessTtlSeconds;

  public AuthController(
      UserService userService,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      JwtProperties jwtProperties) {
    this.userService = userService;
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.accessTtlSeconds = jwtProperties.getAccessTtlSeconds();
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    UserEntity user = userService.register(request);
    return buildAuthResponse(user);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    UserEntity user =
        userService
            .findByUsername(request.username())
            .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException(INVALID_CREDENTIALS);
    }
    userService.updateLastLogin(user.getId());
    // Re-read so the response carries the freshly updated lastLoginAt timestamp.
    UserEntity refreshed = userService.findById(user.getId()).orElseThrow();
    return buildAuthResponse(refreshed);
  }

  /**
   * Exchanges a valid refresh token for a brand-new access + refresh pair. The presented refresh
   * token is rotated (single-use): {@link RefreshTokenService#verifyAndRotate(String)} marks it
   * revoked, so replaying it afterwards yields 401.
   */
  @PostMapping("/refresh")
  public AuthResponse refresh(
      @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false)
          String authorizationHeader) {
    String refreshToken = extractBearerToken(authorizationHeader);
    RotatedTokens rotated = refreshTokenService.verifyAndRotate(refreshToken);
    return new AuthResponse(
        UserMapper.toResponse(rotated.user()),
        rotated.accessToken(),
        rotated.refreshToken(),
        "Bearer",
        accessTtlSeconds);
  }

  /**
   * Revokes the presented refresh token. Returns 204 on success. Revoking an already-revoked or
   * unknown token yields 401 rather than a no-op 204: a client that logs out twice has a bug and
   * should be told, not silently humoured.
   */
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false)
          String authorizationHeader) {
    String refreshToken = extractBearerToken(authorizationHeader);
    refreshTokenService.revoke(refreshToken);
  }

  private AuthResponse buildAuthResponse(UserEntity user) {
    String accessToken = jwtService.issueAccess(user);
    String refreshToken = refreshTokenService.issue(user).rawToken();
    return new AuthResponse(
        UserMapper.toResponse(user), accessToken, refreshToken, "Bearer", accessTtlSeconds);
  }

  /**
   * Pulls the bearer credential out of an {@code Authorization} header. A missing or non-Bearer
   * header on these endpoints is itself an authentication failure (401), so it is reported as a
   * {@link JwtVerificationException} rather than letting Spring answer 400 for a missing header.
   */
  private static String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new JwtVerificationException("missing or malformed Authorization Bearer header");
    }
    String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new JwtVerificationException("empty bearer token");
    }
    return token;
  }
}
