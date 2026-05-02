# CLAUDE.md — Istruzioni operative per Claude Code

> **Lettura obbligatoria all'inizio di ogni sessione.**
> Questo file definisce il workflow di sviluppo del progetto **Dama Italiana Multiplayer**.
> La fonte di verità autoritativa del progetto è `SPEC.md`.

---

## 1. Identità e fonte di verità

Il documento autoritativo del progetto è **`SPEC.md`** nella root del repository.

Tutte le decisioni implementative, tutti i requisiti funzionali e non funzionali, tutti i flussi operativi, tutti gli acceptance criteria sono definiti lì. In caso di conflitto tra `SPEC.md` e qualsiasi altro file (incluso questo), **prevale `SPEC.md`**.

`SPEC.md` può evolvere nel tempo, ma **solo dopo approvazione esplicita dell'utente**. Le modifiche allo SPEC seguono questo flusso:

1. Una review identifica un'ambiguità, un buco, o un requisito da aggiornare.
2. La review documenta la proposta nella sezione "SPEC change requests".
3. L'utente approva o rifiuta.
4. Se approvata, la modifica viene applicata a `SPEC.md` con commit dedicato `docs(spec): ...` e annotata in `CHANGELOG.md`.

**Mai modificare `SPEC.md` di iniziativa propria.** Mai. Anche se sembra che ci sia un errore evidente.

---

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

#### 2.4.1 Composizione

| Tipo | Target % | Tooling | Cosa testa |
|---|---|---|---|
| **Unit** | ~70% | JUnit 5 + AssertJ + Mockito | Logica pura: `RuleEngine`, AI, `TournamentEngine`, parser, serializzatori, `TieBreakerPolicy` |
| **Integration** | ~25% | Spring Boot Test + Testcontainers (MySQL) | REST endpoints, JPA repositories, WebSocket flow, autenticazione, persistenza match |
| **E2E UI** | ~3% | TestFX | Flussi UI critici single-player |
| **E2E multi-client** | ~2% | Test Java custom con due/più client embedded | Flussi LAN match, LAN tournament, Internet match, riconnessione |

#### 2.4.2 Coverage target

Da `SPEC.md` NFR-M-01 e NFR-M-02:

| Modulo | Coverage minima | Note |
|---|---|---|
| `shared` | ≥ 80% (90% raccomandato per `RuleEngine`) | Logica pura, test esaustivi |
| `core-server` | ≥ 80% | Logica torneo e match |
| `client` (escluse view JavaFX) | ≥ 60% | Controller e adapter |
| `server` | ≥ 70% | REST + persistenza |

JaCoCo configurato in parent POM con check obbligatorio. Coverage misurata escludendo classi DTO senza logica e classi di configurazione Spring.

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

#### 2.4.4 Test corpus per le regole italiane

Le regole della Dama Italiana sono il punto più rischioso del progetto. Si DEVE mantenere un **test corpus parametrizzato** in `shared/src/test/resources/test-positions.json`.

**Formato di una posizione**:

```json
{
  "id": "rule-quantity-001",
  "description": "Cattura obbligatoria di 2 pedine vs cattura di 1: deve essere imposta quella di 2.",
  "specReference": "3.4 - Legge della quantità",
  "boardRepresentation": {
    "white": ["12", "19"],
    "black": ["16", "23", "26"],
    "kings": []
  },
  "sideToMove": "WHITE",
  "expectedLegalMoves": ["12x19x26"],
  "rejectedMoves": ["12x19", "19x26"],
  "notes": "Optional rationale and edge case notes"
}
```

> Nota: la rappresentazione esatta del board (FEN-like vs explicit) è da definire in fase 1; il piano di fase 1 deve includere questa decisione e applicarla coerentemente al corpus.

**Test parametrizzato**: `RuleEngineCorpusTest` itera su tutte le posizioni del corpus, per ognuna invoca `RuleEngine.legalMoves` e verifica che il risultato corrisponda a `expectedLegalMoves` e non contenga nessuna delle `rejectedMoves`.

