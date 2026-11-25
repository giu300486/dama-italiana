# CLAUDE.md — sezioni rimosse (metodo SDD-4)

> Testo rimosso da `CLAUDE.md` il 2026-11-21 al ritiro del metodo SDD-4, copiato parola per parola.
> Ogni blocco riporta il suo intervallo di righe nel file originale (649 righe). Archivio, non regole attive.

<!-- lines 26–229 of the original CLAUDE.md: §2 Workflow PIANIFICA → IMPLEMENTA → REVIEW → TEST (§2.1–§2.3, introduzione di §2.4) -->

## 2. Workflow obbligatorio: PIANIFICA → IMPLEMENTA → REVIEW → TEST

Ogni fase della roadmap (sezione 16 di `SPEC.md`) DEVE essere completata seguendo nell'ordine **quattro sotto-fasi**, **mai saltarne nessuna, mai cambiarne l'ordine**.

```
┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ PIANIFICA  │ ──► │ IMPLEMENTA │ ──► │   REVIEW   │ ──► │    TEST    │
│            │     │            │     │            │     │            │
│ plans/     │     │ codebase   │     │ reviews/   │     │ tests/ +   │
│ PLAN-N.md  │     │            │     │ REVIEW-N.md│     │ test code  │
└────────────┘     └────────────┘     └────────────┘     └────────────┘
                                              │
                                              │ SPEC drift?
                                              ▼
                                  ┌──────────────────────────┐
                                  │ SPEC change request      │
                                  │ → approvazione utente    │
                                  │ → aggiornamento SPEC.md  │
                                  └──────────────────────────┘
```

Una fase si considera **conclusa** solo quando tutte e quattro le sotto-fasi sono completate e i loro stop point hanno ricevuto conferma utente.

### 2.1 Sotto-fase: PIANIFICA

**Input**:
- `SPEC.md` (sezioni rilevanti per la fase corrente).
- `AI_CONTEXT.md` (stato corrente del progetto).
- `reviews/` precedenti, se esistono.

**Output**: file `plans/PLAN-fase-N.md`.

**Contenuto del piano**:
- Riferimento alla fase della roadmap (es. "Fase 3 — Client UI single-player").
- Acceptance criteria della fase, copiati dallo SPEC sezione 17.
- Lista dei requisiti funzionali (FR-*) e non funzionali (NFR-*) coperti dalla fase, con riferimento esplicito.
- Decomposizione in task ordinati, con dipendenze.
- Moduli toccati per ciascun task.
- Strategia di test prevista (cosa va a unit, cosa a integration, cosa a end-to-end).
- Rischi identificati e mitigazioni.
- Stima di completamento (in numero di task, non in ore).

**Stop point**: dopo aver scritto il piano, **fermarsi e chiedere conferma all'utente** prima di passare a IMPLEMENTA. L'utente può richiedere modifiche al piano o approvarlo. Solo dopo approvazione esplicita si procede.

### 2.2 Sotto-fase: IMPLEMENTA

**Input**: `plans/PLAN-fase-N.md` approvato dall'utente.

**Regole operative**:
- Seguire l'ordine dei task del piano. Se l'ordine va cambiato, documentarlo in `AI_CONTEXT.md` con motivazione.
- Test-first dove sensato (in particolare per `shared` e `core-server`).
- Eseguire `mvn verify` dopo ogni gruppo di modifiche significativo.
- Aggiornare `AI_CONTEXT.md` ad ogni task completato.
- Aggiornare `ARCHITECTURE.md` se emergono decisioni architetturali nuove (nuovo ADR).
- Commit Conventional Commits, uno per task logico (vedi 4.2).

**Cosa fare se si trova un'ambiguità nello SPEC durante l'implementazione**:
1. Fermarsi.
2. Documentare l'ambiguità in `AI_CONTEXT.md` come voce "SPEC clarification needed", citando la sezione SPEC ambigua.
3. Formulare la domanda all'utente con opzioni in tabella.
4. Attendere la risposta.

**Non inventare. Non scegliere "la cosa più probabile". Non procedere senza chiarimento.**

**Output**:
- Codice committato.
- `CHANGELOG.md` aggiornato con voci sotto la sezione `[Unreleased]`.
- `AI_CONTEXT.md` aggiornato con lo stato corrente.

### 2.3 Sotto-fase: REVIEW

**Input**:
- Codebase nello stato post-implementazione.
- `SPEC.md`.
- `plans/PLAN-fase-N.md`.

