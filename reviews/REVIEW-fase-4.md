# REVIEW — Fase 4: `core-server` skeleton (Tournament Engine vuoto + Match Manager + repository ports + adapter in-memory + event bus + STOMP-compatible publisher)

- **Data apertura**: 2026-05-06
- **Commit codebase apertura**: `096b636` (HEAD `feature/4-core-server-skeleton` post Task 4.13)
- **SPEC version**: 2.2 (2026-04-30)
- **PLAN di riferimento**: [`plans/PLAN-fase-4.md`](../plans/PLAN-fase-4.md)
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale | Resolved |
|-----------------|---------:|-----:|-------:|----:|-------:|---------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |        — |
| REQUIREMENT_GAP |        0 |    1 |      1 |   0 |      2 |      0/2 |
| BUG             |        0 |    0 |      0 |   0 |      0 |        — |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |        — |
| PERFORMANCE     |        0 |    0 |      0 |   0 |      0 |        — |
| CODE_QUALITY    |        0 |    0 |      0 |   0 |      0 |        — |
| DOC_GAP         |        0 |    0 |      0 |   0 |      0 |        — |
| **Totale**      |        0 |    1 |      1 |   0 |      2 |      0/2 |

**Stato complessivo (apertura)**: 0 BLOCKER. 2 REQUIREMENT_GAP (1 High + 1 Medium) entrambi addressabili in REVIEW closure con cambio `core-server/pom.xml` + 2 nuovi test class. Nessun BUG / SECURITY / PERFORMANCE / CODE_QUALITY / DOC_GAP rilevato — il codice IMPLEMENTA F4 è pulito su tutti gli altri assi (anti-pattern scan TODO/FIXME/println/printStackTrace = 0 hit; package-info presente in 8/8 sotto-package prod richiesti da A4.15; 5/5 ArchUnit rule verdi a build-time; SpotBugs 0 issues; Spotless OK; 150/150 test verdi; F-COM-04 sequence strict monotonic provato sotto contesa via `synchronized (log)` block + per-match lock in `MatchManager`).

`mvn -pl core-server verify -DexcludedGroups=slow,performance` BUILD SUCCESS al commit `096b636`: **150 test verdi**, 0 skipped, 38 classi analizzate da JaCoCo, SpotBugs 0 High, Spotless OK, ArchUnit 5/5 rule verdi. Coverage misurata: **lines 92.02% (461/501)**, **branches 78.36% (105/134)**. Il line gate ≥80% è soddisfatto a misura; il branch gate ≥80% **NON** è soddisfatto (78.36 < 80) — vedi finding F-001 sul gate non attivo.

Regression `mvn clean verify` root con `slow`+`performance` (A4.18-A4.19) **DEFERRED** alla sotto-fase TEST Fase 4 come previsto dalla PLAN §5.3 (`RuleEngineCorpusTest` F1 48 posizioni + `AiTournamentSimulationTest` F2 gating ≥95/100 invariati).

Stress concorrenza `InMemoryRepositoryConcurrencyTest @Tag("slow")` (A4.12) **DEFERRED** alla sotto-fase TEST come previsto dalla PLAN §5 (test slow-tagged scope TEST, non IMPLEMENTA).

---

## Acceptance criteria coverage

### SPEC §17 (acceptance globali rilevanti per F4)

| AC ID    | Descrizione                                                                  | Status      | Note |
|----------|------------------------------------------------------------------------------|-------------|------|
| 17.1.2   | Partita Internet (preview F4 — `MatchManager` API end-to-end senza UI/rete)  | ✅ COVERED  | `SoloMatchEndToEndTest` 3 metodi (random game terminale + replay reconstruction + bus/stomp broadcast); F6 incolla il trasporto reale sopra il `MatchManager` di F4 |
| 17.1.10  | Autosave host LAN (preview F4 — `MatchRepository.appendEvent` + `eventsSince` replay) | ✅ COVERED  | `MatchRepositoryContractTest` 3 test su strict monotonic + suffix replay; `SoloMatchEndToEndTest#replayFromEventsReconstructsCurrentState` prova fold da `GameState.initial()`; F7 LAN host incollerà persistenza file-based |
| 17.1.11  | Match LAN end-to-end (preview F4 — la stessa `MatchManager` API che F7 LAN host userà) | ✅ COVERED  | Stesso `SoloMatchEndToEndTest` + `BufferingStompPublisher` con topic `/topic/match/{id}` (SPEC §11.4) verificato |
| 17.2.4   | Coverage ≥ 80% modulo `core-server`                                           | ⚠️ AT RISK | Lines 92.02% ≥ 80% ✅; branches 78.36% < 80% ❌. Vedi F-001 (gate non attivo + branches sotto soglia per via di `InMemoryTournamentRepository`/`InMemoryUserRepository` non testati e validation-branch di alcuni record DTO non esercitate) |
| 17.2.5   | SAST SpotBugs senza warning High                                             | ✅           | 0 warning High al commit `096b636` |

