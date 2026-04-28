# REVIEW — Fase 1: Dominio e regole (`shared`)

- **Data**: 2026-04-28
- **Commit codebase**: `78c7d12` (head di `feature/1-domain-and-rules`)
- **SPEC version**: 2.0 (2026-04-26)
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale |
|-----------------|---------:|-----:|-------:|----:|-------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |
| REQUIREMENT_GAP |        0 |    0 |      0 |   1 |      1 |
| BUG             |        0 |    0 |      0 |   0 |      0 |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |
| PERFORMANCE     |        0 |    0 |      0 |   1 |      1 |
| CODE_QUALITY    |        0 |    0 |      0 |   3 |      3 |
| DOC_GAP         |        0 |    0 |      0 |   2 |      2 |
| **Totale**      |        0 |    0 |      0 |   7 |      7 |

**Stato complessivo**: nessun finding bloccante. Le 4 leggi italiane, la regola "pedina non cattura dama", lo stop alla promozione e i quattro motivi di patta sono tutti coperti da test (unit + corpus + E2E). Coverage `shared` ≥ 90% bundle e ≥ 90% sul package `rules`.

---

## Acceptance criteria coverage

### SPEC §17 (acceptance globali)

| AC ID    | Descrizione                                                                                       | Status      | Note |
|----------|---------------------------------------------------------------------------------------------------|-------------|------|
| 17.1.6   | Tutte le 4 leggi della Dama Italiana applicate                                                    | ✅ COVERED   | `ItalianRuleEngineLawsTest` (7 test) + corpus 16 posizioni delle 4 leggi |
| 17.1.7   | Pedina che raggiunge la promozione durante una sequenza di catture **non continua**               | ✅ COVERED   | `ItalianRuleEngineSequencesTest#manReachingPromotionRowStopsTheSequence` + corpus `promozione-stop-001/002/003` |
| 17.1.8   | Pedina **non può** catturare la dama                                                                | ✅ COVERED   | `ItalianRuleEngineCapturesTest#manCannotCaptureKing` + corpus `pedina-non-cattura-dama-001/002/003` |
| 17.2.4   | Coverage ≥ 80% sul modulo `shared`                                                                  | ✅ COVERED   | JaCoCo gate a ≥ 90% (più stretto del minimo SPEC) |
| 17.2.5   | SAST SpotBugs senza warning High                                                                    | ✅ COVERED   | 0 warning High su tutti i moduli |

### Acceptance criteria operativi della Fase 1 (PLAN §2)

| ID    | Criterio                                                                              | Status      | Note |
|-------|----------------------------------------------------------------------------------------|-------------|------|
| A1.1  | `mvn -pl shared verify` BUILD SUCCESS                                                  | ✅           | 245 test verdi |
| A1.2  | Test corpus ≥ 48 posizioni distribuite per categoria                                   | ✅           | esattamente 48 (CLAUDE.md §2.4.4 vincolante) |
| A1.3  | `RuleEngineCorpusTest` passa per ogni posizione                                        | ✅           | 48/48 verdi |
| A1.4  | `EndToEndGameApiTest` esegue 3 partite complete                                        | ✅           | white-wins, black-wins, draw |
| A1.5  | Coverage `shared` ≥ 90% globale e ≥ 90% sul package `rules`                            | ✅           | JaCoCo `haltOnFailure=true` |
| A1.6  | Spotless OK, SpotBugs 0 High                                                            | ✅           | |
| A1.7  | `package-info.java` in tutti i sotto-package                                           | ✅           | `shared`, `domain`, `notation`, `rules` |
| A1.8  | TRACEABILITY aggiornato                                                                | ✅           | `tests/TRACEABILITY.md` |
| A1.9  | `SharedSmokeTest` rimosso                                                              | ✅           | rimosso al Task 1.11 |
| A1.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`                                    | ⚠️ vedere F-007 | `// TODO Fase 3` in `client/pom.xml` (fuori scope; non in `shared`) |
| A1.11 | AC §17.1.6/7/8 mappati a test specifici                                                | ✅           | tabella TRACEABILITY |

---

## Findings

### F-001 — [REQUIREMENT_GAP, Low] Triplice ripetizione: copertura test sottile

