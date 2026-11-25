package com.damaitaliana.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test for Flyway V1 schema on real MySQL 8 (Testcontainers, slow tag).
 *
 * <p>Re-validates the same schema invariants of {@link FlywayMigrationIntegrationTest} on a real
 * MySQL instance to catch H2/MySQL portability gaps (e.g. ENUM handling, CHECK enforcement
 * pre-MySQL-8.0.16, charset defaults, AUTO_INCREMENT semantics).
 *
 * <p>Inherits the {@code it} active profile, the {@code @SpringBootTest} bootstrap and the shared
 * {@link AbstractMysqlIntegrationTest#MYSQL} container; ServiceConnection wires the datasource.
 * Runs only with {@code mvn -pl server verify} (no {@code -DexcludedGroups=slow}) or {@code mvn
 * clean verify} root.
 */
class FlywayMigrationMysqlIntegrationTest extends AbstractMysqlIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v1MigrationCreatesBothTablesOnRealMysql() {
    Set<String> userColumns = columnsOf("users");
    assertThat(userColumns)
        .containsExactlyInAnyOrder(
            "id", "username", "email", "password_hash", "role", "created_at", "last_login_at");

    Set<String> tokenColumns = columnsOf("refresh_tokens");
    assertThat(tokenColumns)
        .containsExactlyInAnyOrder(
            "id", "user_id", "token_hash", "expires_at", "revoked_at", "created_at");
  }

  @Test
  void usersRoleCheckConstraintIsEnforcedByMysql() {
    jdbc.update(
        "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)",
        "alice-mysql",
        "alice-mysql@example.com",
        "$2a$12$placeholderHashOnlyForSchemaTestingNotForActualAuth..",
        "USER");

    // Spring's default SQLErrorCodeSQLExceptionTranslator does NOT map MySQL 8 error code 3819
    // (CHECK constraint violation, introduced in MySQL 8.0.16) to DataIntegrityViolationException
    // — it surfaces as UncategorizedSQLException. The H2 counterpart maps correctly. We assert on
    // the common parent DataAccessException + the message mentioning the constraint name so the
    // test stays portable across both substrates while still proving the CHECK is enforced.
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO users (username, email, password_hash, role) "
                        + "VALUES (?, ?, ?, ?)",
                    "eve-mysql",
                    "eve-mysql@example.com",
                    "$2a$12$placeholderHashOnlyForSchemaTestingNotForActualAuth..",
                    "ADMIN"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("chk_users_role");
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
}
