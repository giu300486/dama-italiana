# PLAN — Fase 1: Dominio e regole (`shared`)

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 1.
- **SPEC version**: 2.0 (2026-04-26).
- **Data piano**: 2026-04-28.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.

---

## 1. Scopo della fase

Implementare il **modello di dominio puro** della Dama Italiana e il **motore di regole `RuleEngine`** all'interno del modulo `shared`, garantendo:

1. Modellazione completa del board, dei pezzi, delle mosse e dello stato di partita secondo `SPEC.md` §8.1.
2. Generazione delle mosse legali in qualsiasi posizione, applicando correttamente le **4 leggi della variante italiana** (§3.4 SPEC) e le sue regole peculiari (pedina non cattura dama §3.3, stop alla promozione §3.5).
3. Calcolo dello status di partita: vittoria per assenza pezzi/mosse (§3.6), patta per triplice ripetizione e per regola delle 40 mosse (§3.6).
4. Possibilità di giocare partite end-to-end via **API Java pura**, senza UI né rete.
5. Test corpus parametrizzato (`test-positions.json`) con ≥ 48 posizioni distribuite per categoria (CLAUDE.md §2.4.4).
6. Coverage del modulo `shared` ≥ **90%** (SPEC §16 Fase 1) con soglia hard ≥ 80% sul totale (NFR-M-01) e ≥ 90% sul package `rules`.

**Out of scope**: IA (Fase 2), UI (Fase 3), rete (Fasi 6+), tornei (Fasi 8+), persistenza (Fase 5).

---

## 2. Acceptance criteria

Da `SPEC.md` §16 (Fase 1):

> **Acceptance**: tutti i test FID di riferimento passano. Partita end-to-end via API Java pura senza UI né rete.

**Criterio operativo esteso**:

| ID    | Criterio                                                                                                                                  | Verificabile come                                                          |
|-------|-------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| A1.1  | `mvn -pl shared verify` BUILD SUCCESS.                                                                                                    | Output Maven                                                               |
| A1.2  | Test corpus `test-positions.json` contiene ≥ 48 posizioni distribuite secondo la tabella minima di CLAUDE.md §2.4.4.                       | Conteggio tramite test setup + `RuleEngineCorpusCoverageTest`              |
| A1.3  | `RuleEngineCorpusTest` passa per **ogni** posizione (legalMoves matches expectedLegalMoves; nessuna delle rejectedMoves restituita).      | Output JUnit                                                               |
| A1.4  | `EndToEndGameApiTest` esegue almeno 3 partite complete (white-wins, black-wins, draw) via API pura, terminando con `GameStatus` corretto. | Output JUnit                                                               |
| A1.5  | Coverage `shared` ≥ 90% globale **e** ≥ 90% sul package `com.damaitaliana.shared.rules` (JaCoCo `haltOnFailure=true`).                    | Report JaCoCo + check Maven                                                |
| A1.6  | Spotless check passa, SpotBugs 0 High.                                                                                                    | Output Maven                                                               |
| A1.7  | `package-info.java` di tutti i sotto-package di `shared` documenta scopo + vincoli + riferimento SPEC.                                    | Lettura visiva + Javadoc                                                   |
| A1.8  | `tests/TRACEABILITY.md` aggiornato con righe per ogni FR/NFR/AC SPEC coperto in Fase 1 (vedi §3 sotto).                                   | Lettura visiva                                                             |
| A1.9  | Smoke test `SharedSmokeTest` rimosso (CLAUDE.md `AI_CONTEXT.md` riga 42).                                                                  | Assenza file                                                               |
| A1.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`.                                                                                      | grep                                                                       |
| A1.11 | Acceptance §17.1.6 (4 leggi applicate), §17.1.7 (stop alla promozione), §17.1.8 (pedina non cattura dama) coperti da test specifici nel corpus. | Riferimenti incrociati nel TRACEABILITY                                    |

---

## 3. Requisiti SPEC coperti

### 3.1 Funzionali (FR)

In Fase 1 nessun FR di prodotto è coperto end-to-end (manca UI/rete), ma il dominio fornisce le **fondamenta** richieste da:

| FR ID    | SPEC ref | Coperto in F1 come                                                                                          |
|----------|----------|-------------------------------------------------------------------------------------------------------------|
| FR-SP-04 | §4.1     | `RuleEngine.legalMoves(state)` produce l'insieme che la UI userà per highlight (effettivo FR realizzato in F3). |
| FR-SP-05 | §4.1     | `legalMoves` filtra le sequenze obbligatorie via leggi italiane → la UI mostrerà solo catture (F3).         |
| FR-SP-09 | §4.1     | Notazione FID 1-32 in `FidNotation` per render history (effettivo render in F3).                             |
| FR-COM-01 | §4.5    | `RuleEngine` è il validatore canonico; verrà chiamato server-side in F6 (CLAUDE.md anti-pattern §8.2).      |
| FR-RUL-01..03 | §4.6 | Regole testate in modo eseguibile; sezione regole UI userà gli stessi diagrammi (F3+).                      |

### 3.2 Non funzionali (NFR)

| NFR ID   | SPEC ref | Coperto in F1 come                                                                                          |
|----------|----------|-------------------------------------------------------------------------------------------------------------|
| NFR-M-01 | §5       | Coverage `shared` ≥ 80% (verificato; F1 punta a ≥ 90%).                                                      |
| NFR-M-04 | §5       | Spotless Google Java Style 2-spazi su tutto il codice di F1.                                                 |
| NFR-P-02 | §5       | _Indiretto_: `legalMoves` performante è prerequisito per IA Campione ≤ 5s in F2. Misurazione informativa in F1 (vedi §6 R-05). |

### 3.3 Acceptance criteria globali (§17)

| AC §17  | Descrizione                                                                       | Coperto in F1 da                                            |
|---------|-----------------------------------------------------------------------------------|-------------------------------------------------------------|
| 17.1.6  | Tutte le 4 leggi della Dama Italiana applicate (test specifici in `shared`).      | `RuleEngineCorpusTest` (categoria `legge-quantità`, `legge-qualità`, `legge-precedenza-dama`, `legge-prima-dama`). |
| 17.1.7  | Pedina che raggiunge promozione durante sequenza catture **non continua**.        | `RuleEngineCorpusTest` (categoria `promozione-stop`).        |
| 17.1.8  | Pedina **non può** catturare la dama.                                              | `RuleEngineCorpusTest` (categoria `pedina-non-cattura-dama`). |
| 17.2.4  | Coverage ≥ 80% sul modulo `shared`.                                                | JaCoCo gate.                                                |
| 17.2.5  | SAST SpotBugs senza warning High.                                                  | SpotBugs gate.                                              |

### 3.4 ADR coinvolti

| ADR     | Vincolo per Fase 1 |
|---------|--------------------|
| ADR-001 | Java 21 LTS → uso di `record`, `sealed`, pattern matching `switch`. |
| ADR-013 | Solo regole **Dama Italiana FID** (variante internazionale e anglosassone esplicitamente escluse). |
| ADR-016 | (Indiretto) salvataggi: il `GameState` deve essere serializzabile. Jackson è già in classpath. |

**Possibili nuovi ADR generati da F1** (da formalizzare in `ARCHITECTURE.md`):
- **ADR-020 — Notazione FID 1-32 come convenzione interna**: orientamento, mappa ↔ `Square(file, rank)`. (Vedi stop point §7.1.)
- **ADR-021 — Identità di posizione per triplice ripetizione**: `PositionKey(Board, sideToMove, castlingFlags?)` con equals/hashCode espliciti. In Dama non ci sono flag tipo arrocco; basta `(Board, sideToMove)`. (Vedi stop point §7.3.)
- **ADR-022 — Schema JSON del corpus regole** (vedi stop point §7.4).

---

## 4. Decomposizione in task

I task sono ordinati. Ogni task ha precondizioni esplicite. Ogni task termina con `mvn -pl shared verify` verde e un commit Conventional Commits.

> **Nota branching**: tutto il lavoro di Fase 1 si svolge sul branch `feature/1-domain-and-rules` staccato da `develop`, mergiato `--no-ff` su `develop` a fine fase (CLAUDE.md §4.3).

### Task 1.1 — Notazione FID e utility

**Output**:
- `shared/src/main/java/com/damaitaliana/shared/notation/FidNotation.java`
- `shared/src/test/java/com/damaitaliana/shared/notation/FidNotationTest.java`

**Contenuto**:
- Mapping bijettivo `Square(file, rank) ↔ int 1..32` (solo case scure).
- Convenzione di numerazione decisa nello **stop point §7.1**.
- Parsing/format di mosse: `"12-16"`, `"12x19"`, `"12x19x26"`.
- Validazione: numero in [1, 32]; case bianche → `IllegalArgumentException`.
- Utility statiche pure, nessuno stato.

**Test**: ~20 metodi (mapping bidirezionale a tappeto, parsing valido/invalido, formati cattura singola/multipla).

**Dipendenze**: nessuna oltre Fase 0.

---

### Task 1.2 — Modello di dominio (`shared.domain`)

**Output** (in `shared/src/main/java/com/damaitaliana/shared/domain/`):
- `Square.java` (record con `file ∈ [0,7]`, `rank ∈ [0,7]`; validazione `compact constructor`; `isDark()` true se `(file+rank) % 2 == 1` — convenzione: case scure su parità dispari, allineata con la regola §3.1 SPEC "casa scura nell'angolo basso-sinistra del Bianco").
- `Color.java` (enum `WHITE`, `BLACK` + `opposite()`).
- `PieceKind.java` (enum `MAN`, `KING`).
- `Piece.java` (record `(Color color, PieceKind kind)`; metodi `isMan()`, `isKing()`, `promote()`).
- `Board.java` (final class, immutabile):
  - Storage interno: `Piece[]` 64 elementi (32 utili, 32 sempre `null`).
  - `static Board initial()` factory (12 white men righe 0-2 case scure, 12 black men righe 5-7 case scure).
  - `Optional<Piece> at(Square s)`.
  - `Board with(Square s, Piece p)` / `Board without(Square s)` (return new Board).
  - `Stream<Square> occupiedSquares(Color c)`.
  - `equals` / `hashCode` (basati su contenuto).
- `Move.java` (sealed interface permits `SimpleMove`, `CaptureSequence`):
  - `Square from()`, `Square to()`.
  - `List<Square> capturedSquares()` (vuota per `SimpleMove`).
  - `boolean isCapture()` default.
- `SimpleMove.java` (record; `capturedSquares()` ritorna `List.of()`).
- `CaptureSequence.java` (record `(Square from, List<Square> path, List<Square> captured)`; `to()` = ultimo della path; almeno un captured).
- `GameStatus.java` (enum `ONGOING`, `WHITE_WINS`, `BLACK_WINS`, `DRAW_REPETITION`, `DRAW_FORTY_MOVES`, `DRAW_AGREEMENT`, `DRAW_STALEMATE_NA` ← non applicabile in Dama Italiana, perché stallo è sconfitta; lasciamo solo le 6 voci sopra senza la settima).
  - Allineamento SPEC §8.1: lo SPEC originale dice `ONGOING, WHITE_WINS, BLACK_WINS, DRAW`. Si propone l'**estensione interna** (per ogni motivo di patta) con metodo `isDraw()` di compat. Se l'utente preferisce strict-SPEC, si torna alle 4 voci letterali.
- `GameState.java` (record `(Board board, Color sideToMove, int halfmoveClock, List<Move> history, GameStatus status)`):
  - `static GameState initial()` factory.
  - Invariante: `history` immutabile (`List.copyOf`).

**Vincoli**:
- Nessuna dipendenza framework (`shared` resta puro per CLAUDE.md §8.7).
- Nessun setter, nessun mutable state.
- Javadoc su ogni tipo pubblico, con riferimento alla sezione SPEC.

**Test**: ~30 metodi unit (factory `initial()` corretto, `apply` non muta, `equals` consistente, `Board.at` per ogni casa, `Square` rifiuta valori invalidi, ecc.).

**Dipendenze**: Task 1.1.

---

### Task 1.3 — `RuleEngine` skeleton + movimenti semplici

**Output**:
- `shared/src/main/java/com/damaitaliana/shared/rules/RuleEngine.java` (interface SPEC §8.2):
  ```java
  public interface RuleEngine {
    List<Move> legalMoves(GameState state);
    GameState applyMove(GameState state, Move move) throws IllegalMoveException;
    GameStatus computeStatus(GameState state);
  }
  ```
- `ItalianRuleEngine.java` (implementazione di default).
- `IllegalMoveException.java`.

**Implementazione (questo task copre solo i movimenti SEMPLICI)**:
- Pedina: 1 casa diagonale **avanti** (Bianco verso rank crescente, Nero decrescente).
- Dama: 1 casa diagonale in tutte e 4 le direzioni.
- `legalMoves` ritorna `List<SimpleMove>` quando non ci sono catture.
- Catture e leggi italiane → Task 1.4-1.6.

**Test**: ~15 metodi (movimento pedina avanti sì, indietro no; movimento dama 4 direzioni; bordo damiera; case occupate; sideToMove rispettato).

**Dipendenze**: Task 1.2.

---

### Task 1.4 — `RuleEngine` — catture singole

**Estensione di `ItalianRuleEngine`**:
- Cattura pedina: salto avanti su pezzo avversario adiacente, atterraggio su casa successiva vuota.
- Cattura dama: salto in tutte e 4 le diagonali.
- **Vincolo §3.3 SPEC: pedina NON può catturare dama** (test esplicito al §17.1.8).
- Generazione di tutte le `CaptureSequence` di lunghezza 1 (singola cattura).

**Test**: ~12 metodi (cattura pedina avanti, cattura dama 4 dir, pedina non cattura dama, atterraggio bloccato da pezzo, atterraggio fuori board).

**Dipendenze**: Task 1.3.

---

### Task 1.5 — `RuleEngine` — sequenze di cattura multiple (DFS)

**Estensione di `ItalianRuleEngine`**:
- DFS ricorsivo: dopo ogni cattura, controllare se il pezzo (nella sua nuova casa) può catturare ancora.
- I pezzi catturati nella stessa sequenza **non possono essere ripresi** (anti-loop). Tracciati in un `Set<Square> capturedSoFar`.
- **§3.5 SPEC**: se una pedina raggiunge l'ultima riga durante la sequenza, **la sequenza si ferma** lì (la pedina diventa dama ma non continua a catturare).
- Generazione di tutte le `CaptureSequence` (anche di lunghezza > 1).

**Test**: ~10 metodi unit + 5 dal corpus (presa multipla, presa che termina con promozione, sequenze ramificate scelte una sola alla volta).

**Dipendenze**: Task 1.4.

---

### Task 1.6 — `RuleEngine` — applicazione delle 4 leggi italiane

**Estensione di `ItalianRuleEngine`** (cuore del task §3.4 SPEC):

Pipeline `legalMoves` quando `allCaptureSequences` non è vuoto:

1. **Legge della quantità** — filtra: tieni solo sequenze con `captured.size()` massima.
2. **Legge della qualità** — fra quelle rimaste, tieni solo quelle con il maggior numero di **dame** catturate.
3. **Legge della precedenza dama** — se rimane sia una sequenza che parte da pedina sia una che parte da dama, scarta quelle che partono da pedina.
4. **Legge della prima dama** — fra le sequenze che partono da dama, se più di una è disponibile e una di esse cattura **per prima** una dama avversaria, scarta quelle che catturano prima una pedina.

Ogni filtro è un `private static List<CaptureSequence> applyLawN(List<CaptureSequence>)` separato → testabilità isolata e leggibilità.

**Test**: ~15 metodi unit + ≥16 posizioni dal corpus (5 + 5 + 3 + 3 secondo CLAUDE.md §2.4.4).

**Dipendenze**: Task 1.5.

---

### Task 1.7 — `applyMove` + promozione + halfmoveClock

**Estensione di `ItalianRuleEngine`**:
- `applyMove(state, move)`:
  - Verifica che `move ∈ legalMoves(state)`; altrimenti `IllegalMoveException`.
  - Costruisce `Board` nuovo: rimuove pezzo `from`, applica catture (rimuove tutti i `capturedSquares`), posiziona pezzo su `to`.
  - **Promozione §3.5**: se `to` è sull'ultima riga avversaria e il pezzo è `MAN`, viene promosso a `KING`.
  - Aggiorna `halfmoveClock`:
    - Reset a 0 se la mossa è una cattura **oppure** un movimento di pedina.
    - Incrementa di 1 altrimenti (mossa di dama senza cattura).
  - Toggle `sideToMove`.
  - Append in `history`.
  - Calcola nuovo `status` invocando `computeStatus`.

**Test**: ~10 metodi unit (promozione corretta, halfmoveClock reset/incremento, illegal move rifiutata, history immutabile).

**Dipendenze**: Task 1.6.

---

### Task 1.8 — `computeStatus` (vittoria + patta)

**Estensione di `ItalianRuleEngine`**:
- **Vittoria avversaria**: se l'avversario non ha pezzi → `WHITE_WINS` o `BLACK_WINS`.
- **Vittoria per stallo §3.6**: se l'avversario non ha mosse legali (lista vuota) → vittoria di chi ha mosso ultimo. (In Dama Italiana lo stallo è sconfitta del bloccato, NON patta come negli scacchi.)
- **Patta per regola 40 mosse §3.6**: `halfmoveClock >= 80` (40 mosse per parte = 80 half-move). _Riferimento_: SPEC §3.6 "40 mosse consecutive senza catture e senza spostamenti di pedina" — interpretazione: 40 mosse complete = 80 ply.
- **Patta per triplice ripetizione §3.6**: la **stessa posizione** (Board + sideToMove) si è verificata 3 volte nella history. Implementazione: `PositionKey` con equals/hashCode + `Map<PositionKey, Integer> repetitionCount` ricalcolato dalla history (vedi stop point §7.3 per scelta tra "ricalcolo dalla history" vs "carry nel GameState").
- **Patta per accordo §3.6**: NON gestita in F1 (è un evento di rete/UI; rinviata a F6).
- Altrimenti `ONGOING`.

**Test**: ~12 metodi (vittoria per assenza pezzi, vittoria per stallo bianco/nero, regola 40 mosse, triplice ripetizione, ONGOING su posizione iniziale).

**Dipendenze**: Task 1.7.

---

### Task 1.9 — Test corpus regole italiane

**Output**:
- `shared/src/test/resources/test-positions.json` (≥ 48 posizioni).
- `shared/src/test/java/com/damaitaliana/shared/rules/RuleEngineCorpusTest.java` (parametrizzato `@ParameterizedTest` + `@MethodSource`).
- `shared/src/test/java/com/damaitaliana/shared/rules/CorpusLoader.java` (parser Jackson del file).

**Schema JSON proposto** (vedi stop point §7.4):

```json
{
  "version": 1,
  "positions": [
    {
      "id": "rule-quantity-001",
      "description": "Cattura obbligatoria di 2 pedine vs cattura di 1: deve essere imposta quella di 2.",
      "specReference": "3.4 - Legge della quantità",
      "category": "legge-quantità",
      "board": {
        "whiteMen":   [12, 19],
        "whiteKings": [],
        "blackMen":   [16, 23, 26],
        "blackKings": []
      },
      "sideToMove": "WHITE",
      "expectedLegalMoves": ["12x19x26"],
      "rejectedMoves": ["12x19", "19x26"],
      "notes": "Catturare solo la 16 lascia disponibile la sequenza più lunga, quindi è illegale."
    }
  ]
}
```

**Distribuzione minima** (CLAUDE.md §2.4.4 — vincolante):

| Area regole                                                | Posizioni | Categoria JSON |
|------------------------------------------------------------|----------:|----------------|
| Movimento pedina (avanti diagonale, mai indietro)          |         3 | `mov-pedina` |
| Movimento dama (1 casa, 4 diagonali)                       |         4 | `mov-dama` |
| Cattura semplice pedina                                    |         4 | `cap-pedina` |
| Cattura semplice dama                                      |         4 | `cap-dama` |
| **Pedina non cattura dama**                                |         3 | `pedina-non-cattura-dama` |
| Presa multipla con sequenze diverse                        |         5 | `presa-multipla` |
| **Legge della quantità**                                   |         5 | `legge-quantità` |
| **Legge della qualità**                                    |         5 | `legge-qualità` |
| **Legge della precedenza dama**                            |         3 | `legge-precedenza-dama` |
| **Legge della prima dama**                                 |         3 | `legge-prima-dama` |
| Promozione con stop sequenza                               |         3 | `promozione-stop` |
| Triplice ripetizione                                        |         2 | `tripla-ripetizione` |
| Regola 40 mosse                                            |         2 | `regola-40-mosse` |
| Stallo = sconfitta                                         |         2 | `stallo-sconfitta` |
| **Totale**                                                 |    **48** |  |

**Test secondari**:
- `RuleEngineCorpusCoverageTest`: verifica che ogni `category` abbia almeno il numero minimo di posizioni; fallisce se il corpus si depaupera per errore.
- `RuleEngineCorpusSchemaTest`: ogni posizione ha tutti i campi obbligatori e una `id` unica.

**Dipendenze**: Task 1.8.

---

### Task 1.10 — Test end-to-end via API pura

**Output**: `shared/src/test/java/com/damaitaliana/shared/EndToEndGameApiTest.java`.

**Contenuto**:
- 3 partite scriptate (sequenze di mosse hard-coded), ciascuna terminante con `GameStatus` distinto:
  1. **White wins** per assenza pezzi avversario (partita realistica corta).
  2. **Black wins** per stallo avversario (white bloccato).
  3. **Draw** per regola 40 mosse (loop di dame).
- Loop: `state = applyMove(state, move)` finché `status != ONGOING`.
- Asserzioni finali su `status` e su numero pezzi residui.

**Test secondari**:
- `RandomGameSurvivalTest` (informativo, non gating): partita con mosse casuali pesate per evitare stalli; deve terminare in tempi finiti senza eccezioni. Utile a stanare bug emergenti. Disabilitato di default (`@Disabled` con motivazione documentata).

**Dipendenze**: Task 1.9.

---

### Task 1.11 — Coverage gate, JaCoCo strict, cleanup

**Output**:
- `shared/pom.xml` aggiornato:
  - JaCoCo `haltOnFailure=true`.
  - Regola modulo: 90% line + branch.
  - Regola package `com.damaitaliana.shared.rules`: 90% line + branch.
- Rimozione di `shared/src/test/java/com/damaitaliana/shared/SharedSmokeTest.java` (CLAUDE.md `AI_CONTEXT.md` riga 42).
- `mvn -pl shared verify`: BUILD SUCCESS con coverage ≥ soglie.

**Dipendenze**: Task 1.10.

---

### Task 1.12 — Documentazione, ADR, TRACEABILITY

**Output**:
- `shared/src/main/java/com/damaitaliana/shared/package-info.java`: aggiornato con i nuovi sotto-package effettivi (`domain`, `rules`, `notation`).
- `shared/src/main/java/com/damaitaliana/shared/domain/package-info.java` (nuovo).
- `shared/src/main/java/com/damaitaliana/shared/rules/package-info.java` (nuovo).
- `shared/src/main/java/com/damaitaliana/shared/notation/package-info.java` (nuovo).
- `ARCHITECTURE.md`: aggiunti ADR-020, ADR-021, ADR-022 nella sezione "Decisioni successive" (vedi §3.4 sopra).
- `tests/TRACEABILITY.md`: righe per FR-SP-04, FR-SP-05, FR-SP-09, FR-COM-01, NFR-M-01, NFR-M-04, AC §17.1.6, §17.1.7, §17.1.8, §17.2.4, §17.2.5.
- `CHANGELOG.md`: voce in `[Unreleased]` con riepilogo F1.
- `AI_CONTEXT.md`: stato avanzato a "Fase 1 — IMPLEMENTA completa, REVIEW pending".

**Dipendenze**: Task 1.11.

---

## 5. Strategia di test (Fase 1)

Riferimento: CLAUDE.md §2.4.

| Tipo                         | Numero indicativo | Tooling                       | Cosa testa                                                                                                                    |
|------------------------------|------------------:|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Unit (modello)**           | ~30               | JUnit 5 + AssertJ              | `Square`, `Piece`, `Board`, `Move`, `GameState`: factory, equals, immutabilità, invarianti.                                  |
| **Unit (notazione)**         | ~20               | JUnit 5                        | `FidNotation`: bijezione, parsing, format, casi invalidi.                                                                     |
| **Unit (RuleEngine elementare)** | ~50           | JUnit 5                        | Movimenti, catture singole, sequenze, leggi (ognuna isolata), promozione, status.                                             |
| **Corpus parametrizzato**    | ≥ 48              | JUnit 5 + Jackson              | `RuleEngineCorpusTest` su `test-positions.json`. Una posizione = un caso di test.                                             |
| **End-to-end API**           | 3                 | JUnit 5                        | Partite scriptate complete; assert su status finale e pezzi residui.                                                          |
| **Schema/coverage corpus**   | 2                 | JUnit 5                        | `RuleEngineCorpusSchemaTest`, `RuleEngineCorpusCoverageTest`. Garantiscono che il corpus non si degradi nel tempo.            |
| **Smoke (rimosso)**          | 0                 | —                              | `SharedSmokeTest` cancellato in Task 1.11.                                                                                    |

**Coverage target Fase 1**:

| Scope                                          | Soglia minima (gate)            | Target operativo |
|------------------------------------------------|---------------------------------|------------------|
| `shared` (intero modulo)                       | **90%** line + branch (gate)    | 92-95%           |
| `com.damaitaliana.shared.rules` (sub-package)  | **90%** line + branch (gate)    | 95%              |

> Nota: SPEC §16 Fase 1 indica "Coverage ≥ 90%" per la fase; NFR-M-01 fissa la soglia permanente a ≥ 80% sul modulo. Si applica la soglia **più alta** in Fase 1 (90%) come gate, perché il dominio è auto-contenuto e testabile in isolamento — un calo sotto 90% qui sarebbe un campanello d'allarme.

**Naming convention** (CLAUDE.md §2.4.5):
- Unit test: `<ClasseProduzione>Test`. Stile metodo: `should<Espressione>_when<Condizione>` per i test di `RuleEngine`; `<Feature><Scenario>` per i test del corpus (id della posizione).
- E2E API: `EndToEndGameApiTest`.

---

## 6. Rischi e mitigazioni

| ID   | Rischio                                                                                       | P    | I     | Mitigazione |
|------|-----------------------------------------------------------------------------------------------|------|-------|-------------|
| R-01 | **Errore nelle 4 leggi italiane** (la più rischiosa: confondere ordine, dimenticare un caso). | Alta | Alto  | Test corpus parametrizzato come rete primaria; ognuna delle leggi ha un metodo di filtro isolato + test dedicati; richiamo SPEC §3.4 nel Javadoc di ogni filtro. |
| R-02 | DFS su sequenze di cattura: cicli o esplosione combinatoria.                                  | Media| Medio | Tracciare set di pezzi catturati nella sequenza corrente; profondità DFS limitata dal numero di pezzi avversari (≤ 12); test su posizioni "presa multipla 5+". |
| R-03 | Triplice ripetizione: errata identità di posizione (es. dimenticare `sideToMove`).             | Media| Medio | `PositionKey` record con equals/hashCode espliciti; ADR-021; test corpus dedicato a `tripla-ripetizione`. |
| R-04 | Numerazione FID 1-32: orientamento ambiguo (top-bottom o bottom-top, e rispetto a quale lato). | Bassa| Medio | **Stop point §7.1**. Decisione in ADR-020 prima di implementare Task 1.1. |
| R-05 | Performance `legalMoves` su posizioni complesse (esplosione DFS).                              | Bassa| Basso | Misurazione informativa con micro-test in `RuleEnginePerformanceTest` (`@Disabled` di default). Soglia di allerta: < 50 ms su una posizione di metà partita. Ottimizzazione piena in F2 con bitboard / move ordering se necessario. |
| R-06 | Test corpus fragile (parsing JSON, formato mosse, naming inconsistente).                      | Media| Basso | Schema JSON validato in `RuleEngineCorpusSchemaTest`; "golden positions" semplici (1-2 catture) implementate prima delle complesse; convenzioni di id rigorose (`<categoria>-NNN`). |
| R-07 | Coverage 90% sul package `rules` difficile da raggiungere per branch di errori (`IllegalMoveException`). | Media | Basso | Test espliciti su rami eccezionali; eccezioni con messaggi parametrizzati testati; possibile esclusione mirata da JaCoCo solo se motivata (es. metodo `toString` di sealed types). |
| R-08 | Confusione tra Dama Italiana e altre varianti durante implementazione.                         | Media| Alto  | Citare nel Javadoc di ogni regola la sezione SPEC §3 corrispondente; preambolo CLAUDE.md §1 ("DEVE NOT confondere le regole") richiamato nel package-info di `rules`. |
| R-09 | `Move` sealed con `SimpleMove`/`CaptureSequence` diventa scomodo in switch esaustivo.          | Bassa| Basso | Pattern matching `switch` di Java 21 con coverage automatico; test su entrambi i sotto-tipi. |
| R-10 | Validazione `Board.initial()` divergente dallo standard FID (orientamento).                    | Media| Alto  | Fissato dalla §3.1 SPEC: casa scura nell'angolo basso-sinistra del Bianco. Test: `BoardTest#initialPositionMatchesSpec` con asserzioni su 24 case specifiche. |
| R-11 | Regola 40 mosse: ambiguità "40 mosse" = 40 ply o 80 ply?                                       | Bassa| Medio | SPEC §3.6 "40 mosse consecutive senza catture e senza spostamenti di pedina" → interpretazione standard: 40 mosse complete = 80 ply (40 per giocatore). Documentato in Javadoc di `applyMove`. |

