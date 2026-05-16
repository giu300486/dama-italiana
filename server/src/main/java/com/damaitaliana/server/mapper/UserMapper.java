package com.damaitaliana.server.mapper;

import com.damaitaliana.server.dto.UserResponse;
import com.damaitaliana.server.entity.UserEntity;

/**
 * Static conversion between {@link UserEntity} (persistence) and {@link UserResponse} (API).
 *
 * <p>Kept as a stateless utility class (private constructor) rather than a Spring bean — the
 * conversion has no dependencies, no need for DI overhead. Future mappers with cross-entity
 * resolution (e.g. a {@code MatchMapper} that needs a {@code UserRepository}) will be Spring beans.
 */
public final class UserMapper {

  private UserMapper() {
    // Utility class, not instantiable.
  }

  public static UserResponse toResponse(UserEntity entity) {
    return new UserResponse(
        entity.getId(),
        entity.getUsername(),
        entity.getEmail(),
        entity.getRole().name(),
        entity.getCreatedAt(),
        entity.getLastLoginAt());
  }
}
