package com.damaitaliana.server.web;

import com.damaitaliana.server.exception.UserAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised mapping from application exceptions to RFC 7807 {@code application/problem+json}
 * responses.
 *
 * <p>This class lands in Task 5.8 with just the three handlers the {@code AuthController} needs to
 * fulfill its contract: {@link UserAlreadyExistsException} → 409, {@link BadCredentialsException} →
 * 401, {@link MethodArgumentNotValidException} → 400. Task 5.12 extends it with handlers for {@code
 * JwtVerificationException} / {@code RefreshTokenInvalidException} / {@code
 * RateLimitExceededException} and a catch-all for unexpected failures.
 *
 * <p>The conflict response carries a deliberately generic detail ("Username or email already
 * registered") rather than naming the specific field that collided — that would let an attacker
 * enumerate accounts.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ProblemDetail handleUserAlreadyExists(
      UserAlreadyExistsException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT, "Conflict", "Username or email already registered", request);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {
    return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::toFieldError)
            .toList();
    ProblemDetail p = problem(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed", request);
    p.setProperty("errors", errors);
    return p;
  }

  private static Map<String, String> toFieldError(FieldError fe) {
    return Map.of(
        "field",
        fe.getField(),
        "message",
        fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
  }

  private static ProblemDetail problem(
      HttpStatus status, String title, String detail, HttpServletRequest request) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
    p.setTitle(title);
    p.setInstance(URI.create(request.getRequestURI()));
    return p;
  }
}
