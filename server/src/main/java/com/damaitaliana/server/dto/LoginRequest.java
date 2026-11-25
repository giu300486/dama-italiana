package com.damaitaliana.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /v1/auth/login}.
 *
 * <p>Validation is intentionally lighter than {@link RegisterRequest}: we don't re-validate
 * password complexity (the user is just trying to authenticate; rejecting an attempt because the
 * password they typed doesn't match the current complexity policy would leak that the policy
 * exists). We only check non-emptiness and a sane length cap to avoid abuse (very long inputs that
 * waste BCrypt CPU).
 */
public record LoginRequest(
    @NotBlank @Size(max = 50) String username, @NotBlank @Size(max = 72) String password) {}
