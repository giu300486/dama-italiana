# ARCHITECTURE

> Decisioni architetturali del progetto **Dama Italiana Multiplayer**.
> Le decisioni iniziali (ADR-001 ÷ ADR-017) sono fissate in `SPEC.md` Appendice B e qui riportate per comodità di consultazione.
> Nuovi ADR (ADR-018+) emersi durante l'implementazione vanno aggiunti in coda alla sezione "Decisioni successive".

---

## ADR fissati dallo SPEC

Riferimento autoritativo: `SPEC.md` Appendice B.

| ADR     | Titolo                       | Decisione                                                                                  |
|---------|------------------------------|---------------------------------------------------------------------------------------------|
| ADR-001 | Linguaggio                   | Java 21 LTS                                                                                 |
| ADR-002 | Build tool                   | Maven multi-modulo                                                                          |
| ADR-003 | UI framework                 | JavaFX 21+ con CSS custom design system                                                     |
| ADR-004 | DI client                    | Spring Boot starter (non-web)                                                               |
| ADR-005 | Container LAN host           | Jetty embedded via Spring Boot WebSocket starter, on-demand                                 |
| ADR-006 | Server framework             | Spring Boot 3.4+                                                                            |
| ADR-007 | DB Internet                  | MySQL 8 (vs PostgreSQL)                                                                     |
| ADR-008 | Real-time protocol           | STOMP su WebSocket                                                                          |
| ADR-009 | Discovery LAN                | mDNS via JmDNS                                                                              |
| ADR-010 | Architettura                 | Monorepo Maven 4 moduli (shared, core-server, client, server)                              |
| ADR-011 | Modulo `core-server`         | Libreria condivisa client (host LAN) ↔ server                                                |
| ADR-012 | Storage match                | Log mosse append-only con sequence number monotonico + stato corrente materializzato        |
| ADR-013 | Variante gioco               | Solo Dama Italiana FID (rimossa Internazionale)                                              |
| ADR-014 | Tornei                       | Single elimination + round-robin con punti                                                   |
| ADR-015 | IA livelli                   | 3 (Principiante, Esperto, Campione)                                                          |
| ADR-016 | Salvataggi single-player     | Multi-slot + autosave                                                                        |
| ADR-017 | Tie-breaker round-robin      | Scontro diretto → Sonneborn-Berger → numero vittorie → sorteggio (configurabile via `TieBreakerPolicy`) |

---

## Decisioni successive (ADR-018+)

> Vuota all'inizio del progetto. Ogni nuovo ADR si aggiunge qui con il formato:
>
> ### ADR-NNN — Titolo
>
> - **Data**: YYYY-MM-DD
> - **Stato**: Accepted / Superseded by ADR-XXX / Deprecated
> - **Contesto**: cosa ha generato la decisione.
> - **Decisione**: cosa si è scelto.
> - **Conseguenze**: trade-off, vincoli, rischi residui.
> - **Alternative considerate**: opzioni scartate e perché.

### ADR-018 — Ambiente di sviluppo MySQL locale, Docker Compose rimosso

- **Data**: 2026-04-27
- **Stato**: Accepted
- **Contesto**: Il progetto era partito con un `docker-compose.yml` per fornire MySQL 8 + Adminer come ambiente di sviluppo standardizzato. L'unico developer attuale dispone già di un'istanza MySQL in esecuzione localmente, gestita via MySQL Workbench / DBeaver. Mantenere Docker come prerequisito introdurrebbe attrito (avvio container, conflitto porte, gestione volumi) senza beneficio reale per il singolo developer.
- **Decisione**: Rimuoviamo `docker-compose.yml` e `.env.example`. L'ambiente di sviluppo richiede un MySQL 8.0+ già installato sulla macchina del developer, raggiungibile su `localhost:3306`. Lo schema viene comunque gestito da Flyway, che è agnostico rispetto a come MySQL è stato avviato.
- **Conseguenze**:
  - Prerequisito Docker rimosso dal README.
  - `application.yml` ha default coerenti con un MySQL locale standard (porta 3306, db `dama_italiana`, utente `dama`).
  - Il setup di DB e utente è ora una procedura SQL manuale documentata nel README (eseguibile da Workbench/DBeaver in pochi secondi).
  - Quando il progetto avrà più contributor o un ambiente CI/CD, sarà opportuno reintrodurre Docker Compose (eventualmente come opzione in un altro PR). Non c'è perdita di lavoro: il file precedente è recuperabile da git history (commit `b355823` e `9432e44`).
- **Alternative considerate**:
  - Tenere `docker-compose.yml` come "fallback opzionale" → rumore non utilizzato.
  - Tenere Docker e disabilitare MySQL locale → richiederebbe uno spegnimento del processo nativo o una mappatura porta non standard.

### ADR-019 — CI GitHub Actions predisposta ma disattivata per scelta in Fase 0

- **Data**: 2026-04-27 (rivista 2026-04-28)
- **Stato**: Accepted
- **Contesto**: Il workflow `.github/workflows/ci.yml` era stato creato in Fase 0 (Task 0.8). Il repository remoto `origin` su GitHub esiste (`github.com/giu300486/dama-italiana`) e i commit verranno pushati regolarmente. Tuttavia, in questa fase del progetto:
  - Non c'è ancora una macchina dedicata al deploy/staging.
  - La codebase è quasi vuota e i test sono smoke; far girare la CI ad ogni push produrrebbe rumore senza catturare regressioni reali.
  - Il developer preferisce non consumare minuti CI gratuiti del piano GitHub finché il quality gate locale (`mvn verify`) basta da solo.
- **Decisione**: Rinominiamo `.github/workflows/ci.yml` → `.github/workflows/ci.yml.disabled`. GitHub Actions ignora workflow con estensione non `.yml`/`.yaml`: anche dopo `git push`, la pipeline non viene eseguita. Quando le motivazioni sopra cesseranno (es. dominio sostanzioso da Fase 1+, oppure desiderio di un badge verde/rosso pubblico), basta `git mv ci.yml.disabled ci.yml` per riattivarla.
- **Conseguenze**:
  - I tre quality gate (build/lint/sast) restano completamente verificabili in locale via `mvn clean verify` (il parent POM lega Spotless e SpotBugs alla phase `verify`).
  - Push e pull verso `origin/main` continuano normalmente; cambia solo l'esecuzione automatizzata del workflow.
  - Acceptance A0.6 della Fase 0 (CI verde) resta marcato come "DEFERRED": sarà rivalutato al momento della riattivazione.
- **Alternative considerate**:
  - **Lasciare CI attiva**: produce run su ogni push con codebase quasi vuota → rumore + consumo minuti gratuiti.
  - **Trigger solo `workflow_dispatch`** (manuale): meno rumore ma fragile (è facile dimenticarsi). La rinomina in `.disabled` è più esplicita.
  - **Eliminare `ci.yml`**: perde il lavoro fatto. Riattivare richiederebbe ricreare il workflow.
  - **Adottare CI self-hosted (Gitea Actions, Drone, Woodpecker)**: richiede infrastruttura nuova non strettamente necessaria. Documentato nel README come proposta futura.

### ADR-020 — Notazione FID 1-32 (orientamento standard)

- **Data**: 2026-04-28
- **Stato**: Accepted
- **Contesto**: SPEC §3.8 prescrive la notazione FID con caselle 1-32 sulle sole case scure ma non specifica l'orientamento (numero 1 in alto a sinistra dal Bianco oppure in basso a sinistra). I database FID e la maggior parte dei testi del regolamento federale numerano la casa 1 in alto-sinistra rispetto al Bianco e procedono per righe verso il basso, in modo che il Bianco muova da numeri alti (sua riga di partenza, 21-32) verso numeri bassi (sua riga di promozione, 1-4). La scelta opposta sarebbe più intuitiva ("avanti = numeri crescenti") ma incompatibile con corpus FID esistenti.
- **Decisione**: Adottiamo l'orientamento FID standard. La mappa `int 1..32 ↔ Square(file, rank)` è documentata in `FidNotation` con il diagramma ASCII completo. Il Bianco parte sui numeri 21-32, il Nero sui numeri 1-12. Le case scure (e quindi le 32 case numerate) sono quelle dove `(file + rank) % 2 == 0`, coerentemente con SPEC §3.1 che pone la casa scura nell'angolo in basso a sinistra del Bianco.
- **Conseguenze**:
  - `FidNotation.toFid` e `toSquare` sono bijettive su `[1,32]`.
  - Il corpus regole (ADR-022) usa direttamente questa numerazione → import semplice di posizioni FID future.
  - Le mosse vengono parsate/formattate come `"12-16"`, `"12x19"`, `"12x19x26"` (SPEC §3.8).
- **Alternative considerate**:
  - Numerazione "intuitiva" (Bianco da basso verso alto, file 0 prima): scartata per incompatibilità con il regolamento federale e i database FID.

### ADR-021 — Identità di posizione per triplice ripetizione

- **Data**: 2026-04-28
- **Stato**: Accepted
- **Contesto**: SPEC §3.6 elenca la triplice ripetizione fra le condizioni di patta. Servono due decisioni: (a) quali campi compongono l'identità di una posizione; (b) dove si tiene il conteggio delle occorrenze. SPEC §8.1 non include un contatore di posizioni nel record `GameState`.
- **Decisione**: L'identità di una posizione è la coppia `(Board, Color sideToMove)` materializzata in un record privato `PositionKey` con equals/hashCode automatici di Java 21. Il conteggio NON è memorizzato in `GameState`: viene **ricalcolato dalla history** ogni volta che `computeStatus` ha bisogno di valutare la ripetizione. Il replay parte da `GameState.initial()` e applica ogni mossa via un metodo privato `applyCore` che esegue la transizione di stato senza richiamare `computeStatus`, evitando la ricorsione `applyMove → computeStatus → applyMove`.
- **Conseguenze**:
  - `GameState` resta uno snapshot puramente derivabile dalla sua history (oltre a `Board.initial()`): semplice da serializzare e replicare.
  - Costo computazionale `O(n²)` su `n` mosse per ogni `applyMove`: accettabile per F1 dove `n` resta nell'ordine delle decine; rivedibile in F2 quando l'IA esplora alberi profondi.
  - Il replay implicitamente assume che la history corrisponda a una sequenza dalla posizione iniziale standard. Stati costruiti a mano con history arbitrarie possono produrre falsi negativi sulla ripetizione: documentato nel Javadoc di `isThreefoldRepetition`.
- **Alternative considerate**:
  - **Conteggio carry in `GameState`**: efficiente ma rompe l'invariante "stato derivabile dalla history" e richiederebbe un campo dedicato.
  - **Confronto solo degli ultimi N stati**: euristica fragile.
  - **Hash Zobrist per `PositionKey`**: ottimizzazione rinviata a F2 (transposition table per IA).

### ADR-022 — Schema JSON del corpus regole

- **Data**: 2026-04-28
- **Stato**: Accepted
- **Contesto**: CLAUDE.md §2.4.4 prescrive un test corpus parametrizzato con almeno 48 posizioni distribuite per categoria. L'esempio mostrato nel CLAUDE.md (`white`/`black` + `kings` separati) presenta una potenziale ambiguità: se un numero di casa compare in entrambe le liste, è una pedina o una dama? La validazione richiederebbe `kings ⊆ white ∪ black`.
- **Decisione**: Adottiamo uno schema JSON **disgiunto** con quattro liste mutualmente esclusive: `whiteMen`, `whiteKings`, `blackMen`, `blackKings`. Il loader (`CorpusLoader`) verifica al caricamento che nessun numero compaia in più di una lista. Ogni posizione è un oggetto con: `id` (univoco), `description`, `specReference`, `category` (chiave fra le 14 di CLAUDE.md §2.4.4), `board`, `sideToMove` (`"WHITE"|"BLACK"`), `expectedLegalMoves` (lista di stringhe FID), `rejectedMoves` (lista opzionale), `notes` (opzionale).
- **Conseguenze**:
  - Costruzione del `Board` immediata: una sola passata per lista, una `Board.with(square, piece)` per ciascun numero. Niente check incrociati a runtime.
  - Schema robusto a errori di trascrizione: una posizione che ripete un numero in due liste fallisce subito al caricamento.
  - I test di copertura (`RuleEngineCorpusCoverageTest`, `RuleEngineCorpusSchemaTest`) garantiscono il rispetto dei minimi e della struttura ad ogni build.
