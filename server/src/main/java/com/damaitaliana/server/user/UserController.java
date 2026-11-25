package com.damaitaliana.server.user;

import com.damaitaliana.server.config.OpenApiConfig;
import com.damaitaliana.server.dto.UserResponse;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.mapper.UserMapper;
import com.damaitaliana.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "users", description = "Profile of the authenticated caller")
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
  @Operation(
      summary = "Get the caller's profile",
      description =
          "Returns the profile identified by the access token. Never includes the password"
              + " hash.")
  @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Profile of the authenticated user"),
    @ApiResponse(
        responseCode = "401",
        description = "Missing, expired or invalid access token",
        content = @Content),
    @ApiResponse(
        responseCode = "404",
        description = "The token's user no longer exists",
        content = @Content)
  })
  public UserResponse me(@AuthenticationPrincipal Long userId) {
    UserEntity user =
        userService
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
    return UserMapper.toResponse(user);
  }
}
