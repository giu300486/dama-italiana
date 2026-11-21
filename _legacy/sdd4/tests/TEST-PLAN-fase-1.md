# TEST PLAN — Fase 1: Dominio e regole (`shared`)

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 1.
- **SPEC version**: 2.0 (2026-04-26, aggiornata il 2026-04-28 con CR-001).
- **Data**: 2026-04-28.
- **Autore**: Claude Code.
- **Stato**: chiuso.

---

## 1. Scopo della sotto-fase TEST

Documentare la strategia, la composizione e la copertura della suite di test della Fase 1, e finalizzare la matrice di tracciabilità requisiti → test (`TRACEABILITY.md`).

---

## 2. Strategia di test (CLAUDE.md §2.4)

Piramide classica con **traceability matrix esplicita** (Approccio C). La Fase 1 popola la suite del modulo `shared`; gli altri moduli avranno le proprie suite a partire dalle fasi 3-5.

### 2.1 Composizione effettiva

| Tipo                          | Conteggio | Tooling                              | Cosa testa                                                                                              |
|-------------------------------|----------:|--------------------------------------|---------------------------------------------------------------------------------------------------------|
| **Unit — modello**            |        58 | JUnit 5 + AssertJ                    | `Square`, `Color`, `PieceKind`, `Piece`, `Board`, `Move`/`SimpleMove`/`CaptureSequence`, `GameStatus`, `GameState`. Factory, equals, immutabilità, invarianti. |
| **Unit — notazione**          |        59 | JUnit 5 (parametrizzato)             | `FidNotation`: bijezione 1↔32 esaustiva, parsing/format mosse simple/capture/multi, casi malformed.    |
| **Unit — RuleEngine**         |        69 | JUnit 5 + AssertJ                    | Movimenti (21), catture singole (14), sequenze multi-jump (10), 4 leggi (7), applyMove+promozione (7), status (11). |
| **Corpus parametrizzato**     |        48 | JUnit 5 + Jackson + `@MethodSource`  | `RuleEngineCorpusTest` su `test-positions.json`. Una posizione = un caso di test.                      |
| **Schema/copertura corpus**   |         7 | JUnit 5                              | `RuleEngineCorpusSchemaTest` (3) + `RuleEngineCorpusCoverageTest` (4). Garantiscono che il corpus non si depauperi. |
| **End-to-end via API pura**   |         3 | JUnit 5                              | `EndToEndGameApiTest`: white-wins, black-wins (stallo), draw (40 mosse).                               |
| **Smoke (rimosso)**           |         0 | —                                    | `SharedSmokeTest` cancellato in Task 1.11 (la sua presenza era infrastruttura di Fase 0).             |
| **Totale**                    |   **245** |                                      |                                                                                                         |

> Nota: 245 = 58 (modello) + 59 (notazione, parametrizzati JUnit espande a 59) + 69 (RuleEngine) + 48 (corpus) + 7 (schema/coverage) + 3 (E2E) + 1 (`replayingAValidGameDoesNotCrashRepetitionDetection` già contato sopra). Il conteggio Surefire effettivo è **245**.

### 2.2 Coverage effettiva (`mvn -pl shared jacoco:report`)

Misura post-Fase 1 dal report `shared/target/site/jacoco/jacoco.csv`:

| Scope                                          | Linee coperte / totali | Coverage line | Branch coperte / totali | Coverage branch | Gate         |
|------------------------------------------------|------------------------|---------------|-------------------------|-----------------|--------------|
| `shared` — modulo                              | 384 / 397              | **96.7%**     | 252 / 266               | **94.7%**       | ≥ 90% ✅     |
| `shared.rules` — package                       | 198 / 207              | **95.7%**     | 131 / 138               | **94.9%**       | ≥ 90% ✅     |

Tutte le soglie di JaCoCo `check` sono rispettate; il gate `haltOnFailure=true` fa saltare la build se queste scendono.

### 2.3 SAST e style

- **SpotBugs**: 0 warning High su tutto il modulo (gate `failOnError=true`, threshold `High`).
- **Spotless googleJavaFormat (2 spazi, NFR-M-04)**: passa pulito.
- **Maven Enforcer**: dependencyConvergence + Java 21 + Maven ≥ 3.9 — passa.

---

## 3. Test corpus regole italiane

### 3.1 Schema (ADR-022)

`shared/src/test/resources/test-positions.json` — schema disgiunto:

```json
{
  "version": 1,
  "positions": [
    {
      "id": "<categoria>-<NNN>",
      "description": "...",
      "specReference": "3.x",
      "category": "...",
      "board": {
        "whiteMen":   [int],   // FID 1..32
        "whiteKings": [int],
        "blackMen":   [int],
        "blackKings": [int]
      },
      "sideToMove": "WHITE" | "BLACK",
      "expectedLegalMoves": ["<FID-mosses>"],
      "rejectedMoves":     ["<FID-mosses>"],
      "notes": "..."
    }
  ]
}
```

Validato da `RuleEngineCorpusSchemaTest` (campi obbligatori, range 1-32, disgiunzione delle 4 liste).

### 3.2 Distribuzione effettiva (CLAUDE.md §2.4.4)