- **Alternative considerate**:
  - **Schema dell'esempio SPEC** (`white`/`black` + `kings`): leggermente meno verboso ma richiede validazione a runtime e duplica i numeri delle dame.
  - **Schema FEN-like** (una stringa per riga): più compatto ma meno leggibile per chi scrive le posizioni a mano.

### ADR-023 — `GameStatus` esteso a 6 voci (motivo della patta esplicito)

- **Data**: 2026-04-28
- **Stato**: Accepted (SPEC §8.1 aggiornato di conseguenza, CR-001 della REVIEW-fase-1)
- **Contesto**: SPEC §8.1 v2.0 prescriveva originariamente 4 valori per `GameStatus`: `ONGOING`, `WHITE_WINS`, `BLACK_WINS`, `DRAW`. Durante la Fase 1 è emerso che la UI (FR-RUL, FR-NET-09 replay viewer) e i log di partita necessitano di distinguere il *motivo* della patta — triplice ripetizione, regola delle 40 mosse, accordo. Aggiungere un campo separato `Optional<DrawReason>` su `GameState` produrrebbe doppia rappresentazione (status + reason) e logica di consistenza ridondante.
- **Decisione**: `GameStatus` ha 6 valori — `ONGOING`, `WHITE_WINS`, `BLACK_WINS`, `DRAW_REPETITION`, `DRAW_FORTY_MOVES`, `DRAW_AGREEMENT` — più tre helper boolean: `isOngoing()`, `isWin()`, `isDraw()`. SPEC §8.1 è stato aggiornato con commit `docs(spec): align §8.1 GameStatus to 6-value extension (CR-001)` per riflettere la decisione.
- **Conseguenze**:
  - La UI può fare `switch` esaustivo sull'enum per mostrare il motivo della patta senza accedere a un campo accessorio.
  - I log strutturati (NFR-O-02) hanno un campo `status` univoco e self-describing, niente parallelismo.
  - L'estensione è retro-compatibile: chi tratta solo "ongoing/win/draw" usa gli helper.
  - In Dama Italiana lo stallo è sconfitta, NON patta (§3.6): non c'è una voce `DRAW_STALEMATE`. La modellazione della sconfitta-per-stallo passa per `WHITE_WINS`/`BLACK_WINS` calcolati da `computeStatus`.
- **Alternative considerate**:
  - **Strict-SPEC originale (4 voci) + `Optional<DrawReason>` su `GameState`**: doppia rappresentazione, validazione manuale che il reason sia non-null sse status==DRAW.
  - **Strict-SPEC senza reason esposto**: motivo della patta perso → degrada UX e replay.

### ADR-024 — Architettura motore IA: sealed `AiEngine` + 3 livelli concreti

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 2)
- **Contesto**: SPEC §12.2 e ADR-015 fissano tre livelli di difficoltà fissi (Principiante, Esperto, Campione) con depth/timeout/caratteristiche distinti. Il motore deve essere tipato in modo esaustivo (la UI di Fase 3 fa `switch` sull'enum) e niente livello "in mezzo" deve essere creabile per errore.
- **Decisione**: `sealed interface AiEngine permits PrincipianteAi, EspertoAi, CampioneAi`. Tre classi `final` con costanti `DEPTH`/`MAX_DEPTH`, `DEFAULT_TIMEOUT` e `NOISE_PROBABILITY` (Principiante) `public static final` per consultazione esterna. Factory `AiEngine.forLevel(AiLevel, RandomGenerator)`. Tutti e tre delegano a `IterativeDeepeningSearch`: deviazione minore vs piano (Esperto userebbe `MinimaxSearch` puro), motivata dalla cooperative cancellation (deadline → IDS ritorna best-so-far invece di crashare).
- **Conseguenze**:
  - Pattern matching esaustivo nei consumer (UI, executor) senza branch default sospetti.
  - Aggiungere un livello richiede esplicitamente `permits` + classe — passaggio rituale che invita a documentare la scelta.
  - Configurazione (depth, timeout, noise) è auto-descrittiva sui tipi — niente magic numbers nel codice client.
- **Alternative considerate**:
  - Singolo `MinimaxAiEngine` + `record AiConfig`: più flessibile ma mappa male sul "3 livelli fissi" di SPEC; tooling/debug peggiore.
  - Strategy con `interface SearchPolicy`: livelli come tre policies preconfezionate. Aggiunge un livello d'indirezione rispetto al "tre classi finali" del SPEC senza beneficio nel breve.

### ADR-025 — Funzione di valutazione modulare a somma pesata

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 2)
- **Contesto**: SPEC §12.1 elenca cinque componenti euristici (materiale, mobilità, posizione/avanzamento, sicurezza-bordi, controllo-centro) con pesi indicativi. Vogliamo poter testare ogni termine in isolamento, sostituirne uno per esperimenti, e mantenere chiari i pesi.
- **Decisione**: `interface Evaluator { int evaluate(state, perspective); }` come API consumata dalla search. Un `interface EvaluationTerm` per ogni componente puro (un metodo `score`). `WeightedSumEvaluator(List<WeightedTerm>)` somma `weight·score` per ogni termine. `WeightedSumEvaluator.defaultEvaluator()` istanzia la composizione SPEC §12.1: materiale ×1, mobilità ×5, avanzamento ×2, sicurezza-bordi ×8, centro ×10.
- **Conseguenze**:
  - Ogni termine è testabile da solo (`MaterialTermTest`, `MobilityTermTest`, ecc.).
  - Tuning futuro = nuova istanza di `WeightedSumEvaluator` con pesi diversi; nessun cambio di codice nei termini.
  - Pesi documentati in un solo punto (`defaultEvaluator()`).
- **Alternative considerate**:
  - Pesi configurabili via `record EvaluationWeights` esposto: più flessibile ma sposta la mappa SPEC §12.1 dal codice ai chiamanti.
  - Solo materiale + mobilità in F2: troppo "cieco" per battere Principiante in modo solido.

### ADR-026 — Hashing Zobrist deterministico + Transposition Table always-replace

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 2)
- **Contesto**: La specifica del livello Campione richiede transposition table (SPEC §12.2). Tre sotto-decisioni: come generare i numeri Zobrist, dimensione e politica di sostituzione della TT, integrazione con la search.
- **Decisione**:
  - **Tabelle Zobrist generate da `SplittableRandom(0xDA4A172L)` — seed costante**. Stesso jar = stessi hash; tutti i test sulla TT sono bit-deterministici.
  - **`TranspositionTable` ad array circolare di `2^20 = 1 048 576` slot** (~32 MB a 32 byte/slot). Slot index = `hash & (size-1)`. Probe rifiuta entry con hash diverso (slot collision). Replacement: **always-replace** (la più semplice corretta — possibile evoluzione "prefer deeper/newer" se il profiling lo giustifica).
  - **Integrazione in `MinimaxSearch`**: probe all'ingresso del nodo; entry con `depth >= depthRemaining` produce ritorno anticipato con semantica `EXACT`/`LOWER_BOUND`/`UPPER_BOUND`. **Mai ritorno anticipato al root** (`plyFromRoot == 0`) — il chiamante richiede `bestMove` settato dal loop, non recuperato dalla TT. Al ritorno, store con bound type derivato da `(alphaOrig, beta, score)`.
  - La TT bestMove viene promossa al primo posto del move ordering (PV-from-TT) per innescare cutoff alpha-beta più presto.
- **Conseguenze**:
  - Determinismo end-to-end nei test (`AiTournamentSimulationTest` può asserire risultati esatti su seed dato).
  - 32 MB di RAM per Campione attivo — accettabile su hardware moderno.
  - Iterazioni successive di IDS riusano TT cross-iteration: depth k+1 raccoglie valori di depth k senza ricalcolarli.
- **Alternative considerate**:
  - Tabelle randomizzate ad ogni avvio (seed `nanoTime()`): rompe i test deterministici, niente vantaggio.
  - TT 2^18 = 8 MB: meno collisioni risparmiate, perdita perf 5-10% stimata.
  - Politica "prefer deeper": più complessa, beneficio marginale nei depth budget di Campione.
  - TT ricreata ad ogni `chooseMove`: niente memory leak ma perde benefici cross-call dell'IDS.

### ADR-027 — Modello di cancellazione cooperativa

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 2)
- **Contesto**: SPEC §12.2 vuole l'IA "cancellabile, con timeout, non blocca la UI". `Future#cancel(true)` interrompe il thread JDK ma se la search non controlla il flag interrupt esplicitamente, può proseguire fino al termine naturale; e l'API standard del `Future` perde il valore intermedio una volta cancellato.
- **Decisione**: Cooperative cancellation tramite `interface CancellationToken`. La search controlla `cancel.throwIfCancelled()` all'ingresso di ogni nodo (overhead trascurabile su `boolean volatile`). Tre factory standard:
  - `never()` — singleton mai cancellato.
  - `deadline(Instant)` / `deadline(Instant, Clock)` — cancellato a/oltre l'istante; il `Clock` iniettabile rende i test deterministici.
  - `composite(token1, token2, …)` — OR logico.
  - `MutableCancellationToken` — implementazione con `cancel()` esplicito, usata da `VirtualThreadAiExecutor`.
- L'`IterativeDeepeningSearch` cattura `SearchCancelledException` al boundary di ogni iterazione e ritorna il `bestMove` dell'iterazione completata più profonda (graceful). Se nemmeno la prima itera completa, fallback alla prima mossa legale (`chooseMove` non ritorna mai null su stati ongoing).
- **Conseguenze**:
  - Risposta a cancellazione entro ~200 ms (overhead di check di `volatile boolean` per nodo).
  - Soft-cancel via `Submission.cancelGracefully()` ritorna best-so-far; hard-cancel via `Future.cancel(true)` dà semantica JDK standard (`CancellationException`).
  - Test deterministici di cancellazione tramite `deadline(past)` e `MutableCancellationToken.cancel()`.
- **Alternative considerate**:
  - Solo `Thread.interrupted()`: API meno ricca (niente deadline, niente composite); dipendenza dall'interrupt flag del thread che alcune call JDK resettano.
  - `CompletableFuture.cancel`: overkill; SPEC esplicita "su virtual thread" → API sincrona del search basta.

### ADR-028 — Determinismo del rumore "Principiante"

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 2)
- **Contesto**: SPEC §12.2 dice "Probabilità 25% di scegliere mossa subottima (rumore)". Definizione di "subottima" lasciata aperta. Per testabilità il rumore deve essere riproducibile.
- **Decisione**: Con probabilità 25% (controllata da `RandomGenerator.nextDouble() < NOISE_PROBABILITY`), `PrincipianteAi` scarta la `bestMove` calcolata e sceglie **uniformemente a caso** fra le altre legali. Se `legalMoves.size() == 1` (cattura forzata, mossa unica), il rumore è bypassato. Il `RandomGenerator` è iniettato (`PrincipianteAi(RandomGenerator)` + `AiEngine.forLevel(level, rng)` factory). Default in produzione: `SplittableRandom(System.nanoTime())`. Nei test: seed fisso (`SplittableRandom(42L)` o derivato).
- **Conseguenze**:
  - Test gating `AiTournamentSimulationTest` deterministico — stesso seed produce stesso risultato bit-per-bit.
  - "Subottima" significa "una qualunque non-best", coerente con un giocatore principiante che gioca senza piano.
  - Aggiungere un livello "noisy" diverso significa una nuova classe — non si tunina il livello esistente runtime.
