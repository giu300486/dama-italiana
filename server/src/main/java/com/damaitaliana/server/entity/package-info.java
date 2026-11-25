/**
 * JPA entities mapped on the schema created by Flyway (ADR-046).
 *
 * <p>{@link com.damaitaliana.server.entity.UserEntity} maps {@code users}, {@link
 * com.damaitaliana.server.entity.RefreshTokenEntity} maps {@code refresh_tokens}, and {@link
 * com.damaitaliana.server.entity.Role} is stored as a string matching the {@code users.role} CHECK
 * constraint. Hibernate only validates the schema ({@code ddl-auto: validate}); it never creates or
 * alters tables.
 */
package com.damaitaliana.server.entity;
