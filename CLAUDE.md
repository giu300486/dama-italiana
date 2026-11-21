# CLAUDE.md — Istruzioni operative per Claude Code

> **Metodo SDD-4 ritirato il 2026-11-21**: il ciclo PIANIFICA → IMPLEMENTA → REVIEW → TEST e i suoi documenti (`AI_CONTEXT.md`, `plans/`, `reviews/`, test plan, traceability matrix) sono archiviati in [`_legacy/sdd4/`](_legacy/sdd4/README.md).
> Il testo rimosso da questo file è in [`_legacy/sdd4/CLAUDE-removed.md`](_legacy/sdd4/CLAUDE-removed.md) con le righe originali; le sezioni rimaste conservano la loro numerazione.

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

## 2.4 Regole di qualità dei test

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
10.–12. *(ritirati con il metodo SDD-4 il 2026-11-21: testo in `_legacy/sdd4/CLAUDE-removed.md`)*
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

## 10. Versionamento di questo file

Modifiche a `CLAUDE.md` seguono la stessa cautela di `SPEC.md`:
- Solo dopo discussione con l'utente.
- Commit dedicato `docs(claude): ...`.
- Annotato in `CHANGELOG.md`.

Questo per evitare drift silenzioso del workflow stesso.

---

**FINE DOCUMENTO**

<!-- aw:begin -->
## aw workflow
This project uses the aw workflow (plugin `aw`). At session start a message "aw: context restored" should appear.
If it did not, the plugin is not active: before doing anything, read `.aw/RESUME.md` (if present) and the dossiers in `.aw/work/`, show the user the open workstreams and wait for a choice. Tell the user the aw plugin seems inactive.
<!-- aw:end -->
