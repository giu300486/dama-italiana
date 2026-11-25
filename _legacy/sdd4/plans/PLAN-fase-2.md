# PLAN — Fase 2: IA (`shared.ai`)

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 2.
- **SPEC version**: 2.0 (2026-04-26).
- **Data piano**: 2026-04-28.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.

---

## 1. Scopo della fase

Implementare il **motore IA** della Dama Italiana all'interno del modulo `shared`, sotto-package `com.damaitaliana.shared.ai`. Tre livelli di difficoltà giocabili (Principiante, Esperto, Campione) basati su Minimax con alpha-beta pruning e una funzione di valutazione modulare. L'IA è cancellabile, ha timeout per mossa, gira su virtual thread (Java 21) e non blocca la UI (`SPEC.md` §12.2).

Obiettivi specifici (SPEC §16 Fase 2):

1. **Minimax + alpha-beta** sopra l'API pubblica del `RuleEngine` (Fase 1).
2. **Funzione di valutazione modulare**: 5 componenti pesati (materiale, mobilità, posizione, sicurezza, centro — SPEC §12.1) ognuno isolato e testabile.
3. **Tre livelli** con profondità, timeout e ottimizzazioni distinte (SPEC §12.2):
   - Principiante: 2 ply, 500 ms, **25% rumore** (mossa subottima).
   - Esperto: 5 ply, 2000 ms, alpha-beta + move ordering, sempre ottimale entro la profondità.
   - Campione: 8 ply, 5000 ms, **+ iterative deepening + transposition table (Zobrist)**.
4. **Cancellabilità** (`Future#cancel(true)`) e **timeout** per mossa, eseguita su **virtual thread**.
5. **Acceptance SPEC §16 Fase 2**: Campione vince contro Principiante in ≥ **95% delle 100 partite simulate**.

**Out of scope**:
- UI per scegliere il livello (Fase 3).
- Salvataggio multi-slot (Fase 3).
- Endgame tablebase, opening book, neural networks.
- Tuning empirico dei pesi della valutazione (si parte dai default SPEC; eventuali aggiustamenti dopo dati di gioco reale, post-Fase 2).
- Personalità/stili di gioco diversi per livello (oltre a profondità/rumore).

---

## 2. Acceptance criteria

### 2.1 SPEC §16 Fase 2 (autoritativo)

> **Acceptance**: Campione vince contro Principiante in ≥ 95% delle 100 partite simulate.

### 2.2 Criteri operativi estesi