- **Alternative considerate**:
  - "Seconda migliore per score": più sottile ma non è ciò che dice SPEC; richiede ricerca duplicata o dell'intero `List<MoveScore>` invece del solo `bestMove`.
  - Epsilon-greedy sul punteggio: più "smooth" ma cambia il senso di "subottima".

### ADR-029 — Bootstrap JavaFX con Spring Boot DI non-web

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 3)
- **Contesto**: Il client deve avere DI completa (per riuso in mode "host LAN" che attiverà Jetty embedded in Fase 7) ma è una desktop app, non un server. JavaFX richiede che `Application.launch(...)` sia chiamato dal main thread; Spring Boot vuole il suo `ApplicationContext` come singleton. I due framework hanno tradizioni incompatibili sul ciclo di vita.
- **Decisione**: `ClientApplication` è il `@SpringBootApplication` main: avvia il container con `webEnvironment=NONE` (no Tomcat, no Jetty), poi delega a `Application.launch(JavaFxApp.class)`. `JavaFxApp` riceve l'`ApplicationContext` via singleton statico `JavaFxAppContextHolder` (settato da `ClientApplication` prima del launch). `JavaFxApp.start(Stage)` istanzia `SceneRouter` (recuperato dal context) e mostra la prima scena. Lo shutdown chiude il context Spring nel `stop()` JavaFX.
- **Conseguenze**:
  - L'ordine di bootstrap è fissato: Spring start → context holder set → JavaFX launch → first scene.
  - I bean `@Component`/`@Configuration` sono usabili dovunque, inclusi i FXML controller (vedi ADR-030).
  - Test `@SpringBootTest` con `webEnvironment=NONE` non avviano JavaFX; i test FXML usano `Platform.startup` esplicitamente con guard `Assumptions.assumeTrue(fxToolkitReady)`.
  - Una sessione "LAN host" (Fase 7) potrà cambiare il `webEnvironment` runtime profile-driven senza toccare l'entry point.
- **Alternative considerate**:
  - Spring senza Boot (`AnnotationConfigApplicationContext`): più leggero ma perde profili, `@ConfigurationProperties`, auto-configuration. Costo di config troppo alto.
  - JavaFX prima di Spring (Application come main, Spring lazy): rompe `@ConfigurationProperties` binding al boot e complica i test.

### ADR-030 — Architettura schermate: FXML + controller-factory Spring-aware

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 3)
- **Contesto**: Ogni schermata della UI è un FXML con un controller annotato `@FXML`. Senza intervento, `FXMLLoader` istanzia il controller via `Class.newInstance()` — niente DI, niente accesso ai bean (`I18n`, `SceneRouter`, services).
- **Decisione**: Ogni `FXMLLoader` configurato con `loader.setControllerFactory(applicationContext::getBean)`. I controller sono `@Component @Scope("prototype")`: prototype perché `FXMLLoader.load()` può essere chiamato più volte nella stessa sessione (es. modal save dialog) e ogni invocazione deve avere campi FXML freschi. Singleton sarebbe un bug: il secondo `load()` resetterebbe i `@FXML` del primo. `SceneRouter.show` carica FXML, applica tema + scaling globale, mostra. Splash è l'unica schermata `@Component` non-prototype perché unica e "first loaded".
- **Conseguenze**:
  - I controller possono avere ctor `(SceneRouter, I18n, ...services)` con parametri arbitrari, iniettati da Spring.
  - I test "context-resolution" verificano che il bean si risolva nel context (lezione `feedback_spring_ui_tests`: i ctor multi-arg di `@Component` possono ingannare i test unit ma fallire al runtime se Spring non sa quale scegliere).
  - I test "FXML smoke" verificano che `loader.load()` non lanci eccezioni — copertura della parte FXML che il context-resolution test non vede.
- **Alternative considerate**:
  - DI manuale con setter: scala male, errori di ordine difficili da diagnosticare.
  - Singleton scope con reinizializzazione manuale: più boilerplate, errori silenti.

### ADR-031 — Schema file salvataggi v1

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 3)
- **Contesto**: SPEC §11 specifica multi-slot save + autosave. Serve uno schema JSON forward-compatible: il client di domani deve riconoscere i file di oggi e rifiutare in modo controllato file di un domani che non capisce.
- **Decisione**: File JSON con campi top-level: `schemaVersion` (intero, attualmente `1`), `kind` (string discriminante; in F3 univoca `"SINGLE_PLAYER_GAME"` — vedi `SavedGame.KIND_SINGLE_PLAYER`; future fasi 6/7 introdurranno valori distinti per match LAN/Internet), `name` (string user-facing), `createdAt` / `updatedAt` (ISO-8601 UTC), `aiLevel` (`"PRINCIPIANTE"` | `"ESPERTO"` | `"CAMPIONE"`), `humanColor` (`"WHITE"` | `"BLACK"`), `currentState` (oggetto annidato: `whiteMen`, `whiteKings`, `blackMen`, `blackKings` — quattro liste FID disgiunte; `sideToMove`, `halfmoveClock`, `history` — lista di mosse FID-encoded). La distinzione **manuale ↔ autosave** non è codificata in `kind` ma nel **nome del file**: gli autosave usano lo slot riservato `_autosave.json` (escluso da `SaveService.listSlots`), i salvataggi manuali usano slot user-named. `SaveService.load` rifiuta `schemaVersion != 1` con `UnknownSchemaVersionException` (eccezione tipata, propagata fino a un toast localizzato).
- **Conseguenze**:
  - La rappresentazione `currentState` riusa `SerializedGameState` (4 liste FID), già scelto come canonical board format dal corpus regole (ADR-022). Coerenza: stesso formato in test + saves.
  - Lista `moves` esposta nella rappresentazione interna del salvataggio per future viste "replay"; oggi `SinglePlayerGame.fromSaved` non la consuma — `currentState` è bastante per riprendere, gli `RandomGenerator` sono ricostruiti freschi.
  - Una migrazione schema v2 (futuro) deve introdurre un loader specifico e fall-through controllato; il toast `[load|autosave].toast.error.schema.*` esiste già IT/EN.
- **Alternative considerate**:
  - Solo `moves` (replay-from-start) senza `currentState`: economico in spazio ma costa O(n) ad ogni load + non funziona per posizioni hand-built. Rifiutato.
  - Solo `currentState` senza `moves`: niente replay/storia visibile per future feature. La storia è già mantenuta dentro `currentState.history`, ma `moves` rimane come campo future-proof dedicato.
  - Binary format (Protobuf, Kryo): forward-compat più rigida, ma diff in code-review impossibili e debugging tedioso. JSON va bene per il volume di dati attesi.

### ADR-032 — Autosave atomico (write-temp + ATOMIC_MOVE)

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 3)
- **Contesto**: L'autosave si scrive ad ogni `applyMove` (umana e IA). Una scrittura interrotta a metà (kill, crash, batteria) lascerebbe `_autosave.json` corrotto, il che a riavvio si manifesta come "schema mismatch" o JSON parse error sul flusso più caldo del recovery — peggio dell'assenza dell'autosave.
- **Decisione**: `SaveService.save` (e per estensione `AutosaveService.writeAutosave`) scrive prima su `<target>.tmp` nella stessa directory, poi `Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)`. Su file system che non supportano `ATOMIC_MOVE` (FAT32, alcune share di rete) il fallback è `REPLACE_EXISTING` solo, con log WARN. Il file `.tmp` orfano (se la rename fallisce a metà) è ripulito al prossimo write.
- **Conseguenze**:
  - Nessun half-write visibile a un altro processo (es. il prossimo riavvio del client).
  - `SinglePlayerAutosaveTrigger` swallowa `UncheckedIOException` con log WARN: la mossa successiva ritenta naturalmente, no popup d'errore (failure-tolerant per FR-SP-08).
  - Il test `AutosaveE2ETest#writeFailureToleratedWhenSavesDirIsAFile` verifica che un `savesDir` impossibile da creare non rompa il flusso di gioco.
- **Alternative considerate**:
  - File lock: complica il polling cross-platform e aggiunge race tra processi diversi (improbabile per desktop single-user, ma overkill).
  - Fsync esplicito: ortogonale; `Files.move` con `ATOMIC_MOVE` è già la primitiva giusta.
  - Append-only log + replay: storage robusto ma file unbounded; gli autosave restano in posto fino a Termina/cancel.

### ADR-033 — Localizzazione: `MessageSource` Spring + `LocaleService` bridge

- **Data**: 2026-04-28
- **Stato**: Accepted (Fase 3)
- **Contesto**: SPEC §13.6 e NFR-U-01 richiedono IT/EN runtime-toggleable. L'utente cambia lingua in Settings → la UI riflette immediatamente. JavaFX `ResourceBundle` ha API ingombrante (FXML `%key` lookup) e niente fallback chain; Spring `MessageSource` è la primitiva di riuso.
- **Decisione**: `MessageSourceConfig` espone un `ReloadableResourceBundleMessageSource` su `classpath:i18n/messages` con encoding UTF-8 esplicito (default Windows = `windows-1252` mangia gli accenti) e `defaultLocale=ITALIAN` (fallback). `useCodeAsDefaultMessage=false` → chiavi mancanti lanciano `NoSuchMessageException`. `LocaleService` (`@Component`) è il bridge tra `UserPreferences.locale()` e `Locale.setDefault(...)`, ed espone `current()`. `I18n` (`@Component`) wrappa `MessageSource.getMessage(code, args, localeService.current())` e converte `NoSuchMessageException` in `[code]` (placeholder visibilmente sbagliato durante lo sviluppo, niente crash a runtime). I FXML controller iniettano `I18n` (no `MessageSource` né `LocaleService` direttamente). Convenzione chiavi: hierarchical lowercase dotted (`screen.element.role`, es. `menu.singleplayer.title`, `setup.button.confirm`).
- **Conseguenze**:
  - Cambio lingua in Fase 3 richiede riavvio (§7.10 opzione A): `Locale.setDefault` non re-trigger-a i `setText` già emessi; runtime dynamic toggle in Fase 11 con observable bindings.
  - `MessageSourceConfigTest#bothBundlesHaveSameKeySet` enforce parità IT↔EN: ogni nuova chiave aggiunta da un task deve apparire in entrambi i file properties altrimenti la build fallisce.
  - `LocalizationE2ETest` (Task 3.21) pinna la contract controller→bundle per 87 chiavi referenziate dai controller principali.
  - I diagrammi delle regole (ADR-022 schema posizioni) hanno caption keys `rules.diagram.<id>.caption` localizzati IT/EN, non strings hardcoded.
- **Alternative considerate**:
  - JavaFX `ResourceBundle` puro (`%key` in FXML): niente `MessageFormat` automatico, niente fallback chain elegante, niente integrazione DI. Rifiutato.
  - i18next-style JSON: dipendenza extra, niente vantaggio per IT/EN-only.
  - String catalog Java 21: ancora preview/incubator, troppo presto.

---

### ADR-034 — Visual rework "videogame premium wood" (Fase 3.5)

