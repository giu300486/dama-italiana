package com.damaitaliana.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /v1/auth/register}.
 *
 * <p>Validation rules (enforced by Bean Validation on the controller method's {@code @Valid}
 * parameter):
 *
 * <ul>
 *   <li>{@code username}: 3-50 characters, alphanumeric + underscore + hyphen only.
 *   <li>{@code email}: RFC 5322-compliant, max 255 characters.
 *   <li>{@code password}: 8-72 characters (BCrypt input max), at least one letter and one digit. 72
 *       chars is the maximum BCrypt input length; passwords longer than 72 are silently truncated
 *       by BCrypt, so we reject them at validation time to make the limit explicit.
 * </ul>
 *
 * <p>The validation messages here are intentionally minimal; the GlobalExceptionHandler (Task 5.12)
 * formats {@code MethodArgumentNotValidException} into a Problem Details response with per-field
 * errors.
 */
public record RegisterRequest(
    @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "must match [a-zA-Z0-9_-]")
        String username,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "must contain at least one letter and one digit")
        String password) {}
