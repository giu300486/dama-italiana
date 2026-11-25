package com.damaitaliana.server.repository;

import com.damaitaliana.server.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 *
 * <p>The query methods are derived from method names by Spring Data; no custom JPQL/SQL is needed
 * for F5. Both {@code findByUsername} and {@code findByEmail} are O(1) lookups thanks to the UNIQUE
 * indexes on those columns (Flyway V1).
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);
}