- **Data**: 2026-05-02
- **Stato**: Accepted (Fase 3.5)
- **Contesto**: SPEC §1.5 e SPEC §13.2 (v2.2, CR-F3.5-001) richiedono uno stile videogame classico ispirato all'app Play Store del cliente. Il design system di F3 (palette neutra, font Inter unico) era "moderno controllato" ma percepito come gestionale. Per la demo cliente Win 10/11 (SPEC §16 Fase 3.5) servono una pelle wood premium e una serie di affordance percettive (bevel, glow, gradient gold) coerenti su tutte le 8 schermate e sulla scacchiera/pezzi.
- **Decisione**: Riscrittura completa di `theme-light.css` con un nuovo vocabolario duale: token color `bg-primary/bg-surface/bg-elevated` (dark roast / deep walnut / cream parchment) accoppiati a `text-on-dark/text-on-light/text-secondary` per gestire le superfici miste (sfondo dark + card cream). Tipografia bi-classe: Playfair Display variabile per i display titles (`-font-family-display`, classi `.label-display{,-md,-lg}`), Inter variabile per UI/body. Entrambi i font binari bundled in `client/src/main/resources/fonts/` (SIL OFL 1.1, license text alongside). Token motion `-easing-out-back` per overshoot juicy. Componenti rifatti: `.button-primary` (gradient gold + bevel innershadow + dropshadow + hover glow), `.button-secondary` (wood-frame outline su `bg-surface` deep walnut). Texture wood (Poly Haven CC0) applicate via `BackgroundImage` ai 4 backdrop principali (`splash-root`, `main-menu-root`, `screen-root`, `board-cell-{light,dark}`). Pezzi ridisegnati come composizione `Group(Circle bg radial gradient + ring scanalato + gloss highlight + Text king-marker)` con DropShadow ombra. Il dark theme resta stub coordinato (token speculari) ma non attivato in F3.5: il toggle runtime e la verifica WCAG AA in dark mode restano F11.
- **Conseguenze**:
  - `ThemeService.applyTheme(Scene)` resta single source of truth: ogni Scene istanziata fuori da `SceneRouter` (es. `SaveDialogController` modal Stage) deve invocarlo esplicitamente, altrimenti gli stylesheet non vengono caricati. Finding F-001 di Task 3.5.14 reso visibile e fixato in Task 3.5.14a (commit `19e5227`) — `SaveDialogController` ora inietta `ThemeService` e chiama `applyTheme(scene)` dopo `new Scene(root)`.
  - Il bundle JaCoCo `client` cresce (~70 classi totali) ma le esclusioni F3 (Task 3.22) coprono ancora le classi pure-JavaFX/Spring bootstrap; il gate 60% line+branch resta verde.
  - Le esistenti FXML smoke test (Task 3.18 + 3.21) hanno retto al rework senza modifiche (Task 3.5.13: 0 fix necessari) perché scritte fin dall'inizio con lookup by `fx:id`/styleClass robusto.
  - Eventuale dark mode richiederà sintonia separata su contrasti su entrambi i palette (oggi solo light verificato manualmente).
- **Alternative considerate**:
  - Mantenere il design system F3 e fare solo "skinning" via overlay CSS: rifiutato perché l'effetto premium richiede la riscrittura di token/tipografia/ombre, non solo copertura.
  - JavaFX CSS `theme="Modena"` con override mirati: rifiutato per la stessa ragione e per perdere la consistenza cross-component.
  - Precaricare il dark theme in F3.5: rifiutato — il toggle runtime non è scope F3.5 (resta F11), e il dark con palette wood richiede una passata di contrast checking che dilata la fase oltre il budget demo.

---

### ADR-035 — Architettura `AudioService` (single MediaPlayer music + per-cue SFX cache)

- **Data**: 2026-05-02
- **Stato**: Accepted (Fase 3.5)
- **Contesto**: SPEC §13.4 (v2.2, CR-F3.5-003) richiede ambient music orchestrale (3-5 tracce, random shuffle, default 30%, mutabile) + 6 SFX gameplay (MOVE, CAPTURE, PROMOTION, ILLEGAL, VICTORY, DEFEAT). I controller di gameplay (`SinglePlayerController`) e di setup (`SettingsController`) devono dispatchare audio senza dipendenze sul toolkit JavaFX (testabilità) e con persistenza allineata a `UserPreferences`.
- **Decisione**: Dualismo interfaccia / impl. **`AudioService`** (`@Component` interface in `client/audio`) espone solo l'API testabile: `playMusicShuffle()`, `stopMusic()`, `playSfx(Sfx)`, `setVolume(AudioBus, int 0-100)`, `volume(AudioBus)`, `setMuted(AudioBus, boolean)`, `isMuted(AudioBus)`. **`JavaFxAudioService`** è l'unica impl prod. Music bus = single `MediaPlayer` driven da `MusicPlaylist` (deck shuffle deterministico con `RandomGenerator` injectable, anti back-to-back across reshuffle); `setOnEndOfMedia` dispone il player e ne crea uno nuovo per il prossimo brano. SFX bus = `EnumMap<Sfx, MediaPlayer>` cache lazy: ogni cue è un `MediaPlayer` riusato via `seek(Duration.ZERO) + play()` (one-shot pattern). Threading: helper `runOnFxThread()` con `try/catch IllegalStateException` per resilienza unit-test (toolkit non avviato → run inline). Persistence: ogni `setVolume`/`setMuted` invoca `persistAudioPreferences()` (`load + with* + save`); failure swallowed con WARN log per non bloccare il game loop. Mute semantics: `setMuted(MUSIC, true)` dispone il player corrente; `false` ri-avvia se la music era richiesta. `@PreDestroy shutdown()` dispone tutti i player. **JaCoCo** esclude `JavaFxAudioService.class` dal bundle (stesso pattern di `JavaFxUserPromptService`): l'interface `AudioService` è la testable abstraction, mockabile dai controller test.
- **Conseguenze**:
  - JavaFX Media su Windows non decodifica OGG Vorbis (verifica empirica 2026-05-01: `MediaException — Unrecognized file signature!`). Pipeline OGG→WAV pure-Java con `OggToWavConverter` (build-tool, scope `test`, basato su JOrbis low-level — la rotta `javax.sound.sampled` SPI ha un bug con clip <400ms). Master `.ogg` preservati in `assets/audio/sfx-master/`; runtime carica `.wav` PCM 16-bit signed LE.
  - Schema `UserPreferences` bumpato a v2 con 4 campi audio (`musicVolumePercent`/`sfxVolumePercent`/`musicMuted`/`sfxMuted`) + migrazione v1→v2 trasparente (`@JsonCreator` riempie i defaults SPEC).
  - Music bus tollera l'assenza dei binari (warn log "Music bus has no playable tracks") — utile in CI/test environments senza i WAV/MP3 (~64MB) a runtime classpath; il prod jar/MSI li include sempre.
  - `SettingsController` ctor cresce a 6 arg (sceneRouter, i18n, prefs, prompt, scaling, audio); `SinglePlayerController` ctor a 4 arg (... + audio). Test: 6 ctor call sites aggiornati.
- **Alternative considerate**:
  - `javax.sound.sampled` puro: rifiutato per la music (no OGG/MP3 nativo, serve SPI esterno). Mantenuto in `OggToWavConverter` build-time.
  - JavaZoom JLayer per MP3: aggiunge dipendenza runtime; JavaFX Media legge MP3 nativo, basta convertire OGG.
  - Multi-source mixer (più SFX simultanei): SFX di gameplay sono one-shot e non si sovrappongono nello stesso evento; cache singola per cue è sufficiente.
  - Dispatch SFX via Observable pattern (event bus): rifiutato — overhead di subscribe/unsubscribe, complica testing rispetto a injection diretta.

---

### ADR-036 — Packaging Windows MSI via `jpackage` + `org.panteleyev:jpackage-maven-plugin`

- **Data**: 2026-05-02
- **Stato**: Accepted (Fase 3.5)
- **Contesto**: SPEC §16 Fase 3.5 (v2.2, CR-F3.5-004) richiede un installer Windows demo-ready: doppio-click su `.msi` → installazione senza prerequisiti Java → shortcut Start menu → app lanciata. Pull-forward parziale di Fase 11 (lì restano `.dmg` Mac + `.deb` Linux). Il bundle JRE è obbligatorio per evitare attriti su macchine cliente.
- **Decisione**: Pipeline Maven a 3 step opt-in via profilo `installer` (assente dal default per non rallentare il fast loop). Step 1: `maven-dependency-plugin@jpackage-stage-runtime-deps` copia 89 jar runtime in `target/jpackage-input/`. Step 2: `maven-resources-plugin@jpackage-stage-self-jar` aggiunge `client-0.1.0-SNAPSHOT.jar` come `--main-jar`. Step 3: `org.panteleyev:jpackage-maven-plugin:1.6.5` invoca `jpackage --type MSI` con `--name "Dama Italiana"`, `--app-version 0.3.5`, `--vendor "Dama Italiana"`, `--main-class com.damaitaliana.client.ClientApplication`, `--icon assets/icons/app-icon.ico`, `--win-shortcut --win-menu --win-menu-group "Dama Italiana" --win-dir-chooser`, `--win-upgrade-uuid 9d8c4a02-3f1b-4f7e-9c2a-5a6e1b3c8d92` (UUID stabile per upgrade in-place automatico tra build). Output: `client/target/jpackage/Dama Italiana-0.3.5.msi` ~152 MB col JRE bundled. Prerequisiti macchina build: JDK 21 + WiX Toolset 3.x (3.11+) sul PATH (`candle.exe`, `light.exe`); WiX 4.x non supportato da `jpackage` di JDK 21. Fallback portable: `-Djpackage.type=APP_IMAGE` produce una cartella eseguibile senza WiX. **Icona**: `AppIconGenerator` (Java standalone in `client/src/test/java/buildtools/`, scope test fuori dal jar prod, stesso pattern di `OggToWavConverter`) disegna disco wood-themed multi-size (16/32/48/64/128/256 px) e scrive `app-icon.ico` come PNG-in-ICO (formato supportato da Vista+, copre Win 10/11).
- **Conseguenze**:
  - L'MSI è bundled JRE (~80 MB) + asset (~70 MB texture+audio); non richiede Java sul target. Cliente apre installer e gioca, no friction.
  - `--runtime-image` jlink non utilizzato in F3.5 (Spring Boot non-modular richiederebbe `--ignore-missing-deps`); deferred a follow-up come ottimizzazione di tagliare ulteriormente la JRE bundled (~80 → ~60 MB stimato). Documentato nel changelog Task 3.5.12.
  - Test in CI/headless skippano la fase `jpackage` perché il profilo è opt-in (`-Pinstaller`). Build manuale dello sviluppatore + Task 3.5.14 manual demo coprono la verifica end-to-end.
  - WiX Toolset deve restare sul PATH della macchina build: documentato nel README sezione "Build installer Windows (Fase 3.5+)". Workaround inline `PATH=...` se WiX è installato ma non sul PATH globale.
- **Alternative considerate**:
  - `launch4j` + JRE separato: pre-jpackage, non genera MSI nativo; rifiutato.
  - `jlink` + `jpackage --runtime-image` per JRE custom modular: bloccato da Spring Boot non-modular (CLASSPATH); deferred.
  - `install4j`/`Advanced Installer`: commerciali, fuori scope budget.
  - `--type APP_IMAGE` come default: niente shortcut Start menu, esperienza non-installer; mantenuto come fallback documentato (no WiX).
  - WiX 4.x: non supportato da `jpackage` JDK 21 (limit upstream).

---

### ADR-037 — Asset licensing strategy: CC0 visual+audio, OFL fonts, audit `CREDITS.md`