**Copertura minima del corpus**:

| Area regole | N. posizioni minime |
|---|---|
| Movimento pedina (avanti diagonale, mai indietro) | 3 |
| Movimento dama (1 casa in tutte e 4 diagonali) | 4 |
| Cattura semplice pedina | 4 |
| Cattura semplice dama | 4 |
| **Pedina non cattura dama** | 3 |
| Presa multipla con sequenze diverse | 5 |
| **Legge della quantità** | 5 |
| **Legge della qualità** | 5 |
| **Legge della precedenza dama** | 3 |
| **Legge della prima dama** | 3 |
| Promozione con stop sequenza | 3 |
| Triplice ripetizione | 2 |
| Regola 40 mosse | 2 |
| Stallo = sconfitta | 2 |
| **Totale minimo** | **48** |

**Regola di crescita del corpus**: quando emerge un bug sulle regole durante una review, **prima di fixare** si aggiunge la posizione che lo riproduce al corpus. Test rosso → fix → test verde. Il corpus cresce nel tempo e diventa la rete di sicurezza più solida del progetto.

#### 2.4.5 Naming convention

- **Unit test**: `<ClasseProduzione>Test`. Es. `RuleEngineTest`.
- **Integration test**: `<Feature>IntegrationTest`. Es. `MatchPersistenceIntegrationTest`.
- **E2E test**: `<Feature>E2ETest`. Es. `LanTournamentE2ETest`.
- **Test method**: stile `should<EspressioneAttesa>_when<Condizione>` oppure `<Feature><Scenario>`. Scegliere uno stile per modulo e applicarlo coerentemente.

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

---

## 4. Convenzioni di codice e versioning

(Estende SPEC sezione 18.2.)

### 4.1 Codice

- Identificatori e Javadoc: **inglese**.
- Messaggi UI: ResourceBundle (`messages_it.properties`, `messages_en.properties`).
- Stile: Google Java Style Guide via Spotless (`mvn spotless:apply` prima di ogni commit).
- Niente `System.out.println`, niente `e.printStackTrace()`. Solo SLF4J.
- Niente TODO/FIXME senza issue tracciata o riferimento a un finding di review.
- `var` ammesso quando il tipo è ovvio dal lato destro; preferire tipo esplicito altrimenti.
- Records e sealed types per modello di dominio (Java 21).
- `Optional` solo per ritorni; mai per parametri o campi.

### 4.2 Commit (Conventional Commits)

Formato: `<type>(<scope>): <subject>`.

Type: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `style`.

Scope (consigliati): `shared`, `core-server`, `client`, `server`, `lan`, `ai`, `ui`, `spec`, `claude`, `arch`.

Esempi:
- `feat(shared): implement Italian draughts rule engine`
- `fix(server): correct sequence number race in MatchManager`
- `test(shared): add corpus positions for legge della prima dama`
- `docs(spec): clarify round-robin tie-breaker (CR-003)`
- `refactor(client): extract LAN host setup into dedicated service`
- `perf(ai): add transposition table to Campione level`

Body opzionale ma consigliato per fix non triviali e per spiegare il "perché".

### 4.3 Branch

Modello: **GitFlow leggero**, due branch a vita lunga + branch effimeri.

```
main      ──●─────────────────────────●───────────●──── (production / tag)
            ↓ (una tantum)            ↑           ↑
develop   ──●──●──●──●──●──●──●──●──●──●──────●──●──── (integrazione, default branch GitHub)
                  ↑     ↑     ↑           ↑
                  │     │     │           │
feature/X ────────●──●──●     │           │
fix/Y ────────────────────────●           │
feature/Z ────────────────────────────────●
```