### Acceptance criteria operativi della Fase 4 (PLAN-fase-4 §2.2)

| ID    | Criterio                                                                                            | Status      | Note |
|-------|-----------------------------------------------------------------------------------------------------|-------------|------|
| A4.1  | `mvn -pl core-server verify` BUILD SUCCESS, JaCoCo gate ≥ 80% line + branch (NFR-M-01)              | ⚠️ PARTIAL  | BUILD SUCCESS ✅; JaCoCo gate **non attivo** in `core-server/pom.xml` (eredita default permissivo di parent `haltOnFailure=false`+`min=0.00`). Lines 92% PASSEREBBERO, branches 78.36% FALLIREBBERO. **F-001** |
| A4.2  | Match end-to-end via API Java (autoritativo): partita random fino a stato terminale via `MatchManager` API; status FINISHED, MatchEnded ultimo evento | ✅           | `SoloMatchEndToEndTest#playsRandomGameUntilTerminal` (seed 42, MAX_PLIES 500 ceiling difensivo); asserisce `events.size() == plies+1`, ultimo è `MatchEnded`, sequence number monotonic da 0 |
| A4.3  | Sequence number monotonico (FR-COM-04)                                                              | ✅           | `MatchRepositoryContractTest#appendEventRejectsNonMonotonicSequenceNo`, `firstEventMustBeSequenceZero`, `eventsSinceReturnsSuffixOfMatchingEvents`; `SoloMatchEndToEndTest` (assert `events.get(i).sequenceNo() == i`) |
| A4.4  | Anti-cheat 5-illegal forfeit (FR-COM-01, SPEC §9.8.3, ADR-040)                                      | ✅           | `AntiCheatTest` 3 test: forfait WHITE su 5ª illegale, reset counter su mossa legale, per-player independence (Mockito-stubbed `RuleEngine` per interleaving illegali senza flippare turno) |
| A4.5  | `MatchManager.applyMove` valida turno: mossa fuori turno → MoveRejected NOT_YOUR_TURN, sequence + counter avanzano | ✅           | `MatchManagerTurnTest` 8 test |
| A4.6  | Validazione via `RuleEngine`: tutte le mosse passano da `RuleEngine.applyMove`                       | ✅           | `MatchManagerValidationTest#illegalMoveProducesMoveRejectedAndIncrementsCounter`, `engineThrowingIllegalMoveExceptionBecomesMoveRejected` |
| A4.7  | Resign: `resign(matchId, who)` → Resigned + MatchEnded(opposite winner, RESIGN); resign su match FINISHED → IllegalStateException | ✅           | `ResignFlowTest` 6 test |
| A4.8  | Draw offer/response: offer → DrawOffered; accept → DrawAccepted + MatchEnded DRAW_AGREEMENT; decline → DrawDeclined + ONGOING; doppia offer pending → IllegalStateException | ✅           | `DrawFlowTest` 6 test |
| A4.9  | Event bus interno: ogni evento broadcast via Spring `@EventListener(MatchEventEnvelope)`; ordine FIFO per match | ✅           | `SpringMatchEventBusTest` 4 test (broadcast order, per-match FIFO, bean-class assertion, NPE guard) |
| A4.10 | STOMP-compatible publisher: ogni evento pubblicato su `/topic/match/{id}` via `StompCompatiblePublisher.publishToTopic` | ✅           | `MatchManagerTurnTest` mock `verify`; `SoloMatchEndToEndTest#eventsBroadcastOnBusAndStomp` con `BufferingStompPublisher @Primary` + topic-prefix assertion sul payload |
| A4.11 | TournamentEngine compila + ritorna stub                                                              | ✅           | `TournamentEngineSkeletonTest` 24 test (createTournament + registerParticipant funzionanti, startTournament/generate/schedule throws UOE) |
| A4.12 | In-memory adapter thread-safe stress 16 thread × 1000 match × 10000 mosse                            | ⚠️ DEFERRED  | Scope sotto-fase TEST per design (PLAN §5.3); il `synchronized (log)` block esplicito in `InMemoryMatchRepository.appendEvent` + per-match lock in `MatchManager` sono in atto e provati functionalmente dai 13 contract test |
| A4.13 | Anti-pattern check (CLAUDE.md §8.7-8.8) + sub-package layering (Option F refactor)                   | ✅           | `CoreServerArchitectureTest` 5/5 rule ArchUnit verdi al build (no JavaFX, no Spring Boot Web, no Tomcat/Jetty, no JPA/Hibernate, match non dipende da tournament post Option F + ADR-042) |
| A4.14 | Spotless OK, SpotBugs 0 High su `core-server`                                                        | ✅           | Conferma Maven verify |
| A4.15 | `package-info.java` per ogni nuovo sotto-package                                                     | ✅           | 8/8 presenti: `core/`, `match/`, `match/event/`, `tournament/`, `repository/`, `repository/inmemory/`, `eventbus/`, `stomp/`. (Il sub-package test-only `architecture/` non era nel PLAN A4.15 e non è obbligato.) |
| A4.16 | TRACEABILITY aggiornato: FR-COM-01, FR-COM-04, AC §17.1.2 + §17.1.10 (preview F6/F7)                 | ✅           | Task 4.13: estesa FR-COM-01 (anti-cheat MatchManager-side), nuova riga FR-COM-04, AC §17.1.2/§17.1.10/§17.1.11 preview, sezione AC F4 A4.1..A4.19 |
| A4.17 | Nessun TODO/FIXME pending in `core-server/src/main/java/`                                            | ✅           | Grep 0 match (`TODO|FIXME|XXX`) |
| A4.18 | Regression `RuleEngineCorpusTest` F1 + `AiTournamentSimulationTest` F2                               | ⚠️ DEFERRED  | Scope sotto-fase TEST (PLAN §5.3) |
| A4.19 | `mvn clean verify` (root, `slow`+`performance` inclusi): BUILD SUCCESS                               | ⚠️ DEFERRED  | Scope sotto-fase TEST (PLAN §5.3) |