- **Data**: 2026-05-02
- **Stato**: Accepted (Fase 3.5)
- **Contesto**: SPEC §13.2.3 (v2.2, CR-F3.5-001) e PLAN-fase-3.5 §3.2 richiedono che tutti gli asset visivi e audio siano CC0 o CC-BY (preferenza CC0) con audit trail completo. La pelle "videogame premium wood" introduce ~10 binary asset (3 texture + 4 music + 6 SFX-master + 6 SFX-wav + 2 fonts + 1 icona generata). Necessario un policy uniforme + un inventario cito-citabile per evitare sorprese di compliance.
- **Decisione**: Doppia regola di licensing dichiarata in `CREDITS.md` (root del repo) come `license-policy` frontmatter: **CC0 1.0 Universal per visual+audio** (preferenza assoluta, fallback CC-BY 4.0 con attribution); **SIL OFL 1.1 per i font binari** (license text redistribuita alongside il `.ttf`, unico obbligo OFL). `CREDITS.md` mantiene un inventario tabellato per ogni asset: local file path, source page URL, direct file URL, license, author, durata o size on disk. Audit trail della verifica licenza al momento dell'acquisizione (data + nota). Esclusioni documentate in dettaglio (Patreon-locked, 404, sito chiuso, mood non aderente, custom non-CC0). Asset generati programmaticamente (es. `app-icon.ico` da `AppIconGenerator`) sono opera del progetto (autore "Dama Italiana", licensing implicito = repo license). Task 3.5.1 ha consolidato questo pattern; tutti i task successivi (3.5.2 Inter+Playfair, 3.5.4 music binari, 3.5.4 follow-up SFX WAV) hanno appiccato voce all'inventario nello stesso commit dell'asset.
- **Conseguenze**:
  - Aggiungere asset al repo richiede sempre l'aggiornamento di `CREDITS.md` nello stesso commit (regola cresciuta de facto in F3.5; possibile spec-change request F11 per istituirla nei prossimi cicli).
  - LGPL build-time tooling (`jorbis`, `vorbisspi` per `OggToWavConverter`) tracciato come **eccezione SPEC §6** in AI_CONTEXT.md / Task 3.5.4 follow-up: scope test, NON shippato nel jar/MSI prod, quindi nessun obbligo LGPL di redistribuzione.
  - Le esclusioni mantenute esplicite (CC-BY require attribution stringente, custom Pixabay/Mixkit non OK) impediscono drift futuro su asset comodi-ma-ambigui.
  - Eventuali asset CC-BY introdotti in futuro richiederanno una sotto-sezione "Attribution" in `CREDITS.md` con il testo di credito esatto come da licenza.
- **Alternative considerate**:
  - Solo CC0 senza fallback CC-BY: rifiutato — la lista di sorgenti CC0 di qualità per orchestral music è stretta (OpenGameArt non abbondante), CC-BY accettabile con disciplina.
  - License `package.json`-style (file JSON parsabile): overkill per questa scala, markdown leggibile vince.
  - SPDX expressions in headers `package-info.java`: non si applica agli asset binari, e la duplicazione con `CREDITS.md` introduce drift.

---

### ADR-038 — `MatchEventBus` su Spring `ApplicationEventPublisher`

- **Data**: 2026-05-05
- **Stato**: Accepted (Fase 4)
- **Contesto**: Fase 4 (PLAN §4.5) richiede un bus interno per propagare gli eventi `MatchEvent` (sealed: `MoveApplied` / `MoveRejected` / `DrawOffered` / `DrawAccepted` / `DrawDeclined` / `Resigned` / `MatchEnded` / `PlayerDisconnected` / `PlayerReconnected`) ai listener in-process — replay JPA-side in F6, broadcast STOMP nello stesso processo, audit/metriche future. Il bus DEVE essere transport-agnostic (CLAUDE.md §8.7-8.8: niente Spring Boot Web, niente Tomcat/Jetty in `core-server`); la cross-JVM distribution viaggia separatamente sullo `StompCompatiblePublisher` (vedi ADR-039), non sul bus interno.
- **Decisione**: il bus è una thin facade Spring (`SpringMatchEventBus @Component implements MatchEventBus`) sopra `ApplicationEventPublisher` (auto-injected — la `ApplicationContext` stessa lo implementa). Ogni `publish(MatchEvent)` wrappa l'evento in un `MatchEventEnvelope extends ApplicationEvent` (`source = bus`, `payload = MatchEvent`) e delega a `publisher.publishEvent(envelope)`. Listener registrano `@EventListener void on(MatchEventEnvelope env)` e fanno pattern-match esaustivo su `env.payload()` — il sealed `MatchEvent` garantisce esaustività compile-time.
- **Conseguenze**:
  - Ordine FIFO per publisher thread: il default Spring multicaster è sincrono (`publish` ritorna solo dopo che tutti i listener hanno processato); per-match FIFO segue per costruzione perché `MatchManager` serializza le scritture per match con `synchronized (lockFor(matchId))`.
  - `MatchEventEnvelope` ha `serialVersionUID = 1L` + `transient MatchEvent payload`: il `transient` documenta l'intent (l'envelope NON viaggia oltre il bus in-process; il wire layer F6/F7 serializza il payload via Jackson, non l'envelope) e silenzia SpotBugs `SE_BAD_FIELD`.
  - Apre la strada a `@TransactionalEventListener` in F6 quando JPA arriverà (Spring lo supporta out-of-the-box).
  - Zero deps nuove: Spring `spring-context` era già in `core-server` per la DI (SPEC §6 line 76).
- **Alternative considerate**:
  - LMAX Disruptor / RxJava `Subject`: overkill per workload single-LAN-host single-server di F4-F11; aggiunge una dep significativa.
  - Listener pattern manuale (`List<MatchEventListener>` + iteration in `publish`): re-inventa Spring; perde gli hook transazionali futuri.
  - Bus separato per match (`Map<MatchId, EventBus>`): premature partitioning — Spring multicaster è già abbastanza veloce per un singolo JVM e i test FIFO per-match passano filtrando per `matchId` lato listener.

---

### ADR-039 — `StompCompatiblePublisher` come port lato `core-server`, impl reale lato transport

- **Data**: 2026-05-05
- **Stato**: Accepted (Fase 4)
- **Contesto**: Fase 4 deve broadcastare gli eventi `MatchEvent` su un canale topic-based (SPEC §11.4 prescrive `/topic/match/{id}`) ma `core-server` NON DEVE conoscere WebSocket / Tomcat / Jetty (CLAUDE.md §8.8). Il trasporto reale arriva in F6 (server, Spring Boot Web + Tomcat embedded) e F7 (client LAN host, Spring Boot Web + Jetty embedded). Serve un'astrazione che permetta a `MatchManager` di "broadcastare" senza sapere cos'è il trasporto.
- **Decisione**: definito `StompCompatiblePublisher` interface single-method `void publishToTopic(String topic, Object payload)` in `com.damaitaliana.core.stomp` come **port**. Default impl `LoggingStompPublisher @Component` (SLF4J `LOGGER.info("[stomp] {} <- {}", topic, payload)`) per dev/test/standalone JVM senza trasporto. F6 e F7 introdurranno un `WebSocketStompPublisher @Component @Primary` (delegando a Spring `SimpMessagingTemplate`) che vincerà l'autowire quando entrambi i bean coesistono. Test scope: `BufferingStompPublisher` (in `core-server/src/test/java`, NON shippato) per asserzioni di test.
- **Conseguenze**:
  - `core-server` rispetta CLAUDE.md §8.8 — zero dipendenze da `org.springframework.web.socket..` o `org.springframework.boot.web..`. Enforcement a build-time via ArchUnit (ADR-042 / Task 4.12 rule 2 e 3).
  - Il contratto specifica `NPE` su null args (fail-fast nei test) ma le impl reali F6/F7 NON DEVONO throw su transport failure: il broadcast è best-effort, l'autoritativo è già persistito via `MatchRepository.appendEvent`.
  - SLF4J authorized esplicitamente da SPEC §6 line 330 (`Logging | SLF4J + Logback`); aggiunto `slf4j-api` come dep prod a `core-server/pom.xml` in Task 4.6 — non "nuova dep outside SPEC §6", è enabling di una scelta già approvata.
  - PLAN-fase-4 §4.6 originalmente suggeriva `@ConditionalOnMissingBean` su `LoggingStompPublisher` — annotation in `spring-boot-autoconfigure`, viola §8.7-8.8. Soluzione adottata: plain `@Component`, F6/F7 marcheranno il loro publisher `@Primary`.
- **Alternative considerate**:
  - Far conoscere a `core-server` il `SimpMessagingTemplate` direttamente: viola §8.8.
  - Usare il bus interno (ADR-038) anche per il broadcast cross-process: non distribuisce; richiederebbe un secondo listener "ponte" in F6/F7 — più giri di indirezione senza vantaggi rispetto al port.
  - Niente STOMP, usare bare WebSocket: SPEC §11.4 prescrive STOMP per il routing topic-based; cambiare sarebbe una SPEC change request grossa.

---

### ADR-040 — Anti-cheat counter location: in-memory transient su `Match`, threshold 5

- **Data**: 2026-05-05
- **Stato**: Accepted (Fase 4)
- **Contesto**: SPEC §9.8.3 + FR-COM-01: 5 mosse illegali consecutive da uno stesso giocatore → forfait automatico. La SPEC NON specifica dove vive il counter né cosa succede al recovery (riconnessione F6+).
- **Decisione**: counter `EnumMap<Color, Integer> consecutiveIllegalMoves` su `Match` (Task 4.7), inizializzato `{WHITE: 0, BLACK: 0}` nel ctor. Read accessor pubblico `int consecutiveIllegalMoves(Color)`; mutator package-private `recordIllegalMove(Color)` (`merge(who, 1, Integer::sum)`) e `clearIllegalMoves(Color)` (`put(who, 0)`). Il counter è **in-memory transient** — non serializzato, non parte del wire shape `MatchEvent`, non persistito. `MatchManager.applyMove` lo bumpa in caso di `MoveRejected` con reason `NOT_YOUR_TURN` o `ILLEGAL_MOVE`, e lo resetta su `MoveApplied`. La constant `ILLEGAL_MOVES_FORFEIT_THRESHOLD = 5` è package-private in `MatchManager`. Quando il counter raggiunge 5, `MatchManager` emette `MatchEnded(opposite winner, FORFEIT_ANTI_CHEAT)` e `match.status(FINISHED)`. **`MATCH_NOT_ONGOING` rejection NON triggera anti-cheat** — un client stale che retry su un match terminato non è "cheating" nello stesso senso.
- **Conseguenze**:
  - **Recovery (F6+)**: alla riconnessione il counter parte da 0 (scelta benevola). Un giocatore che cheata, disconnette pre-5ª illegale, riconnette riparte fresco. Documentato come trade-off accettabile: lo SPEC dice "5 consecutive" e una ri-sessione legittimamente "rompe" la consecutività.
  - **Persistere nel DB?** Rifiutato (vedi alternative). Se F6 deciderà diversamente, sarà cambio di contratto tracciato in REVIEW.
  - **Per-player indipendenza**: il counter è per-color, non per-match-shared. WHITE può accumulare 4 illegali e BLACK 4 illegali in parallelo senza che il match termini — solo la 5ª della stessa parte triggera forfait. Coperto da `AntiCheatTest#eachPlayerHasItsOwnCounterAndOnlyTheOffenderForfeits` (Task 4.9).
- **Alternative considerate**:
  - Counter persistito su `matches.consecutive_illegal_moves_white/black` (F6 schema): porta lo stato attraverso le riconnessioni ma è policy più severa di quanto SPEC §9.8.3 richieda; introduce migration F6 senza guadagno concreto.
  - Counter shared `int illegalMovesThisMatch` (non per-color): viola la semantica "stesso giocatore"; due giocatori dispettosi cumulerebbero senza distinzione.
  - Threshold configurabile da SPEC: l'utente l'ha fissata a 5 nello SPEC §9.8.3, non motivo di parametrizzarlo in F4.

---

### ADR-041 — In-memory repository concurrency strategy

