# REVIEW — Fase 2: IA (`shared.ai`)

- **Data**: 2026-04-28
- **Commit codebase**: `1875497` (head di `feature/2-ai`)
- **SPEC version**: 2.0 (2026-04-26)
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale |
|-----------------|---------:|-----:|-------:|----:|-------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |
| REQUIREMENT_GAP |        0 |    0 |      0 |   1 |      1 |
| BUG             |        0 |    0 |      0 |   0 |      0 |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |
| PERFORMANCE     |        0 |    0 |      1 |   0 |      1 |
| CODE_QUALITY    |        0 |    0 |      0 |   4 |      4 |
| DOC_GAP         |        0 |    0 |      0 |   1 |      1 |
| **Totale**      |        0 |    0 |      1 |   6 |      7 |

**Stato complessivo**: nessun finding bloccante. Tutti gli acceptance criteria operativi della Fase 2 (A2.1 ÷ A2.13) sono COVERED. Il **gating A2.2** (Campione ≥ 95/100 vs Principiante) è ✅ PASSED — `mvn -pl shared test -Dtest=AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante -Dgroups=slow` chiuso BUILD SUCCESS in 16:02 min. Coverage `shared` ≥ 90% bundle, `rules` ≥ 90%, `ai` ≥ 85%. SpotBugs 0 High. Spotless OK.

I 7 finding sono tutti `Low` (eccetto F-002 PERFORMANCE Medium) e sono prevalentemente debiti tecnici noti (`isThreefoldRepetition` replay-based, hash incrementale Zobrist non implementato) deferred a F4 per allinearli con l'introduzione del `core-server`/`Match` persistente, oppure micro-cleanup di API non bloccanti.

---

## Acceptance criteria coverage

### SPEC §17 (acceptance globali)

| AC ID    | Descrizione                                                                                       | Status      | Note |
|----------|---------------------------------------------------------------------------------------------------|-------------|------|
| 17.1.1   | Partita SP vs IA Campione si conclude entro 30 minuti senza crash                                 | ⚠️ PARTIAL  | F2 valida correttezza+perf via `AiTournamentSimulationTest` (sanity 5 partite + gating 100 partite). End-to-end UI in F3 (vedi F-006). |
| 17.2.4   | Coverage ≥ 80% sul modulo `shared`                                                                  | ✅ COVERED   | JaCoCo gate a ≥ 90% bundle, ≥ 90% `rules`, ≥ 85% `ai` (più stretto del minimo SPEC) |
| 17.2.5   | SAST SpotBugs senza warning High                                                                    | ✅ COVERED   | 0 warning High su tutti i moduli |

### Acceptance criteria operativi della Fase 2 (PLAN-fase-2 §2.2)

| ID    | Criterio                                                                                          | Status      | Note |
|-------|---------------------------------------------------------------------------------------------------|-------------|------|
| A2.1  | `mvn -pl shared verify` BUILD SUCCESS                                                              | ✅           | 387/387 verdi (esclusi `slow,performance`); con tutti gli include i tag aggiungono 4 test di durata significativa |
| A2.2  | **Gating**: Campione ≥ 95/100 vittorie vs Principiante                                             | ✅           | `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` BUILD SUCCESS in 16:02 min |
| A2.3  | Campione produce mossa valida entro 5s in posizioni di metà partita                                 | ✅           | `AiPerformanceTest#campioneRespondsWithinBudget` (`@Tag("performance")`, tolleranza 1.5x §7.7) |
| A2.4  | Esperto ≤ 2s, Principiante ≤ 500 ms                                                                | ✅           | `AiPerformanceTest#espertoRespondsWithinBudget`, `principianteRespondsWithinBudget` |
| A2.5  | Cancellazione `Future.cancel(true)` entro ~200 ms; ritorno graceful della migliore mossa            | ✅           | `VirtualThreadAiExecutorTest#hardCancelMakesGetThrow`, `gracefulCancelReturnsBestMoveSoFar` |
| A2.6  | Coverage `shared` ≥ 90%, `rules` ≥ 90%, `ai` ≥ 85%                                                  | ✅           | JaCoCo gate `haltOnFailure=true` |
| A2.7  | Spotless OK, SpotBugs 0 High                                                                        | ✅           | confermato |
| A2.8  | `package-info.java` per `shared.ai`, `shared.ai.evaluation`, `shared.ai.search`                    | ✅           | tre file presenti, riferimenti SPEC §12 + ADR + vincoli |
| A2.9  | TRACEABILITY aggiornato                                                                             | ✅           | FR-SP-02, NFR-P-02, AC §17.1.1 parziale + AC F2 (A2.1..A2.13) |
| A2.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`                                                | ✅           | grep verde |
| A2.11 | Test corpus regole italiane (Fase 1, 48 posizioni) **continua a passare** (regression)              | ✅           | `RuleEngineCorpusTest` 48/48 verde |
| A2.12 | Determinismo simulazione: stesso seed → stesso risultato bit-per-bit                                 | ✅           | seed Principiante = `42 + gameIndex`; Campione fully deterministico |
| A2.13 | Posizioni tattiche golden trovate dai livelli giusti                                                | ✅           | `AiTacticalPositionsTest` (5 posizioni) |

---

## Findings

### F-001 — [CODE_QUALITY, Low] Hardening fix di `isThreefoldRepetition` è un workaround

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/ItalianRuleEngine.java:182-201`
- **Descrizione**: per supportare il search di F2 che esplora stati hand-built (i.e. board sintetiche dei test tattici e dei test corpus quando passati attraverso `applyMove` durante la ricorsione), `isThreefoldRepetition` ora cattura `IllegalMoveException` durante il replay-from-initial e ritorna `false`. Conservativo (no repetition detected) ma fragile: la "vera" soluzione è abbandonare il replay e usare hashing Zobrist incrementale per tenere un `Map<Long, Integer>` di occorrenze in `GameState`. Questa ottimizzazione era esplicitamente Task 2.14 del piano e l'utente l'ha approvata come deferred-F4 (§7.9 del piano).
- **SPEC reference**: §3.6 — triplice ripetizione.
- **Proposta di fix**: nessuna in F2 (deferred). In F4, quando `core-server` introdurrà la persistenza dei match e i tornei richiederanno tracciamento ripetizione efficiente, sostituire l'implementazione replay-based con uno schema Zobrist-incremental.
- **Status**: ACKNOWLEDGED (deferred to F4)