### Requisiti SPEC funzionali coperti (FR)

| FR ID    | Status     | Note |
|----------|------------|------|
| FR-COM-01 | ✅ COVERED | Validazione mosse server-side via `RuleEngine` + anti-cheat counter MatchManager-side. `AntiCheatTest` (3) + `MatchManagerValidationTest` (2 counter increment) + `MatchManagerTurnTest` (NOT_YOUR_TURN bumpa counter) |
| FR-COM-02 | ⚠️ PARTIAL | Preview JSON serializability via Jackson default: `EventSerializationTest` 8 roundtrip; `MoveApplied` deferred a F6 con custom Module per `Move` sealed + `Board` (documentato in test class Javadoc, AI_CONTEXT, ADR-038) |
| FR-COM-04 | ✅ COVERED | Sequence number strict monotonic. `MatchRepositoryContractTest` (3 test su `appendEvent` + `currentSequenceNo` + `eventsSince`) + `SoloMatchEndToEndTest` (random game con assertion `events.get(i).sequenceNo() == i`) |

### Requisiti SPEC non funzionali coperti (NFR)

| NFR ID   | Status      | Note |
|----------|-------------|------|
| NFR-M-01 | ⚠️ AT RISK | Lines 92.02% ✅ ≥ 80%; **branches 78.36% < 80% ❌**. Gate non attivo nel POM. **F-001** |
| NFR-M-04 | ✅ COVERED  | Spotless Google Java Style (parent POM) — 77 file kept clean al commit `096b636` |

---

## Findings

### F-001 — [REQUIREMENT_GAP, High] JaCoCo gate non attivo in `core-server/pom.xml` + branches sotto soglia 80%