- **Data**: 2026-05-05
- **Stato**: Accepted (Fase 4)
- **Contesto**: F4 richiede 3 adapter in-memory (`InMemoryMatchRepository`, `InMemoryTournamentRepository`, `InMemoryUserRepository`) come stand-in dei port persistenti che arriveranno in F6 (JPA/MySQL). Il workload F4 è single-threaded LAN-host single-process, ma i test stress (`InMemoryRepositoryConcurrencyTest @Tag("slow")` previsto in REVIEW Fase 4) e il riuso F7 LAN host con multi-client connessi richiedono safety sotto contesa. La validazione di `MatchRepository.appendEvent` è **strict monotonic** (FR-COM-04, SPEC §7.5: `event.sequenceNo() == currentSeq + 1`); questa check + l'append DEVONO essere atomici per non perdere update.
- **Decisione**:
  - Snapshot store: `ConcurrentHashMap<Id, Aggregate>` per ognuno dei 3 adapter (Match / Tournament / User).
  - Event log: `ConcurrentHashMap<MatchId, List<MatchEvent>>` con `Collections.synchronizedList(new ArrayList<>())` per match. La list è acquisita con `computeIfAbsent`; **validation + append eseguiti sotto `synchronized (log) { ... }` block esplicito** così che strict monotonic check e append siano atomici sotto contesa.
  - `currentSequenceNo`: ritorna `-1L` se match unknown o log vuoto, `(long) log.size() - 1L` altrimenti (sotto lock).
  - `eventsSince(matchId, fromSeq)`: ritorna `List.of()` per match unknown, altrimenti sotto lock costruisce ArrayList col suffix dove `e.sequenceNo() > fromSeq` e ritorna `List.copyOf(suffix)` (immutabile).
  - `MatchManager` aggiunge un secondo livello di lock per match (`ConcurrentHashMap<MatchId, Object> matchLocks`) attorno a ogni write op (`applyMove` / `resign` / `offerDraw` / `respondDraw`) per garantire che la check FR-COM-04 non fallisca anche sotto burst contesa concettualmente differente da quella interna del repo.
- **Conseguenze**:
  - In-memory adapter è "production-ready per LAN host" entro F4-F11; F6 introdurrà `JpaMatchRepository` con `@Lock(LockModeType.PESSIMISTIC_WRITE)` + transactional boundary che soppianterà questo strato per il server Internet ma riuserà il `MatchRepositoryContractTest` abstract scritto in F4 Task 4.3.
  - Lock pessimistico più forte (es. fairness, ordered queue) NON necessario: il test stress single-LAN-host con 16 thread assicurerà che `synchronized` basti.
  - Read op (`findById`, `findByStatus`, `findByPlayer`) sono lock-free, ritornano `List.copyOf(snapshots.values().filter(...))` per immutability — accettato il piccolo race in cui un read concomitante a un write può vedere lo stato pre-write o post-write (mai uno stato corrotto).
- **Alternative considerate**:
  - `AtomicLong` per il sequence number per match: aggiungerebbe un'altra struttura sincronizzata; `log.size()` sotto lock è già autoritativo.
  - `ReentrantReadWriteLock`: read non sono frequenti abbastanza per giustificare la complessità; il lock di scrittura intrinseco di `synchronized` basta.
  - Lock-free queue (`ConcurrentLinkedDeque`): non serializza la check+append atomic.

---

### ADR-042 — `TournamentMatchRef` package placement: Option F (match-side, UUID raw)

- **Data**: 2026-05-06
- **Stato**: Accepted (Fase 4)
- **Contesto**: Task 4.12 introduce ArchUnit con la rule "match non dipende da tournament" (PLAN-fase-4 §4.12). Task 4.3 aveva intenzionalmente collocato `TournamentMatchRef` nel package `com.damaitaliana.core.tournament` con campo `TournamentId tournamentId`, generando una dep transitiva `match → tournament` via il field di `Match.tournamentRef`. La rule 5 fallirebbe alla prima esecuzione; documentato come "reviewable in REVIEW Fase 4 / Task 4.12" già in Task 4.3. La SPEC §8.3 elenca tutti i tipi side-by-side senza prescrivere il package layout.
- **Decisione**: **Option F**. (a) `TournamentMatchRef` viene spostato da `com.damaitaliana.core.tournament` a `com.damaitaliana.core.match` — concettualmente è metadato match-side (back-pointer di un Match al torneo che lo ha schedulato; SPEC §8.3 colloca `Optional<TournamentMatchRef>` su Match). (b) Il campo `TournamentId tournamentId` viene cambiato in `UUID tournamentId` (raw) — pattern DDD ID-only per cross-aggregate references. Chi vuole il TournamentId tipato lo costruisce al lookup site: `new TournamentId(ref.tournamentId())`. La rule ArchUnit "match non dipende da tournament" ora passa pulita.
- **Conseguenze**:
  - **Layering pulito a 2 livelli enforced**: `match` indipendente da `tournament`; `tournament → match` ammessa (per `UserRef` + `TimeControl` — i value types vivono in match perché Match è il primo aggregate ad usarli; SPEC §8.3 implicito).
  - **Refactor minimale (5 file)**: DELETE `tournament/TournamentMatchRef.java`, CREATE `match/TournamentMatchRef.java` con `UUID`, EDIT `Match.java` (rimosso import same-package), EDIT `Tournament.java` (Javadoc), MIGRATE `TournamentMatchRefValidation` 4 test da `TournamentValueTypesTest` a `MatchValueTypesTest` con `UUID.randomUUID()` invece di `TournamentId.random()`.
  - **Trade-off accettati**: 1 linea extra al lookup site `new TournamentId(ref.tournamentId())` (0 occorrenze in F4, sarà F8+); 4 occorrenze test `new TournamentMatchRef(tid.value(), 0, 0)` invece di `new TournamentMatchRef(tid, 0, 0)`.
  - **Wire serialization F6**: `UUID` round-trippa direttamente come stringa JSON, semplifica il `MatchEvent` Jackson Module senza custom converter per il wrapper TournamentId.
- **Alternative considerate**:
  - **Option A** (move TournamentMatchRef in match, mantenere campo TournamentId): NON risolve — TournamentId resta in tournament, dep transitiva persiste. Errore originale di analisi.
  - **Option B'** (estrarre UserRef + TimeControl + TournamentId + TournamentMatchRef in nuovo package `core.common`): ~50 file churn (UserRef in 38 file, TimeControl in 20). Architetturalmente più pulito (3 livelli completi) ma sproporzionato per F4. Resta sul tavolo per F8/F9 quando il tournament cresce di scope reale.
  - **Option C** (droppare la rule 5): le altre 4 rule (no JavaFX/Boot Web/Tomcat-Jetty/JPA) sono il vero valore del PLAN §4.12; rule 5 è "nice to have". Rifiutato perché la rule cattura un anti-pattern reale e Option F è low-cost.
  - **ArchUnit `freezing`** (baseline di violazioni esistenti): aggiunge complessità per un caso isolato.

### ADR-043 — Strategia layout responsive desktop (Fase 4.5)

- **Contesto**: post-demo F3.5, il client mostra clipping della parte bassa della scacchiera su PC del cliente (hardware ~2014-2015, Win 10, display verosimilmente 1366×768) e non si adatta al window resize. Il design F3.5 era stato calibrato implicitamente su ~1080p verticali. CR-F4.5-001 (SPEC v2.3) introduce NFR-U-05 + §13.7 per coprire il gamut desktop standard 1024×720 → 4K + DPI scaling Windows 100/125/150/200%.
- **Decisione**: layout responsive **basato esclusivamente su API JavaFX 21 standard**, nessun framework esterno. Stack:
  1. **`Stage` constraints**: `setMinWidth(1024)` + `setMinHeight(720)`. Initial size = 80% di `Screen.getPrimary().getVisualBounds()`, centrato. Stage state (width/height/x/y/maximized) persistito in `~/.dama-italiana/config.json` schema v3 (estensione di v2 F3.5 con migrazione trasparente).
  2. **`BoardView`**: refactor da layout fisso (presunto `GridPane` con cell size hardcoded) a `StackPane` root + `Pane` figlio centrato. Lato della scacchiera = `min(viewport.width − sidePanelMin, viewport.height − headerFooterReserved) − frameThickness`. Le 64 celle e i pezzi posizionati con coordinate scalate `cellSize = boardSide / 8`. Bind su `widthProperty`/`heightProperty` per re-layout automatico al resize.
  3. **`BorderPane`** strutturale per ogni schermata: center = contenuto principale (BoardView, card form, ecc.), right = side panel (ove applicabile, con auto-hide < 1024px). Padding/spacing via token CSS già esistenti.
  4. **Side panel adattivo**: `prefWidth = 320`, `minWidth = 240`, `maxWidth = 400`. Sotto viewport 1024px → auto-hide con toggle button overlay (nuovo controllo, posizionato nel header secondo design system F3.5).
  5. **Tipografia display fluida**: binding programmatico `JavaFxScalingHelper.bindFluidFontSize(label, baseSize, scaleFactor)` per i ~10 sites di titoli "display" (Playfair Display). UI body invariata (token statici).
  6. **Aspect ratio handling**: 16:9 / 16:10 default. Ultrawide 21:9 / 32:9: cluster centrato con `maxWidth ≈ viewport × 0.70` o `1920px`, margini decorativi wood texture ai lati (no board indefinitamente grande su super-ultrawide).
  7. **`BackgroundImage`** con `BackgroundRepeat.REPEAT` (non STRETCH) per tutte le texture wood. Asset audit ≥ 2048×2048 e ≤ 4096×4096 (sweet spot tra nitidezza 4K e load time su HDD vecchio).
  8. **DPI awareness**: nessun pixel hardcoded; logical size sempre. JavaFX 21 gestisce HiDPI implicitamente sotto Windows DPI scaling 100/125/150/200%.
- **Scelte alternative valutate e scartate**:
  - **MigLayout / FormsFX / ControlsFX**: framework esterni che semplificano constraint-based layout. Rifiutati perché aggiungono dipendenza non in SPEC §6 e il problema è risolvibile con JavaFX puro; coerente con il "no third-party UI framework" implicito di F3.5.
  - **`ScrollPane` di fallback su tutto**: shortcut che maschera il problema (la board comunque non sarebbe quadrata e il cliente vedrebbe scrollbar inattese). Antipattern UX desktop.
  - **CSS `clamp()` per font fluidi**: JavaFX CSS NON supporta `clamp()` nativo (è una limitation di JavaFX rispetto a CSS web). Necessario binding programmatico per i ~10 sites display.
  - **Stage state persistence schema v4 (separato)**: rifiutato — estensione di v2 a v3 con migrazione trasparente è meno invasivo; v2 F3.5 ha già pattern di migrazione consolidato (`PreferencesService`).
  - **Aspect ratio: lasciar crescere board indefinitamente su 32:9**: rifiutato — UX awkward, cluster centrato con margini decorativi è la convenzione PC games desktop.