---

## 7. Stop point e decisioni che richiedono utente

Sotto-fase PIANIFICA — punti che richiedono chiarimento prima di IMPLEMENTA.

### 7.1 Numerazione FID 1-32: orientamento (ADR-020)

SPEC §3.8 afferma "caselle numerate 1-32 (solo case scure)" senza specificare l'orientamento. Lo standard FID definisce 1-4 sulla prima riga vista dal Nero (= ultima riga di promozione del Bianco) e 29-32 sulla prima riga vista dal Bianco. Bianco muove dalle case 21-32 verso 1-12.

| Opzione | Descrizione | Trade-off |
|---|---|---|
| **A** (standard FID) | Casella 1 = angolo top-left dal punto di vista del Bianco; casella 32 = angolo bottom-right. Bianco muove da numeri alti a numeri bassi. | Aderente al regolamento federale e a tutti i database di posizioni FID. **Raccomandato.** |
| B (custom alternativa) | Casella 1 = angolo bottom-left dal punto di vista del Bianco; Bianco muove da numeri bassi a numeri alti. | Più intuitivo per chi pensa "in avanti = numeri crescenti", ma incompatibile con corpus FID esistenti. |

**Proposta**: opzione A. ADR-020 documenta il mapping `int 1..32 ↔ Square(file, rank)` con tabella esplicita.