| ID    | Criterio                                                                                                                                                              | Verificabile come                                              |
|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| A2.1  | `mvn -pl shared verify` BUILD SUCCESS.                                                                                                                                | Output Maven                                                   |
| A2.2  | `AiTournamentSimulationTest`: 100 partite Campione vs Principiante, Campione ≥ 95 vittorie. Deterministico (seed RNG fisso).                                          | Output JUnit (test di gating)                                  |
| A2.3  | Campione produce mossa valida entro **5 secondi** in posizioni di metà partita (NFR-P-02). Soglia hard nel test: timeout = 5s, deve restituire qualcosa di legale.    | `AiPerformanceTest#campionRespondsWithinTimeout`               |
| A2.4  | Esperto produce mossa valida entro **2 secondi**. Principiante entro **500 ms**.                                                                                       | `AiPerformanceTest` parametrizzato                             |
| A2.5  | Cancellazione: `Future.cancel(true)` durante una ricerca interrompe l'IA entro **200 ms** e restituisce la migliore mossa trovata fino a quel momento (graceful).       | `AiCancellationTest`                                           |
| A2.6  | Coverage modulo `shared` ≥ **90%** mantenuto (gate Fase 1) anche dopo l'aggiunta del package `ai`. Coverage del package `com.damaitaliana.shared.ai` ≥ **85%**.        | JaCoCo `haltOnFailure=true` con regola dedicata                |
| A2.7  | Spotless OK, SpotBugs 0 High su tutto `shared`.                                                                                                                         | Output Maven                                                   |
| A2.8  | `package-info.java` per `shared.ai` documenta: scopo, riferimento SPEC §12, vincoli (puro, no framework), regole di estensione (aggiungere componenti di valutazione). | Lettura visiva                                                 |
| A2.9  | `tests/TRACEABILITY.md` aggiornato: FR-SP-02, NFR-P-02, AC §17.1.1 (parziale), AC F2 sotto.                                                                            | Lettura visiva                                                 |
| A2.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`.                                                                                                                  | grep                                                           |
| A2.11 | Test corpus regole italiane (Fase 1, 48 posizioni) **continua a passare** (regression).                                                                                | `RuleEngineCorpusTest` verde                                   |
| A2.12 | Determinismo dei test simulazione: stesso seed → stesso risultato bit-per-bit.                                                                                          | Asserzione esplicita di hash sequenza partite                  |
| A2.13 | Golden test su posizioni tattiche: in 5 posizioni con mossa migliore nota, Esperto e Campione trovano la mossa attesa entro la profondità nominale.                    | `AiTacticalPositionsTest`                                      |

---

## 3. Requisiti SPEC coperti

### 3.1 Funzionali (FR)

| FR ID    | SPEC ref | Coperto in F2 come                                                                                                            |
|----------|----------|-------------------------------------------------------------------------------------------------------------------------------|
| FR-SP-01 | §4.1     | _Indiretto_: l'IA è la controparte senza account né connessione. UI in F3, ma `AiEngine.chooseMove(state)` è già richiamabile. |
| FR-SP-02 | §4.1     | **Coperto pienamente**: 3 livelli implementati come sotto-classi/strategie di `AiEngine`.                                      |
| FR-SP-03 | §4.1     | _Indiretto_: l'IA gioca indifferentemente Bianco o Nero (prende `GameState.sideToMove` dall'API).                              |

### 3.2 Non funzionali (NFR)

| NFR ID   | SPEC ref | Coperto in F2 come                                                                                                |
|----------|----------|-------------------------------------------------------------------------------------------------------------------|
| NFR-P-02 | §5       | **Coperto direttamente**: timeout 5s per Campione, validato in `AiPerformanceTest`.                                |
| NFR-M-01 | §5       | Coverage modulo ≥ 90% mantenuto. Soglia hard ≥ 80% per SPEC; F2 segue lo standard di F1 (≥ 90%).                  |
| NFR-M-04 | §5       | Spotless Google Java Style su tutto il codice F2.                                                                  |
| NFR-P-01 | §5       | _Indiretto_: l'IA su virtual thread non blocca il main thread JavaFX (validato in F3 effettivamente).             |

### 3.3 Acceptance criteria globali (§17)

| AC §17  | Descrizione                                                                       | Coperto in F2 da                                                                |
|---------|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| 17.1.1  | Partita SP vs IA Campione si conclude entro 30 minuti senza crash.                | _Parziale_: F2 valida correttezza+performance del solver; partita end-to-end via UI in F3. `AiE2EGameTest` valida la chiusura via API pura. |
| 17.2.4  | Coverage ≥ 80% modulo `shared`.                                                   | JaCoCo gate (mantenuto da F1).                                                  |
| 17.2.5  | SAST SpotBugs senza warning High.                                                 | SpotBugs gate (mantenuto da F1).                                                |

### 3.4 ADR coinvolti

| ADR     | Vincolo per Fase 2                                                                                                     |
|---------|------------------------------------------------------------------------------------------------------------------------|
| ADR-001 | Java 21 LTS → uso di `Thread.ofVirtual()`, sealed types per `AiEngine`, pattern matching `switch`.                     |
| ADR-013 | Solo regole **Dama Italiana FID** → l'IA usa `RuleEngine` di F1 senza varianti.                                        |
| ADR-015 | 3 livelli (Principiante / Esperto / Campione).                                                                          |
| ADR-021 | `PositionKey(Board, sideToMove)` per identità posizione. La transposition table di Campione lo usa come chiave (con hash Zobrist materializzato per perf). |

**Possibili nuovi ADR generati da F2** (da formalizzare in `ARCHITECTURE.md`):

- **ADR-024 — Architettura motore IA**: `AiEngine` interface + `MinimaxAiEngine` con strategy `SearchPolicy` (depth, time, ordering, TT on/off, noise). Vedi stop point §7.1.
- **ADR-025 — Funzione di valutazione modulare**: `Evaluator` interface + 5 componenti `EvaluationTerm` pesati (materiale, mobilità, avanzamento, sicurezza-bordi, controllo-centro). Composizione via `WeightedSumEvaluator`. Vedi stop point §7.2.
- **ADR-026 — Hashing Zobrist e transposition table**: `ZobristHasher` con tabella di numeri casuali deterministica; TT `Map<long, TtEntry>` con politica di sostituzione "always replace + age". Vedi stop point §7.3.
- **ADR-027 — Modello di cancellazione e timeout**: `CancellationToken` cooperativo + `VirtualThreadAiExecutor` che interpreta `Future.cancel(true)` come "ferma e ritorna best so far". Vedi stop point §7.4.
- **ADR-028 — Determinismo del rumore "Principiante"**: rumore implementato come `NoisyAiEngine` decoratore di `MinimaxAiEngine` con `RandomGenerator` iniettabile (default: `SplittableRandom` con seed da clock; nei test, seed fisso). Vedi stop point §7.5.

---

## 4. Decomposizione in task

I task sono ordinati. Ogni task termina con `mvn -pl shared verify` verde e un commit Conventional Commits.

> **Nota branching** (CLAUDE.md §4.3): tutto il lavoro di Fase 2 si svolge sul branch `feature/2-ai` staccato da `develop`, mergiato `--no-ff` su `develop` a fine fase. Tag `v0.2.0` sul commit di merge in `main`.

### Task 2.1 — `Evaluator` skeleton + componente Materiale

**Output** (in `shared/src/main/java/com/damaitaliana/shared/ai/`):
- `Evaluator.java` (interface):
  ```java
  public interface Evaluator {
    /** Score in centipawns dal punto di vista di {@code perspective}.
     *  Positivo = vantaggioso per {@code perspective}. */
    int evaluate(GameState state, Color perspective);
  }
  ```
- `EvaluationTerm.java` (interface + Javadoc su come scrivere un termine puro).
- `MaterialTerm.java`: pedina = +100, dama = +300 (SPEC §12.1).
- `WeightedSumEvaluator.java`: composizione di `List<EvaluationTerm>` con peso. Default factory `defaultEvaluator()` che restituisce `new WeightedSumEvaluator(List.of(new MaterialTerm()))` in questo task; gli altri termini si aggiungono nei task seguenti.

**Test** (`MaterialTermTest`, `WeightedSumEvaluatorTest`): ~10 metodi.
- `materialIsZeroAtStart` (12 vs 12 men).
- `materialPositiveWhenWhiteHasMoreMen`.
- `materialReflectsKingValue` (1 dama nera = 3 pedine bianche).
- `perspectiveFlipsSign`.
- `compositeReturnsWeightedSum`.

**Dipendenze**: nessuna oltre Fase 1.

---

### Task 2.2 — Componenti di valutazione (Mobilità, Avanzamento, Sicurezza, Centro)

**Output** (in `com.damaitaliana.shared.ai.evaluation/` o `ai/`):
- `MobilityTerm.java`: `+W * (mosseLegaliPerspective - mosseLegaliOpponent)`. Peso default: 5 centipawn per mossa.
- `AdvancementTerm.java`: somma per ogni pedina della distanza dalla riga di promozione. Peso default: 2 centipawn per riga.
- `EdgeSafetyTerm.java`: bonus per pezzi sui bordi (file 0 / file 7) che non possono essere catturati. Peso default: 8 centipawn.
- `CenterControlTerm.java`: bonus per pezzi sulle 4 case centrali (rank 3-4 / file 3-4 sulle case scure). Peso default: 10 centipawn.

Per ogni termine: classe pubblica + Javadoc che cita SPEC §12.1 + test dedicato.

**Vincoli**:
- Termini puri, deterministici, senza stato.
- Non chiamano `RuleEngine.applyMove` (sarebbe troppo costoso); usano `RuleEngine.legalMoves` solo per `MobilityTerm` (al peggio O(N) sul board).
- Pesi configurabili come `final int weight` con default come da SPEC.

**Test**: ~15 metodi totali. Per ogni termine almeno 3 casi: posizione neutra, posizione vantaggiosa per Bianco, vantaggiosa per Nero.

**Coverage check**: dopo questo task, `WeightedSumEvaluator.defaultEvaluator()` compone tutti e 5 i termini.

**Dipendenze**: Task 2.1.

---

### Task 2.3 — `MoveOrderer` (cattura > centro > altro)

**Output**: `com.damaitaliana.shared.ai.search.MoveOrderer.java`.

- Interface `MoveOrderer { List<Move> order(List<Move> moves, GameState state); }`.
- Implementazione `StandardMoveOrderer` (SPEC §12.1):
  1. Catture prima delle mosse semplici (le leggi italiane le rendono già le uniche legali in molte posizioni; ridondante ma utile per coerenza).
  2. Fra catture: priorità a sequenze più lunghe (proxy della legge della quantità).
  3. Fra mosse semplici: priorità a destinazioni centrali (case 14, 15, 18, 19 in notazione FID).
  4. Tie-break stabile: id FID di `from` crescente (per determinismo).

**Test**: ~6 metodi.
- `capturesComeFirst`.
- `longerCaptureBeforeShorter`.
- `centralDestinationBeforeEdge`.
- `stableOrderingForEqualPriority`.

**Dipendenze**: Task 2.1.

---

### Task 2.4 — `MinimaxSearch` con alpha-beta (Esperto baseline)

**Output**: `com.damaitaliana.shared.ai.search.MinimaxSearch.java`.

- Algoritmo **negamax** (variante simmetrica di minimax) con alpha-beta pruning.
- Firma:
  ```java
  public final class MinimaxSearch {
    SearchResult search(GameState state, int depth, Evaluator eval,
                        MoveOrderer orderer, CancellationToken cancel);
  }
  public record SearchResult(Move bestMove, int score, int depthReached, long nodesVisited) { }
  ```
- Profondità in **ply** (mezza-mossa).
- Score: centipawn dal punto di vista di `state.sideToMove()`.
- Termina ricorsione se: `depth == 0` (ritorna `eval.evaluate`), `state.status().isWin()` o `isDraw()` (ritorna +∞/-∞/0 con padding per mate-in-N), `cancel.isCancelled()` (eccezione `SearchCancelledException`, propagata).
- **Nessuna** transposition table in questo task (semplicità per Esperto).

**Test** (`MinimaxSearchTest`): ~12 metodi.
- `depthZeroReturnsStaticEval`.
- `depthOnePicksImmediateCapture`.
- `findsForcedWinInTwoPly`.
- `prefersAvoidingForcedLossInTwoPly`.
- `respectsAlphaBetaCutoff` (usa un `Evaluator` mock counting).
- `cancellationStopsSearch`.
- `terminalPositionsReturnExtremeScore`.
- `nodesVisitedIsTrackedMonotonically`.
- `bestMoveIsAmongLegalMoves`.

**Dipendenze**: Task 2.2, Task 2.3.

---

### Task 2.5 — Iterative deepening + cancellazione cooperativa

**Output**:
- `com.damaitaliana.shared.ai.search.IterativeDeepeningSearch.java`.
- `com.damaitaliana.shared.ai.CancellationToken.java`.

**`CancellationToken`**:
- Interface piccola: `boolean isCancelled()`, `void throwIfCancelled()`.
- Implementazioni: `CancellationToken.never()`, `CancellationToken.deadline(Instant)`, `CancellationToken.composite(token1, token2)`.

**`IterativeDeepeningSearch`**:
- Esegue `MinimaxSearch` a profondità crescente (1, 2, 3, ..., maxDepth).
- Conserva il `bestMove` dell'iterazione completata più profonda.
- Se `CancellationToken.isCancelled()` durante l'iterazione k, ritorna il `bestMove` dell'iterazione k-1 (graceful).
- Se l'iterazione 1 viene cancellata prima di restituire, fallback alla **prima mossa legale** (non NPE; SPEC §12 impone "non blocca la UI" → mai null).
- Move ordering: nelle iterazioni successive, la `bestMove` precedente è messa per prima (PV-first).

**Test** (`IterativeDeepeningSearchTest`): ~8 metodi.
- `returnsDeepestCompletedDepth`.
- `cancellationMidIterationReturnsPreviousDepthBestMove`.
- `cancellationBeforeFirstResultReturnsAnyLegalMove`.
- `pvMoveIsTriedFirstInNextIteration` (mock orderer).
- `noCrashOnTerminalState`.

**Dipendenze**: Task 2.4.

---

### Task 2.6 — Hashing Zobrist + Transposition Table (Campione)

**Output**:
- `com.damaitaliana.shared.ai.search.ZobristHasher.java`.
- `com.damaitaliana.shared.ai.search.TranspositionTable.java`.

**`ZobristHasher`**:
- Tavole di numeri casuali a 64 bit per ogni `(pieceKind, color, square)` + 1 per `sideToMove == BLACK`.
- Inizializzazione **deterministica** con seed fisso (es. `0xDAMA_2026_ITALIANA_XOR`) → stesso jar = stessi hash.
- `long hash(GameState state)` ricalcola da zero.
- `long hashAfterMove(long oldHash, Move m, GameState before, GameState after)` per update incrementale.

**`TranspositionTable`**:
- `record TtEntry(long hash, int score, int depth, NodeType type, Move bestMove, int age)`.
- `NodeType { EXACT, LOWER_BOUND, UPPER_BOUND }`.
- Storage: `TtEntry[]` ad array circolare con dimensione potenza di 2 (default 2^20 entry = ~32 MB di RAM); index = `hash & (size-1)`.
- Politica di sostituzione: **always replace + prefer deeper / newer**.
- API: `Optional<TtEntry> probe(long hash)`, `void store(TtEntry entry)`, `void clearOldEntries(int currentAge)`.

**Integrazione in `MinimaxSearch`**:
- Variante `MinimaxSearch.searchWithTT(...)` che probe la TT prima di valutare; se hit con `depth >= depthRemaining`, usa il valore (rispettando i bounds `LOWER_BOUND`/`UPPER_BOUND`).
- Salva sempre il risultato in TT al ritorno.

**Test** (`ZobristHasherTest`, `TranspositionTableTest`, `MinimaxSearchTtTest`): ~12 metodi.
- `sameStateGivesSameHash` (deterministico).
- `differentStatesGiveDifferentHashes` (su 1000 random states; collisione ammessa entro p < 0.001).
- `hashAfterMoveEqualsHashOfNewState` (incrementale = ricalcolato).
- `ttHitReducesNodesVisited` (rispetto a `MinimaxSearch` baseline a parità di profondità).
- `ttDoesNotChangeBestMoveCorrectness` (stessa scelta del baseline su 5 golden positions).

**Dipendenze**: Task 2.5.

---

### Task 2.7 — `AiEngine` interface + 3 livelli

**Output** (in `com.damaitaliana.shared.ai/`):
- `AiEngine.java`:
  ```java
  public sealed interface AiEngine permits PrincipianteAi, EspertoAi, CampioneAi {
    /** Sincrono: ritorna la migliore mossa. Rispetta cancel/timeout. */
    Move chooseMove(GameState state, CancellationToken cancel);
    AiLevel level();
  }
  public enum AiLevel { PRINCIPIANTE, ESPERTO, CAMPIONE }
  ```
- `PrincipianteAi.java`: `MinimaxSearch` depth=2, niente TT, **rumore 25%**:
  - Calcola `bestMove`. Con probabilità 25% (RNG iniettato), sostituisce con una random fra le altre legali (se ce ne sono).
  - Se è disponibile una sola mossa legale, gioca quella senza rumore.
- `EspertoAi.java`: `MinimaxSearch` depth=5, move ordering, niente TT, niente rumore.
- `CampioneAi.java`: `IterativeDeepeningSearch` maxDepth=8, move ordering, **TT abilitata**, niente rumore.
- Factory: `AiEngine.forLevel(AiLevel level, RandomGenerator rng)`.

**Vincoli**:
- Le 3 classi sono **finali** e nel sealed con `AiEngine`.
- Configurazione (depth, time budget, weights) costanti pubbliche `static final` documentate con SPEC ref.
- Nessun campo statico mutabile (per concorrenza).

**Test** (`AiEngineFactoryTest`, smoke test per livello): ~6 metodi.
- `forLevelReturnsCorrectImpl`.
- `principianteIsNoisyWithGivenRng` (con seed fisso, verifica che rumore agisce).
- `espertoIsDeterministic` (stesso state → stessa mossa, indipendente dal seed).
- `campioneIsDeterministic`.
- `allLevelsReturnLegalMove` su `GameState.initial()`.

**Dipendenze**: Task 2.6.

---

### Task 2.8 — `VirtualThreadAiExecutor` + integrazione timeout

**Output**: `com.damaitaliana.shared.ai.VirtualThreadAiExecutor.java`.

- Wrapper attorno a `Thread.ofVirtual().factory()` + `ExecutorService`.
- API:
  ```java
  public final class VirtualThreadAiExecutor implements AutoCloseable {
    Future<Move> submitChooseMove(AiEngine ai, GameState state, Duration timeout);
    @Override public void close();   // shutdownNow
  }
  ```
- Internamente:
  - Crea un `CancellationToken.deadline(now + timeout)`.
  - Lancia `ai.chooseMove(state, token)` su virtual thread.
  - Restituisce `Future<Move>`. Se `Future.cancel(true)` viene chiamato dall'esterno, propaga interrupt al thread; il `chooseMove` rispetta il token e ritorna la migliore mossa trovata.
- Garanzia: il `Future` ritorna **una mossa legale** entro `timeout + 200 ms` di tolleranza (NFR-P-02 + soglia di overhead). Mai null.

**Test** (`VirtualThreadAiExecutorTest`): ~6 metodi.
- `submitsAndReturnsMove`.
- `respectsTimeoutCampione` (5s).
- `cancellationReturnsBestSoFar` (mocked engine che rilascia best ad ogni iterazione).
- `multipleConcurrentSubmissions` (2 thread → 2 risultati indipendenti).
- `closeShutsDownExecutor`.

**Dipendenze**: Task 2.7.

---

### Task 2.9 — Posizioni tattiche golden

**Output**: `shared/src/test/java/com/damaitaliana/shared/ai/AiTacticalPositionsTest.java`.

- 5 posizioni curate dove la mossa migliore è nota e **calcolabile entro la profondità nominale**:
  1. **Mate in 1**: il sideToMove può catturare l'ultima dama avversaria → vittoria immediata. Esperto e Campione devono trovarla a depth ≥ 2.
  2. **Mate in 3 ply**: forzato. Solo Esperto (5 ply) e Campione (8 ply) la trovano.
  3. **Cattura sacrificale vincente**: cattura immediata locale è subottima; la sequenza migliore richiede profondità 4. Solo Campione la trova affidabilmente.
  4. **Difesa forzata**: la sideToMove è in svantaggio; esiste una sola mossa che evita la perdita di un pezzo entro 4 ply.
  5. **Promozione strategica**: vale di più portare una pedina in promozione a 3 ply che catturare una pedina libera adesso.

Posizioni codificate con la stessa convenzione del corpus (FID 1-32 + JSON disgiunto), in `shared/src/test/resources/ai-tactical-positions.json`.

Loader riutilizza `CorpusLoader` di Fase 1 (estensione minore se serve).

**Test parametrizzato** sui livelli: per ogni posizione, dichiara la profondità minima a cui la mossa migliore è attesa.

**Dipendenze**: Task 2.7.

---

### Task 2.10 — Test di simulazione torneo (acceptance principale)

**Output**: `shared/src/test/java/com/damaitaliana/shared/ai/AiTournamentSimulationTest.java`.

**Contenuto** (gating della Fase 2):

- 100 partite simulate Principiante vs Campione, alternando colori (50 con Campione bianco, 50 con Campione nero).
- Per ciascuna partita: parte da `GameState.initial()`, ogni IA usa il suo `AiEngine` con timeout fissati come da SPEC §12.2.
- Limite hard di mosse per partita: 200 ply (oltre, la partita è considerata patta tecnica → conta come **non-vittoria** per Campione, conservativo).
- Determinismo: `Principiante` usa `SplittableRandom(42)` come seed; ogni partita ha un sub-seed `42 + i`. Test è **bit-deterministico**: stesso run → stesso risultato.
- Asserzione: `campionWins >= 95`.
- Soglia di sicurezza: la simulazione richiede tempo. Stimare: se Principiante impiega 100 ms/mossa e Campione 500 ms/mossa (depth 8 raramente esauribile in early-game) → `100 * 100 ply * (100+500) ms = ~ 600 s = 10 min`. **Marcato come `@Tag("slow")`** ed eseguito di default (ma non in fast loop). Fallback `@Tag("acceptance")` per CI dedicata.

**Stop point §7.6**: confermare che 10 minuti di test simulazione è accettabile in `mvn verify` standard, oppure spostarlo in profilo Maven dedicato.

**Test secondari**:
- `EspertoBeatsPrincipianteOver100Games` con soglia ≥ 70 vittorie (sanity check intermedio).
- `CampioneBeatsEspertoOver50Games` con soglia ≥ 60% (sanity informativo, **non gating**).

**Dipendenze**: Task 2.9.

---

### Task 2.11 — Test di performance (NFR-P-02)

**Output**: `shared/src/test/java/com/damaitaliana/shared/ai/AiPerformanceTest.java`.

- Per ogni livello, su 5 posizioni di metà partita selezionate, verifica che `chooseMove` ritorni entro il timeout SPEC §12.2:
  - Principiante: < 500 ms (margine: timeout deadline 700 ms → mai supera; controllo che ≤ 500 ms wall-clock).
  - Esperto: < 2000 ms.
  - Campione: < 5000 ms.
- Misurazione con `System.nanoTime()` direttamente attorno al `submit + Future.get(timeout)`.
- Test **non sensibile a JIT cold start**: warmup di 1 chiamata scartata.
- Test marcato `@Tag("performance")`. Esecuzione di default ma con tolleranza 1.5x (per ambienti CI lenti); soglia hard non-tollerata = 1.5 × SPEC. Logging di "tempo medio + p95" per visibilità.

**Stop point §7.7**: tolleranza 1.5x è ragionevole? Oppure meglio gate con tolleranza zero su SPEC e marcare `@Disabled` su CI lenti?

**Dipendenze**: Task 2.7.

---

### Task 2.12 — Test di cancellazione

**Output**: `shared/src/test/java/com/damaitaliana/shared/ai/AiCancellationTest.java`.

- Sottomette `Campione.chooseMove` su una posizione complessa, attende 100 ms, chiama `Future.cancel(true)`.
- Asserzioni:
  - Il `Future.get()` restituisce `CancellationException` (semantica standard) **oppure** una mossa valida (se la cancel arriva dopo che IDS ha già completato l'iterazione e restituito).
  - L'`AiEngine.chooseMove` interno termina entro **200 ms** dalla cancel (verificato via stopwatch).
  - Variante 2: `chooseMove` chiamato direttamente con `CancellationToken.deadline(short)` ritorna `Move` (non eccezione) con la migliore mossa fino a quel momento.

**Dipendenze**: Task 2.7.

---

### Task 2.13 — Coverage gate, JaCoCo, cleanup

**Output**:
- `shared/pom.xml` aggiornato:
  - JaCoCo: aggiunta regola `PACKAGE com.damaitaliana.shared.ai` ≥ **85%** line + branch (gate).
  - Mantiene 90% modulo + 90% package `rules` da F1.
- `mvn -pl shared verify`: BUILD SUCCESS con tutte le soglie.
- Verifica che il package `ai` non sfori la complessità SpotBugs (max cyclomatic 15).

**Stop point §7.8**: soglia coverage `ai` 85% (proposta) — la valutazione modulare ha branch facili da coprire ma la search ha rami tipo "early exit" non facili. Opzioni:
- A: 85% line + 85% branch (proposta).
- B: 90% line + 80% branch (più severo su line, più tollerante su branch).
- C: 80% line + branch (allineato a NFR-M-01 di base).

**Dipendenze**: Task 2.12.

---

### Task 2.14 — (Opzionale) Risolvere F-005 di REVIEW-fase-1: `isThreefoldRepetition` con Zobrist

**Stop point §7.9**: includere in F2 oppure rinviare?

**Contesto**: REVIEW-fase-1 F-005 ACKNOWLEDGED-deferred-F2. `RuleEngine.computeStatus` usa replay della history per la triplice ripetizione (O(n²)). Con Zobrist disponibile (Task 2.6), si può:
- Rendere disponibile l'hasher anche al `RuleEngine` (tramite costruttore opzionale o factory method `ItalianRuleEngine.withHasher(ZobristHasher)`).
- `computeStatus` tiene un `Map<Long, Integer>` di occorrenze hash, costruito incrementalmente in `applyMove`. **Cambia l'API di `RuleEngine`?** No: Zobrist è interno; la firma resta uguale. Però `GameState` non è più puro derivato dalla history (nuovo trade-off, già discusso in PLAN F1 §7.3 opzione B).

**Output (se incluso)**:
- Refactor `ItalianRuleEngine.computeStatus`: O(1) per controllo ripetizione, costo amortizzato O(1) in `applyMove` per update.
- Test corpus `tripla-ripetizione-*` continua a passare.
- Aggiornamento ADR-021 con nota di evoluzione.

**Proposta**: **escludere** da F2 e mantenerla deferred a F4 (quando il `core-server` introdurrà `Match` con storage persistente e i requisiti di performance ripetizione diventeranno reali). F2 resta focalizzata sull'IA.

**Dipendenze**: Task 2.6 (se incluso). Nessuna se rinviato.

---

### Task 2.15 — Documentazione, ADR, TRACEABILITY

**Output**:
- `shared/src/main/java/com/damaitaliana/shared/ai/package-info.java`: scopo, riferimento SPEC §12, vincoli (puro, no framework, no I/O), regola di estensione (come aggiungere un `EvaluationTerm`).
- `shared/src/main/java/com/damaitaliana/shared/ai/search/package-info.java`.
- `ARCHITECTURE.md`: ADR-024 ÷ ADR-027 (e ADR-028 se confermato, vedi §7.5). Aggiornamento ADR-015 con nota implementativa.
- `tests/TRACEABILITY.md`: righe per FR-SP-02 (3 livelli), NFR-P-02 (timeout Campione), AC §17.1.1 parziale, AC F2 (A2.1 ÷ A2.13).
- `CHANGELOG.md`: voce in `[Unreleased]` con riepilogo F2.
- `AI_CONTEXT.md`: stato avanzato a "Fase 2 — IMPLEMENTA completa, REVIEW pending".

**Dipendenze**: Task 2.13 (e Task 2.14 se incluso).

---

## 5. Strategia di test (Fase 2)

Riferimento: CLAUDE.md §2.4.

| Tipo                                | Numero indicativo | Tooling           | Cosa testa                                                                                                          |
|-------------------------------------|------------------:|-------------------|---------------------------------------------------------------------------------------------------------------------|
| **Unit (Evaluator components)**     | ~15               | JUnit 5 + AssertJ | `MaterialTerm`, `MobilityTerm`, `AdvancementTerm`, `EdgeSafetyTerm`, `CenterControlTerm`. Termini puri.             |
| **Unit (MoveOrderer)**              | ~6                | JUnit 5           | Ordering capture-first / longer-first / center-first / stable.                                                      |
| **Unit (MinimaxSearch)**            | ~12               | JUnit 5 + Mockito | Depth, alpha-beta cutoff, terminal positions, mate-in-N detection, cancellation propagation.                        |
| **Unit (IterativeDeepening)**       | ~8                | JUnit 5           | PV-first, graceful cancellation, deadline respect, fallback to legal move.                                          |
| **Unit (Zobrist + TT)**             | ~12               | JUnit 5           | Determinismo, no-collision empirica, hash incrementale, TT bound semantics.                                          |
| **Unit (AiEngine factory + livelli)** | ~6              | JUnit 5           | Factory routing, principiante noise determinismo, livelli deterministici (esperto/campione).                        |
| **Unit (VirtualThreadAiExecutor)**  | ~6                | JUnit 5 + concurrent | Submit/cancel/timeout/concurrenza/close.                                                                         |
| **Tactical positions (parametrizzato)** | 5 × 3 livelli (15) | JUnit 5 + JSON | Mossa attesa entro profondità nominale.                                                                             |
| **Performance (`@Tag("performance")`)** | 5 × 3 (15)     | JUnit 5 + nanoTime | NFR-P-02 + corrispettivi Esperto/Principiante.                                                                       |
| **Cancellation**                     | ~3                | JUnit 5            | `Future.cancel(true)` graceful + token deadline + timeout legalMove fallback.                                       |
| **Tournament simulation (`@Tag("slow")` gating)** | 3 (Camp vs Princ, Esp vs Princ, Camp vs Esp) | JUnit 5 deterministic | **A2.2: Campione ≥ 95/100 vs Principiante**. Sanity Esperto e Campione vs Esperto.                  |
| **Regression corpus F1**            | 48 (ereditati)    | JUnit 5 (Fase 1)   | Rerun, garantisce che F2 non rompa il `RuleEngine`.                                                                 |

**Coverage target Fase 2**:

| Scope                                          | Soglia minima (gate)            | Target operativo |
|------------------------------------------------|---------------------------------|------------------|
| `shared` (intero modulo)                       | **90%** line + branch (gate F1) | 92-95%           |
| `com.damaitaliana.shared.rules` (mantenuto)    | **90%** line + branch           | 95%              |
| `com.damaitaliana.shared.ai` (nuovo)           | **85%** line + branch (proposta — vedi §7.8) | 88%       |

**Naming convention** (CLAUDE.md §2.4.5):
- Unit: `<ClasseProduzione>Test`. Stile: `should<Espressione>_when<Condizione>`.
- Posizioni tattiche: `AiTacticalPositionsTest` parametrizzato su id posizione.
- Simulazione: `AiTournamentSimulationTest` (gating); `AiPerformanceTest` (perf); `AiCancellationTest` (cancel).

**Tag JUnit**:
- `@Tag("slow")` per simulazione 100 partite (~10 min).
- `@Tag("performance")` per gating timeout.
- I test rapidi (unit) sono untagged ed eseguiti sempre.
- `mvn verify` esegue **tutto** di default. Fast loop locale: `mvn -pl shared test -DexcludedGroups=slow,performance` (~30 s).

---

## 6. Rischi e mitigazioni

| ID    | Rischio                                                                                                  | P    | I     | Mitigazione |
|-------|----------------------------------------------------------------------------------------------------------|------|-------|-------------|
| R-01  | **A2.2 fallisce**: Campione non vince 95/100 contro Principiante.                                         | Bassa| Alto  | Principiante è 2 ply + 25% rumore: gioca tatticamente molto male. Letteratura su minimax depth 8 vs depth 2 conferma > 95% in giochi simili. Mitigazione attiva: se < 95%, alzare Campione a depth 9 (ancora entro 5s con TT). Fallback estremo: ridurre rumore Principiante? **No**: viola SPEC. Il rumore è specificato. Tuning solo lato Campione. |
| R-02  | **NFR-P-02 violato su CI lento**: Campione > 5s.                                                          | Media| Alto  | Iterative deepening + cancellazione: a 5s si ferma e ritorna best-so-far. Quindi mai supera il timeout. Test perf con tolleranza 1.5x (vedi §7.7). |
| R-03  | TT collisioni Zobrist causano scelte mossa sbagliate.                                                    | Bassa| Medio | TT salva la `bestMove` insieme allo score; in caso di collisione la mossa potrebbe essere illegale per la posizione corrente → check `legalMoves.contains(ttMove)` prima di usarla; se illegale, scarta entry e calcola normalmente. Test dedicato. |
| R-04  | `Thread.cancel(true)` su virtual thread non interrompe se il codice non controlla il token (busy loop).  | Media| Alto  | Disciplina: il `MinimaxSearch` invoca `cancel.throwIfCancelled()` all'ingresso di ogni nodo (overhead trascurabile). Test cancellazione ne verifica il rispetto entro 200 ms. |
| R-05  | Determinismo simulazione: `SplittableRandom` su virtual thread può avere fork inaspettati.                | Media| Medio | Il rumore Principiante è l'**unica** fonte di non-determinismo. Viene iniettato come `RandomGenerator` nei test con seed fisso, **non legato al thread**. Asserzione: stesso seed → stesso conteggio vittorie. |
| R-06  | Coverage `ai` < 85% per branch di alpha-beta non raggiungibili in test rapidi.                            | Media| Basso | Test mirati con `Evaluator` mockato che forza specifici cutoff (golden + ad-hoc); `MoveOrderer` mock per indurre branch coverage. |
| R-07  | Esplosione tempo di esecuzione `mvn verify` sopra 15 minuti.                                              | Media| Medio | Tag `@Tag("slow")` separa la simulazione. Default `mvn verify` può essere configurato per skiparla via property `-DskipSlowTests=true` (default false in dev locale, true in CI rapida — TBD §7.6). |
| R-08  | Pesi default della valutazione mal bilanciati → Campione gioca male nonostante depth 8.                  | Media| Alto  | SPEC §12.1 dà valori indicativi (100/300 per pezzi). Mobilità, posizione, sicurezza, centro: pesi proposti in Task 2.2 derivati da letteratura (e.g. Schaeffer 1996). Sanity check via tactical positions: se Campione fallisce mate-in-3 noto, stop e tuning. |
| R-09  | `AdvancementTerm` ed `EdgeSafetyTerm` interagiscono in modo controintuitivo (es. AI tiene pedine nei bordi e non avanza). | Media | Medio | Test per termine in isolamento + test di gioco completo (simulazione) cattura regressioni globali. Se simulazione patta troppo spesso, rivedere pesi. |
| R-10  | `Move.equals` non semanticamente corretto per TT: due `CaptureSequence` con stesso from/to ma diverso path interno. | Bassa | Medio | `Move` ha già `equals` (record). Ricontrollo invariante in F1; nessun problema noto. Test esplicito `differentPathsAreDifferentMoves`. |
| R-11  | `Iterative deepening` non termina mai su posizione patta complicata (oscillazione).                       | Bassa | Basso | Cancellation deadline è hard. `MinimaxSearch` controlla `state.status().isDraw()` come terminale → ritorno immediato. |
| R-12  | Virtual thread ha overhead di context switch su molte chiamate `Thread.interrupted()`.                    | Bassa | Basso | Java 21 virtual threads hanno overhead di interrupt comparabile ai platform thread. `MinimaxSearch` non chiama `Thread.interrupted()` ma `cancel.isCancelled()` (boolean field volatile) → praticamente gratis. |
| R-13  | Codice IA puro in `shared` ma esecuzione su virtual thread richiede `java.util.concurrent`: ammesso?     | Bassa | Basso | `VirtualThreadAiExecutor` è in `shared.ai` ma usa solo JDK (`Thread.ofVirtual`, `ExecutorService`). Nessun framework esterno. Coerente con CLAUDE.md §8.7. |

---

## 7. Stop point e decisioni che richiedono utente

Sotto-fase PIANIFICA — punti che richiedono chiarimento prima di IMPLEMENTA.

### 7.1 Architettura `AiEngine` (ADR-024)

| Opzione | Descrizione | Trade-off |
|---|---|---|
| **A** (proposta) | `sealed interface AiEngine permits PrincipianteAi, EspertoAi, CampioneAi`. Tre classi finali, configurazione costanti in ognuna. Polimorfismo via `chooseMove`. | Tipo-safe, esaustivo nei pattern matching. Aderente a CLAUDE.md §4.1 ("sealed types per modello di dominio"). Aggiungere un livello = aggiungere un permits + classe. |
| B | `MinimaxAiEngine` unica + `record AiConfig(int depth, Duration timeout, double noise, boolean useTt, ...)`. Un solo tipo, configurabile. | Più flessibile per esperimenti di tuning, ma meno aderente al "3 livelli fissi" di SPEC §12.2 / ADR-015. Pattern less idiomatic. |
| C | Strategy con `interface SearchPolicy`: `MinimaxAiEngine(Evaluator, SearchPolicy)`. SearchPolicy ha varianti `FixedDepth(2)`, `IterativeDeepening(8, 5s)`, `NoisyFixed(2, 0.25)`. | Flessibile e testabile a strati, ma overhead concettuale: 3 livelli del SPEC mappati a 3 SearchPolicy preconfezionate. |

**Proposta**: A. Coerente con stile sealed di F1 (es. `Move`, `GameStatus`).

### 7.2 Funzione di valutazione (ADR-025)

**Proposta**: `Evaluator` interface + `WeightedSumEvaluator` con `List<EvaluationTerm>`. Pesi default da SPEC §12.1, configurabili via costruttore.

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | 5 termini come da SPEC §12.1, classi separate. Pesi fissi nelle costanti. Nessun tuning runtime in F2. |
| B | Pesi configurabili come `record EvaluationWeights(int material, int mobility, ...)` esposto al chiamante. | Più flessibile per tuning futuro, overhead API minimo. |
| C | Solo materiale + mobilità in F2, gli altri termini in F3 dopo dati di gioco reali. Più semplice. | Rischia di non superare A2.13 (golden positions) se Campione è "cieco" alla posizione. |

**Proposta**: A. SPEC §12.1 elenca i 5 termini come parte della specifica, non sono opzionali.

### 7.3 Hashing Zobrist + TT (ADR-026)

**Proposta**: tavole inizializzate con `SplittableRandom(0xDAMA_2026_ITALIANA_SEED)` (seed costante). TT array circolare `2^20` entry, politica "always replace + prefer deeper".

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Tavole deterministiche (seed costante). TT 2^20 entry (~32 MB) globale per `CampioneAi`. | Deterministico, predicibile, sufficiente per depth 8. |
| B | Tavole randomizzate ad ogni avvio (seed = `System.nanoTime`). | Più "random" ma rompe determinismo dei test → da escludere. |
| C | TT più piccola (2^18 = 8 MB) per ridurre footprint. | Meno collisioni risparmiate, perdita perf 5-10% stimata. |
| D | TT ricreata ad ogni `chooseMove`. | Niente memory leak ma perde benefici cross-call dell'iterative deepening. |

**Proposta**: A. La TT vive per la durata di `CampioneAi` (di solito una partita); `clear()` esposto per riuso a partite successive (chiamato da `AiEngine.forLevel` o esposto pubblicamente?).

### 7.4 Modello di cancellazione (ADR-027)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | `CancellationToken` cooperativo: il search code lo controlla esplicitamente. `VirtualThreadAiExecutor` traduce `Future.cancel(true)` in `token.cancel()`. | Chiaro, prevedibile. Standard nei motori di scacchi (Stockfish, Carballo). |
| B | Solo `Thread.interrupted()`: nessun token. La search controlla `Thread.interrupted()` ad ogni nodo. | Semplice ma fragile (alcuni JDK call resettano interrupted flag). Meno controllo per il client (`CancellationToken.deadline` non è esprimibile). |
| C | Reactive `CompletableFuture.cancel`: pure async. | Overkill per il dominio; SPEC §12 dice "su virtual thread" → l'API sincrona è fine. |

**Proposta**: A.

### 7.5 Determinismo del rumore Principiante (ADR-028)

SPEC §12.2 dice "Probabilità 25% di scegliere mossa subottima (rumore)". Definizione di "subottima":

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Con probabilità 25%, scarta la `bestMove` calcolata e sceglie **uniformemente a caso** fra le altre legali. Se `legalMoves.size() == 1`, niente rumore (forzato). | Semplice, deterministico, ovvio dal punto di vista del test. |
| B | Con probabilità 25%, sceglie la **seconda migliore** (per score). | "Subottima ma non casuale". Più giocabile? Forse, ma non è ciò che dice SPEC. |
| C | Aggiunge rumore epsilon-greedy alla valutazione (depth 2 + ε al score). | Più "smooth" ma cambia il senso di "subottima" rispetto a SPEC. |

**Proposta**: A. Il `RandomGenerator` è iniettato (`Principiante` lo accetta nel costruttore + factory `forLevel(level, rng)`); nei test si passa seed fisso.

### 7.6 Esecuzione del test simulazione 100 partite

Tempo stimato: ~10 minuti.

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Marcato `@Tag("slow")`. Eseguito di default in `mvn verify`. Saltabile via property `-DskipSlowTests=true` per fast loop. CI (futura, ADR-019) lo eseguirà. | Bilancia rapidità sviluppo locale e gating effettivo. |
| B | Marcato `@Tag("acceptance")` ed eseguito **solo** in profilo Maven dedicato `mvn verify -P acceptance`. Default `mvn verify` lo salta. | Sviluppatore deve ricordarsi di eseguirlo prima di chiudere F2. Maggior rischio di "dimenticarsi". |
| C | Eseguito sempre, senza tag. | `mvn verify` standard impiega 12+ min. Frustrante. |
| D | Ridurre il numero di partite a 30 (e accettance ≥ 95% → ≥ 28/30) per stare entro 4 min. | Meno robusto statisticamente; SPEC dice 100. Da evitare. |

**Proposta**: A. Documentato nel Javadoc del test e nel README (sezione "Esecuzione test").

### 7.7 Tolleranza performance test

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Tolleranza 1.5x sui timeout SPEC §12.2 (es. Campione 5s × 1.5 = 7.5s come gate hard). Logging del tempo medio + p95. | Realistico per CI condivisa; non maschera regression > 50%. |
| B | Tolleranza zero: 5s hard. | Falsi negativi su CI lenti. |
| C | `@DisabledOnCi` con env var. | Perdita visibilità su perf in CI. |

**Proposta**: A. Documentato come "soglia di allerta operativa, non regressione di SPEC". La SPEC effettiva (5s) è verificata a sviluppo locale.

### 7.8 Coverage gate package `ai`

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | 85% line + 85% branch. | Realistico: search ha rami "early exit" difficili. |
| B | 90% line + 80% branch. | Più severo su line, riconosce difficoltà branch. |
| C | 80% line + branch (allineato NFR-M-01). | Tollerante. |

**Proposta**: A.

### 7.9 Inclusione di Task 2.14 (Zobrist per `isThreefoldRepetition`)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Escludere da F2. Resta deferred-F4 (quando `core-server` introduce match persistente). | F2 focalizzata. |
| B | Includere come "fix opportunistico" data la nuova disponibilità di `ZobristHasher`. | +1-2 task di lavoro, possibile refactor `RuleEngine.computeStatus` (cambio API interna). |

**Proposta**: A. Lo `ZobristHasher` resta interno a `ai.search`; il `RuleEngine` di F1 conserva la sua API e implementazione replay-based. Quando `core-server` (F4) o tornei (F8/F9) renderanno la performance ripetizione critica, si farà il refactor con il beneficio del senno di poi.

### 7.10 Branch di lavoro

**Proposta**: `feature/2-ai` come unico branch di Fase 2, staccato da `develop`. Merge `--no-ff` su `develop` a fine fase. Merge `develop → main` con tag `v0.2.0` sul commit di merge in `main` (CLAUDE.md §4.4).

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Singolo branch `feature/2-ai`. Commit per Task. |
| B | Sotto-branch per Task (es. `feature/2-evaluator`, `feature/2-search`, `feature/2-tt`). | Overhead di merge per single-developer; non giustificato. |

**Proposta**: A.

---

## 8. Stima di completamento

In numero di task (CLAUDE.md §2.1):

- Task 2.1 ÷ 2.13 + 2.15 → **14 task** obbligatori.
- Task 2.14 → **+1 opzionale** (proposta: rinviato).
- Ogni task termina con un commit Conventional Commits. Esempi:
  - `feat(shared): add evaluator skeleton with material term`
  - `feat(shared): add mobility, advancement, edge-safety, center-control evaluation terms`
  - `feat(shared): add move orderer with capture-first policy`
  - `feat(shared): implement minimax with alpha-beta pruning`
  - `feat(shared): add iterative deepening with cooperative cancellation`
  - `feat(shared): add zobrist hasher and transposition table`
  - `feat(shared): add AiEngine sealed interface and three difficulty levels`
  - `feat(shared): add virtual-thread AI executor with timeout and cancel`
  - `test(shared): add tactical golden positions for AI search depth verification`
  - `test(shared): add tournament simulation Campione vs Principiante (≥95/100)`
  - `test(shared): add AI performance tests covering NFR-P-02`
  - `test(shared): add AI cancellation graceful-stop tests`
  - `chore(shared): add JaCoCo gate for ai package at 85% coverage`
  - `docs(shared): add ai package-info, ADR-024..028, and traceability rows`

---

## 9. Output finale della Fase 2

Albero file atteso a chiusura fase (delta rispetto a F1, escludendo `target/`):

```
shared/
├── pom.xml                                  (aggiornato: JaCoCo gate ai 85%)
└── src/
    ├── main/java/com/damaitaliana/shared/
    │   ├── ai/
    │   │   ├── package-info.java
    │   │   ├── AiEngine.java                (sealed)
    │   │   ├── AiLevel.java
    │   │   ├── PrincipianteAi.java
    │   │   ├── EspertoAi.java
    │   │   ├── CampioneAi.java
    │   │   ├── CancellationToken.java
    │   │   ├── VirtualThreadAiExecutor.java
    │   │   ├── evaluation/
    │   │   │   ├── package-info.java
    │   │   │   ├── Evaluator.java
    │   │   │   ├── EvaluationTerm.java
    │   │   │   ├── WeightedSumEvaluator.java
    │   │   │   ├── MaterialTerm.java
    │   │   │   ├── MobilityTerm.java
    │   │   │   ├── AdvancementTerm.java
    │   │   │   ├── EdgeSafetyTerm.java
    │   │   │   └── CenterControlTerm.java
    │   │   └── search/
    │   │       ├── package-info.java
    │   │       ├── MinimaxSearch.java
    │   │       ├── IterativeDeepeningSearch.java
    │   │       ├── MoveOrderer.java
    │   │       ├── StandardMoveOrderer.java
    │   │       ├── ZobristHasher.java
    │   │       ├── TranspositionTable.java
    │   │       └── SearchResult.java
    └── test/
        ├── java/com/damaitaliana/shared/ai/
        │   ├── evaluation/
        │   │   ├── MaterialTermTest.java
        │   │   ├── MobilityTermTest.java
        │   │   ├── AdvancementTermTest.java
        │   │   ├── EdgeSafetyTermTest.java
        │   │   ├── CenterControlTermTest.java
        │   │   └── WeightedSumEvaluatorTest.java
        │   ├── search/
        │   │   ├── MoveOrdererTest.java
        │   │   ├── MinimaxSearchTest.java
        │   │   ├── IterativeDeepeningSearchTest.java
        │   │   ├── ZobristHasherTest.java
        │   │   ├── TranspositionTableTest.java
        │   │   └── MinimaxSearchTtTest.java
        │   ├── AiEngineFactoryTest.java
        │   ├── AiTacticalPositionsTest.java
        │   ├── AiTournamentSimulationTest.java
        │   ├── AiPerformanceTest.java
        │   ├── AiCancellationTest.java
        │   └── VirtualThreadAiExecutorTest.java
        └── resources/
            └── ai-tactical-positions.json
