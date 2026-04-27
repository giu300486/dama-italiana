# Changelog

Tutte le modifiche significative al progetto sono documentate qui.

Il formato è basato su [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) e il progetto adotta [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- File di workflow SDD: `AI_CONTEXT.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, `README.md` (Fase 0, Task 0.1).
- Skeleton matrice di tracciabilità `tests/TRACEABILITY.md` (vuota, popolata dalla Fase 1).
- Struttura directory di workflow `plans/`, `reviews/`, `tests/` con `.gitkeep`.
- `.gitignore` per Java/Maven/IDE/OS, `.editorconfig` con preset 2-spazi Java, `.gitattributes` con normalizzazione LF.
- Parent POM `pom.xml` con BOM Spring Boot 3.4.5 + Testcontainers + Ikonli + JJWT (Fase 0, Task 0.2).
- `dependencyManagement` per JavaFX 21.0.5, JmDNS 3.5.12, springdoc 2.6.0, TestFX 4.0.18, logstash-logback-encoder 7.4.
- `pluginManagement` con maven-compiler 3.13 (release=21), Spotless 2.43 con google-java-format 1.22 (style GOOGLE, 2 spazi), JaCoCo 0.8.12 (`haltOnFailure=false` in Fase 0), SpotBugs 4.8 (threshold High), Enforcer 3.5 (Maven ≥3.9, Java 21).
- Plugin Enforcer e Spotless attivi a livello parent (ereditati da tutti i moduli).
- Modulo `shared` (Fase 0, Task 0.3): solo Jackson + JUnit 5 + AssertJ. Vincolo "no Spring/JavaFX/JPA/WebSocket" documentato nel POM.
- Modulo `core-server` (Fase 0, Task 0.4): `shared` + `spring-context` (DI) + `spring-messaging` (DTO STOMP) + Mockito. Vincolo "no Tomcat/Jetty/JPA" documentato nel POM.
- Modulo `client` (Fase 0, Task 0.5): `shared` + `core-server` + Spring Boot starter (no -web) + Spring Boot WebSocket starter con **Tomcat escluso e Jetty incluso** + JavaFX 21 (controls/fxml/graphics/media) + Ikonli (Material 2 + FontAwesome 5) + JmDNS + TestFX.
- Modulo `server` (Fase 0, Task 0.6): Spring Boot completo (web/websocket/security/data-jpa/validation/actuator) + MySQL connector + Flyway + Caffeine + JJWT + springdoc-openapi + micrometer-registry-prometheus + logstash-logback-encoder + Testcontainers MySQL + H2 (test).
- `application.yml` minimal del server con datasource via env var (default → MySQL Docker su porta 3307).
- Smoke test per ogni modulo (`<Modulo>SmokeTest`), da rimuovere quando ogni modulo avrà test reali.
- `docker-compose.yml` (Fase 0, Task 0.7): MySQL 8.0 (porta host **3307** per non confliggere col MySQL locale dell'utente) con healthcheck e volume persistente; Adminer su 8081 con `ADMINER_DEFAULT_SERVER=mysql`. `.env.example` committato; `.env` in gitignore.
- CI GitHub Actions `.github/workflows/ci.yml` (Fase 0, Task 0.8): job `build` (mvn clean verify + upload JaCoCo HTML), `lint` (spotless:check), `sast` (SpotBugs via verify -DskipTests + upload report). Trigger su push/PR su `main` e `workflow_dispatch`. Cache Maven attiva. Concurrency cancella run obsoleti.
- `package-info.java` per ogni modulo (Fase 0, Task 0.9): garantisce che `target/classes` esista e che JaCoCo generi il report; documenta i sotto-package previsti dalle fasi successive e i vincoli architetturali (CLAUDE.md §8). Sostituisce i `.gitkeep` precedenti in `src/main/java`.

### Changed

- Parent POM: rimosso `<dependency>jjwt-bom</dependency>` (BOM JJWT non esiste); le tre artifact `jjwt-api`/`jjwt-impl`/`jjwt-jackson` sono ora dichiarate singolarmente in `dependencyManagement` con `${jjwt.version}`.
- `application.yml` server: default `DB_URL` ora `jdbc:mysql://localhost:3306/dama_italiana` (era `:3307`); allineato all'uso di MySQL locale.
- Workflow GitHub Actions rinominato in `.github/workflows/ci.yml.disabled` (ADR-019). GitHub Actions ignora estensioni diverse da `.yml`/`.yaml`: il workflow è preservato ma non viene eseguito anche in caso di push remoto. Riattivabile via `git mv ci.yml.disabled ci.yml`.
- README riscritto: rimossa sezione Docker Compose; aggiunta sezione "Continuous Integration" con stato CI disattivata, validazione locale equivalente via `mvn verify`, e proposte per CI/CD self-hosted future (Gitea Actions, Drone/Woodpecker).
- ARCHITECTURE.md: aggiunti **ADR-018** (MySQL locale come ambiente dev, Docker rimosso) e **ADR-019** (CI disattivata).

### Removed

- `docker-compose.yml` e `.env.example` (ADR-018). Il developer attuale usa il MySQL locale gestito via Workbench/DBeaver. I file sono recuperabili dalla git history (commit `b355823`).

### Fixed

_(nessuna voce in Fase 0)_
