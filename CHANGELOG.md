# Changelog

Tutte le modifiche significative al progetto sono documentate qui.

Il formato è basato su [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) e il progetto adotta [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `plans/PLAN-fase-1.md` (sotto-fase PIANIFICA Fase 1, approvata 2026-04-28).
- Fase 1 / Task 1.1 — `com.damaitaliana.shared.notation.FidNotation`: bijezione `Square ↔ 1..32` in orientamento standard FID (ADR-020), parsing/format mosse (`12-16`, `12x19`, `12x19x26`), record interno `ParsedMove`. 59 test in `FidNotationTest`.
- Fase 1 / Task 1.1 — `com.damaitaliana.shared.domain.Square` (minimal): record `(file, rank)` con validazione `[0,7]` e `isDark()`. Sarà esteso al Task 1.2.

### Changed

- **Modello branch git**: adozione di **GitFlow leggero** (`CLAUDE.md` §4.3-§4.4). `main` = production / tag, `develop` = integrazione e default branch su GitHub, branch effimeri `feature/<fase>-<topic>` e `fix/review-N-F-<id>` staccati da `develop` e mergiati `--no-ff`. Tag delle fasi (`v0.<fase>.0`) sul commit di merge in `main` (eccezione: `v0.0.0` taggato direttamente su `main` prima dell'introduzione del modello GitFlow).
- README sezione "Convenzioni": riflette il nuovo modello branch.

### Removed

### Fixed

---

## [0.0.0] — 2026-04-28

Tag git: `v0.0.0` (commit `e68335f`). Chiusura della **Fase 0 — Setup infrastruttura** della roadmap (`SPEC.md` §16).

### Added

- File di workflow SDD: `AI_CONTEXT.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, `README.md` (Fase 0, Task 0.1).
- Skeleton matrice di tracciabilità `tests/TRACEABILITY.md` (vuota, popolata dalla Fase 1).
- Struttura directory di workflow `plans/`, `reviews/`, `tests/` con `.gitkeep`.
- `.gitignore` per Java/Maven/IDE/OS, `.editorconfig` con preset 2-spazi Java, `.gitattributes` con normalizzazione LF.
- Parent POM `pom.xml` con BOM Spring Boot 3.4.5 + Testcontainers + Ikonli (Fase 0, Task 0.2).
- `dependencyManagement` per JavaFX 21.0.5, JmDNS 3.5.12, springdoc 2.6.0, TestFX 4.0.18, logstash-logback-encoder 7.4, JJWT 0.12.6.
- `pluginManagement` con maven-compiler 3.13 (release=21), Spotless 2.43 con google-java-format 1.22 (style GOOGLE, 2 spazi), JaCoCo 0.8.12 (`haltOnFailure=false` in Fase 0), SpotBugs 4.8 (threshold High), Enforcer 3.5 (Maven ≥3.9, Java 21, dependencyConvergence).
- Plugin Enforcer, Spotless e SpotBugs attivi a livello parent (ereditati da tutti i moduli).
- Modulo `shared` (Task 0.3): solo Jackson + JUnit 5 + AssertJ. Vincolo "no Spring/JavaFX/JPA/WebSocket" documentato nel POM.
- Modulo `core-server` (Task 0.4): `shared` + `spring-context` (DI) + `spring-messaging` (DTO STOMP) + Mockito. Vincolo "no Tomcat/Jetty/JPA" documentato nel POM.
- Modulo `client` (Task 0.5): `shared` + `core-server` + Spring Boot starter (no -web) + Spring Boot WebSocket starter con **Tomcat escluso e Jetty incluso** + JavaFX 21 (controls/fxml/graphics/media) + Ikonli (Material 2 + FontAwesome 5) + JmDNS + TestFX.
- Modulo `server` (Task 0.6): Spring Boot completo (web/websocket/security/data-jpa/validation/actuator) + MySQL connector + Flyway + Caffeine + JJWT + springdoc-openapi + micrometer-registry-prometheus + logstash-logback-encoder + Testcontainers MySQL + H2 (test).
- `application.yml` minimal del server con datasource via env var (default `localhost:3306`).
- Smoke test per ogni modulo (`<Modulo>SmokeTest`), da rimuovere quando ogni modulo avrà test reali.
- `package-info.java` per ogni modulo (Task 0.9): garantisce `target/classes` per JaCoCo report; documenta i sotto-package previsti dalle fasi successive e i vincoli architetturali (CLAUDE.md §8).
- ADR-018 (MySQL locale come ambiente dev, Docker Compose rimosso).
- ADR-019 (CI GitHub Actions disattivata per scelta in questa fase).
- `plans/PLAN-fase-0.md` (sotto-fase PIANIFICA).
- `reviews/REVIEW-fase-0.md` (sotto-fase REVIEW): 13 finding totali, 6 RESOLVED, 7 ACKNOWLEDGED, nessuno bloccante.
- `tests/TEST-PLAN-fase-0.md` (sotto-fase TEST): documenta la natura infrastrutturale della fase e la validazione della Definition of Done.

### Changed

- REVIEW closure: 6 fix applicati ai findings:
  - F-001 (CODE_QUALITY Medium): aggiunta regola `<dependencyConvergence/>` al maven-enforcer-plugin.
  - F-002 (Low): rimossa configurazione vestigiale Lombok da `spring-boot-maven-plugin` in `server/pom.xml`.
  - F-003 (Low): rimosso default vuoto da `${DB_PASSWORD:}` in `application.yml`.
  - F-004 (Low): SpotBugs attivato a livello parent `<build><plugins>` (gating globale).
  - F-007 (Low): commento `TODO Fase 3` aggiunto a `javafx-maven-plugin` in `client/pom.xml`.
  - F-008 (Low): rimossa voce ridondante `jacoco.exec` da `.gitignore`.

### Removed

- `docker-compose.yml` e `.env.example` durante revisione post-feedback utente (ADR-018).

### Fixed

- Parent POM: import non valido `io.jsonwebtoken:jjwt-bom` (artifact non pubblicato) sostituito con dichiarazione esplicita di `jjwt-api`, `jjwt-impl`, `jjwt-jackson` in `dependencyManagement`.