### 7.2 Soglia coverage Fase 1

Lo SPEC §16 dice "Coverage ≥ 90%"; NFR-M-01 dice ≥ 80%; CLAUDE.md §2.4.2 dice "≥ 80% (90% raccomandato per RuleEngine)".

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | 90% modulo + 90% package `rules` come gate `haltOnFailure=true`. |
| B | 80% modulo (NFR-M-01 letterale) + 90% solo su `rules`. Più permissivo sul resto del modulo. |
| C | 90% modulo, niente vincolo specifico su `rules`. Meno granulare. |

**Proposta**: A. Il modulo `shared` in F1 contiene quasi solo logica testabile, quindi 90% è raggiungibile senza sforzo eccessivo.

### 7.3 Identità di posizione per triplice ripetizione (ADR-021)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | `PositionKey(Board, Color sideToMove)` come record con equals/hashCode automatici. Il `Board.equals` confronta il contenuto delle case. Conteggio ripetizioni ricalcolato dalla `history` ad ogni `computeStatus`. |
| B | `GameState` mantiene un `Map<PositionKey, Integer>` esplicito, aggiornato in `applyMove`. Più efficiente ma rende `GameState` non più puramente derivabile dalla history. |

**Proposta**: A. La performance non è critica in F1 (la history ha al massimo qualche centinaio di mosse) e mantiene `GameState` autoderivante.