**Output**: file `reviews/REVIEW-fase-N.md`.

**Procedura passo-passo**:

1. Mappare ogni acceptance criterion della fase corrente al codice che lo realizza.
2. Mappare ogni FR/NFR coinvolto al codice che lo realizza.
3. Eseguire una review code-by-code dei file modificati nella fase.
4. Identificare findings. Per ognuno: categorizzare (vedi 2.3.1), localizzare (path:linea), riferire allo SPEC se applicabile, proporre fix.
5. Compilare il file `reviews/REVIEW-fase-N.md` secondo il formato di 2.3.2.
6. **Stop point**: presentare la review all'utente. L'utente decide:
   - Quali findings affrontare subito.
   - Quali down-graded o deferred.
   - Quali generano richieste di modifica dello SPEC (`SPEC change requests`).
7. Applicare i fix concordati (rispettando il ciclo: ogni fix sostanziale ha un mini-piano in `AI_CONTEXT.md`).
8. Per ogni finding risolto, marcare nel file di review come `RESOLVED` con riferimento al commit.
9. Quando tutti i `BLOCKER` e tutti i `REQUIREMENT_GAP` sono risolti (e tutti i `Critical/High` di `BUG` e `SECURITY`), la review è chiusa.

#### 2.3.1 Categorie di findings

In ordine di severità decrescente:

| Categoria | Definizione | Bloccante per closure? |
|---|---|---|
| `BLOCKER` | Impedisce il funzionamento di una feature richiesta dallo SPEC. Es. la pedina cattura la dama (viola sezione 3.3 SPEC). | Sì, sempre |
| `REQUIREMENT_GAP` | Un requisito SPEC non è implementato del tutto, o è implementato in modo divergente. | Sì, sempre |
| `BUG` | Comportamento errato non legato direttamente a un FR ma evidente. NPE in edge case, leak risorsa, stato inconsistente. | Sì se severity ≥ High |
| `SECURITY` | Vulnerabilità: password loggata, query SQL non parametrizzata, secret in chiaro, validazione mancante. | Sì se severity ≥ High |
| `PERFORMANCE` | Violazione di un NFR di performance. Es. IA Campione che non rispetta i 5s, query N+1 evidente. | Sì se viola un NFR |
| `CODE_QUALITY` | Debito tecnico. Duplicazione, naming, complessità ciclomatica > 15, classi god. | No, ma tracciato |
| `DOC_GAP` | Javadoc o OpenAPI mancanti, sbagliati, fuori sincrono col codice. | No, ma tracciato |

Severity per ogni finding: `Critical` / `High` / `Medium` / `Low`.

#### 2.3.2 Formato file `reviews/REVIEW-fase-N.md`

```markdown
# REVIEW — Fase N: <titolo fase>

- **Data**: YYYY-MM-DD
- **Commit codebase**: <hash short>
- **SPEC version**: <version dal frontmatter SPEC.md>
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale |
|-----------------|---------:|-----:|-------:|----:|-------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |
| REQUIREMENT_GAP |        0 |    0 |      0 |   0 |      0 |
| BUG             |        0 |    0 |      0 |   0 |      0 |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |
| PERFORMANCE     |        0 |    0 |      0 |   0 |      0 |
| CODE_QUALITY    |        0 |    0 |      0 |   0 |      0 |
| DOC_GAP         |        0 |    0 |      0 |   0 |      0 |
| **Totale**      |        0 |    0 |      0 |   0 |      0 |

## Acceptance criteria coverage

| AC ID | Descrizione | Status | Note |
|---|---|---|---|
| 17.1.1 | ... | ✅ COVERED | Test: ... |
| 17.1.2 | ... | ⚠️ PARTIAL | Vedi finding F-003 |
| 17.1.3 | ... | ❌ MISSING | Vedi finding F-007 |

## Findings

### F-001 — [BLOCKER, Critical] La pedina cattura la dama

- **Posizione**: `shared/src/main/java/com/damaitaliana/shared/rules/RuleEngine.java:142`
- **SPEC reference**: sezione 3.3 — "La pedina NON può catturare la dama".
- **Descrizione**: il metodo `generateCaptures` non filtra le destinazioni che contengono una dama avversaria quando il pezzo che cattura è una pedina. Test riproduttivo: `RuleEngineTest#manCannotCaptureKing` (da aggiungere al corpus).
- **Proposta di fix**: aggiungere check esplicito prima di considerare la cattura legale. Aggiornare anche test corpus con almeno 3 posizioni dimostrative.
- **Status**: OPEN