---

### F-002 — [PERFORMANCE, Medium] `applyMove` durante la search di stati hand-built lancia `IllegalMoveException` come parte del flusso di controllo

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/ItalianRuleEngine.java:182-201` (try/catch nel replay) + chiamata da `MinimaxSearch.negamax` (indiretta via `applyMove → computeStatus → isThreefoldRepetition`).
- **Descrizione**: per ogni nodo della search dove `history.size() >= 4` e lo stato non è coerente con `GameState.initial()`, viene lanciato e catturato un `IllegalMoveException` per ogni `computeStatus`. Le eccezioni in Java costano qualche µs ciascuna; la search di Campione a depth 8 può visitare migliaia di nodi per mossa. Per partite reali (history coerente con initial) l'eccezione non si lancia; il finding interessa solo i test su stati sintetici e l'AI-search-of-synthetic-positions del corpus tactical.
- **Impatto misurato**: la simulazione gating 100 partite chiude in 16:02 min — entro un tempo accettabile. Su hardware lento questo potrebbe diventare visibile.
- **SPEC reference**: NFR-P-02 (≤ 5s Campione).
- **Proposta di fix**: stessa di F-001 — la sostituzione di `isThreefoldRepetition` con hash Zobrist incrementale elimina sia il replay che l'exception-based control flow. Coerente con piano §7.9.
- **Status**: ACKNOWLEDGED (deferred to F4 — stessa fix di F-001)

---

### F-003 — [REQUIREMENT_GAP, Low] AC §17.1.1 (partita SP vs Campione entro 30 min) è coperto solo via API

- **Posizione**: `shared/src/test/java/com/damaitaliana/shared/ai/AiTournamentSimulationTest.java`
- **SPEC reference**: §17.1.1.
- **Descrizione**: l'AC SPEC dice "Partita single-player vs IA Campione si conclude entro 30 minuti senza crash". F2 verifica via simulazione API (Campione vs Principiante, 100 partite gating + 5 partite quick) la chiusura corretta delle partite (gating A2.2 PASSED). Manca la validazione end-to-end UI: lancio del client, scelta del livello Campione, partita giocata via interfaccia, salvataggio, nessun crash, durata sotto 30 min. Tutto questo richiede UI JavaFX (Fase 3) e `core-server` non è coinvolto in single-player.
- **Proposta di fix**: nessuna in F2. Documentato come PARTIAL nel TRACEABILITY. Sarà completato dal test E2E UI di Fase 3 (CLAUDE.md §2.4.1: `<Feature>E2ETest`).
- **Status**: ACKNOWLEDGED (deferred to F3 — è il design scope-by-phase del progetto)

---

### F-004 — [DOC_GAP, Low] `ZobristHasher.hashAfterMove` incrementale non implementato

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/ai/search/ZobristHasher.java`
- **Descrizione**: il piano §4 task 2.6 menziona un metodo `hashAfterMove(long oldHash, Move m, GameState before, GameState after)` per update incrementale del hash. L'implementazione attuale ricalcola il hash da zero a ogni nodo (`hash(GameState)`). Per Campione a depth 8 con TT, ogni nodo costa ~16 XOR (uno per pezzo). Non un collo di bottiglia su ~20 pezzi. La documentazione (Javadoc + ADR-026) non menziona la mancata implementazione del metodo incrementale.
- **SPEC reference**: §12.1 (transposition table).
- **Proposta di fix**: aggiornare il Javadoc di `ZobristHasher` con una nota: "{@code hashAfterMove} (incremental update) is not implemented — the search recomputes from scratch at every node, which is fast enough for the current depth budget". Se il profiling di F2/F3 dovesse mostrare il hash come bottleneck, l'API può crescere senza breaking changes.
- **Status**: OPEN (proposta di mini-fix Javadoc — vedi sezione "Closure")

