/**
 * Custom metrics exposed on {@code /actuator/prometheus} (NFR-O-01).
 *
 * <p>{@link com.damaitaliana.server.observability.AuthMetrics} owns the counters {@code
 * auth_login_attempts_total{result}} and {@code auth_register_attempts_total} and the gauge {@code
 * users_registered}. The gauge is not called {@code users_total} because the Prometheus client
 * strips the {@code _total} suffix from gauges.
 */
package com.damaitaliana.server.observability;
