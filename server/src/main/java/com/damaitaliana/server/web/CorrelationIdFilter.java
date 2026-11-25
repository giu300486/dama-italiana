package com.damaitaliana.server.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates a per-request correlation id (NFR-O-03, A5.17). Registered with the highest
 * precedence, so it runs ahead of the Spring Security filter chain and every other filter: the id
 * is in the MDC for every log line written while serving the request, including authentication
 * failures and rate-limit rejections.
 *
 * <p>The id comes from the {@code X-Correlation-Id} request header when the client sends a
 * well-formed one, otherwise a random UUID is generated. A client-supplied value is accepted only
 * if it matches {@code [A-Za-z0-9._-]{1,64}}: anything else (oversized, CR/LF, other characters) is
 * discarded and replaced, so a caller cannot inject forged lines into the logs through this header.
 *
 * <p>The id is echoed in the response header before the chain runs, so it is present on every
 * response, error responses written directly by filters included. The MDC key is removed in a
 * {@code finally} block: only this key, leaving any other MDC entry untouched, because the servlet
 * container reuses its worker threads.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** Request and response header carrying the correlation id. */
  public static final String HEADER = "X-Correlation-Id";

  /** MDC key under which the correlation id is stored for logging. */
  public static final String MDC_KEY = "correlationId";

  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = resolve(request.getHeader(HEADER));
    MDC.put(MDC_KEY, correlationId);
    response.setHeader(HEADER, correlationId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private static String resolve(String headerValue) {
    if (headerValue != null && VALID_ID.matcher(headerValue).matches()) {
      return headerValue;
    }
    return UUID.randomUUID().toString();
  }
}
