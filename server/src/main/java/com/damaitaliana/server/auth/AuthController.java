package com.damaitaliana.server.auth;

import com.damaitaliana.server.auth.RefreshTokenService.RotatedTokens;
import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.config.OpenApiConfig;
import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.LoginRequest;
import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.mapper.UserMapper;
import com.damaitaliana.server.observability.AuthMetrics;
import com.damaitaliana.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
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
@Tag(name = "auth", description = "Registration, login, JWT refresh rotation and logout")
public class AuthController {

  private static final String INVALID_CREDENTIALS = "Invalid username or password";
  private static final String BEARER_PREFIX = "Bearer ";

  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final AuthMetrics authMetrics;
  private final long accessTtlSeconds;

  public AuthController(
      UserService userService,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      PasswordEncoder passwordEncoder,
      AuthMetrics authMetrics,
      JwtProperties jwtProperties) {
    this.userService = userService;
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
    this.authMetrics = authMetrics;
    this.accessTtlSeconds = jwtProperties.getAccessTtlSeconds();
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Register a new account",
      description = "Creates a user and returns an access + refresh token pair.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Account created, tokens issued"),
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
    @ApiResponse(
        responseCode = "409",
        description = "Username or email already registered",
        content = @Content)
  })
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    authMetrics.registerAttempted();
    UserEntity user = userService.register(request);
    return buildAuthResponse(user);
  }

  @PostMapping("/login")
  @Operation(
      summary = "Log in with username and password",
      description =
          "Returns an access + refresh token pair. The same 401 is returned whether the"
              + " username is unknown or the password is wrong (no account enumeration).")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Authenticated, tokens issued"),
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
    @ApiResponse(
        responseCode = "401",
        description = "Invalid username or password",
        content = @Content)
  })
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    Optional<UserEntity> found = userService.findByUsername(request.username());
    if (found.isEmpty()
        || !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
      authMetrics.loginFailed();
      throw new BadCredentialsException(INVALID_CREDENTIALS);
    }
    UserEntity user = found.get();
    userService.updateLastLogin(user.getId());
    // Re-read so the response carries the freshly updated lastLoginAt timestamp.
    UserEntity refreshed = userService.findById(user.getId()).orElseThrow();
    AuthResponse response = buildAuthResponse(refreshed);
    authMetrics.loginSucceeded();
    return response;
  }

  /**
   * Exchanges a valid refresh token for a brand-new access + refresh pair. The presented refresh
   * token is rotated (single-use): {@link RefreshTokenService#verifyAndRotate(String)} marks it
   * revoked, so replaying it afterwards yields 401.
   */
  @PostMapping("/refresh")
  @Operation(
      summary = "Rotate a refresh token",
      description =
          "Send the refresh token as 'Authorization: Bearer <refreshToken>'. Returns a new"
              + " access + refresh pair; the presented token is single-use and becomes invalid.")
  @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "New token pair issued"),
    @ApiResponse(
        responseCode = "401",
        description = "Refresh token invalid, expired, revoked or of the wrong type",
        content = @Content)
  })
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
  @Operation(
      summary = "Revoke a refresh token",
      description =
          "Send the refresh token as 'Authorization: Bearer <refreshToken>'. Revoking an"
              + " already-revoked or unknown token yields 401 (not an idempotent no-op).")
  @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Token revoked"),
    @ApiResponse(
        responseCode = "401",
        description = "Refresh token already revoked or unknown",
        content = @Content)
  })
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
