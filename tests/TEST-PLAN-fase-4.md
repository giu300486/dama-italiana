# TEST PLAN — Fase 4: `core-server` skeleton

- **Data piano**: 2026-05-06.
- **Data finalizzazione**: 2026-05-06 (chiusura sotto-fase TEST).
- **SPEC version**: 2.2 (post CR-F3.5-001..005).
- **Branch**: `feature/4-core-server-skeleton`.
- **PLAN di riferimento**: [`plans/PLAN-fase-4.md`](../plans/PLAN-fase-4.md).
- **REVIEW di riferimento**: [`reviews/REVIEW-fase-4.md`](../reviews/REVIEW-fase-4.md) (chiusa 2026-05-06, commit `8f5c862`).
- **Stato**: FINAL.

---

## 1. Scope

Vedi `plans/PLAN-fase-4.md` §1.

In sintesi, F4 costruisce il modulo **`core-server`** come libreria di dominio **transport-agnostic** (CLAUDE.md §8.7-8.8): tournament engine + match manager + repository ports + adapter in-memory + event bus interno + STOMP-compatible publisher, **senza** Tomcat, Jetty, JPA, MySQL, JavaFX. Sotto test:

1. **Domain value types + sealed `MatchEvent` hierarchy** (Task 4.2): records `MatchId`/`UserRef`/`TimeControl`/`TournamentId`/`TournamentMatchRef`, enum `MatchStatus`/`MatchResult`/`EndReason`/`RejectionReason`/`TournamentStatus`/`TimeControlPreset`/`TournamentFormat`, `sealed interface MatchEvent` con 9 permits; ogni record valida null + `sequenceNo >= 0`, `MatchEnded` valida `result != UNFINISHED`.
2. **Repository ports + adapter in-memory** (Task 4.3-4.4): `MatchRepository`/`TournamentRepository`/`UserRepository` come Java interface (no JPA); `InMemoryMatchRepository`/`InMemoryTournamentRepository`/`InMemoryUserRepository` `@Component` con `ConcurrentHashMap` + `Collections.synchronizedList` + `synchronized (log)` block per atomic check+append della FR-COM-04 strict monotonic sequence.
3. **Event bus interno** (Task 4.5): `MatchEventBus` interface + `SpringMatchEventBus` impl che delega a `ApplicationEventPublisher` (ADR-038); `MatchEventEnvelope extends ApplicationEvent` con `transient` payload (silenzia SpotBugs `SE_BAD_FIELD`, l'envelope NON è serializzato — il bus è in-process).
4. **STOMP-compatible publisher** (Task 4.6): `StompCompatiblePublisher` interface + `LoggingStompPublisher @Component` SLF4J + `BufferingStompPublisher` test-scope per assertion (ADR-039). F6/F7 introdurranno il `WebSocketStompPublisher @Primary` reale.
5. **`Match` state machine + anti-cheat counter** (Task 4.7): transition validation `WAITING→ONGOING / ONGOING→{FINISHED, ABORTED}`; counter per-color `EnumMap<Color, Integer>` transient, reset su mossa legale, threshold 5 → forfait (ADR-040).
6. **`MatchManager @Service`** (Task 4.8): orchestrazione `applyMove`/`resign`/`offerDraw`/`respondDraw` con `ConcurrentHashMap<MatchId, Object> matchLocks` per per-match write serialization; ogni evento applicato attraversa `repo.appendEvent → bus.publish → stompPublisher.publishToTopic("/topic/match/{id}", event)`.
7. **Anti-cheat end-to-end** (Task 4.9): 3 test che coprono A4.4 (forfait WHITE su 5ª illegale, reset counter su mossa legale, per-player independence con Mockito-stubbed `RuleEngine`).
8. **`TournamentEngine` skeleton** (Task 4.10): `createTournament`/`registerParticipant`/`findById` funzionanti; `startTournament` + `BracketGenerator.generate` + `RoundRobinScheduler.schedule` lanciano `UnsupportedOperationException("deferred to F8/F9")` (PLAN risk R-2 mitigation).
9. **End-to-end Java API match test** (Task 4.11): `SoloMatchEndToEndTest` 3 metodi top-level — random game vs `ItalianRuleEngine` reale fino a stato terminale (seed 42, MAX_PLIES 500), replay-from-events reconstruction (fold da `GameState.initial()`), bus + STOMP broadcast verifica via Spring DI (`AnnotationConfigApplicationContext` + `BufferingStompPublisher @Primary`).
10. **ArchUnit anti-pattern check** (Task 4.12): 5 rule che bloccano import di `javafx..`, `org.springframework.boot.web..`, `org.apache.tomcat..`, `org.eclipse.jetty..`, `jakarta.persistence..`, `org.hibernate..`, e `match → tournament` (post Option F refactor `TournamentMatchRef` con UUID raw, ADR-042).
11. **Concurrency stress** (`@Tag("slow")`, scope sotto-fase TEST): `InMemoryRepositoryConcurrencyTest` 16 thread × {1000 match save, 10000 appendEvent su 64 match disjoint, 1000 appendEvent stesso match con caller-side lock} → no `ConcurrentModificationException`, sequence ancora monotonico per match (A4.12).
12. **Architecture invariants extension**: bidirezionalità package `match ↔ tournament` rotta lato match→tournament (Option F: `TournamentMatchRef` ora vive in `match` package con `UUID tournamentId` raw); tournament→match resta ammessa per `UserRef` + `TimeControl` (scelta SPEC §8.3, documentata in `Tournament.java` Javadoc).

**Out of scope** (deferred):
- **Trasporto reale STOMP/WebSocket**: F6 (server centrale) + F7 (LAN host).
- **JPA adapter** (`server` module): F6, riuserà `MatchRepositoryContractTest` come test contract.
- **Bracket generation reale** (single-elim, byes, brackets diseguali): F8.
- **Round-robin scheduling reale** (algoritmo Berger): F9.
- **`StandardTieBreakerPolicy`** (scontro diretto → Sonneborn-Berger → vittorie → sorteggio): F9.
- **Time control attivo** (clock + timeout): F6.
- **`MoveApplied` Jackson roundtrip completo**: F6 con custom Module per `Move` sealed + `Board` non-record.

**Modifiche al codice prod F4**:
- `core-server/`: 56 file prod nuovi + 1 modulo configurato (parent POM ha già `core-server` da F0).
- `shared/`/`client/`/`server/`: **invariati per costruzione** (F4 non li tocca; le 3 deviazioni `@ConditionalOnMissingBean` non usato + `spring-test` non aggiunto + spring-context only sono PLAN deviations già documentate task-by-task).

---

## 2. Strategia di test

Vedi `plans/PLAN-fase-4.md` §5 e CLAUDE.md §2.4.1 (piramide classica).

| Tipo | Cosa | Tooling |
|---|---|---|
| **Unit** | Domain types + sealed events + sealed switch exhaustiveness, repo adapter contract (`MatchRepositoryContractTest` riusabile), `MatchManager` flows (turn / illegal / terminal / resign / draw), `TournamentEngine` stub, anti-cheat counter, sequence monotonic | JUnit 5 + AssertJ + Mockito 5.14.2 (inline mock maker per sealed types) |
| **Integration (Spring DI)** | `SpringMatchEventBus` end-to-end con `@EventListener(MatchEventEnvelope)` via `AnnotationConfigApplicationContext`, `MatchManager + repo + bus + stomp` via `CoreServerConfiguration @ComponentScan`, `BufferingStompPublisher @Primary` test override (pattern `SoloMatchEndToEndTest#eventsBroadcastOnBusAndStomp`) | JUnit 5 + `spring-context` only (NO `spring-boot`, NO `spring-test` — invariante CLAUDE.md §8.7-8.8) |
| **E2E API** | `SoloMatchEndToEndTest` 3 metodi top-level — random game vs `ItalianRuleEngine` reale fino a terminale + replay reconstruction + bus/stomp broadcast via DI | JUnit 5 + Mockito + `AnnotationConfigApplicationContext` |
| **Architecture** | `CoreServerArchitectureTest` 5 rule ArchUnit (no JavaFX, no Boot Web, no Tomcat/Jetty, no JPA/Hibernate, match-not-depend-tournament) | ArchUnit 1.3.0 (test-scope, autorizzata da PLAN stop point §7.10) |
| **Stress (`@Tag("slow")`)** | `InMemoryRepositoryConcurrencyTest` 16 thread × {1000 match save, 10000 appendEvent disjoint, 1000 appendEvent contesa) | JUnit 5 `@Tag("slow")` + `Executors.newFixedThreadPool(16)` + `CountDownLatch` start gate |
| **Coverage** | Gate `core-server` ≥ 80% line + branch (NFR-M-01); 19 esclusioni record DTO/enum/`CoreServerConfiguration`/`MatchEventEnvelope`/`MatchNotFoundException` per misurare solo logica reale. Gate `haltOnFailure=true` post-REVIEW (commit `5320428`, F-001 RESOLVED) | JaCoCo 0.8.12 |
| **Regression** | Tutti F1+F2+F3+F3.5: corpus regole 53 posizioni (`RuleEngineCorpusTest`), gating IA Campione ≥95/100 vs Principiante (`AiTournamentSimulationTest @Tag("slow")`), 3 performance NFR-P-02 (`AiPerformanceTest @Tag("performance")`), 321 client (visual rework + audio + jpackage) verdi al closure | `mvn clean verify` root |

---

## 3. Coverage target

Da SPEC NFR-M-01 e PLAN-fase-4 §5.2:

| Modulo | Coverage minima | Note |
|---|---|---|
| `shared` | ≥ 80% (90% raccomandato per `RuleEngine`) | Invariato vs F3.5 (no codice `shared` toccato in F4) |
| `core-server` | ≥ 80% line + branch | **Attuale: lines 94.95% (301/317), branches 88.64% (78/88)** post-REVIEW closure (commit `5320428`); JaCoCo gate `haltOnFailure=true` con 19 esclusioni (record DTO event package + value-type records + enum DTO + `CoreServerConfiguration` + `MatchNotFoundException` + `MatchEventEnvelope`); 11 classi nel bundle post-esclusione vs 38 pre-esclusione |
| `client` | ≥ 60% line+branch | Invariato vs F3.5 (no codice `client` toccato in F4) |
| `server` | ≥ 70% | Invariato (singolo smoke test, F6+ aggiunge tutto il transport reale) |

**F-001 RESOLVED** (REVIEW Fase 4): pre-fix il gate ereditava il default permissivo del parent (`haltOnFailure=false`, `min=0.00`); branches misurate al 78.36% (sotto soglia); post-fix override esplicito con `haltOnFailure=true` LINE/BRANCH 0.80, 19 esclusioni allineate al pattern client F3.5 (record DTO senza logica, enum DTO, glue Spring/exception non testabile in isolation), branches risale a 88.64%.

---

## 4. Test corpus regole italiane

**Invariato** rispetto a F3.5 (PLAN-fase-1 §3.5 corpus 48 + Task 3.21 tactical 5 → 53 posizioni totali). F4 NON tocca `RuleEngine` né il corpus JSON. La regression `mvn clean verify` root con `slow`+`performance` (esecuzione E4, §5.2) verifica che `RuleEngineCorpusTest` parametrizzato continui a passare su 53 posizioni invariate.

---

## 5. Esecuzioni pianificate

| # | Esecuzione | Quando | Scope | Esito | Commit |
|---|---|---|---|---|---|
| E1 | `mvn -pl core-server verify -DexcludedGroups=slow,performance` | Ad ogni task IMPLEMENTA (4.1 → 4.13) | core-server unit + integration, fast tags | ✅ BUILD SUCCESS ad ogni task; conta finale post-IMPLEMENTA: **150 verdi** | `096b636` (Task 4.13 closure) |
| E2 | `mvn -pl core-server verify -DexcludedGroups=slow,performance` | Pre-REVIEW closure | core-server fast (post fix F-001 + F-002) | ✅ BUILD SUCCESS in 25.2s, **163 verdi** (150 + 13 nuovi: 6 tournament repo + 7 user repo); JaCoCo gate ATTIVO `haltOnFailure=true`, 11 classi bundle post-esclusione, lines 94.95% + branches 88.64% | `5320428` (REVIEW closure) + `8f5c862` (backfill) |
| E3 | `mvn -pl core-server test -Dgroups=slow -Dtest=InMemoryRepositoryConcurrencyTest` | Sotto-fase TEST (Task 4.13.T1) | A4.12 stress concorrenza standalone | ✅ BUILD SUCCESS in 6.7s, **3/3 test verdi** in 0.435s | `bdc3b82` |
| E4 | `mvn clean verify` (root, no `excludedGroups`) | Sotto-fase TEST closure | tutti i moduli + corpus F1 (53 pos) + gating IA F2 (≥95/100) + performance NFR + slow stress | ✅ BUILD SUCCESS — vedi §5.2 sotto | `bdc3b82` |

### 5.1 Stress concorrenza A4.12 (E3) — eseguita 2026-05-06 11:59 CET

`mvn -pl core-server test -Dgroups=slow -Dtest=InMemoryRepositoryConcurrencyTest` post-creation del test class:

| Test | Scope | Esito |
|---|---|---|
| `manyMatchesCreatedConcurrentlyDoNotCorruptRepository` | 16 thread × 1000 match `save()` parallel; verifica size = 1000, ID univoci (`HashSet` count = 1000), ogni `findById` presente, `findByStatus(ONGOING)` ritorna 1000 senza CME, `findByPlayer(ALICE)` ritorna 1000 (entrambi i giocatori sono ALICE/BOB su tutti) | ✅ PASS |
| `manyAppendsOnDifferentMatchesPreserveMonotonicSequencePerMatch` | 64 match pre-creati; 16 thread × 10000 `appendEvent` parallel, ognuno serializza per-match con `Map<MatchId, Object>` caller-side lock (pattern `MatchManager`); verifica per ogni match log strict monotonic da 0 (`log.get(i).sequenceNo() == i`), somma totale eventi = 10000, `currentSequenceNo == size - 1` per ogni match | ✅ PASS |
| `manyAppendsOnSameMatchUnderCallerLockPreserveMonotonicSequence` | bonus contention test: 16 thread × 1000 `appendEvent` su un singolo match con caller-side `Object lock`; verifica log size = 1000, sequence strict monotonic da 0 a 999 — esercita il `synchronized (log)` interno di `InMemoryMatchRepository.appendEvent` sotto contesa massima | ✅ PASS |
| **Totale** | **3 test slow-tagged in 0.435s, 0 failures, 0 CME, 0 dropped events** | ✅ |

**A4.12 ✅ COVERED** (era DEFERRED in REVIEW).

Note implementative:
- Helper `runConcurrently(threads, timeoutSeconds, worker, failures)` centralizza il pattern `ExecutorService.newFixedThreadPool + CountDownLatch start gate + CountDownLatch done gate + try/catch → failures list`. I worker partono tutti allo stesso momento (gate) per massimizzare la contesa.
- AtomicInteger `scheduled.getAndIncrement()` sopra il limite per work-stealing pattern; eventuale overshoot è gated dal check `idx >= LIMIT → return`.
- Test #2 dimostra che la responsabilità di per-match write serialization è **esplicitamente** del caller (Javadoc di `InMemoryMatchRepository`): in produzione `MatchManager` lo fa via `ConcurrentHashMap<MatchId, Object> matchLocks` + `synchronized(lockFor(matchId))`. Test #3 dimostra che anche con caller-side lock corretto, il `synchronized (log)` block interno funge da seconda barriera (defense in depth) e produce log integro sotto contesa estrema.

### 5.2 Regression finale (E4) — eseguita 2026-05-06 12:01 CET

`mvn clean verify` (root, **inclusi** `slow,performance`) sul branch `feature/4-core-server-skeleton` HEAD post-REVIEW closure:

| Modulo | Tempo | Test | Esito |
|---|---:|---:|---|
| Parent (POM agg) | 3.2 s | — | ✅ SUCCESS |
| `shared` | **16:49 min** | 391 (387 default + 1 slow + 3 performance) | ✅ SUCCESS |
| `core-server` | 37.6 s | 166 (163 fast + 3 slow stress nuovi) | ✅ SUCCESS |
| `client` | 1:10 min | 321 | ✅ SUCCESS |
| `server` | 14.0 s | 1 smoke | ✅ SUCCESS |
| **Totale build** | **18:55 min** | **879 test verdi** | ✅ **BUILD SUCCESS** |

Highlights:
- **Gating IA F2** `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` (`@Tag("slow")`) PASS — il tempo `shared` 16:49 min è dominato da questo test (~16 min wall-clock per simulare 100 partite Campione vs Principiante; soglia ≥95/100 superata, **invariato vs F3.5 baseline 14:53 min** — la varianza è normale per JIT warmup + load macchina).
- **Corpus regole F1** `RuleEngineCorpusTest` parametrizzato PASS sulle 53 posizioni in `shared/src/test/resources/test-positions.json` (corpus invariato vs F2/F3/F3.5).
- **Performance NFR-P-02** `AiPerformanceTest` (`@Tag("performance")`, 3 test) PASS — Campione produce mossa valida entro 5s × 1.5x tolerance.
- **Modulo `core-server` post-F4**: 166 test (149 nuovi vs F0 baseline 1 smoke; +3 vs REVIEW closure 163 per le slow stress di E3). JaCoCo gate `haltOnFailure=true` ≥ 80% line+branch ✅, lines 94.95% + branches 88.64%.
- **Modulo `client` post-F3.5**: 321 invariato (F4 non tocca client). JaCoCo gate ≥ 60% line+branch ✅.
- **SAST SpotBugs**: 0 High su tutti i moduli.
- **Spotless**: 0 file da riformattare su tutti i moduli (160+ file Java kept clean).
- **ArchUnit**: 5/5 rule verdi al build (`CoreServerArchitectureTest` blocca import di JavaFX, Spring Boot Web, Tomcat, Jetty, JPA, Hibernate; verifica che `match → tournament` non ci sia post Option F refactor ADR-042).

**A4.18 ✅ COVERED** (era DEFERRED in REVIEW): regression `RuleEngineCorpusTest` F1 + `AiTournamentSimulationTest` F2 invariati e verdi.
**A4.19 ✅ COVERED** (era DEFERRED in REVIEW): `mvn clean verify` root (slow + performance inclusi) BUILD SUCCESS su tutti i moduli.

Nessuna regressione cross-module rispetto al baseline F3.5 — i moduli `shared`/`client`/`server` non sono stati toccati in F4 e si confermano invariati nei conteggi e tempi.

---

## 6. Test code aggiunto/modificato in F4

Lista cumulativa per task. Riferirsi a CHANGELOG `[Unreleased]` per dettagli granulari; AI_CONTEXT.md per descrizione discorsiva di ogni task.

| Task | Test class/file | Tipo | Δtest core-server |
|---|---|---|---|
| 4.1 | `CoreServerSmokeTest` (vestigial F0) **eliminato** + `CoreServerConfigurationTest` 1 test | Unit | -1 + 1 = 0 (1 totale) |
| 4.2 | `MatchValueTypesTest` 15 test in 3 nested + `TournamentValueTypesTest` 7 test in 2 nested + `MatchEventSealedTest` 4 test + `EventSerializationTest` 9 test | Unit | +35 (37 — Spotless count post-Task 4.2) |
| 4.3 | `MatchRepositoryContractTest` abstract package-private 13 test (skipped da JUnit fino al concrete subclass di 4.4) | Unit (abstract contract) | +0 attivi (13 dormienti) |
| 4.4 | `InMemoryMatchRepositoryTest extends MatchRepositoryContractTest` (attiva i 13 contract test) | Unit | +13 (50 — i contract si attivano) |
| 4.5 | `SpringMatchEventBusTest` 4 test (`AnnotationConfigApplicationContext` direct + `CollectingListener @Component` + `synchronizedList<MatchEvent> received`) | Integration (Spring DI) | +4 (54) |
| 4.6 | `LoggingStompPublisherTest` 3 test + `BufferingStompPublisherTest` 6 test (test-scope helper) | Unit | +9 (63) |
| 4.7 | `MatchStateMachineTest` 26 test in 2 nested (10 status transitions inclusi 3 `@ParameterizedTest @EnumSource(MatchStatus)` × 4 cases ciascuno + 7 anti-cheat counter) | Unit | +26 (89) |
| 4.8 | `MatchManagerTurnTest` 8 + `MatchManagerValidationTest` 6 + `ResignFlowTest` 6 + `DrawFlowTest` 6 = 26 test | Unit + integration | +26 (115) |
| 4.9 | `AntiCheatTest` 3 test (5-illegal forfait, reset counter, per-player independence) | Unit | +3 (118) |
| 4.10 | `TournamentEngineSkeletonTest` 24 test in 8 nested + 1 top-level (`registrationClosedAfterStartIsTreatedAsIllegalState`) | Unit | +24 (142) |
| 4.11 | `SoloMatchEndToEndTest` 3 test top-level (random game terminale + replay reconstruction + bus/stomp broadcast via Spring DI) | E2E API | +3 (145) |
| 4.12 | `CoreServerArchitectureTest` 5 ArchUnit rule + `TournamentMatchRefValidation` 4 test migrati da tournament a match (count invariato post Option F refactor) | Architecture + Unit | +5 (150) |
| 4.13 | Nessun nuovo test (docs only: ADR-038..042 + CHANGELOG + TRACEABILITY + AI_CONTEXT) | Doc | 0 (150) |
| **REVIEW closure (F-002)** | `InMemoryTournamentRepositoryTest` 6 test + `InMemoryUserRepositoryTest` 7 test | Unit | +13 (163) |
| **TEST closure (E3)** | `InMemoryRepositoryConcurrencyTest` 3 test slow-tagged | Stress | +3 slow-tagged (166 inclusi slow) |

**Saldo finale F4**: **+165 test core-server** vs F0 baseline (1 → 166). Nessun test cancellato senza sostituzione (`CoreServerSmokeTest` F0 vestigial sostituito da `CoreServerConfigurationTest`). Nessun test in stato `@Disabled` o `@Ignore`.

---

## 7. Closure check

- [x] Coverage target raggiunti (`mvn -pl core-server jacoco:report` post regression: lines 94.95% + branches 88.64%, gate `haltOnFailure=true` verde — F-001 RESOLVED commit `5320428`).
- [x] TRACEABILITY aggiornata: A4.1..A4.19 (con A4.12, A4.18, A4.19 promossi da DEFERRED a ✅ COVERED post §5.1+§5.2), FR-COM-01/02/04, AC §17.1.2/§17.1.10/§17.1.11 preview F6/F7 — vedi `tests/TRACEABILITY.md` sezione "Acceptance criteria di Fase 4".
- [x] Test corpus regole italiane invariato (F4 non tocca `RuleEngine` né `test-positions.json`).
- [x] `mvn clean verify` (root, **slow + performance inclusi**) BUILD SUCCESS — vedi §5.2.
- [x] Test plan §1-§7 popolato.
- [x] Nessun test in stato `@Disabled` o `@Ignore` senza issue tracciata (grep `@Disabled|@Ignore` in `core-server/src/test/java/` = 0 match).
- [x] Findings REVIEW Fase 4 (F-001 + F-002) entrambi RESOLVED prima dell'apertura sotto-fase TEST.

**Test plan chiuso il**: 2026-05-06.
**Commit di chiusura**: `bdc3b82` (`test(core-server): close TEST Fase 4 — concurrency stress + regression root`).

---

## 8. Conseguenze per Fase 5+

Il `MatchRepository` è scritto come **port abstract** + `MatchRepositoryContractTest` abstract: in F6, `JpaMatchRepository` (modulo `server`) estenderà la stessa classe contract con `@Transactional` boundary + `createRepository()` su impl JPA, ottenendo identiche 13 asserzioni contro lo stesso contratto su due adapter eterogenei. Pattern già usato in industry per "test once, swap impls". Stesso vale per `TournamentRepository`/`UserRepository`.

Il `BufferingStompPublisher` test-scope rimane in `core-server/src/test/java/` come fixture riusabile per F6 (dove i test transport dovranno verificare che i `MatchEvent` di F4 passino end-to-end attraverso WebSocket reale fino al subscriber); la abstraction `StompCompatiblePublisher` permette ai test core-server di non dipendere mai da Tomcat/Jetty.

Il corpus regole italiane (53 posizioni) e il gating IA Campione ≥95/100 sono parte permanente del regression al closure di ogni fase futura — qualsiasi PR di F5+ che faccia fallire E4 è un finding `BUG` o `REQUIREMENT_GAP` (regression) alla review della fase corrispondente.