---

### F-002 — [REQUIREMENT_GAP, High] Manca autosave LAN host del torneo

- **Posizione**: `client/src/main/java/com/damaitaliana/client/lan/host/TournamentHostService.java`
- **SPEC reference**: FR-LAN-10, sezione 9.4.6 SPEC.
- **Descrizione**: ...
- **Proposta di fix**: ...
- **Status**: OPEN

---

(altri findings...)

## SPEC change requests

> Vuota se nessun finding suggerisce di modificare lo SPEC.

### CR-001 — Chiarimento sulla regola di promozione durante presa multipla con dama

- **Contesto**: durante review di F-005 è emerso che lo SPEC sezione 3.5 specifica che la pedina si ferma alla promozione, ma non chiarisce esplicitamente cosa succede se è già una dama che attraversa l'ultima riga durante una sequenza.
- **Proposta**: aggiungere clausola "Una dama che attraversa l'ultima riga continua normalmente la sequenza, in quanto è già promossa".
- **Decisione utente**: PENDING

## Closure

- [ ] Tutti i `BLOCKER` risolti
- [ ] Tutti i `REQUIREMENT_GAP` risolti
- [ ] Tutti i `Critical/High` `BUG` risolti
- [ ] Tutti i `Critical/High` `SECURITY` risolti
- [ ] `PERFORMANCE` che violano NFR risolti
- [ ] SPEC change requests con stato non-PENDING

**Review chiusa il**: YYYY-MM-DD
**Commit di chiusura**: <hash>
```

### 2.4 Sotto-fase: TEST

**Input**:
- Codebase post-review.
- `SPEC.md`, in particolare:
  - Sezione 4 — Requisiti funzionali (`FR-*`).
  - Sezione 5 — Requisiti non funzionali (`NFR-*`).
  - Sezione 17 — Acceptance criteria.
- `plans/PLAN-fase-N.md`.

**Output**:
- File `tests/TEST-PLAN-fase-N.md` — piano di test della fase.
- Codice di test nei moduli corrispondenti.
- Aggiornamento di `tests/TRACEABILITY.md`.

**Strategia**: piramide classica con traceability matrix esplicita (Approccio C).


<!-- lines 252–293 of the original CLAUDE.md: §2.4.3 Traceability matrix -->

#### 2.4.3 Traceability matrix

Il file `tests/TRACEABILITY.md` mantiene la mappatura completa **requisito SPEC → test**.

Formato:

```markdown
# Traceability Matrix

## Requisiti funzionali (FR)

| FR ID | Descrizione SPEC (breve) | Test class | Test method(s) | Tipo |
|---|---|---|---|---|
| FR-SP-01 | Avvio partita vs CPU offline | `SinglePlayerSetupTest` | `shouldStartGameOffline` | Unit |
| FR-SP-02 | Tre livelli IA | `AILevelsTest` | `shouldComputePrincipianteDepth`, `shouldComputeEspertoDepth`, `shouldComputeCampioneDepth` | Unit |
| FR-COM-01 | Validazione mosse server-side | `MatchAntiCheatIntegrationTest` | `shouldRejectIllegalMove`, `shouldForfeitAfterFiveIllegalMoves` | Integration |
| ... |

## Requisiti non funzionali (NFR)

| NFR ID | Descrizione SPEC | Test class | Test method(s) | Tipo |
|---|---|---|---|---|
| NFR-P-02 | IA Campione ≤ 5s | `AIPerformanceTest` | `shouldComputeMoveWithinTimeoutCampione` | Unit |
| NFR-S-01 | BCrypt cost ≥ 12 | `UserRegistrationTest` | `shouldHashPasswordWithBcryptCost12` | Integration |
| ... |

## Acceptance criteria (SPEC sezione 17)