### 7.4 Schema JSON del corpus (ADR-022)

SPEC e CLAUDE.md §2.4.4 mostrano un format esemplificativo con liste di numeri di casa separate per colore + lista `kings` a parte. Per evitare duplicazione e ambiguità (un numero compare in `white` e/o in `kings`?), si propone uno schema **disgiunto**:

```json
"board": {
  "whiteMen":   [12, 19],
  "whiteKings": [],
  "blackMen":   [16, 23, 26],
  "blackKings": []
}
```

Le 4 liste sono mutualmente esclusive (validato nel loader). `sideToMove` è obbligatorio. `expectedLegalMoves` e `rejectedMoves` usano la notazione FID. Il loader ha test di schema dedicati.

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Schema disgiunto (4 liste + sideToMove + expected/rejected). |
| B | Schema SPEC esempio (white/black + kings) con validazione che `kings ⊂ white ∪ black`. |

**Proposta**: A.

### 7.5 `GameStatus`: 4 voci letterali da SPEC vs 6 voci con motivo di patta

SPEC §8.1 dichiara `ONGOING, WHITE_WINS, BLACK_WINS, DRAW`. Per UX (mostrare il motivo della patta) e per debugging è utile distinguere `DRAW_REPETITION`, `DRAW_FORTY_MOVES`, `DRAW_AGREEMENT`. Si tratta di estensione retro-compatibile (un metodo `isDraw()` raggruppa).

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Estensione interna: 6 voci, `isDraw()` per compat. Documentato in Javadoc come "estensione di precisione, comportamento esterno equivalente". |
| B | Strict-SPEC: 4 voci letterali. Il motivo della patta è esposto via campo separato di `GameState` (es. `Optional<DrawReason>`). |
| C | Strict-SPEC e basta: motivo della patta non esposto. |

