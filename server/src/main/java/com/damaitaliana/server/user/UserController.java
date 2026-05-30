package com.damaitaliana.server.user;

import com.damaitaliana.server.dto.UserResponse;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.mapper.UserMapper;
import com.damaitaliana.server.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated user endpoints under {@code /v1/users}.
 *
 * <p>Unlike {@code /v1/auth/**}, this path is <em>not</em> whitelisted in {@code SecurityConfig},
 * so every request first passes {@link com.damaitaliana.server.auth.JwtAuthenticationFilter}: a
 * missing, expired, or tampered access token is rejected with 401 before the handler runs. By the
 * time {@link #me(Long)} executes, the {@code SecurityContext} principal is guaranteed to be the
 * {@code userId} (Long) decoded from a structurally valid, in-date access token.
 *
 * <p>A 401-passing token whose {@code userId} no longer maps to a row (the user was deleted after
 * the token was issued) is the one residual edge case: it surfaces as {@link
 * EntityNotFoundException} and is mapped to 404 by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Returns the profile of the caller identified by the access token. The principal is injected
   * from the {@code SecurityContext} populated by the JWT filter; no token is parsed here.
   */
  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal Long userId) {
    UserEntity user =
        userService
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
    return UserMapper.toResponse(user);
  }
}