| AC ID | Descrizione | Test class | Test method(s) | Tipo |
|---|---|---|---|---|
| 17.1.1 | Partita SP vs Campione termina entro 30 min | `SinglePlayerE2ETest` | `shouldCompleteFullGameVsCampioneWithin30Min` | E2E UI |
| 17.1.5 | Le 4 leggi italiane applicate correttamente | `RuleEngineCorpusTest` | parametrizzato su `test-positions.json` | Unit |
| ... |
```

**Regole della matrice**:
- Ad ogni nuovo test, aggiornare la matrice nello stesso commit.
- Ad ogni nuovo FR/NFR aggiunto allo SPEC, verificare che ci sia almeno una riga.
- Ad ogni rimozione di test, aggiornare lo status di copertura.
- Un FR può essere coperto da più test (è normale e desiderabile per requisiti complessi).
- Un FR senza nessun test è un **finding `REQUIREMENT_GAP`** alla review successiva.


<!-- lines 350–368 of the original CLAUDE.md: §2.4.6 Closure del TEST + §3 Granularità: hybrid scope -->

#### 2.4.6 Closure del TEST

- [ ] Coverage target raggiunti (`mvn jacoco:report` su tutti i moduli).
- [ ] Traceability matrix aggiornata: ogni FR/NFR/AC della fase ha almeno un test.
- [ ] Test corpus regole italiane aggiornato con eventuali nuove posizioni.
- [ ] `mvn verify` passa pulito su tutti i moduli.
- [ ] Test plan `tests/TEST-PLAN-fase-N.md` documenta scelte e copertura.
- [ ] Nessun test in stato `@Disabled` o `@Ignore` senza issue tracciata.

---

## 3. Granularità: hybrid scope

Il ciclo a 4 sotto-fasi gira **a ogni fase** della roadmap (sezione 16 SPEC). Tuttavia lo scope di review e test cresce in modo controllato:

- La **REVIEW** della fase N esamina prioritariamente il codice nuovo, ma **estende lo scope a moduli precedenti se la fase N li ha modificati**. Esempio: se in fase 6 il client viene aggiornato per gestire match Internet, la review include anche le parti del client già esistenti che sono state toccate.
- I **TEST** della fase N si concentrano sui nuovi requisiti, ma **rieseguono tutta la suite precedente**. Se un test precedente fallisce, è un finding `BUG` o `REQUIREMENT_GAP` (regression).
- La **traceability matrix** è cumulativa: ogni fase aggiunge righe, mai le rimuove (a meno di refactoring esplicito tracciato in `CHANGELOG.md`).


<!-- lines 473–545 of the original CLAUDE.md: §6 Struttura file di workflow (+ §6.1 AI_CONTEXT) + §7 Avvio sessione Claude Code -->

## 6. Struttura file di workflow

```
dama-italiana/
├── SPEC.md                           # Fonte di verità (read-only senza approvazione utente)
├── CLAUDE.md                         # Questo file
├── ARCHITECTURE.md                   # Decisioni architetturali (ADR esteso)
├── AI_CONTEXT.md                     # Stato corrente del progetto (vedi 6.1)
├── CHANGELOG.md                      # Storia modifiche
├── README.md                         # Istruzioni di setup
├── plans/
│   ├── PLAN-fase-0.md
│   ├── PLAN-fase-1.md
│   └── ...
├── reviews/
│   ├── REVIEW-fase-0.md
│   ├── REVIEW-fase-1.md
│   └── ...
├── tests/
│   ├── TEST-PLAN-fase-0.md
│   ├── TEST-PLAN-fase-1.md
│   ├── TRACEABILITY.md
│   └── ...
├── shared/
├── core-server/
├── client/
└── server/
```

### 6.1 Struttura di `AI_CONTEXT.md`

`AI_CONTEXT.md` è il "running state" del progetto, leggibile in 30 secondi. Struttura raccomandata:

```markdown
# AI Context

## Stato corrente
- **Fase roadmap**: <numero e titolo>
- **Sotto-fase**: PIANIFICA | IMPLEMENTA | REVIEW | TEST
- **Ultimo task completato**: <descrizione>
- **Prossimo task**: <descrizione>
- **Ultimo commit**: <hash + message>

## Decisioni recenti
- Lista di decisioni architetturali o di design prese di recente, con riferimento a ADR.

## SPEC clarifications needed
- Eventuali ambiguità incontrate, in attesa di risposta utente.