- **Posizione**:
  - `core-server/pom.xml:88-99` — il `<plugin>org.jacoco</plugin>` è dichiarato senza `<configuration>`, ereditando il default permissivo del parent `pom.xml:254-273` (`<haltOnFailure>false</haltOnFailure>` + rule `minimum=0.00`).
  - Confronto: `shared/pom.xml` override con `haltOnFailure=true` + LINE/BRANCH 0.90 + sub-rule `rules` 0.90 + sub-rule `ai` 0.85; `client/pom.xml` override con `haltOnFailure=true` + LINE/BRANCH 0.60 + 9 esclusioni JavaFX-bound. **Solo `core-server` non ha override**, quindi A4.1 ("JaCoCo gate ≥ 80% line + branch") è satisfatto a livello apparente (`All coverage checks have been met` in output) ma il gate effettivo è `min=0%`.
- **SPEC reference**: NFR-M-01 (≥ 80% sul motore di gioco/core-server), A4.1 (operativo F4), PLAN-fase-4 stop point §7.7 chiuso il 2026-05-02 dall'utente con la formula esplicita "JaCoCo ≥80% line+branch con esclusioni record/enum DTO + CoreServerConfiguration".
- **Descrizione**: a misura attuale (commit `096b636`) la coverage core-server è:
  - **Instructions**: 2213/2415 = **91.64%**
  - **Lines**: 461/501 = **92.02%** ✅ passerebbe il gate 80%
  - **Branches**: 105/134 = **78.36%** ❌ fallirebbe il gate 80% di 1.64 punti percentuali
  
  Le 29 branches mancanti si concentrano in:
  - **`InMemoryUserRepository`** 0/4 branches (logica `findById` ANONYMOUS_LAN_ID guard + `register` index-population — nessun test diretto, vedi F-002)
  - **`InMemoryTournamentRepository`** 0/4 branches (logica `findByStatus` filter — nessun test diretto, vedi F-002)
  - **9 record DTO** al 50% branches (uno dei `Objects.requireNonNull` non esercitato negativamente: `DrawDeclined`, `DrawOffered`, `MoveApplied`, `MoveRejected`, `PlayerDisconnected`, `PlayerReconnected`, `Resigned`, `EliminationTournament`, `RoundRobinTournament`)
  - `MatchEnded` 75% branches (3/4 — il check `result == UNFINISHED` non testato), `Match` 80% (12/15), `MatchManager` 81.1% (30/37)

  **Doppio problema**: (i) il gate non è wired (config drift dal parent Fase 0), (ii) anche se wired, branches restano sotto soglia.
- **Proposta di fix** (3 opzioni, in ordine di preferenza):
  - **Opzione A — wire gate + esclusioni record/enum DTO** (allineata a PLAN §7.7 letterale): override jacoco-check in `core-server/pom.xml` con `haltOnFailure=true` + LINE 0.80 + BRANCH 0.80 + esclusioni esplicite `**/event/*.class` (10 record sealed event), `**/MatchId.class`, `**/UserRef.class`, `**/TimeControl.class`, `**/TimeControlPreset.class`, `**/MatchStatus.class`, `**/MatchResult.class`, `**/EndReason.class`, `**/RejectionReason.class`, `**/TournamentId.class`, `**/TournamentMatchRef.class`, `**/TournamentSpec.class`, `**/TournamentFormat.class`, `**/TournamentStatus.class`, `**/EliminationTournament.class`, `**/RoundRobinTournament.class`, `**/CoreServerConfiguration.class`, `**/MatchNotFoundException.class`, `**/MatchEventEnvelope.class`. Stima post-esclusione branches: ~95/106 = 89.6% ✅. Pattern coerente con `client/pom.xml` esclusioni JavaFX.
  - **Opzione B — wire gate senza esclusioni + nuovi test in F4** (più severa, richiede F-002 risolta + più test): aggiungere ~8 test totali — `InMemoryTournamentRepositoryTest` ~5 + `InMemoryUserRepositoryTest` ~5 + 2 test negativi su `MatchEnded.UNFINISHED` rejection + 1 test extra su `MatchManager` branch missed. Stima post-test branches: ~115/134 = 85.8% ✅ senza esclusioni. Più "test-driven" ma double-up con F-002 fix.
  - **Opzione C — abbassare temporaneamente la soglia branches a 75%, line resta 80%** (fallback): wire gate con LINE 0.80 + BRANCH 0.75; lascia margine documentato per F8/F9 quando il tournament guadagna logica reale. Rifiutato per default — devierebbe da PLAN §7.7 ("80% line+branch").
  - **Raccomandazione**: **A + parte di B**. Wire il gate (Opzione A) come da PLAN; aggiungere comunque tests per `InMemoryTournamentRepository` + `InMemoryUserRepository` come da F-002 perché sono infrastrutture che meritano test indipendentemente dal gate. Le esclusioni record sono giustificate (validazione strutturale + DTO trasporto, no logica) e coerenti col precedente di `client/pom.xml`.