**Proposta**: A. Se l'utente preferisce strict-SPEC, B è il fallback più pulito.

### 7.6 Branch di lavoro

Si propone `feature/1-domain-and-rules` staccato da `develop` come unico branch di Fase 1. Merge `--no-ff` su `develop` a chiusura fase, poi merge `develop → main` con tag `v0.1.0` (CLAUDE.md §4.4).

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Singolo branch `feature/1-domain-and-rules`. Commit per Task. |
| B | Sotto-branch per Task (`feature/1-rule-engine-laws`, ecc.). Più granulare ma overhead di merge. |

**Proposta**: A. Lo sviluppo è monolitico (un solo modulo, una sola persona).

---

## 8. Stima di completamento

In numero di task (CLAUDE.md §2.1):

- Task 1.1 ÷ 1.12 → **12 task**.
- Ogni task termina con un commit Conventional Commits. Esempi:
  - `feat(shared): add FID notation utility (1-32)`
  - `feat(shared): add core domain model (Square, Piece, Board, Move, GameState)`
  - `feat(shared): implement RuleEngine simple movements`
  - `feat(shared): implement single captures with man-cannot-capture-king rule`
  - `feat(shared): implement multi-jump capture sequences with promotion stop`
  - `feat(shared): apply Italian draughts four laws of precedence`
  - `feat(shared): implement applyMove with promotion and halfmove clock`
  - `feat(shared): implement game status (win, stalemate-loss, repetition, 40-move)`
  - `test(shared): add RuleEngine corpus with 48 baseline positions`
  - `test(shared): add end-to-end Java API game tests`
  - `chore(shared): enable JaCoCo strict gate at 90% and remove smoke test`
  - `docs(shared): add package-info, ADR-020/021/022 and traceability rows`