- **Conseguenze**:
  - 8 schermate F3.5 (splash, main menu, sp setup, board, save dialog, load screen, settings, rules) ridisegnate **solo lato layout/binding/CSS responsive**. Token CSS v2, texture, font-family, animation params **invariati** (anti-pattern CLAUDE.md §8 #15).
  - `BoardView` refactor è il core del fix (Task 4.5.4 PLAN-fase-4.5). `MoveAnimator`/`ParticleEffects`/`AnimationOrchestrator` continuano a funzionare invariati perché operano su coordinate Cell-based, non pixel hardcoded.
  - Nuovo helper `JavaFxScalingHelper.bindFluidFontSize` in `client.layout` (sotto-package nuovo) — testabile via JUnit standard.
  - Test `ResponsivenessParametricTest @Tag("slow")` 7 combinazioni × 8 schermate = 56 assertion bundle in CI; manuale per ultrawide (no parametric automatico, market share residuo).
  - F11 / F4.6 (eventuale) potrà aggiungere "Performance mode" toggle se hardware molto lento (Intel HD 3000 era) mostra jank durante animazioni — fuori scope F4.5.
- **Stato post-implementazione F4.5 (Task 4.5.10, 2026-05-06)**:
  - Tasks chiusi: 4.5.1 → 4.5.10 (Task 4.5.6b skipped per Opzione B utente — aspect ratio handling ultrawide deferred a manual review post-fix; ultrawide screenshot Task 4.5.9 conferma board centrata, no defect critico).
  - **2 helper realmente introdotti** in `client.layout`: `JavaFxScalingHelper` (fluid typography) + `BoardFrameThicknessHelper` (proportional frame padding); `BoardSizingHelper.computeBoardSide(...)` ipotizzato in PLAN §5.1 NON realizzato — la centratura del board è risolta in `BoardRenderer.layoutChildren` direttamente con `cellSize = min(w, h) / 8` + `xOffset/yOffset = (w/h - 8 × cellSize) / 2` (Task 4.5.4).
  - **Side panel auto-hide < 1024 NON implementato**: `PrimaryStageInitializer` enforce `Stage.minWidth = 1024` quindi il viewport < 1024 è unreachable (CLAUDE.md "Don't add fallbacks for scenarios that can't happen"). Hook documentato nel commento FXML del side panel; se in futuro il min size cambia, l'auto-hide si aggiunge then.
  - **`BackgroundImage` REPEAT mode** applicato a 4 selettori CSS (`.splash-root`, `.main-menu-root`, `.screen-root`, `.board-frame`); cell selectors `.board-cell-light/dark` mantengono `100% 100% no-repeat` (corretto per cell-sized backgrounds). `BorderImage` con nine-slice non utilizzato — `frame.jpg` è texture seamless, non un border ornament image.
  - **DPI scaling 125/150/200%** verificata manualmente in TEST sotto-fase (Task 4.5.16) — single-JVM run automatico non può esercitare multipli scaling factors; il layout math è DPI-independent quindi la correttezza è garantita dal parametric test al 100% logical DPI.
  - **Coverage**: client da 70 → 74 classi (+`PrimaryStageInitializer`, +`StagePersistenceValidator`, +`StagePersistenceCoordinator`, +`JavaFxScalingHelper`, +`BoardFrameThicknessHelper`, post merge); JaCoCo gate ≥ 60% line+branch invariato e met. Test count fast loop 321 (F4 baseline) → 379 (+58: +4 PrimaryStageInitializer, +7 BoardRendererLayout, +7 SidePanelLayout, +5 ScreenLayout, +15 JavaFxScalingHelper, +9 StagePersistenceValidator + 1 PreferencesService v2→v3 migration, +10 BoardFrameThicknessHelper); slow tag aggiunge +56 ResponsivenessParametricTest.

### ADR-044 — Server bootstrap & profile strategy (Fase 5)

- **Contesto**: F5 introduce il modulo `server` Spring Boot. Quattro contesti d'uso distinti — sviluppo locale, fast loop test, integration test full-stack, deploy produzione — richiedono configurazioni di datasource, secret JWT, logging, e Flyway diverse. Storicamente il client (F0+) usa solo profilo `default` Spring; il server è il primo modulo con multi-profile non triviale. Decisione utente §9.4 PLAN-fase-5.md (2026-05-10): MySQL via Testcontainers per dev e it; nativo per prod; H2 per fast test loop.
- **Decisione**: 4 profili Spring espliciti con responsabilità chiara:
  1. **`dev`** (`mvn -pl server spring-boot:test-run`): datasource MySQL via Testcontainers Spring Boot dev mode (`TestServerApplication` `@TestConfiguration` con `@Bean @ServiceConnection MySQLContainer<>("mysql:8.0")`). JWT secret = default hardcoded `dev-only-secret-change-me-32bytes-min-padding-xxxxxx` con warning log al boot. Flyway on. Logging console plain. BCrypt cost 12.
  2. **`test`** (`mvn -pl server verify -DexcludedGroups=slow`, fast loop): datasource H2 in-memory in modalità MySQL-compat (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`) per supporto CHECK constraint. JWT secret hardcoded in `application-test.yml`. Flyway on (coerenza con prod). BCrypt cost 12. **No Docker, no MySQL nativo richiesti**.
  3. **`it`** (`@SpringBootTest @Tag("slow")`): estende `test` overridando datasource via `AbstractMysqlIntegrationTest` base class con Testcontainers MySQL programmatic.
  4. **`prod`** (deploy reale): datasource MySQL nativo installato sul server (no Docker). JWT secret + DB password da env var `JWT_SECRET`/`SPRING_DATASOURCE_PASSWORD`. **Fail-fast** se env var assenti (`JwtProperties.@PostConstruct`). `spring.flyway.clean-disabled=true`. Logback JSON via `logstash-logback-encoder`. BCrypt cost 12.
- **Scelte alternative valutate e scartate**:
  - **Single profile + env var defaults**: troppo magia, hard da debuggare quando un test fallisce per profilo sbagliato.
  - **`docker-compose.yml` per dev MySQL persistente**: rifiutato (decisione utente §9.4); riapriva ADR-018 (Docker Compose rimosso) senza beneficio significativo. Testcontainers + Spring Boot dev mode offre identica DX senza file infrastruttura nel repo.
  - **`dev` punta a MySQL nativo localhost (come prod)**: rifiutato — costringe a setup MySQL nativo separato; Testcontainers offre DB sempre clean-state e identico tra dev e it.
  - **Profile `dev` = profile `it`**: rifiutato — `it` ha lifecycle `@SpringBootTest` (un container per test class), `dev` ha runtime lifecycle (un container per tutto il run). Stesso meccanismo Testcontainers, scope diverso.
- **Conseguenze**:
  - Niente `docker-compose.yml` in repo. ADR-018 (Docker Compose rimosso 2026-04-26) **resta valido e rafforzato in F5+**.
  - Per ispezionare il DB dev in MySQL Workbench: porta MySQL container è random ad ogni boot (loggata al startup come `MySQL container started on port <N>`); l'utente aggiorna la connessione Workbench una volta per sessione di sviluppo.
  - `prod` deploy operatore deve fornire env var `JWT_SECRET` (≥ 32 byte) e credentials MySQL — fail-fast immediato se mancanti, niente startup silenzioso con secret leakable.
  - TLS 1.3 (NFR-S-03) gestito da reverse proxy (nginx/Traefik) davanti a Spring Boot — **fuori scope F5 codice**, soddisfatto via deploy doc.
  - Rotazione JWT secret ogni 90gg + grace 24h (SPEC §15.2): **fuori scope F5** (concern ops/deploy via env var rotation, non Spring config).
  - Test `MultiProfileBootTest` (Task 5.2) asserisce che ciascuno dei 4 profili carica un context Spring valido (eccetto verifica fail-fast `prod` senza env var).

---

### ADR-045 — JWT signing & secret strategy (Fase 5)

- **Contesto**: F5 emette token JWT al login (NFR-S-02: access 15min + refresh 7gg). Servono decisioni su algoritmo di firma, gestione secret per profilo (4 ambienti diversi — dev, test, it, prod), claims, persistenza refresh token. SPEC §15.2 vincola "secrets via env variable, mai in `application.yml` committato"; SPEC §15.1 lista "refresh rotation" come mitigazione account takeover.
- **Decisione**:
  1. **Algoritmo**: HS256 (HMAC-SHA256) con secret simmetrico ≥ 32 byte (= 256 bit, requisito minimo HS256). Niente RS256/EdDSA in v1 — single-instance senza JWK distribution endpoint, HMAC è semplice e sufficiente.
  2. **Claims access token** (TTL 15 min): `sub=String.valueOf(userId)`, `username`, `role`, `iat`, `exp`, `type="ACCESS"`. Niente PII oltre `username`.
  3. **Claims refresh token** (TTL 7gg): identici ma `type="REFRESH"`. Verifica `type` esplicita lato server impedisce di usare access come refresh o viceversa (mismatch → 401).
  4. **Persistenza refresh tokens**: tabella `refresh_tokens` con `token_hash VARCHAR(64) UNIQUE NOT NULL` (SHA-256 hex del raw JWT). **Mai il token in chiaro nel DB**. Lookup O(1) via index unique. Su rotazione: vecchio token marcato `revoked_at = now()`, nuovo token issued + persistito.
  5. **Strategia secret per profilo** (decisione utente §9.1 PLAN-fase-5.md, 2026-05-10):
     - `prod`: secret da env var `JWT_SECRET`. Server **rifiuta di partire** se env var assente (fail-fast in `JwtProperties.@PostConstruct` ispezionando `Environment.getActiveProfiles()`).
     - `dev`: se env var assente → fallback default `dev-only-secret-change-me-32bytes-min-padding-xxxxxx` + `log.warn("⚠️ Using DEV-ONLY JWT secret — NEVER use in production")` molto visibile al boot.
     - `test`/`it`: secret hardcoded in `application-test.yml` / `application-it.yml` (ambiente di test, env var non desiderata per riproducibilità).
  6. **Razionale conformità SPEC §15.2**: il default `dev-only-*` non è un "secret di produzione" — è marcato esplicitamente come dev, il warning runtime esplicita la cosa, e `prod` fail-fast garantisce impossibilità di usarlo accidentalmente in deploy reale. **Spirito di §15.2 ("nessun secret di produzione mai in repo committato") preservato**, niente CR a SPEC necessaria.
- **Scelte alternative valutate e scartate**:
  - **RS256 con keypair**: setup complesso (PEM files, gestione rotation), eccessivo per single-instance. Da considerare in V2 se si scala multi-istanza con JWK endpoint.
  - **JWE (encrypted) invece di JWS (signed)**: i claims attuali non sono sensibili (userId, username, role pubblico), no need per encryption.
  - **Refresh token NON persistito (stateless verify)**: non permetterebbe revoca server-side (logout, security incident response). NFR-S-02 implica lifetime 7gg + rotation = serve revoca → persistenza necessaria.
  - **Refresh token = opaque random string (non JWT)**: avrebbe senso (più sicuro, no parsing client-side); rifiutato in v1 per uniformità DX (client tratta access e refresh allo stesso modo, entrambi JWT bearer). Da rivalutare in V2.
  - **Fail-fast in `dev` se env var assente** (decisione 9.1 strada A): rifiutato per DX single-developer; warning runtime + impossibilità accidentale in `prod` sono mitigation sufficienti.
- **Conseguenze**:
  - `JwtService` API: `issueAccess(UserEntity) → String`, `issueRefresh(UserEntity) → String`, `parse(String) → JwtClaims` (throws `JwtVerificationException`).
  - `RefreshTokenService.verifyAndRotate(String) → {UserEntity, newAccess, newRefresh}` con transazione + lock pessimistico su `refresh_tokens.id` per evitare race su doppio refresh concorrente.
  - `JwtBootValidationTest`: simula profilo `prod` con env var `JWT_SECRET` assente, asserisce fail-fast all'avvio context.
  - SecurityFilterChain stateless (no HttpSession), `JwtAuthenticationFilter` legge `Authorization: Bearer <jwt>`, popola SecurityContext con `Authentication(userId, [Role])`.

---

### ADR-046 — Flyway baseline strategy & schema portability (Fase 5)

- **Contesto**: F5 introduce il primo schema MySQL versionato. SPEC §8.4 v2.4 mostra schema completo (users, matches, match_events, tournaments, ...) ma F5 implementa solo auth (`users` + `refresh_tokens`); F6 aggiungerà `matches`/`match_events`; F8 aggiungerà `tournaments` e relations. Servono decisioni su versioning Flyway, suddivisione tra fasi, portabilità tra MySQL (prod/it/dev) e H2 (test), e gestione `users.role` (post CR-F5-001).
- **Decisione**:
  1. **Versioning Flyway**: `Vn__descrizione.sql` numerazione monotonica. Mai modificare migration applicate (SHA checksum Flyway le proteggerebbe comunque, fail-fast su mismatch). Ogni schema change futuro = nuova migration.
  2. **Suddivisione tra fasi**:
     - **V1 (F5)**: `V1__init_users_and_refresh_tokens.sql` — solo auth core (`users`, `refresh_tokens`). Niente match/tournament (motivo: out of scope F5; preferire migration dedicata in F6 piuttosto che colonne stub o tabelle vuote).
     - **V2 (F6)**: `V2__add_matches_and_events.sql` — `matches` + `match_events` (FK su `users.id`).
     - **V3 (F8)**: `V3__add_tournaments.sql` — `tournaments` + `tournament_rooms` + `tournament_participants` + `tournament_matches` + FK `matches.tournament_match_id` (ALTER TABLE per aggiungere la colonna su V2).
  3. **Schema `users.role`**: `VARCHAR(32) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN'))` — decisione utente §9.3 strada α + **CR-F5-001** a SPEC §8.4 v2.4. NON `ENUM` MySQL puro. Razionale:
     - **Portabilità**: H2 (profilo `test`) gestisce ENUM in modo non standard; CHECK constraint è SQL standard supportato identico da entrambi (MySQL 8.0.16+ enforced, H2 nativo).
     - **Workbench-friendly**: il vincolo è applicato anche su scritture manuali in MySQL Workbench (l'utente userà Workbench attivamente sia in lettura sia scrittura su DB locale prod-equivalente), evitando inserimento di valori non validi (es. `'admin'` invece di `'SYSTEM_ADMIN'`).
     - **Lato Java**: `@Enumerated(EnumType.STRING)` su `Role { USER, TOURNAMENT_ADMIN, SYSTEM_ADMIN }`; JPA serializza solo i 3 valori validi, completando il vincolo end-to-end (DB CHECK + JPA enum).
  4. **`spring.flyway.clean-disabled=true` in `prod`**: previene `flyway:clean` distruttivo accidentale. In `dev`/`test`/`it` clean abilitato per reset rapido (Testcontainers comunque ricrea il container ad ogni boot, rendendo clean ridondante ma innocuo).
  5. **`hibernate.ddl-auto=validate`** in tutti i profili: Hibernate verifica corrispondenza entity ↔ schema reale ad ogni startup, fail-fast se discrepanza. **Mai `update`/`create-drop`** (bypasserebbero Flyway e nasconderebbero divergenze).
- **Scelte alternative valutate e scartate**:
  - **V1 con schema completo SPEC §8.4 (users + matches + tournaments tutto in una migration)**: rifiutato — prematuro (F5 non implementa match/tournament code), una V1 monolitica complica review per fase, e ALTER TABLE successivi su tabelle vuote non hanno value.
  - **`role` ENUM MySQL puro + Hibernate `ddl-auto=create-drop` su H2**: due strade di schema (Flyway prod, Hibernate test) con potenziali divergenze non scoperte. Rifiutato per fedeltà test/prod.
  - **`role` VARCHAR senza CHECK + vincolo solo applicativo**: prima proposta, rifiutata dopo decisione utente di usare MySQL Workbench attivamente in scrittura — il CHECK protegge anche le scritture manuali.
  - **Liquibase invece di Flyway**: SPEC §6.5 specifica Flyway. Niente decisione da rivedere.
- **Conseguenze**:
  - V1 schema applicato identico su MySQL (dev/it/prod) e H2 (test). Test verifica entrambi (Task 5.3): `FlywayMigrationIntegrationTest` H2 + `FlywayMigrationMysqlIntegrationTest` Testcontainers `@Tag("slow")`.
  - F6 PLAN dovrà aggiungere V2 senza toccare V1; F8 PLAN dovrà aggiungere V3 senza toccare V1/V2.
  - SPEC §8.4 v2.4 è la specifica logica del schema completo. Le V1/V2/V3 implementano questa spec incrementalmente. Eventuali divergenze (es. tipi colonne, FK ON DELETE behavior) devono essere CR a SPEC.
  - `enum Role` lato Java centralizzato in `server.entity` package per riuso futuro (auth + future endpoint admin).

---

### ADR-047 — Rate limiter implementation (Fase 5)

- **Contesto**: SPEC NFR-S-04 richiede rate limit `/auth/login` ≤ 5/min e `/auth/register` ≤ 3/ora **per IP**. SPEC §15.1 lista anche "lock account dopo 10 tentativi/ora" (out of scope F5, deferred F6 quando esistono utenti reali). F5 implementa solo il rate limit per IP. Spring Security non ha rate limiter built-in — serve implementazione propria o libreria.
- **Decisione**:
  1. **Backend**: Caffeine `Cache<String, AtomicInteger>` per IP (key = remote IP). **Già in pom** (SPEC §6.5 lista Caffeine come cache server-side), niente nuova dipendenza.
  2. **Sliding window via TTL Caffeine**: `expireAfterWrite(60, TimeUnit.SECONDS)` per login bucket, `expireAfterWrite(1, TimeUnit.HOURS)` per register bucket. Counter reset dopo TTL, accuratezza sufficiente per protezione brute force.
  3. **Filter MVC**: `RateLimitFilter` (`OncePerRequestFilter`) applicato selettivamente via path matching (solo `/v1/auth/login` e `/v1/auth/register`). Su superamento → 429 Too Many Requests + header `Retry-After: <seconds>` (RFC 6585).
  4. **IP detection**: header `X-Forwarded-For` first IP se presente (proxy-aware per deploy dietro reverse proxy nginx/Traefik), altrimenti `request.getRemoteAddr()`.
  5. **Per-instance only**: contatori in memoria, persi a restart, non condivisi tra istanze. **Accettabile per v1** (single-instance deploy).
- **Scelte alternative valutate e scartate**:
  - **Bucket4j library**: feature-rich (token bucket, async, distributed), ma overkill per v1 e aggiunge dipendenza non in SPEC §6. Caffeine + counter è semplice e sufficiente.
  - **Redis-backed rate limit (`bucket4j-redis` o `spring-cloud-gateway`)**: necessario solo quando si scala multi-istanza. Out of scope F5 (single-instance deploy).
  - **Spring Security throttling built-in**: non esiste; `concurrency-control` di SecurityContext non è la stessa cosa.
  - **Token bucket vs sliding window vs fixed window**: scelta sliding window via TTL Caffeine — implementazione più semplice. Token bucket richiederebbe stato più complesso (capacity + refill rate); fixed window ha "edge spike" classico (5 attempts a 11:59:59 + 5 a 12:00:01).
  - **Rate limit per username invece che per IP**: rifiutato per `/login` (un attaccante può iterare username diversi); per `/register` non applicabile (username non noto pre-registrazione). Per IP è la convenzione web standard.
- **Conseguenze**:
  - `RateLimitConfig` `@Configuration` con 2 `@Bean Cache<String, AtomicInteger>` (loginBucket, registerBucket) + `@Bean RateLimitProperties` (`@ConfigurationProperties("dama.rate-limit")` con TTL/limit injectable, override-able in test profile per ridurre TTL a 2s e accelerare test).
  - `RateLimitIntegrationTest` (`@SpringBootTest` H2): 6 login consecutivi rapidi → 6° è 429 con `Retry-After`; reset dopo TTL.
  - Account lockout (10 tentativi/ora per username, SPEC §15.1) + Redis distributed: **deferred F6** quando esistono utenti reali a rischio.
  - Falso positivo: utenti dietro CGNAT condividono IP — possibile rate limit accidentale su gruppo di utenti; accettabile in v1, da rivalutare se diventa un problema reale.

---

### ADR-048 — Test strategy & isolation (Fase 5)

- **Contesto**: F5 introduce auth REST + JPA + DB → necessario testing strategy stratificata. SPEC NFR-M-02 richiede coverage ≥ 60% su rete; PLAN F5 §5.1 alza a 70% per `server`. Servono decisioni su tipologie di test, profile usage, isolamento, e gestione Docker (Testcontainers sì o no per fast loop).
- **Decisione**: **3 layer test pyramid** + `@Tag("slow")` per scoping:
  1. **Unit** (~40%): JUnit 5 + Mockito + AssertJ. Logica pura: `JwtService`, `RateLimiter`, `UserMapper`, validators, exception mapping. **No Spring context, no DB**. Esecuzione < 1s per classe.
  2. **Slice** (~30%): Spring Boot test slice con H2 in-memory.
     - `@WebMvcTest(Controller.class)` per controller layer (mock service, focus su HTTP contract + validation + exception mapping).
     - `@DataJpaTest` per repository layer (real H2 + real Hibernate, focus su query + JPA mapping).
     - Profilo `test` attivo via `@ActiveProfiles("test")`.
  3. **Full integration H2** (~25%): `@SpringBootTest webEnvironment=RANDOM_PORT` con H2. E2E auth flow su context completo, **fast loop**.
  4. **Full integration MySQL** (~5%): `@SpringBootTest` + `@Tag("slow")` + `AbstractMysqlIntegrationTest` base class con `@Testcontainers` + `MySQLContainer<>("mysql:8.0")` + `@DynamicPropertySource` per inject datasource. Verifica: schema Flyway compatibile MySQL reale, CHECK constraint enforced, BCrypt cost 12 funziona in stack reale.
  5. **`@Tag("slow")`**: esclude i test Testcontainers MySQL dal fast loop `mvn -pl server verify -DexcludedGroups=slow` (no Docker richiesto). Inclusi nel closure regression `mvn clean verify` root.
  6. **BCrypt cost 12 ovunque** (decisione utente §9.2 PLAN-fase-5.md): nessun override in test per fedeltà a prod. Accettato +20s sulla suite finché < 60s totali.
  7. **TDD strategy**: SPEC §18.1 raccomanda test-first dove sensato. F5 segue test-first per layer pure (JwtService, RateLimiter, validators) e test-after pragmatico per layer infrastructure (controllers, exception handlers, filter) — più produttivo in early-stage server.
- **Scelte alternative valutate e scartate**:
  - **H2 only per tutti i test (no Testcontainers)**: rifiutato — discrepanze H2/MySQL (CHECK constraint enforcement detail, JSON column type future F6, charset/collation default) non scoperte fino a deploy.
  - **Testcontainers per tutti i test (no H2)**: rifiutato — fast loop richiede Docker, blocca fast iteration quando Docker non disponibile o lento; H2 in-memory è 10× più veloce per slice test ripetitivi.
  - **Database test dedicato sul MySQL nativo (no Docker)**: rifiutato dopo decisione utente §9.4 (Testcontainers).
  - **BCrypt cost 4 in profilo `test`** (decisione §9.2 strada B): rifiutato per fedeltà; rivalutabile se suite > 60s.
  - **Test-first puro (TDD strict)**: rifiutato per pragma — controller + filter + exception handler hanno scaffolding Spring Boot evidente, scrivere il test prima della classe è più rituale che produttivo per quel layer specifico.
- **Conseguenze**:
  - 3 comandi distinti Maven per F5:
    1. `mvn -pl server verify -DexcludedGroups=slow` — fast loop (unit + slice + full H2). **No Docker**.
    2. `mvn -pl server verify` — full server test (include Testcontainers MySQL slow). Richiede Docker.
    3. `mvn clean verify` (root) — tutti i moduli inclusi server slow. Richiede Docker.
  - JaCoCo gate `haltOnFailure=true` su `server` ≥ 70% line+branch (configurato in pom).
  - Profilo `dev` Testcontainers Spring Boot dev mode (ADR-044) condivide il setup `@ServiceConnection MySQLContainer` con profilo `it` — **identità di stack tra dev runtime e integration test**: se un E2E auth flow funziona in `it` test, funziona in `dev` runtime.
  - SAST SpotBugs 0 High esteso a `server` (NFR-M-04, già richiesto via parent pom).

---

## Vincoli architetturali invariabili

Di seguito i vincoli che CLAUDE.md §8 impone come "anti-pattern" — qui esplicitati come positivi:

1. La logica torneo vive **solo** in `core-server`. Il client la usa, non la duplica.
2. Tutte le mosse passano dal `RuleEngine` di `shared`, anche server-side, anche se il client ha già validato.
3. Persistenza relazionale **solo via JPA** (parametrizzato). SQL nativo motivato in commit.
4. Tutte le stringhe UI in `messages_*.properties`. Niente literal in classi UI.
5. Niente secret nel repo. Solo env var e profili Spring.
6. `client` con WebSocket → **Jetty embedded**, Tomcat **escluso esplicitamente**.
7. `shared` non dipende da framework di runtime (no Spring, JavaFX, JPA, WebSocket lib).
8. `core-server` non dipende dal trasporto (no Tomcat, Jetty, JPA). Solo `shared` + `spring-context` + `spring-messaging` (DTO).
9. UI controller usa astrazioni di trasporto (es. `MatchClient` interface), implementazioni LAN/Internet intercambiabili.
