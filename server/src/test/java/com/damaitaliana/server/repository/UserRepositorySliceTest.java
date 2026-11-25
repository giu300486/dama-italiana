package com.damaitaliana.server.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.entity.UserEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * JPA slice test for {@link UserRepository}.
 *
 * <p>Profilo {@code test} (H2 in-memory MySQL-compat mode). {@code @DataJpaTest} loads only the JPA
 * layer, wraps each test in a transaction that rolls back at the end, and lets Flyway apply V1
 * before tests run. {@code Replace.NONE} prevents Spring Boot from substituting our configured H2
 * datasource with a generic embedded one.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class UserRepositorySliceTest {

  @Autowired private UserRepository repository;
  @Autowired private TestEntityManager entityManager;

  @Test
  void save_persistsWithGeneratedIdAndCreatedAt() {
    UserEntity user = new UserEntity("alice", "alice@slicetest.local", "hash", Role.USER);

    UserEntity saved = repository.save(user);
    entityManager.flush();

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getLastLoginAt()).isNull();
    assertThat(saved.getRole()).isEqualTo(Role.USER);
  }

  @Test
  void findByUsername_returnsUser_whenExists() {
    repository.save(new UserEntity("bob", "bob@slicetest.local", "hash", Role.TOURNAMENT_ADMIN));
    entityManager.flush();

    Optional<UserEntity> found = repository.findByUsername("bob");

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("bob@slicetest.local");
    assertThat(found.get().getRole()).isEqualTo(Role.TOURNAMENT_ADMIN);
  }

  @Test
  void findByUsername_returnsEmpty_whenNotExists() {
    assertThat(repository.findByUsername("ghost")).isEmpty();
  }

  @Test
  void findByEmail_returnsUser_whenExists() {
    repository.save(new UserEntity("carol", "carol@slicetest.local", "hash", Role.SYSTEM_ADMIN));
    entityManager.flush();

    Optional<UserEntity> found = repository.findByEmail("carol@slicetest.local");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("carol");
  }

  @Test
  void existsByUsername_returnsTrue_whenExists() {
    repository.save(new UserEntity("dave", "dave@slicetest.local", "hash", Role.USER));
    entityManager.flush();

    assertThat(repository.existsByUsername("dave")).isTrue();
    assertThat(repository.existsByUsername("nobody")).isFalse();
  }

  @Test
  void existsByEmail_returnsTrue_whenExists() {
    repository.save(new UserEntity("eve", "eve@slicetest.local", "hash", Role.USER));
    entityManager.flush();

    assertThat(repository.existsByEmail("eve@slicetest.local")).isTrue();
    assertThat(repository.existsByEmail("nobody@slicetest.local")).isFalse();
  }

  @Test
  void save_failsWithDuplicateUsername() {
    repository.save(new UserEntity("frank", "frank1@slicetest.local", "hash", Role.USER));
    entityManager.flush();

    assertThatThrownBy(
            () -> {
              repository.save(new UserEntity("frank", "frank2@slicetest.local", "hash", Role.USER));
              entityManager.flush();
            })
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void save_failsWithDuplicateEmail() {
    repository.save(new UserEntity("grace1", "grace@slicetest.local", "hash", Role.USER));
    entityManager.flush();

    assertThatThrownBy(
            () -> {
              repository.save(new UserEntity("grace2", "grace@slicetest.local", "hash", Role.USER));
              entityManager.flush();
            })
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void roleEnumIsPersistedAsString() {
    UserEntity saved =
        repository.save(
            new UserEntity("henry", "henry@slicetest.local", "hash", Role.SYSTEM_ADMIN));
    entityManager.flush();
    entityManager.clear();

    UserEntity reloaded = repository.findById(saved.getId()).orElseThrow();
    assertThat(reloaded.getRole()).isEqualTo(Role.SYSTEM_ADMIN);
  }
}