---

## 9. Output finale della Fase 1

Albero file atteso a chiusura fase (delta rispetto a F0, escludendo `target/`):

```
shared/
├── pom.xml                                  (aggiornato: JaCoCo strict 90%)
└── src/
    ├── main/java/com/damaitaliana/shared/
    │   ├── package-info.java                (aggiornato)
    │   ├── domain/
    │   │   ├── package-info.java
    │   │   ├── Square.java
    │   │   ├── Color.java
    │   │   ├── PieceKind.java
    │   │   ├── Piece.java
    │   │   ├── Board.java
    │   │   ├── Move.java
    │   │   ├── SimpleMove.java
    │   │   ├── CaptureSequence.java
    │   │   ├── GameStatus.java
    │   │   └── GameState.java
    │   ├── rules/
    │   │   ├── package-info.java
    │   │   ├── RuleEngine.java                  (interface)
    │   │   ├── ItalianRuleEngine.java
    │   │   └── IllegalMoveException.java
    │   └── notation/
    │       ├── package-info.java
    │       └── FidNotation.java
    └── test/
        ├── java/com/damaitaliana/shared/
        │   ├── domain/
        │   │   ├── SquareTest.java
        │   │   ├── BoardTest.java
        │   │   ├── PieceTest.java
        │   │   ├── MoveTest.java
        │   │   └── GameStateTest.java
        │   ├── notation/
        │   │   └── FidNotationTest.java
        │   ├── rules/
        │   │   ├── ItalianRuleEngineMovementsTest.java
        │   │   ├── ItalianRuleEngineCapturesTest.java
        │   │   ├── ItalianRuleEngineSequencesTest.java
        │   │   ├── ItalianRuleEngineLawsTest.java
        │   │   ├── ItalianRuleEngineApplyMoveTest.java
        │   │   ├── ItalianRuleEngineStatusTest.java
        │   │   ├── RuleEngineCorpusTest.java
        │   │   ├── RuleEngineCorpusSchemaTest.java
        │   │   ├── RuleEngineCorpusCoverageTest.java
        │   │   └── CorpusLoader.java
        │   └── EndToEndGameApiTest.java
        └── resources/
            └── test-positions.json
```

