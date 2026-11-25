/**
 * Domain exceptions raised by the services and mapped to HTTP status codes by {@link
 * com.damaitaliana.server.web.GlobalExceptionHandler}.
 *
 * <p>{@link com.damaitaliana.server.exception.UserAlreadyExistsException} becomes a 409 with a
 * generic detail that does not reveal whether the username or the email was taken. Token-related
 * exceptions live in {@code auth}.
 */
package com.damaitaliana.server.exception;