---

### F-005 — [CODE_QUALITY, Low] `MoveOrderer.order(moves, state)` accetta `state` non utilizzato dall'implementazione standard

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/ai/search/MoveOrderer.java` + `StandardMoveOrderer.java`
- **Descrizione**: l'interfaccia `MoveOrderer.order(List<Move>, GameState)` espone il `GameState` come parametro per future implementazioni context-aware (es. "preferisci catture verso pezzi avanzati", "considera la posizione del re avversario"). `StandardMoveOrderer` non lo usa: il suo comparator si basa solo su `Move.isCapture()`, `Move.capturedSquares().size()`, `Move.to()`, e `Move.from()`. Il parametro è quindi unused ma documenta intent futuro.
- **Proposta di fix**: nessuna — l'API è correttamente future-proof. Aggiungere una nota esplicita nel Javadoc di `MoveOrderer.order` che il parametro `state` può essere ignorato dalle implementazioni che non lo necessitano.
- **Status**: OPEN (proposta di mini-fix Javadoc)

---

### F-006 — [CODE_QUALITY, Low] `AiEngine.forLevel(level, rng)` richiede `RandomGenerator` anche per Esperto/Campione che non lo consumano

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/ai/AiEngine.java:43-52`
- **Descrizione**: la factory richiede un `RandomGenerator` non-null per tutti e 3 i livelli; solo `Principiante` lo usa. La motivazione è "discipline-enforcing" (forzare il chiamante a riflettere sulla determinismo), ma è leggermente awkward. Alternative: factory specializzate per livello, oppure `RandomGenerator` opzionale.
- **Proposta di fix**: nessuna — il design è documentato in ADR-024 e nel Javadoc di `forLevel`. È una scelta deliberata. Se nelle fasi successive l'awkward emerge come reale frizione, valutare un overload `forLevel(level)` che usa `SplittableRandom(System.nanoTime())` per Principiante.
- **Status**: ACKNOWLEDGED (design intentional)

---

### F-007 — [CODE_QUALITY, Low] `PrincipianteAi` rumore via rejection-sampling

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/ai/PrincipianteAi.java:67-72`
- **Descrizione**: per scegliere una mossa "non-best" uniformemente, l'implementazione fa rejection sampling con `do/while`: estrae un index casuale e scarta se uguale a `best`. Numero atteso di iterazioni: `n/(n-1)` dove `n = legal.size() >= 2`, quindi 1.x in media. Corretto e terminating ma non ottimale: si potrebbe estrarre direttamente da `legal.stream().filter(m -> !m.equals(best)).toList()` e poi indicizzare. Marginalmente più CPU per la copia della lista (lo stream + filter/list build è O(n)) vs ~1.x estrazioni del random; il rejection sampling è effettivamente più veloce e più semplice. Nessuna azione richiesta.
- **Proposta di fix**: nessuna.
- **Status**: ACKNOWLEDGED (intentional — rejection sampling is the simpler and faster choice here)

---

## SPEC change requests

> Vuota: nessun finding suggerisce di modificare lo SPEC. La SPEC §12 (motore IA) è sufficientemente dettagliata e tutti i requisiti sono stati implementati senza ambiguità.

---

## Closure

- [ ] Tutti i `BLOCKER` risolti — 0 finding
- [ ] Tutti i `REQUIREMENT_GAP` risolti — F-003 ACKNOWLEDGED (deferred-F3 by design)
- [ ] Tutti i `Critical/High` `BUG` risolti — 0 finding
- [ ] Tutti i `Critical/High` `SECURITY` risolti — 0 finding
- [ ] `PERFORMANCE` che violano NFR risolti — F-002 Medium ma NFR-P-02 (5s) confermato dal gating; deferred-F4
- [ ] SPEC change requests con stato non-PENDING — 0 SCR
- [ ] Mini-fix DOC_GAP F-004 e CODE_QUALITY F-005 (Javadoc): da decidere con utente

**Review chiusa il**: _pending user disposition of findings_
**Commit di chiusura**: _pending_

---

## Sintesi proposta per l'utente

Nessun finding è bloccante. Le opzioni:

1. **Chiudere review così com'è**, con F-001/F-002/F-003/F-006/F-007 ACKNOWLEDGED e F-004/F-005 OPEN come "noti, fix non urgente". Andare direttamente alla sotto-fase TEST per la closure formale di Fase 2.
2. **Applicare i 2 mini-fix Javadoc** (F-004 nota su `hashAfterMove` non implementato, F-005 nota su `state` parameter intenzionalmente unused). Costo: 5 minuti, ~20 righe di doc. Poi closure.
3. **Tirare in F2 anche Task 2.14** (Zobrist-based `isThreefoldRepetition`) chiudendo F-001 + F-002 con un fix vero invece di rinviarli a F4. Costo: ~1 ora, refactor di `RuleEngine.computeStatus` + nuovi test. Lo SPEC §3.6 e ADR-021 non cambiano (semantica equivalente).