- **`main`**: solo commit production-ready. Ogni merge in `main` è candidato al tag `v0.<fase>.0`. Niente push diretti su `main` se non per merge da `develop` (vedi §4.4).
- **`develop`**: branch di integrazione. È il **default branch su GitHub** (configurato in Fase 0): clone, PR, lavoro corrente avvengono qui.
- **Feature branch**: `feature/<fase>-<topic>`. Staccati da `develop`, mergiati su `develop`. Es. `feature/1-rule-engine`, `feature/3-board-view`, `feature/8-bracket-generator`.
- **Fix da review**: `fix/review-N-F-<id>`. Stesso modello, staccati e mergiati su `develop`. Es. `fix/review-3-F-002`.

**Merge strategy**: `git merge --no-ff` (merge commit, niente fast-forward). Preserva la storia del feature branch come "bolla" visibile nella history di `develop`. Niente squash di default — riduce la traceability del lavoro per task.

**Pull Request**: opzionale per single-developer. Merge locale è accettato, purché:
1. Il branch sorgente abbia `mvn verify` verde sull'ultimo commit.
2. Il commit di merge abbia messaggio nel formato `merge: <branch> -> develop` o equivalente Conventional Commits (`merge(feature/<topic>): merge feature/X into develop`).

**Quando la CI verrà riattivata** (vedi `ARCHITECTURE.md` ADR-019): "merge solo dopo build CI verde sul branch sorgente".

### 4.4 Versioning del progetto

SemVer applicato a release pubbliche (post fase 11). Pre-release: tag `v0.<fase>.0` ad ogni fase chiusa.

**Workflow di chiusura fase con GitFlow**:

```bash
# A fase chiusa (tutti i feature/fix mergiati su develop, mvn verify verde):
git checkout main
git merge --no-ff develop -m "release: chiusura Fase N"
git tag -a v0.<fase>.0 -m "Fase N — <titolo> completata"
git push origin main
git push origin v0.<fase>.0
git checkout develop                       # torna su develop per la fase successiva
```

Il tag risiede sul commit di merge in `main`, **non** sul commit corrispondente in `develop`. Questo allinea i tag al branch production e li rende il riferimento autoritativo per le release.

**Eccezione Fase 0**: il tag `v0.0.0` è stato applicato direttamente su `main` (commit `e68335f`) prima dell'introduzione del modello GitFlow. Coincide col commit a cui poi `develop` è stato staccato. Conserva la sua validità.

---

## 5. Comandi standard

| Comando | Scopo |
|---|---|
| `mvn clean verify` | Build completo + test + coverage check |
| `mvn -pl shared verify` | Verify modulo specifico |
| `mvn -pl core-server verify` | Verify core-server |
| `mvn -pl client javafx:run` | Lancia client desktop |
| `mvn -pl server spring-boot:run` | Lancia server centrale (richiede MySQL) |
| `docker-compose up -d` | Avvia MySQL dev |
| `docker-compose down` | Ferma infrastruttura dev |
| `mvn spotless:apply` | Formatta codice secondo Google Style |
| `mvn spotless:check` | Verifica formato senza modificare |
| `mvn jacoco:report` | Genera report coverage HTML in `target/site/jacoco/` |
| `mvn dependency:tree` | Verifica grafo dipendenze |
| `mvn dependency:analyze` | Trova dipendenze inutilizzate o non dichiarate |

---

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

## 8. Anti-pattern (cose da NON fare)

Specifici per questo progetto. Sono regole assolute.

