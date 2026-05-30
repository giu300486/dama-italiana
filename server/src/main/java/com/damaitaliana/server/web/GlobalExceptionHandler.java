package com.damaitaliana.server.web;

import com.damaitaliana.server.auth.JwtVerificationException;
import com.damaitaliana.server.auth.RefreshTokenInvalidException;
import com.damaitaliana.server.exception.UserAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centralised mapping from exceptions to RFC 7807 {@code application/problem+json} responses.
 *
 * <p>Built up across the phase: Task 5.8 added {@link UserAlreadyExistsException} → 409, {@link
 * BadCredentialsException} → 401, and validation → 400; Task 5.9 added {@link
 * JwtVerificationException} / {@link RefreshTokenInvalidException} → 401 (the stateful token
 * failures the {@code /refresh} and {@code /logout} endpoints raise past the security filter); Task
 * 5.10 added {@link EntityNotFoundException} → 404 (the residual "valid token, user gone" case
 * {@code GET /v1/users/me} can hit). Task 5.12 closes it out with the catch-all {@link Exception} →
 * 500.
 *
 * <p>The planned {@code RateLimitExceededException → 429} handler is intentionally absent: rate
 * limiting is enforced at the edge by {@code RateLimitFilter} (Task 5.11), which writes its own 429
 * directly — a filter sits outside this advice's dispatch, so there is no exception to map here.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} is what makes the {@code Exception} catch-all
 * safe: the framework's own MVC exceptions (unreadable body → 400, wrong method → 405, unknown path
 * → 404, ...) keep their correct status and RFC 7807 shape via the base class, so only genuinely
 * unexpected failures fall through to the 500 handler. A naked {@code @ExceptionHandler(Exception)}
 * without the base class would swallow those client errors and report them all as 500.
 *
 * <p>Error bodies never carry a stack trace or internal detail. The 500 logs the cause server-side
 * at {@code WARN} and returns a fixed generic detail; the 401 token handler and the 409 conflict
 * use single generic details so an attacker cannot tell which token check tripped or which field
 * collided (account enumeration).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

  @ExceptionHandler({JwtVerificationException.class, RefreshTokenInvalidException.class})
  public ProblemDetail handleInvalidToken(RuntimeException ex, HttpServletRequest request) {
    return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid or expired token", request);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleEntityNotFound(
      EntityNotFoundException ex, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, "Not Found", "Resource not found", request);
  }

  /**
   * Last-resort handler for anything not mapped above and not a framework MVC exception. The cause
   * is logged at {@code WARN} (server-side, with stack trace) but the response body is a fixed
   * generic 500 — no class name, message, or trace leaks to the client.
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
    log.warn(
        "Unhandled exception processing {} {}", request.getMethod(), request.getRequestURI(), ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "An unexpected error occurred",
        request);
  }

  /**
   * Overrides the base class's bean-validation handler to attach an {@code errors[]} array of
   * {@code {field, message}} pairs alongside the standard 400 problem body.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(GlobalExceptionHandler::toFieldError)
            .toList();
    ProblemDetail body =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    body.setTitle("Bad Request");
    body.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
    body.setProperty("errors", errors);
    return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
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
