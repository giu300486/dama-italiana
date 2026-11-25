/**
 * User-facing REST endpoints for authenticated users.
 *
 * <p>{@link com.damaitaliana.server.user.UserController} serves {@code GET /v1/users/me}, reading
 * the user id that {@link com.damaitaliana.server.auth.JwtAuthenticationFilter} puts in the
 * security context. Registration and login live in {@code auth}.
 */
package com.damaitaliana.server.user;