```

**File esterni a `shared/` aggiornati**:
- `ARCHITECTURE.md` (ADR-024, 025, 026, 027, eventuale 028).
- `tests/TRACEABILITY.md` (righe FR-SP-02, NFR-P-02, AC §17.1.1 parziale, AC F2).
- `CHANGELOG.md` (`[Unreleased]`).
- `AI_CONTEXT.md` (stato).
- `plans/PLAN-fase-2.md` → questo file.

**File rimossi**: nessuno.

---

## 10. Definition of Done della Fase 2

- [ ] Task 2.1 ÷ 2.13 + 2.15 completati e committati su `feature/2-ai`.
- [ ] Acceptance criteria A2.1 ÷ A2.13 verificati.
- [ ] `mvn -pl shared verify`: BUILD SUCCESS con coverage `shared` ≥ 90%, `rules` ≥ 90%, `ai` ≥ 85%.
- [ ] `mvn clean verify` (root): BUILD SUCCESS (regression sui moduli successivi assente).
- [ ] Spotless OK, SpotBugs 0 High.
- [ ] **A2.2 (gating)**: `AiTournamentSimulationTest` Campione ≥ 95/100 vs Principiante.
- [ ] **A2.3 (gating)**: `AiPerformanceTest` Campione ≤ 5s.
- [ ] AC §17.1.1 parziale + AC F2 mappati nella TRACEABILITY.
- [ ] `RuleEngineCorpusTest` (Fase 1, 48 posizioni) **continua a passare**.
- [ ] Sotto-fase REVIEW (CLAUDE.md §2.3) eseguita → `reviews/REVIEW-fase-2.md` creato e chiuso.
- [ ] Sotto-fase TEST (CLAUDE.md §2.4) eseguita → `tests/TEST-PLAN-fase-2.md` creato; `tests/TRACEABILITY.md` aggiornato.
- [ ] Branch `feature/2-ai` mergiato `--no-ff` su `develop`.
- [ ] Merge `develop → main` + tag `v0.2.0` sul commit di merge.

---

**FINE PLAN-fase-2**