- **Posizione**: `shared/src/test/java/com/damaitaliana/shared/rules/ItalianRuleEngineStatusTest.java:125` (`replayingAValidGameDoesNotCrashRepetitionDetection`)
- **SPEC reference**: §3.6 — "triplice ripetizione".
- **Descrizione**: il metodo `isThreefoldRepetition` è implementato e logicamente corretto (replay da `GameState.initial()`, conteggio `(Board, sideToMove)` via `PositionKey`). Tuttavia il test unit valida solo che (a) la funzione non lancia eccezioni con history corta, (b) ritorna `ONGOING` quando nessuna posizione si ripete. Manca un test che verifichi l'evento "DRAW_REPETITION" su una sequenza reale di mosse che produce 3 occorrenze della stessa posizione. Anche il corpus non lo copre (le posizioni del corpus sono stateless senza history).
- **Proposta di fix**: aggiungere alla TEST PLAN della Fase 1 un test E2E di durata sufficiente per riprodurre la condizione (richiede di promuovere almeno una pedina per parte e poi danzare con i kings risultanti). In alternativa, ammettere come limite documentato della Fase 1 e tracciare un test mirato per la Fase 2 quando l'IA potrà produrre lunghe partite di simulazione.
- **Decisione utente (2026-04-28)**: deferred a F2 (opzione b). Quando l'IA simulerà migliaia di partite, sarà naturale costruire uno scenario di triplice ripetizione e verificare end-to-end. Tracciato come future-work in `AI_CONTEXT.md` "Note operative".
- **Status**: ACKNOWLEDGED (deferred to F2)

---

### F-002 — [DOC_GAP, Low] `IllegalMoveException` non documenta di essere `RuntimeException`

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/IllegalMoveException.java`
- **Descrizione**: il Javadoc della classe è di una riga ("Thrown by RuleEngine#applyMove..."). Non rende esplicito che è una `RuntimeException` (unchecked). I client del modulo (server, client UI nelle fasi successive) potrebbero aspettarsi una checked exception leggendo solo l'interfaccia `RuleEngine.applyMove(... throws IllegalMoveException)`.
- **Proposta di fix**: estendere il Javadoc con "This is an unchecked exception (RuntimeException). The {@code throws} clause on RuleEngine.applyMove is documentation-only — callers may catch it but are not forced to."
- **Status**: RESOLVED (commit `3b84e3c` — `fix(shared): apply REVIEW-fase-1 findings F-002, F-003, F-004`)

---

### F-003 — [CODE_QUALITY, Low] Javadoc di `ItalianRuleEngine` ancora task-by-task

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/ItalianRuleEngine.java:21-30`
- **Descrizione**: il Javadoc della classe enumera "Task 1.3 — non-capturing movement", "Task 1.4 — single captures", "Task 1.5+ — multi-jump and four laws". Era utile durante l'implementazione ma ora che la fase è completa diventa rumore: chi legge il codice in F2+ non ha contesto sui task numbers e potrebbe pensare che la classe sia ancora in costruzione.
- **Proposta di fix**: sostituire l'elenco "Task X" con un sommario delle responsabilità della classe (movement, captures incl. man-cannot-capture-king, multi-jump DFS, 4 laws of precedence, promotion, halfmove clock, status). Mantenere i riferimenti SPEC §3.x.
- **Status**: RESOLVED (commit `3b84e3c`)

---

### F-004 — [CODE_QUALITY, Low] `legalMoves` itera due volte sulla stream `occupiedBy(side)`

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/ItalianRuleEngine.java:46-72`
- **Descrizione**: nel ramo "no captures, fall back to simple moves" si esegue una seconda volta `board.occupiedBy(side).forEach(...)` invece di riusare le entries già visitate. Costa un'iterazione extra di 64 elementi sulla `Piece[]` in caso di nessuna cattura disponibile (caso comune nelle prime mosse). Impatto trascurabile in F1 ma utile da pulire prima che l'IA chiami `legalMoves` decine di migliaia di volte (F2).
- **Proposta di fix**: materializzare `List<Square> ourSquares = board.occupiedBy(side).toList();` una volta e riusarla in entrambi i rami.
- **Status**: RESOLVED (commit `3b84e3c`)

---

### F-005 — [PERFORMANCE, Low] `isThreefoldRepetition` è O(n²) per chiamata di `applyMove`

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/ItalianRuleEngine.java:170-185`
- **Descrizione**: ogni `applyMove` invoca `computeStatus` che, nei casi non immediatamente terminali, chiama `isThreefoldRepetition` la quale fa replay completo della history dalla posizione iniziale. Su una partita di 100 mosse ogni `applyMove` esegue un replay di lunghezza crescente → costo cumulativo O(n²). Per F1 è accettabile: i test corpus hanno history vuota e i test E2E hanno ≤ 2-3 mosse. Per F2 (IA che simula migliaia di nodi) è un collo di bottiglia.
- **Proposta di fix** (per F2):
  1. Aggiungere un campo `Map<PositionKey, Integer> repetitionCounts` (o un `int repetitionCount` per la posizione corrente) a `GameState`. `applyMove` lo aggiorna incrementalmente.
  2. Oppure usare `Zobrist hashing` (già previsto in F2 per la transposition table) per rendere `PositionKey` un `long` e mantenere un `Map<Long, Integer>` di partita.