- **Status**: 🔴 **OPEN** — fix in REVIEW closure (richiede edit `core-server/pom.xml` + `mvn -pl core-server verify` per validare gate attivo).

---

### F-002 — [REQUIREMENT_GAP, Medium] `InMemoryTournamentRepository` + `InMemoryUserRepository` non hanno test diretti

- **Posizione**:
  - `core-server/src/main/java/com/damaitaliana/core/repository/inmemory/InMemoryTournamentRepository.java` — 5/11 lines (45%), 0/4 branches. `findByStatus` filter mai eseguito con dati.
  - `core-server/src/main/java/com/damaitaliana/core/repository/inmemory/InMemoryUserRepository.java` — 3/11 lines (27%), 0/4 branches. `findById` (con guard `ANONYMOUS_LAN_ID == -1L`), `findByUsername`, `register` (popolazione doppio indice id+username) tutti senza test diretto.
- **SPEC reference**: A4.4 "in-memory adapters per i repository ports" (PLAN-fase-4 §4.4 + Task 4.4 in AI_CONTEXT). PLAN §5.1 prescrive ≥75% test unit per gli adapter. NFR-M-01 ≥80% globale.
- **Descrizione**: Task 4.4 ha implementato i 3 adapter in-memory ma ha ATTIVATO i contract test solo per `MatchRepository` (`InMemoryMatchRepositoryTest extends MatchRepositoryContractTest` → 13 test). I port `TournamentRepository` e `UserRepository` non hanno né contract test (PLAN non li richiedeva — F4 §4.3 ne aveva uno solo per Match) né smoke test diretti, quindi la logica degli adapter è esercitata solo accidentalmente attraverso `TournamentEngineImpl` (che usa `TournamentRepository.save` e `findById` ma NON `findByStatus`).
  - **Rischio coperto**: il filtro `findByStatus` in `InMemoryTournamentRepository.java:35-43` è dead path operativo — un bug nel predicato `t.status() == status` non sarebbe rivelato.
  - **Rischio coperto**: il guard `if (id == UserRef.ANONYMOUS_LAN_ID) return Optional.empty()` in `InMemoryUserRepository` (vedi line 37 circa) è semantica importante (SPEC §11.1: anonymous LAN users not addressable by id) e non testato.
- **Proposta di fix**: aggiungere 2 test class:
  - **`InMemoryTournamentRepositoryTest`** (~5 test): save+findById roundtrip, save overwrites, findByStatus empty, findByStatus filter multi-tournament (CREATED + IN_PROGRESS + FINISHED → query ognuna ritorna solo il match status), null-arg guards.
  - **`InMemoryUserRepositoryTest`** (~5 test): register populates both indices, `findById` ritorna user authenticated, `findById(ANONYMOUS_LAN_ID)` ritorna `Optional.empty()` (semantica SPEC §11.1), `findByUsername` per anonymous LAN users still works (solo username-side index), null-arg guards.
- **Status**: 🔴 **OPEN** — fix in REVIEW closure (2 nuovi file di test, ~10 test totali).

---

## SPEC change requests

> Vuota. Le 2 finding sono entrambe gap di copertura/configurazione e non richiedono modifiche allo SPEC.

---

## Closure

- [ ] Tutti i `BLOCKER` risolti (0 finding)
- [ ] Tutti i `REQUIREMENT_GAP` risolti (0/2: F-001 OPEN, F-002 OPEN)
- [ ] Tutti i `Critical/High` `BUG` risolti (0 finding)
- [ ] Tutti i `Critical/High` `SECURITY` risolti (0 finding)
- [ ] `PERFORMANCE` che violano NFR risolti (0 finding — NFR-P non si applica a F4, no UI/no AI search)
- [ ] SPEC change requests con stato non-PENDING (0 CR)

**Review aperta il**: 2026-05-06
**Stop point §2.3.6**: presentare findings all'utente per decidere quale opzione di F-001 (A / B / C) applicare e confermare F-002 fix scope.
