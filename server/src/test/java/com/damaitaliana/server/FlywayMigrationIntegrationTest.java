package com.damaitaliana.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for Flyway V1 schema on H2 (fast loop, no Docker).
 *
 * <p>Profilo {@code test} (H2 in MySQL-compat mode). Asserts: (a) the two tables exist with the
 * expected columns; (b) the {@code chk_users_role} CHECK constraint rejects invalid values and
 * accepts the three valid roles (CR-F5-001); (c) the {@code refresh_tokens} foreign key rejects
 * orphaned {@code user_id} insertions.
 *
 * <p>The MySQL-real counterpart is {@link FlywayMigrationMysqlIntegrationTest} (slow tag).
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void usersTableExistsWithAllExpectedColumns() {
    Set<String> columns = columnsOf("users");
    assertThat(columns)
        .containsExactlyInAnyOrder(
            "id", "username", "email", "password_hash", "role", "created_at", "last_login_at");
  }

  @Test
  void refreshTokensTableExistsWithAllExpectedColumns() {
    Set<String> columns = columnsOf("refresh_tokens");
    assertThat(columns)
        .containsExactlyInAnyOrder(
            "id", "user_id", "token_hash", "expires_at", "revoked_at", "created_at");
  }

  @Test
  void usersRoleCheckConstraintAcceptsAllThreeValidValues() {
    insertUser("flymig-user-alice", "flymig-alice@example.com", "USER");
    insertUser("flymig-user-bob", "flymig-bob@example.com", "TOURNAMENT_ADMIN");
    insertUser("flymig-user-carol", "flymig-carol@example.com", "SYSTEM_ADMIN");

    // Scoped count to the rows this test just inserted -- the shared in-memory H2 instance
    // may contain rows committed by other @SpringBootTest classes that ran before this one
    // (see AI_CONTEXT note: H2 cross-class state pollution, deferred to REVIEW F5).
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username LIKE 'flymig-%'", Long.class);
    assertThat(count).isEqualTo(3L);
  }

  @Test
  void usersRoleCheckConstraintRejectsInvalidValue() {
    assertThatThrownBy(() -> insertUser("user-eve", "eve@example.com", "ADMIN"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void refreshTokensForeignKeyRejectsOrphanedUserId() {
    long nonExistentUserId = 999_999L;
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
                    nonExistentUserId,
                    SAMPLE_HASH_A,
                    Timestamp.valueOf("2030-01-01 00:00:00")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void refreshTokensCascadeOnUserDelete() {
    insertUser("user-dave", "dave@example.com", "USER");
    Long userId =
        jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, "user-dave");
    jdbc.update(
        "INSERT INTO refresh_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
        userId,
        SAMPLE_HASH_B,
        Timestamp.valueOf("2030-01-01 00:00:00"));

    jdbc.update("DELETE FROM users WHERE id = ?", userId);

    Long remaining =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Long.class, userId);
    assertThat(remaining).isZero();
  }

  private void insertUser(String username, String email, String role) {
    jdbc.update(
        "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)",
        username,
        email,
        "$2a$12$placeholderHashOnlyForSchemaTestingNotForActualAuth..",
        role);
  }

  private Set<String> columnsOf(String tableName) {
    List<String> rows =
        jdbc.queryForList(
            "SELECT LOWER(column_name) FROM information_schema.columns "
                + "WHERE LOWER(table_name) = ? AND LOWER(table_schema) = LOWER(SCHEMA())",
            String.class,
            tableName.toLowerCase());
    return new HashSet<>(rows);
  }

  // Two distinct 64-char hex strings (SHA-256 sized) used as fake refresh-token hashes.
  // The schema constrains token_hash to VARCHAR(64) UNIQUE, so each test that inserts a token
  // must use a non-colliding 64-char value.
  private static final String SAMPLE_HASH_A =
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String SAMPLE_HASH_B =
      "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
}
