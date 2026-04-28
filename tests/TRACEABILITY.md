# Traceability Matrix

> Mappatura cumulativa **requisito SPEC → test**.
> Aggiornata insieme ad ogni nuovo test. Vedi `CLAUDE.md` §2.4.3.

---

## Requisiti funzionali (FR)

| FR ID     | Descrizione SPEC (breve)                                                | Test class                                       | Test method(s) / criterio                                            | Tipo  |
|-----------|--------------------------------------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------|-------|
| FR-SP-04  | Highlight delle mosse legali al click sulla pedina                       | `ItalianRuleEngineMovementsTest` + `RuleEngineCorpusTest` | tutto il set parametrico, in particolare `mov-pedina-*`, `mov-dama-*` | Unit |
| FR-SP-05  | Highlight rosso pulsante per cattura obbligatoria                        | `ItalianRuleEngineCapturesTest`                  | `simpleMovesExcludedWhenAnyCaptureExists` + corpus `cap-*`           | Unit  |
| FR-SP-09  | Cronologia mosse in notazione FID                                        | `FidNotationTest`                                | `formatsSimpleMove`, `formatsMultiCapture`, `roundTrip*`              | Unit  |
| FR-COM-01 | Validazione mosse server-side (engine canonico)                          | `ItalianRuleEngineMovementsTest` (applyMove path) | `applyMoveRejectsIllegalMove`                                       | Unit  |
| FR-RUL-01 | Sezione Regole accessibile (regole eseguibili)                           | `RuleEngineCorpusTest`                           | tutte le 48 posizioni del corpus = regolamento eseguibile           | Unit  |

---

## Requisiti non funzionali (NFR)

| NFR ID    | Descrizione SPEC                                                         | Test class / gate                                | Note                                                                  |
|-----------|--------------------------------------------------------------------------|--------------------------------------------------|-----------------------------------------------------------------------|
| NFR-M-01  | Coverage ≥ 80% sul motore di gioco (`shared`)                            | JaCoCo `check` su `shared/pom.xml`               | Soglia Fase 1 alzata a ≥ 90% (SPEC §16) — gate `haltOnFailure=true`  |
| NFR-M-04  | Stile codice Google Java Style                                           | Spotless `googleJavaFormat` (parent POM)         | Verifica obbligatoria in `mvn verify`                                |
| NFR-P-02  | IA Campione ≤ 5s (indiretto: legalMoves performante)                     | _(misurazione in F2; F1 valida solo correttezza)_ | Soglia di allerta informativa: < 50 ms su posizione media           |

---

## Acceptance criteria (SPEC sezione 17)

| AC ID    | Descrizione                                                                                        | Test class                                       | Test method(s)                                                       | Tipo |
|----------|----------------------------------------------------------------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------|------|
| 17.1.6   | Tutte le 4 leggi della Dama Italiana applicate                                                     | `ItalianRuleEngineLawsTest` + `RuleEngineCorpusTest` | tutto + corpus `legge-quantita-*`, `legge-qualita-*`, `legge-precedenza-dama-*`, `legge-prima-dama-*` | Unit |
| 17.1.7   | Pedina che raggiunge la promozione durante una sequenza di catture **non continua**                | `ItalianRuleEngineSequencesTest` + corpus         | `manReachingPromotionRowStopsTheSequence`, `blackManReachingRank0StopsTheSequence`, corpus `promozione-stop-*` | Unit |
| 17.1.8   | Pedina **non può** catturare la dama                                                                | `ItalianRuleEngineCapturesTest` + corpus          | `manCannotCaptureKing`, `manCannotCaptureKingButOtherManCanCaptureManIsLegal`, corpus `pedina-non-cattura-dama-*` | Unit |
| 17.2.4   | Coverage ≥ 80% modulo `shared`                                                                      | JaCoCo gate                                       | `shared/pom.xml` — `BUNDLE` line ≥ 0.90 e branch ≥ 0.90 (Fase 1)     | Gate |
| 17.2.5   | SAST SpotBugs senza warning High                                                                    | SpotBugs gate (parent POM)                        | threshold `High`, `failOnError=true`                                  | Gate |

---

## Acceptance criteria di Fase 1 (PLAN-fase-1.md §2)

| ID    | Criterio                                                                                            | Verifica                                                                            |
|-------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| A1.1  | `mvn -pl shared verify` BUILD SUCCESS                                                                | CI/locale                                                                           |
| A1.2  | Test corpus contiene ≥ 48 posizioni distribuite per categoria                                        | `RuleEngineCorpusCoverageTest`                                                      |
| A1.3  | `RuleEngineCorpusTest` passa per ogni posizione                                                      | 48 parametri verdi                                                                  |
| A1.4  | `EndToEndGameApiTest` esegue 3 partite complete (white-wins, black-wins, draw)                       | `EndToEndGameApiTest` (3 metodi)                                                    |
| A1.5  | Coverage `shared` ≥ 90% globale e ≥ 90% sul package `rules`                                          | JaCoCo check con `haltOnFailure=true`                                               |
| A1.6  | Spotless OK, SpotBugs 0 High                                                                          | `mvn verify`                                                                        |
| A1.7  | `package-info.java` presente in tutti i sotto-package di `shared`                                    | Lettura visiva (`shared`, `domain`, `rules`, `notation`)                            |
| A1.8  | `tests/TRACEABILITY.md` aggiornato con righe per ogni FR/NFR/AC della fase                          | Questo file                                                                         |
| A1.9  | `SharedSmokeTest` rimosso                                                                            | Assenza file                                                                        |
| A1.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`                                                 | grep                                                                                |
| A1.11 | AC §17.1.6/7/8 mappati a test specifici                                                              | Tabella sopra                                                                       |

---

## Note di manutenzione

- Una nuova riga è obbligatoria ad ogni test aggiunto che copre un FR/NFR/AC.
- Un FR senza nessuna riga è un finding `REQUIREMENT_GAP` alla review successiva (CLAUDE.md §2.4.3).
- Quando un test viene rimosso o spostato, aggiornare lo status; non lasciare righe orfane.
- Un FR può avere più righe: è normale e desiderabile per requisiti complessi.
