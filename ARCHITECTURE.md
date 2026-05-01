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
