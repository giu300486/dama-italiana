package com.damaitaliana.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Identity contract of the JPA entities {@link UserEntity} and {@link RefreshTokenEntity}: two
 * instances are equal when they represent the same database row (same generated id), even when
 * loaded in different persistence contexts; an entity not yet persisted (no id) is equal only to
 * itself, never to another transient entity with the same field values. Sets and maps of detached
 * entities rely on this.
 *
 * <p>JPA slice on the {@code test} profile (H2); each test rolls back. Usernames use the {@code
 * ident_} prefix to stay clear of the other test classes sharing the H2 database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class EntityIdentitySliceTest {

  @Autowired private TestEntityManager entityManager;

  @Test
  void user_loadedTwiceFromTheSameRow_isEqualWithTheSameHashCode() {
    UserEntity saved = entityManager.persistFlushFind(newUser("ident_u1"));
    entityManager.clear();

    UserEntity reloaded = entityManager.find(UserEntity.class, saved.getId());

    assertThat(reloaded).isNotSameAs(saved);
    assertThat(reloaded).isEqualTo(saved);
    assertThat(reloaded.hashCode()).isEqualTo(saved.hashCode());
  }

  @Test
  void user_fromDifferentRows_isNotEqual() {
    UserEntity first = entityManager.persistFlushFind(newUser("ident_u2"));
    UserEntity second = entityManager.persistFlushFind(newUser("ident_u3"));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void transientUser_isEqualOnlyToItself() {
    UserEntity transientUser = newUser("ident_u4");
    UserEntity sameFields = newUser("ident_u4");

    assertThat(transientUser).isEqualTo(transientUser);
    assertThat(transientUser).isNotEqualTo(sameFields);
    assertThat(transientUser).isNotEqualTo(null);
    assertThat(transientUser).isNotEqualTo("ident_u4");
  }

  @Test
  void refreshToken_loadedTwiceFromTheSameRow_isEqualWithTheSameHashCode() {
    UserEntity user = entityManager.persistFlushFind(newUser("ident_r1"));
    RefreshTokenEntity saved = entityManager.persistFlushFind(newToken(user, "a"));
    entityManager.clear();

    RefreshTokenEntity reloaded = entityManager.find(RefreshTokenEntity.class, saved.getId());

    assertThat(reloaded).isNotSameAs(saved);
    assertThat(reloaded).isEqualTo(saved);
    assertThat(reloaded.hashCode()).isEqualTo(saved.hashCode());
  }

  @Test
  void refreshToken_fromDifferentRows_isNotEqual() {
    UserEntity user = entityManager.persistFlushFind(newUser("ident_r2"));
    RefreshTokenEntity first = entityManager.persistFlushFind(newToken(user, "b"));
    RefreshTokenEntity second = entityManager.persistFlushFind(newToken(user, "c"));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void transientRefreshToken_isEqualOnlyToItself() {
    UserEntity user = newUser("ident_r3");
    RefreshTokenEntity transientToken = newToken(user, "d");
    RefreshTokenEntity sameFields = newToken(user, "d");

    assertThat(transientToken).isEqualTo(transientToken);
    assertThat(transientToken).isNotEqualTo(sameFields);
    assertThat(transientToken).isNotEqualTo(null);
    assertThat(transientToken).isNotEqualTo(user);
  }

  private static UserEntity newUser(String username) {
    return new UserEntity(username, username + "@ident.local", "hash", Role.USER);
  }

  /** Token hash is VARCHAR(64) UNIQUE: a 64-char hex-like string built from one character. */
  private static RefreshTokenEntity newToken(UserEntity user, String hashChar) {
    return new RefreshTokenEntity(user, hashChar.repeat(64), Instant.parse("2030-01-01T00:00:00Z"));
  }
}
