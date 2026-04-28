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
