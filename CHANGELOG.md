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

### Changed

### Fixed

### Removed