**File esterni a `shared/` aggiornati**:
- `ARCHITECTURE.md` (ADR-020, 021, 022).
- `tests/TRACEABILITY.md` (righe FR/NFR/AC F1).
- `CHANGELOG.md` (`[Unreleased]`).
- `AI_CONTEXT.md` (stato).
- `plans/PLAN-fase-1.md` → questo file.

**File rimossi**:
- `shared/src/test/java/com/damaitaliana/shared/SharedSmokeTest.java`.

---

## 10. Definition of Done della Fase 1

- [ ] Task 1.1 ÷ 1.12 completati e committati su `feature/1-domain-and-rules`.
- [ ] Acceptance criteria A1.1 ÷ A1.11 verificati.
- [ ] `mvn -pl shared verify`: BUILD SUCCESS con coverage `shared` ≥ 90% e `rules` ≥ 90%.
- [ ] `mvn clean verify` (root): BUILD SUCCESS (regression sui moduli successivi assente).
- [ ] Spotless OK, SpotBugs 0 High.
- [ ] Test corpus ≥ 48 posizioni distribuite secondo CLAUDE.md §2.4.4.
- [ ] `EndToEndGameApiTest` (3 partite) passa.
- [ ] AC §17.1.6 / 17.1.7 / 17.1.8 mappati a test specifici (TRACEABILITY).
- [ ] `SharedSmokeTest` rimosso.
- [ ] Sotto-fase REVIEW (CLAUDE.md §2.3) eseguita → `reviews/REVIEW-fase-1.md` creato e chiuso.
- [ ] Sotto-fase TEST (CLAUDE.md §2.4) eseguita → `tests/TEST-PLAN-fase-1.md` creato; `tests/TRACEABILITY.md` aggiornato.
- [ ] Branch `feature/1-domain-and-rules` mergiato `--no-ff` su `develop`.
- [ ] Merge `develop → main` + tag `v0.1.0` sul commit di merge.

---

**FINE PLAN-fase-1**
