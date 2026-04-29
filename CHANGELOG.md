# Changelog

Tutte le modifiche significative al progetto sono documentate qui.

Il formato è basato su [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) e il progetto adotta [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Task 3.16** — Autosave + recovery prompt (Fase 3, sotto-fase IMPLEMENTA, branch `feature/3-ui-singleplayer`):
  - `client/persistence/AutosaveService.java` (`@Component`): wrap di `SaveService` vincolato a `AUTOSAVE_SLOT`, espone `writeAutosave/readAutosave/clearAutosave/autosaveExists`. `Clock` e `Supplier<RandomGenerator>` injectable per test (timestamp deterministici e RNG riproducibile).
  - `client/controller/SinglePlayerAutosaveTrigger.java` (`@Component`): impl di `AutosaveTrigger` che inoltra `onMoveApplied(SinglePlayerGame)` a `AutosaveService.writeAutosave`, swallows `UncheckedIOException` con log WARN per non bloccare il game loop. Spring popola automaticamente `Optional<AutosaveTrigger>` di `SinglePlayerController`: ogni mossa applicata produce ora un autosave atomico.
  - `MainMenuController` ridisegnato: nuova firma ctor `(SceneRouter, I18n, AutosaveService, GameSession, UserPromptService)`. `handleAutosavePrompt()` ritorna typed enum `PromptResult` (RESUMED/DISCARDED/SCHEMA_MISMATCH/IO_ERROR/NO_AUTOSAVE); ramo "yes" carica via `AutosaveService.readAutosave`, popola `GameSession`, naviga a `SceneId.BOARD`; ramo "no" cancella via `AutosaveService.clearAutosave`. `UnknownSchemaVersionException` (ADR-031) e `UncheckedIOException` mostrano toast localizzato e cancellano il file.
  - `SplashController` migrato a `AutosaveService.autosaveExists()` (rimosse `Path`/`Files`/`AUTOSAVE_FILENAME` hardcoded).
  - `BoardViewController.terminate()` migrato a `AutosaveService.clearAutosave()` (sostituisce `saveService.delete(AUTOSAVE_SLOT)` diretto).
  - **i18n** IT/EN: `autosave.toast.error.schema.title/content`, `autosave.toast.error.io.title/content`.
  - **22 test nuovi/modificati**: `AutosaveServiceTest` 8, `SinglePlayerAutosaveTriggerTest` 2, `AutosaveE2ETest` 3 (prompt on restart A3.5, clear on terminate, write failure tolerated), `SaveDialogSpringContextTest` +2 context-resolution per `AutosaveService` e `AutosaveTrigger` (lezione `feedback_spring_ui_tests`), `MainMenuControllerTest` riscritto su 8 metodi (3 click + 5 prompt branch), `SplashControllerTest` aggiornato, `BoardViewControllerTest` aggiornato.
  - `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS, 205 test totali (+22 vs Task 3.15), JaCoCo client gate ✅, SpotBugs 0 High, Spotless OK.

---

## [0.2.0] — 2026-04-28

Tag git: `v0.2.0`. Chiusura della **Fase 2 — IA nel modulo `shared.ai`** della roadmap (`SPEC.md` §16). Tutte e 4 le sotto-fasi (PIANIFICA / IMPLEMENTA / REVIEW / TEST) chiuse. Gating SPEC §16 Fase 2 ✅ PASSED (Campione ≥ 95/100 vs Principiante in 16:02 min). Branch `feature/2-ai` mergiato `--no-ff` in `develop` e poi in `main`.

### Added

- `plans/PLAN-fase-2.md` (sotto-fase PIANIFICA Fase 2, approvata in blocco 2026-04-28).
- **Fase 2 — IA nel modulo `shared.ai`** (sotto-fase IMPLEMENTA conclusa). Tre livelli funzionanti, virtual-thread cancellabile, transposition table, ~125 nuovi test. Branch di lavoro `feature/2-ai`, sequenza Tasks 2.1 ÷ 2.13 + 2.15.
  - **Task 2.1** — `com.damaitaliana.shared.ai.evaluation`: `Evaluator` interface, `EvaluationTerm` interface, `MaterialTerm` (man=100, king=300 cp per SPEC §12.1), `WeightedSumEvaluator` con record `WeightedTerm` e factory `defaultEvaluator()` (15 test).
  - **Task 2.2** — `MobilityTerm` (×5), `AdvancementTerm` (×2), `EdgeSafetyTerm` (×8), `CenterControlTerm` (×10) — i 4 termini residui SPEC §12.1; `defaultEvaluator()` ora compone tutti e 5 (19 test).
  - **Task 2.3** — `com.damaitaliana.shared.ai.search.MoveOrderer` interface + `StandardMoveOrderer` (capture-first → longer-capture-first → center-destination-first → FID-from-ascending) (7 test).
  - **Task 2.4** — `MinimaxSearch` (negamax con alpha-beta), `SearchResult`, mate-distance scoring `±(MATE_SCORE − plyFromRoot)`. Introdotti anche `CancellationToken` (interface + `never()`), `MutableCancellationToken`, `SearchCancelledException` (deviazione minore vs piano §2.5: necessari per testare `MinimaxSearch.cancellationStopsSearch`) (18 test).
  - **Task 2.5** — `IterativeDeepeningSearch` con PV-first (`PvFirstOrderer` package-private) e graceful cancellation; `CancellationToken` esteso con `deadline(Instant)`/`deadline(Instant, Clock)`/`composite(...)` (27 test).
  - **Task 2.6** — `ZobristHasher` deterministico (seed `0xDA4A172L`), `TranspositionTable` 2^20 entry always-replace, integrazione TT in `MinimaxSearch.negamax` con bound semantics `EXACT`/`LOWER_BOUND`/`UPPER_BOUND` e niente early-return al root. `IterativeDeepeningSearch` con costruttore TT-aware (22 test).
  - **Task 2.7** — `sealed interface AiEngine permits PrincipianteAi, EspertoAi, CampioneAi`, `AiLevel` enum, factory `forLevel(level, rng)`. Esperto e Principiante deviano dal piano usando `IterativeDeepeningSearch` per cooperative cancellation; Campione usa IDS+TT come da piano. Costanti `DEPTH`/`MAX_DEPTH`/`DEFAULT_TIMEOUT`/`NOISE_PROBABILITY` esposte (19 test).
  - **Task 2.8** — `VirtualThreadAiExecutor` su `Executors.newVirtualThreadPerTaskExecutor()`, `Submission` con graceful + hard cancel; deadline + thread-interrupt + manual token composti via `CancellationToken.composite(...)` (9 test).
  - **Task 2.9** — `AiTacticalPositionsTest` con 5 posizioni golden hardcoded in Java (mate-in-1 bianco, mate-in-1 nero via dama, cattura forzata, due-mosse, terminale).
  - **Task 2.10** — `AiTournamentSimulationTest` con sanity quick (5 partite) + gating `@Tag("slow")` 100 partite Campione vs Principiante (≥ 95 vittorie, A2.2). Determinismo via Principiante seed = `42 + gameIndex`.
  - **Task 2.11** — `AiPerformanceTest` `@Tag("performance")` per ogni livello: budget × 1.5 tolerance (PLAN-fase-2 §7.7).
  - **Task 2.13** — `shared/pom.xml` JaCoCo gate aggiunge regola `PACKAGE com.damaitaliana.shared.ai*` ≥ 85 % line + branch (PLAN-fase-2 §7.8). Gate F1 mantenuto.
  - **Task 2.15** — ADR-024 (architettura `AiEngine` sealed + 3 livelli), ADR-025 (Evaluator modulare), ADR-026 (Zobrist + TT), ADR-027 (cancellation cooperativa), ADR-028 (rumore Principiante deterministico) in `ARCHITECTURE.md`. Matrice tracciabilità popolata con FR-SP-02, NFR-P-02 (perf gate), AC §17.1.1 parziale, e i 13 acceptance criteria operativi della Fase 2.

### Changed

- **F1 hardening (`ItalianRuleEngine.isThreefoldRepetition`)**: replay-from-initial ora cattura `IllegalMoveException` e ritorna `false` quando la history non è coerente con `GameState.initial()` (es. stati hand-built per test fixture o esplorazione AI). Conservativo: "no repetition" è la risposta sicura per il chiamante. Coerente con la limitazione documentata in ADR-021. Necessario perché il search di F2 esplora stati hand-built fino a depth 8 e ne calcola lo status ricorsivamente.
- **REVIEW-fase-2 closure**: 7 finding chiusi — 2 RESOLVED (F-004 Javadoc su `ZobristHasher.hashAfterMove` non implementato, F-005 Javadoc su `MoveOrderer.order(state)` parametro intenzionalmente unused), 5 ACKNOWLEDGED (F-001 + F-002 deferred-F4 con la futura ottimizzazione Zobrist-incremental di `isThreefoldRepetition`, F-003 deferred-F3 by design — AC §17.1.1 richiede UI E2E, F-006 + F-007 design intentional).
- **TEST-PLAN-fase-2 closure**: 391 test totali (387 default + 1 `slow` + 3 `performance`); coverage modulo 97.3% line / 95.5% branch, package `rules` 96.2% / 95.7%, package `ai` 97.7% / 96.2%, tutti sopra i gate (90% / 90% / 85%). Gating A2.2 PASSED in 16:02 min. Limiti tracciati per F4 (Zobrist-incremental).

---

## [0.1.0] — 2026-04-28

Tag git: `v0.1.0` (commit `2f6a14e`). Chiusura della **Fase 1 — Dominio e regole nel modulo `shared`** della roadmap (`SPEC.md` §16). Tutte e 4 le sotto-fasi (PIANIFICA / IMPLEMENTA / REVIEW / TEST) chiuse. Branch `feature/1-domain-and-rules` mergiato `--no-ff` in `develop` (`9fb533f`) e poi in `main` (`2f6a14e`).

- `plans/PLAN-fase-1.md` (sotto-fase PIANIFICA Fase 1, approvata 2026-04-28).
- **Fase 1 — Dominio e regole nel modulo `shared`** (sotto-fase IMPLEMENTA conclusa). 245 test su 4 sotto-package, JaCoCo ≥ 90% modulo + ≥ 90% package `rules`, SpotBugs 0 High.
  - **Task 1.1** — `com.damaitaliana.shared.notation.FidNotation`: bijezione `Square ↔ 1..32` in orientamento standard FID (ADR-020), parsing/format mosse, record `ParsedMove` (59 test).
  - **Task 1.2** — `com.damaitaliana.shared.domain`: `Square`, `Color`, `PieceKind`, `Piece`, `Board` (immutabile, `initial()` con 24 pezzi sulle case scure rank 0-2/5-7), `Move` sealed + `SimpleMove` + `CaptureSequence`, `GameStatus` esteso (6 voci con `isOngoing`/`isWin`/`isDraw`), `GameState` (58 test).
  - **Task 1.3** — `com.damaitaliana.shared.rules.RuleEngine` interface + `IllegalMoveException` + `ItalianRuleEngine` movimenti semplici (22 test).
  - **Task 1.4** — Catture singole con la regola "pedina non cattura dama" (SPEC §3.3) e regola della cattura obbligatoria (14 test).
  - **Task 1.5** — Sequenze multi-jump via DFS con anti-loop (set di pezzi catturati) e stop alla promozione mid-sequenza per le pedine (SPEC §3.5) (10 test).
  - **Task 1.6** — Le 4 leggi italiane di precedenza (SPEC §3.4): quantità → qualità → precedenza dama → prima dama, ognuna come filtro privato isolato (7 test).
  - **Task 1.7** — Promozione su `applyMove`: una pedina che termina su rank 7 (bianco) o rank 0 (nero) viene promossa a dama prima di essere posizionata (7 test).
  - **Task 1.8** — `computeStatus` completo (SPEC §3.6): vittoria per assenza pezzi, vittoria per stallo (sconfitta del bloccato in variante italiana), patta per regola 40 mosse (≥80 ply), patta per triplice ripetizione via replay della history da `GameState.initial()` con record privato `PositionKey` (ADR-021), tramite metodo `applyCore` non ricorsivo (11 test).
  - **Task 1.9** — Test corpus parametrizzato `test-positions.json` con **48 posizioni** distribuite per categoria (CLAUDE.md §2.4.4); schema disgiunto `whiteMen`/`whiteKings`/`blackMen`/`blackKings` (ADR-022); loader Jackson con validazione + `RuleEngineCorpusTest` parametrizzato + `RuleEngineCorpusSchemaTest` + `RuleEngineCorpusCoverageTest` (4 + 3 + 48 = 55 test totali).
  - **Task 1.10** — `EndToEndGameApiTest` con 3 partite scriptate via API pura (white-wins, black-wins per stallo, draw per regola 40 mosse) (3 test).
  - **Task 1.11** — `shared/pom.xml`: JaCoCo `haltOnFailure=true`, regole `BUNDLE` ≥ 90% line+branch e `PACKAGE com.damaitaliana.shared.rules` ≥ 90% line+branch. `SharedSmokeTest` rimosso.
  - **Task 1.12** — `package-info.java` per `shared`, `shared.domain`, `shared.notation`, `shared.rules`. ADR-020/021/022 in `ARCHITECTURE.md`. Matrice tracciabilità `tests/TRACEABILITY.md` popolata con FR-SP-04, FR-SP-05, FR-SP-09, FR-COM-01, FR-RUL-01, NFR-M-01, NFR-M-04, AC §17.1.6/7/8/§17.2.4/5 e gli 11 acceptance criteria operativi della Fase 1.

### Changed

- **Modello branch git**: adozione di **GitFlow leggero** (`CLAUDE.md` §4.3-§4.4). `main` = production / tag, `develop` = integrazione e default branch su GitHub, branch effimeri `feature/<fase>-<topic>` e `fix/review-N-F-<id>` staccati da `develop` e mergiati `--no-ff`. Tag delle fasi (`v0.<fase>.0`) sul commit di merge in `main` (eccezione: `v0.0.0` taggato direttamente su `main` prima dell'introduzione del modello GitFlow).
- README sezione "Convenzioni": riflette il nuovo modello branch.
- **SPEC §8.1 — `GameStatus` esteso a 6 voci** (CR-001 della REVIEW-fase-1, opzione A). L'enum diventa `{ ONGOING, WHITE_WINS, BLACK_WINS, DRAW_REPETITION, DRAW_FORTY_MOVES, DRAW_AGREEMENT }` con helper `isOngoing/isWin/isDraw`. Motivazione: la UI e il replay viewer (FR-RUL, FR-NET-09) devono distinguere il motivo della patta. ADR-023 documenta il rationale. Il codice `shared` era già allineato (Task 1.2).
- **REVIEW-fase-1 closure**: 7 finding chiusi — 4 RESOLVED (F-002 IllegalMoveException Javadoc, F-003 ItalianRuleEngine class Javadoc, F-004 legalMoves duplicate iteration, F-006 GameStatus extension ADR), 3 ACKNOWLEDGED (F-001 thin coverage del repetition test → F2, F-005 isThreefoldRepetition O(n²) → F2 con Zobrist, F-007 `// TODO Fase 3` in `client/pom.xml` fuori scope F1).
- **TEST-PLAN-fase-1 closure**: 245 test verdi, coverage modulo 96.7% line / 94.7% branch e package `rules` 95.7% line / 94.9% branch (entrambi sopra il gate 90%). 48 posizioni del corpus distribuite per le 14 categorie minime (CLAUDE.md §2.4.4). Limiti noti tracciati (REVIEW F-001 e F-005 deferred-F2).

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
