package com.damaitaliana.server.entity;

/**
 * Authorization role of a user.
 *
 * <p>Persisted as {@code VARCHAR(32)} in the {@code users.role} column, with a CHECK constraint
 * accepting only the three values listed here (see Flyway V1 migration + CR-F5-001). JPA maps the
 * enum via {@code @Enumerated(EnumType.STRING)} so the database value is always the enum name.
 *
 * <p>Adding a new role requires: (1) extending this enum, (2) a Flyway migration that updates the
 * CHECK constraint to accept the new value, (3) a SPEC update.
 */
public enum Role {
  USER,
  TOURNAMENT_ADMIN,
  SYSTEM_ADMIN
}