- **Mitigazione interim**: il finding è documentato; il replay è corretto ma non ottimizzato.
- **Decisione utente (2026-04-28)**: deferred a F2 — l'ottimizzazione si farà insieme alla transposition table per minimax+alpha-beta.
- **Status**: ACKNOWLEDGED (deferred to F2)

---

### F-006 — [DOC_GAP, Low] Estensione `GameStatus` a 6 voci non ha un ADR dedicato

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/domain/GameStatus.java`
- **Descrizione**: SPEC §8.1 prescrive 4 valori (`ONGOING`, `WHITE_WINS`, `BLACK_WINS`, `DRAW`). L'implementazione ha 6 valori (estensione di `DRAW` in `DRAW_REPETITION`/`DRAW_FORTY_MOVES`/`DRAW_AGREEMENT`) con un metodo `isDraw()` di compatibilità. La motivazione è documentata nel Javadoc dell'enum, ma una scelta di modello che si discosta dallo SPEC merita un ADR esplicito (e potenzialmente uno SPEC change request).
- **Proposta di fix**: registrare ADR-023 — "GameStatus esteso con motivo di patta" oppure aprire SPEC change request CR-001 per allineare SPEC §8.1.
- **Decisione utente (2026-04-28)**: opzione A di CR-001 → SPEC §8.1 aggiornato (commit `e883445`) + ADR-023 registrato in `ARCHITECTURE.md`.
- **Status**: RESOLVED (commit `e883445` SPEC update + commit di chiusura review per ADR-023)

---

### F-007 — [CODE_QUALITY, Low] `// TODO Fase 3` in `client/pom.xml`

- **Posizione**: `client/pom.xml` (commento sopra `javafx-maven-plugin`)
- **Descrizione**: lasciato in eredità dalla Fase 0 (REVIEW-fase-0 F-007). Non viola CLAUDE.md §11 perché è esplicitamente collegato a una fase tracciata (Fase 3), ma resta un debito visivo. Fuori scope di Fase 1 (tocca `client`, non `shared`).
- **Proposta di fix**: rimuovere il TODO al Task 3.X quando il `mainClass` definitivo viene settato. Nessuna azione su questa fase.
- **Status**: ACKNOWLEDGED (fuori scope F1)

---

## SPEC change requests

### CR-001 — `GameStatus`: 4 voci letterali vs 6 voci con motivo di patta

- **Contesto**: durante review F-006 è emerso che lo SPEC §8.1 dichiara 4 valori ma l'implementazione ne ha 6. Si tratta di estensione retro-compatibile (esiste `isDraw()`) ma SPEC e codice divergono.
- **Proposta**: aggiornare SPEC §8.1 sostituendo `GameStatus` con:
  ```
  GameStatus { ONGOING, WHITE_WINS, BLACK_WINS, DRAW_REPETITION, DRAW_FORTY_MOVES, DRAW_AGREEMENT }
  con metodo helper isDraw() che identifica le tre voci di patta.
  ```
  Motivazione: la UI e i log dovranno comunque distinguere il motivo della patta (FR-RUL nel pannello regole, FR-NET-09 replay viewer); avere il valore già nell'enum evita campi paralleli.
- **Decisione utente (2026-04-28)**: APPROVED — opzione A. Applicato in commit `e883445` (`docs(spec): align §8.1 GameStatus to 6-value extension`). ADR-023 registrato in `ARCHITECTURE.md`.

---

## Closure

- [x] Tutti i `BLOCKER` risolti — _N/A: 0 BLOCKER_
- [x] Tutti i `REQUIREMENT_GAP` risolti — F-001 ACKNOWLEDGED (deferred a F2 per scelta utente)
- [x] Tutti i `Critical/High` `BUG` risolti — _N/A: 0 BUG_
- [x] Tutti i `Critical/High` `SECURITY` risolti — _N/A: 0 SECURITY_
- [x] `PERFORMANCE` che violano NFR risolti — F-005 non viola NFR-P-02 (ACKNOWLEDGED, deferred a F2)
- [x] SPEC change requests con stato non-PENDING — CR-001 APPROVED (commit `e883445`)

**Riepilogo finale**:
- 3 finding RESOLVED (F-002, F-003, F-004) tramite commit `3b84e3c`.
- 1 finding RESOLVED (F-006) tramite commit `e883445` (SPEC update) + ADR-023 in `ARCHITECTURE.md` (commit di chiusura).
- 2 finding ACKNOWLEDGED come deferred-to-F2 (F-001 thin-coverage del repetition test; F-005 O(n²) replay).
- 1 finding ACKNOWLEDGED fuori scope F1 (F-007 — `// TODO Fase 3` in `client/pom.xml`, sarà rimosso al Task 3.X).
- CR-001 APPROVED opzione A: SPEC §8.1 allineato all'enum a 6 voci.

**Review chiusa il**: 2026-04-28
**Commit di chiusura**: TBD (questo commit)