## Note operative
- Workaround temporanei, debiti, cose da ricordare.
```

Da aggiornare ad ogni task completato.

---

## 7. Avvio sessione Claude Code

Ad ogni nuova sessione, Claude Code DEVE seguire questa procedura prima di scrivere qualunque codice:

1. Leggere `SPEC.md` integralmente la prima volta. Nelle sessioni successive, rileggere almeno: sezione 1 (vision), sezione 16 (roadmap), sezione 17 (acceptance criteria), e le sezioni rilevanti per la fase corrente.
2. Leggere `AI_CONTEXT.md` per capire lo stato attuale.
3. Leggere le ultime 20 righe di `CHANGELOG.md`.
4. Identificare la **fase corrente** della roadmap.
5. Identificare la **sotto-fase corrente** (PIANIFICA / IMPLEMENTA / REVIEW / TEST).
6. Se è in corso un piano, una review, o un test plan: leggerlo integralmente.
7. **Confermare con l'utente** lo stato compreso prima di proseguire, sintetizzando in 3-5 righe cosa farà.

> **Nota cross-fase (vincolante da F6+)**: se la fase corrente o un task aggiunge **schermate UI** (lobby online F6, LAN host/discovery/chat F7, tornei F8, classifiche F9, replay/profilo F11), rileggere **§8 anti-pattern #15** prima di disegnare FXML/CSS — il design system di Fase 3.5 è single source of truth per tutto il visual stack.

---


<!-- lines 559–561 of the original CLAUDE.md: §8 Anti-pattern, punti 10–12 -->

10. **Non saltare la sotto-fase REVIEW** anche se il codice "sembra funzionare". È nel workflow per un motivo.
11. **Non saltare la PIANIFICAZIONE.** Anche per fasi piccole. Anche per fix triviali post-review (un mini-piano in `AI_CONTEXT.md` è sufficiente per un singolo finding).
12. **Non modificare `SPEC.md` di iniziativa propria.** Sempre passare per il flusso CR.

<!-- lines 580–606 of the original CLAUDE.md: §9 Trigger di stop-and-ask -->

## 9. Trigger di stop-and-ask

Lista non esaustiva di situazioni in cui Claude Code DEVE fermarsi e chiedere all'utente:

| Trigger | Cosa fare |
|---|---|
| Ambiguità nello SPEC su un requisito che si sta implementando | Stop, documentare in `AI_CONTEXT.md`, formulare domanda con opzioni |
| Decisione architetturale non coperta da SPEC né da ADR esistenti | Stop, proporre 2-3 opzioni con trade-off |
| Trade-off significativo tra due implementazioni plausibili | Stop, proporre opzioni con trade-off |
| Necessità di una dipendenza non in SPEC sez. 6 | Stop, motivare la richiesta |
| Findings di review che potrebbero richiedere modifica SPEC | Documentare come `SPEC change request`, stop |
| Difficoltà a soddisfare un acceptance criterion | Stop, proporre: rilassare il criterio (con CR) o cambiare implementazione |
| Test che fallisce in modo inatteso e richiede ripensamento del design | Stop, descrivere il problema e proporre soluzioni |
| Nuova decisione di design che impatta moduli già completati | Stop, valutare scope di modifica |
| Performance/complexity non in linea con NFR | Stop, valutare se NFR va rilassato (CR) o se serve ottimizzazione |

**Modalità di stop-and-ask**:
1. Aggiornare `AI_CONTEXT.md` con la voce "PENDING USER DECISION".
2. Comporre messaggio strutturato all'utente:
   - Contesto (dove siamo).
   - Problema (cosa è emerso).
   - Opzioni (almeno 2, con trade-off in tabella).
   - Raccomandazione (se applicabile).
3. Attendere risposta. Non procedere.

---


<!-- lines 618–648 of the original CLAUDE.md: §11 Quick reference -->

## 11. Quick reference

### Per iniziare una nuova fase

```
1. Leggere SPEC sezioni 16, 17 e quelle rilevanti per la fase
2. Leggere AI_CONTEXT.md, CHANGELOG.md, eventuali review precedenti
3. Sotto-fase PIANIFICA: scrivere plans/PLAN-fase-N.md
4. ⏸️ Stop point → chiedere conferma utente
5. Sotto-fase IMPLEMENTA: codice + AI_CONTEXT updates + commits
6. Sotto-fase REVIEW: scrivere reviews/REVIEW-fase-N.md
7. ⏸️ Stop point → discutere findings con utente
8. Applicare fix concordati, chiudere review
9. Sotto-fase TEST: scrivere tests/TEST-PLAN-fase-N.md + test code + aggiornare TRACEABILITY.md
10. ⏸️ Stop point → conferma chiusura fase con utente
11. Tag git v0.<fase>.0
```

### Per iniziare una nuova sessione (mid-fase)

```
1. Leggere SPEC sezioni rilevanti per la sotto-fase corrente
2. Leggere AI_CONTEXT.md (stato + sotto-fase)
3. Leggere file di lavoro corrente (PLAN o REVIEW o TEST_PLAN)
4. Sintetizzare in 3-5 righe lo stato all'utente
5. ⏸️ Confermare prossima azione
6. Procedere
```

---


