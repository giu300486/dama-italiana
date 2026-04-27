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

### Changed

### Fixed

### Removed
