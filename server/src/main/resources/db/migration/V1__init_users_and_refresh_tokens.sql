-- Flyway V1 - Auth core schema (Fase 5).
-- See ARCHITECTURE.md ADR-046 for versioning strategy and SPEC.md §8.4 (v2.4) for the
-- logical schema. F6 will add V2 (matches + match_events). F8 will add V3 (tournaments).
--
-- Portability notes: pure SQL standard syntax, compatible with both H2 (profilo `test`,
-- jdbc:h2:mem with MODE=MySQL) and MySQL 8.0+ (profili `dev`, `it`, `prod`). The CHECK
-- constraint on `users.role` is the result of CR-F5-001 (decisione utente §9.3 strada α):
-- VARCHAR + CHECK instead of MySQL ENUM for portability + Workbench-friendliness.

CREATE TABLE users (
  id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
  username        VARCHAR(50)  NOT NULL UNIQUE,
  email           VARCHAR(255) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  role            VARCHAR(32)  NOT NULL DEFAULT 'USER',
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at   TIMESTAMP    NULL,
  CONSTRAINT chk_users_role CHECK (role IN ('USER', 'TOURNAMENT_ADMIN', 'SYSTEM_ADMIN'))
);

CREATE TABLE refresh_tokens (
  id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT      NOT NULL,
  token_hash    VARCHAR(64) NOT NULL UNIQUE,
  expires_at    TIMESTAMP   NOT NULL,
  revoked_at    TIMESTAMP   NULL,
  created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Speeds up cleanup query for expired tokens (e.g. scheduled deletion of stale rows).
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
