package com.damaitaliana.server.dto;

import java.time.Instant;

/**
 * Response shape for a single user (e.g. {@code GET /v1/users/me} or the {@code user} field of
 * {@link AuthResponse}).
 *
 * <p>Deliberately omits {@code passwordHash} — that field never leaves the server boundary. The
 * {@code role} is serialized as the enum name string (e.g. {@code "USER"}, {@code
 * "TOURNAMENT_ADMIN"}).
 */
public record UserResponse(
    Long id, String username, String email, String role, Instant createdAt, Instant lastLoginAt) {}
