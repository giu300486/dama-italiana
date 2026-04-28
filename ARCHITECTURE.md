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
