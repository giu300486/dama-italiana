package com.damaitaliana.core.repository;

import com.damaitaliana.core.match.UserRef;
import java.util.Optional;

/**
 * Port for user lookup. Skeleton in Fase 4: only the read paths needed by {@code MatchManager} to
 * resolve a {@link UserRef} from id or username. Fase 5 (server centrale) introduces the full user
 * lifecycle (registration, BCrypt password hashing, JWT) in the {@code server} module — that module
 * will provide a {@code JpaUserRepository} adapter.
 *
 * <p>Constraint (CLAUDE.md §8.8): plain Java interface — no JPA, no Spring Security.
 */
public interface UserRepository {

  /**
   * Look up by primary key. For Internet-authenticated users this is the {@code users.id} column
   * (SPEC §8.4); for anonymous LAN users the id is {@link UserRef#ANONYMOUS_LAN_ID} and lookup is
   * not meaningful — implementations may always return empty for that sentinel.
   */
  Optional<UserRef> findById(long id);

  /** Look up by username (case-sensitive in v1, SPEC §8.4 {@code users.username UNIQUE}). */
  Optional<UserRef> findByUsername(String username);
}
