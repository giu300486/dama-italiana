package com.damaitaliana.server.auth;

import com.damaitaliana.server.config.JwtProperties;
import com.damaitaliana.server.dto.AuthResponse;
import com.damaitaliana.server.dto.LoginRequest;
import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.mapper.UserMapper;
import com.damaitaliana.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

  private static final String INVALID_CREDENTIALS = "Invalid username or password";

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

  private AuthResponse buildAuthResponse(UserEntity user) {
    String accessToken = jwtService.issueAccess(user);
    String refreshToken = refreshTokenService.issue(user).rawToken();
    return new AuthResponse(
        UserMapper.toResponse(user), accessToken, refreshToken, "Bearer", accessTtlSeconds);
  }
}
