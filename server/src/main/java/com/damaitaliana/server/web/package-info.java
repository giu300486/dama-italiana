/**
 * Cross-cutting web concerns shared by every controller.
 *
 * <ul>
 *   <li>{@link com.damaitaliana.server.web.GlobalExceptionHandler}: maps every 4xx/5xx raised in
 *       the MVC dispatch to {@code application/problem+json} (RFC 7807), never exposing stack
 *       traces or internal details.
 *   <li>{@link com.damaitaliana.server.web.CorrelationIdFilter}: reads or generates {@code
 *       X-Correlation-Id}, stores it in the MDC and echoes it on the response.
 * </ul>
 *
 * <p>Errors written by servlet filters (401, 429) sit outside this dispatch and shape their own RFC
 * 7807 body.
 */
package com.damaitaliana.server.web;