1. **Non duplicare la logica torneo nel client.** Il client (anche in modalità host LAN) USA `core-server`, non lo reimplementa.
2. **Non bypassare `RuleEngine` nel server.** Tutte le mosse passano dal `RuleEngine` di `shared` per validazione, anche se sembra ridondante rispetto a una validazione client-side già fatta.
3. **Non scrivere SQL diretto.** JPA con repository typed. SQL nativo solo se assolutamente necessario, motivato in commit.
4. **Non hardcodare stringhe UI.** Tutte le stringhe visibili all'utente vanno in `messages_*.properties`.
5. **Non committare secrets.** Mai. Env variables, profili Spring.
6. **Non includere Tomcat nel client.** Quando si attiva il container embedded per LAN host, escludere esplicitamente `spring-boot-starter-tomcat` e includere `spring-boot-starter-jetty`.
7. **Non far dipendere `shared` da framework.** `shared` è dominio puro: niente Spring, JavaFX, JPA, WebSocket, Jetty.
8. **Non far dipendere `core-server` dal trasporto specifico.** `core-server` è agnostico: niente Tomcat, Jetty, JPA. Dipende solo da `shared` e da `spring-context` (DI).
9. **Non accoppiare la UI a un trasporto specifico.** I controller JavaFX usano astrazioni (es. `MatchClient` interface) con implementazioni LAN e Internet intercambiabili.
10. **Non saltare la sotto-fase REVIEW** anche se il codice "sembra funzionare". È nel workflow per un motivo.
11. **Non saltare la PIANIFICAZIONE.** Anche per fasi piccole. Anche per fix triviali post-review (un mini-piano in `AI_CONTEXT.md` è sufficiente per un singolo finding).
12. **Non modificare `SPEC.md` di iniziativa propria.** Sempre passare per il flusso CR.
13. **Non aggiungere dipendenze non menzionate in `SPEC.md` sezione 6** senza approvazione esplicita dell'utente.
14. **Non rendere green un test sopprimendo l'asserzione o disabilitando il test.** Se un test fallisce, capire perché.
15. **Non introdurre nuovi stili visuali ad-hoc nelle fasi successive a F3.5.** Tutte le nuove schermate (lobby online F6, LAN host/discovery/chat F7, tornei F8, classifiche/scheduler F9, polish/replay/profilo F11) DEVONO riusare il **design system di Fase 3.5**:
    - `ThemeService.applyTheme(scene)` su ogni nuova `Scene` modale o root (pattern già usato da `SceneRouter` e `SaveDialogController`).
    - Token CSS v2: vocabolario duale `-color-bg-*` / `-color-text-on-*`, classi utility `screen-root` / `card-elevated` / `popover` / `button-primary` / `button-secondary` / `label-display` / `label-display-md` / `label-title` / `label-subtitle` / `label-secondary`.
    - Tipografia bi-classe: **Playfair Display** per heading display + **Inter** per UI/body.
    - Texture wood (Poly Haven CC0) via `BackgroundImage` e cornici frame; mai flat-color piatti.
    - Componenti gameplay riusabili: `BoardRenderer`, `BoardCellNode`, `PieceNode` (composito 3D-look gradient + ring + gloss + crown).
    - Animation system: `MoveAnimator` (easing OUT_BACK), `ParticleEffects` (captureSplash, promotionGlow, mandatoryGlow), `AnimationOrchestrator`.
    - `AudioService` per qualsiasi nuovo cue audio (mai `MediaPlayer` diretto in controller).
    - Asset solo CC0/CC-BY/OFL, audit obbligatorio in `CREDITS.md`.

    Riferimenti normativi: SPEC §13.2 (riscritta a v2.2 da CR-F3.5-001), ADR-034 (visual rework videogame premium wood), ADR-035 (AudioService), ADR-037 (asset licensing), `ThemeService` come single source of truth.

    Eventuali estensioni del design system (es. nuova classe `tournament-bracket-table` per la classifica F9) sono **task dedicati con ADR**, non improvvisazioni per fase. Se una nuova schermata sembra richiedere uno stile non presente nei token v2, fermarsi (CLAUDE.md §9 stop-and-ask) e proporre l'estensione del design system **prima** di scrivere FXML/CSS.

---

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

## 10. Versionamento di questo file

Modifiche a `CLAUDE.md` seguono la stessa cautela di `SPEC.md`:
- Solo dopo discussione con l'utente.
- Commit dedicato `docs(claude): ...`.
- Annotato in `CHANGELOG.md`.

Questo per evitare drift silenzioso del workflow stesso.

---

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

**FINE DOCUMENTO**