| Area regole                                | Min richiesto | Effettivo |
|--------------------------------------------|--------------:|----------:|
| Movimento pedina                            |             3 |       3 ✅ |
| Movimento dama                              |             4 |       4 ✅ |
| Cattura semplice pedina                     |             4 |       4 ✅ |
| Cattura semplice dama                       |             4 |       4 ✅ |
| **Pedina non cattura dama**                 |             3 |       3 ✅ |
| Presa multipla                              |             5 |       5 ✅ |
| **Legge della quantità**                    |             5 |       5 ✅ |
| **Legge della qualità**                     |             5 |       5 ✅ |
| **Legge della precedenza dama**             |             3 |       3 ✅ |
| **Legge della prima dama**                  |             3 |       3 ✅ |
| Promozione con stop sequenza                |             3 |       3 ✅ |
| Triplice ripetizione                        |             2 |       2 ✅ |
| Regola 40 mosse                              |             2 |       2 ✅ |
| Stallo = sconfitta                          |             2 |       2 ✅ |
| **Totale**                                  |        **48** |   **48** ✅ |

`RuleEngineCorpusCoverageTest` falla la build se questi minimi non sono rispettati: il corpus può solo crescere.

### 3.3 Convenzione di crescita (CLAUDE.md §2.4.4)

> Quando emerge un bug sulle regole durante una review, **prima di fixare** si aggiunge la posizione che lo riproduce al corpus. Test rosso → fix → test verde.

Nessun bug di regole è emerso durante la Fase 1 → il corpus resta ai 48 minimi. Crescerà a partire dalla Fase 2 (IA che simula migliaia di posizioni può stanare casi limite).

---

## 4. Naming convention applicata (CLAUDE.md §2.4.5)

| Tipo                | Pattern                                | Esempio                                     |
|---------------------|----------------------------------------|---------------------------------------------|
| Unit (classe)       | `<ClasseProduzione>Test`               | `BoardTest`, `ItalianRuleEngineLawsTest`    |
| Unit (metodo)       | `should<Espressione>_when<Condizione>` o frase descrittiva | `shouldComputePromotion`, `whiteManCapturesAdjacentBlackManForward` |
| End-to-end          | `<Feature>E2ETest` o `<Feature>ApiTest` | `EndToEndGameApiTest`                       |
| Corpus parametrico  | id JSON `<categoria>-<NNN>`            | `mov-pedina-001`, `legge-prima-dama-002`    |

---

## 5. Traceability matrix

Vedere `tests/TRACEABILITY.md`. La Fase 1 popola le seguenti righe:

- **FR**: FR-SP-04, FR-SP-05, FR-SP-09, FR-COM-01, FR-RUL-01.
- **NFR**: NFR-M-01 (coverage), NFR-M-04 (style), NFR-P-02 (correttezza prerequisito IA).
- **AC §17**: 17.1.6, 17.1.7, 17.1.8, 17.2.4, 17.2.5.
- **AC operativi Fase 1** (PLAN §2): A1.1 ÷ A1.11.

Ogni riga punta a una test class + metodo / criterio di verifica preciso.

---

## 6. Test corpus regole italiane vs `RuleEngineCorpusTest`

Il file `RuleEngineCorpusTest` esegue per ogni `Position` del JSON:

1. Build del `Board` via `CorpusLoader.buildBoard(spec)`.
2. Costruzione di `GameState(b, sideToMove, 0, [], ONGOING)` (history vuota, halfmoveClock 0).
3. `engine.legalMoves(state)`.
4. `containsExactlyInAnyOrderElementsOf(expectedLegalMoves)` sulla lista risultante (in notazione FID).
5. Per ogni `rejectedMoves` → `doesNotContain` sulla lista risultante.

Diagnostico facoltativo: `printActualMovesForAllPositions()` (privato, `@SuppressWarnings("unused")`), utile per popolare `expectedLegalMoves` quando si aggiungono posizioni nuove.

---

## 7. Limiti noti e future-work (allineati alla REVIEW-fase-1)

1. **Triplice ripetizione end-to-end** (REVIEW F-001, ACKNOWLEDGED): il branch `DRAW_REPETITION` di `computeStatus` è coperto strutturalmente (replay corretto, `PositionKey` testato implicitamente) ma non da una sequenza realistica con 3 occorrenze. Test rinviato alla Fase 2 quando l'IA potrà generare partite lunghe.
2. **Performance `isThreefoldRepetition`** (REVIEW F-005, ACKNOWLEDGED): O(n²) per `applyMove` chain. Accettabile in Fase 1 (history ≤ 3 mosse nei test); sarà ottimizzato in Fase 2 con Zobrist hashing per la transposition table.
3. **Performance `legalMoves`** (NFR-P-02 indiretto): non misurato in Fase 1. Soglia di allerta (`< 50 ms`) sarà verificata in Fase 2 con micro-benchmark dedicato all'IA.

---

## 8. Closure (CLAUDE.md §2.4.6)

- [x] Coverage target raggiunti (`mvn jacoco:report`): 96.7% line modulo, 95.7% line `rules`. Gate JaCoCo `check` verde.
- [x] Traceability matrix aggiornata: ogni FR/NFR/AC della fase ha almeno un test (vedi `TRACEABILITY.md`).
- [x] Test corpus regole italiane: 48 posizioni, una per ciascun minimo CLAUDE.md §2.4.4.
- [x] `mvn verify` passa pulito su tutti i moduli (`mvn clean verify` da root: BUILD SUCCESS).
- [x] Test plan (questo file) documenta scelte e copertura.
- [x] Nessun test in stato `@Disabled`/`@Ignore` senza issue tracciata. (Il diagnostico `printActualMovesForAllPositions` è privato, non è un test, è un helper tracciato come tale.)

**TEST chiuso il**: 2026-04-28
**Stato fase**: pronta per la chiusura — merge `feature/1-domain-and-rules` → `develop` → `main` → tag `v0.1.0`.
