# PLAN — Fase 4: `core-server` skeleton

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 4.
- **SPEC version**: 2.2 (2026-04-30).
- **Data piano**: 2026-05-02.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.

---

## 1. Scopo della fase

Costruire il modulo **`core-server`** come **libreria di dominio transport-agnostic** (CLAUDE.md §8.8): tournament engine + match manager + ports di repository + adapter in-memory + event bus interno + publisher STOMP-compatibile, **senza** Tomcat, Jetty, JPA o MySQL. Il deliverable è uno **skeleton funzionante** in cui un singolo match si gioca end-to-end via **API Java pura** (no rete, no UI), valida ogni mossa col `RuleEngine` di `shared`, applica anti-cheat (forfait dopo 5 mosse illegali consecutive), genera eventi `MoveApplied`/`MoveRejected`/`MatchEnded` con sequence number monotonico, e li broadcast su due canali: bus interno Spring + publisher STOMP-compat (in F4 implementato come logger / buffer per test, l'impl WebSocket reale arriva in F6/F7).

Il **tournament engine resta a stub** in F4: interfacce compilanti + record domain, ma niente bracket generation reale (deferred a F8) e niente standings (deferred a F8/F9).

Obiettivi specifici (SPEC §16 Fase 4):

1. **Tournament Engine vuoto ma con interfacce** (`TournamentEngine`, `BracketGenerator`, `RoundRobinScheduler`, `TieBreakerPolicy`): contratti compilanti + record domain `EliminationTournament`/`RoundRobinTournament` + stub no-op che permettono ad altri moduli di compilare contro le interface, ma la logica arriva in F8/F9.
2. **Match Manager con repository ports**: ports `MatchRepository`, `TournamentRepository`, `UserRepository` come interface, `MatchManager` `@Service` cabla l'orchestrazione (turno → validazione `RuleEngine` → persist → eventi → broadcast).
3. **Adapter in-memory built-in** (SPEC §7.2 "Adapter in-memory built-in (per LAN host)"): `InMemoryMatchRepository`, `InMemoryTournamentRepository`, `InMemoryUserRepository` thread-safe.
4. **Event bus interno + STOMP-compatible publisher**: `MatchEventBus` interface + `SpringMatchEventBus` impl (delega a `ApplicationEventPublisher`); `StompCompatiblePublisher` interface + impl `LoggingStompPublisher` (log SLF4J) e `BufferingStompPublisher` (per test).
5. **Anti-cheat counter** (FR-COM-01, SPEC §9.8.3): 5 mosse illegali consecutive da uno stesso giocatore → `MatchEnded(reason=FORFEIT_ANTI_CHEAT)`. Counter per-player, reset su mossa legale.
6. **Sequence number monotonico** (FR-COM-04, SPEC §7.5): ogni evento ha un `sequenceNo` strettamente crescente per match; `eventsSince(matchId, fromSeq)` ritorna la subsequence per replay.
7. **Acceptance SPEC §16 Fase 4**: test di unità del core-server con repository in-memory; **un match singolo funziona end-to-end via API Java**.

**Out of scope** (deferred):

- **Trasporto reale STOMP/WebSocket**: in F6 (server centrale) + F7 (LAN host). F4 lascia solo l'interface `StompCompatiblePublisher`.
- **JPA adapter** (`server` module): F6.
- **Bracket generation reale** (single-elim, byes, brackets diseguali): F8.
- **Round-robin scheduling reale** (algoritmo Berger, doppia/singola, schedulazione): F9.
- **TieBreakerPolicy implementazione completa** (`StandardTieBreakerPolicy` con scontro diretto → Sonneborn-Berger → vittorie → sorteggio): F9. F4 lascia solo l'interface + `NoOpTieBreakerPolicy` stub.
- **Time control attivo** (clock + timeout move): F6. F4 introduce solo il record `TimeControl` come metadato del match (preset BLITZ/RAPID/CLASSICAL/UNLIMITED, no clock running).
- **Chat in-game** (events `ChatMessage`): F7 (LAN match dove la chat è feature gameplay).
- **Tournament-level events** (`TournamentStarted`, `BracketUpdated`, `StandingsUpdated`, `MatchAssigned`, `ChallengeRequested`/Accepted/Declined): F8.
- **Authentication / JWT / user creation**: F5. F4 lascia `UserRepository` come stub minimale (`findById`, `findByUsername`) usato solo dal MatchManager per costruire `UserRef`.
- **WebSocket-based replay** (`/v1/matches/{id}/events?since=N` REST endpoint, `/topic/match/{id}` STOMP): F6/F7. F4 espone solo l'API Java `MatchManager.eventsSince(matchId, fromSeq)`.
- **`MoveRejected` con motivo dettagliato i18n**: F4 usa un enum `RejectionReason` (es. `NOT_YOUR_TURN`, `ILLEGAL_MOVE`, `MATCH_NOT_ONGOING`); la localizzazione del messaggio arriva in F6 quando l'evento attraversa il transport.
- **Cifratura/firma degli eventi**: SPEC §15 lascia fuori dal threat model v1.

---

## 2. Acceptance criteria

### 2.1 SPEC §16 Fase 4 (autoritativo)

> **Acceptance**: test di unità del core-server con repository in-memory; un match singolo funziona end-to-end via API Java.

### 2.2 Criteri operativi estesi

| ID    | Criterio                                                                                                                                                                                                                      | Verificabile come                                                            |
|-------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| A4.1  | `mvn -pl core-server verify` BUILD SUCCESS, JaCoCo gate ≥ 80% line + branch (NFR-M-01).                                                                                                                                       | Output Maven                                                                 |
| A4.2  | **Match end-to-end via API Java** (autoritativo): partita scriptata `RuleEngine.legalMoves` random-walk fino a stato terminale, eseguita interamente via `MatchManager` API; nessuna eccezione, status FINISHED, MatchEnded.   | `SoloMatchEndToEndTest#playsRandomGameUntilTerminal`                         |
| A4.3  | **Sequence number monotonico** (FR-COM-04): per ogni match, gli eventi hanno `sequenceNo` strettamente crescenti partendo da 0; `eventsSince(matchId, n)` ritorna esattamente gli eventi con seq > n in ordine.                | `EventLogTest#sequenceMonotonic` + `EventLogTest#eventsSinceFiltersByCursor` |
| A4.4  | **Anti-cheat 5-illegal forfeit** (FR-COM-01, SPEC §9.8.3): 5 mosse illegali consecutive del white → MoveRejected x5 + MatchEnded(reason=FORFEIT_ANTI_CHEAT, winner=BLACK); counter resetta su mossa legale.                     | `AntiCheatTest#fiveConsecutiveIllegalMovesForfeitsThePlayer` + `AntiCheatTest#legalMoveResetsCounter` |
| A4.5  | **`MatchManager.applyMove` valida turno**: mossa di un giocatore quando non è il suo turno → MoveRejected(reason=NOT_YOUR_TURN), sequence counter avanza, anti-cheat counter avanza.                                            | `MatchManagerTurnTest#rejectsMoveOutOfTurn`                                  |
| A4.6  | **Validazione via `RuleEngine`**: tutte le mosse passano da `RuleEngine.applyMove` (anche se sembrasse ridondante post-validazione client). Mossa illegale → MoveRejected(reason=ILLEGAL_MOVE), stato non modificato.            | `MatchManagerValidationTest#illegalMoveDoesNotMutateState`                   |
| A4.7  | **Resign**: `MatchManager.resign(matchId, who)` produce evento `Resigned` + `MatchEnded(winner=other)`; resign quando match già FINISHED → eccezione applicativa, nessun evento aggiunto.                                       | `ResignFlowTest`                                                             |
| A4.8  | **Draw offer/response**: offer da WHITE → DrawOffered; response BLACK accept=true → DrawAccepted + MatchEnded(reason=DRAW_AGREEMENT); accept=false → DrawDeclined, match prosegue. Doppia offer non ammessa finché c'è pending. | `DrawFlowTest` (4 test)                                                      |
| A4.9  | **Event bus interno**: ogni evento applicato è broadcast a tutti i listener registrati via Spring `@EventListener(MatchEventEnvelope.class)`; ordine FIFO per match.                                                            | `SpringMatchEventBusTest#eventListenerReceivesAllEvents`                     |
| A4.10 | **STOMP-compatible publisher**: ogni evento è pubblicato sul topic `/topic/match/{id}` via `StompCompatiblePublisher.publishToTopic`; il payload è il record evento serializzabile a JSON con Jackson default.                  | `StompPublisherTest#eachEventReachesMatchTopic` (con `BufferingStompPublisher`) |
| A4.11 | **TournamentEngine compila e ritorna stub**: `TournamentEngine.createTournament(spec)` ritorna un id valido + persiste un `Tournament` con status CREATED; `BracketGenerator`/`RoundRobinScheduler` interface esistono, impl è no-op che lancia `UnsupportedOperationException("deferred to F8/F9")`. | `TournamentEngineSkeletonTest`                                               |
| A4.12 | **In-memory adapter thread-safe**: 1000 match concorrenti creati da 16 thread + 10000 mosse parallele su match diversi → nessun ConcurrentModificationException, sequence ancora monotonico per match.                          | `InMemoryRepositoryConcurrencyTest` (`@Tag("slow")`)                          |
| A4.13 | **Anti-pattern check** (CLAUDE.md §8.7-8.8): `core-server` NON dipende da `javafx.*`, `org.springframework.boot.web.*`, `org.eclipse.jetty.*`, `org.apache.tomcat.*`, `jakarta.persistence.*`, `org.hibernate.*`. Verifica via ArchUnit. | `CoreServerArchitectureTest` (ArchUnit)                                       |
| A4.14 | Spotless OK, SpotBugs 0 High su tutto `core-server`.                                                                                                                                                                          | Output Maven                                                                 |
| A4.15 | `package-info.java` per ogni nuovo sotto-package (`match`, `match.event`, `tournament`, `repository`, `repository.inmemory`, `eventbus`, `stomp`).                                                                            | Lettura visiva                                                               |
| A4.16 | `tests/TRACEABILITY.md` aggiornato: FR-COM-01, FR-COM-02 (preview JSON serializability), FR-COM-04, AC §17.1.2 (preview, partita Internet→ riusa MatchManager), AC §17.1.10 (preview, autosave host LAN→ riuserà in F7).        | Lettura visiva                                                               |
| A4.17 | Nessun TODO/FIXME pending in `core-server/src/main/java/`.                                                                                                                                                                    | grep                                                                         |
| A4.18 | **Regression**: `RuleEngineCorpusTest` F1 (48 posizioni) + `AiTournamentSimulationTest` F2 (gating IA Campione ≥95/100) continuano a passare con `mvn clean verify` root.                                                       | Output Maven root                                                            |
| A4.19 | `mvn clean verify` (root, **inclusi** `slow`+`performance`): BUILD SUCCESS su tutti i moduli.                                                                                                                                  | Output Maven root                                                            |

---

## 3. Requisiti SPEC coperti

### 3.1 Funzionali (FR)

| FR ID    | SPEC ref       | Coperto in F4 come                                                                                                                                                                |
|----------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FR-COM-01 | §4.5, §9.8.3  | **Coperto pienamente**: anti-cheat in `MatchManager` con counter 5-illegal-forfeit per giocatore. Tutte le mosse passano da `RuleEngine`.                                          |
| FR-COM-02 | §4.5          | **Coperto preview**: tutti i record `MatchEvent` sono Jackson-serializable con default ObjectMapper (smoke test in `EventSerializationTest`). Trasporto JSON reale arriva in F6.   |
| FR-COM-03 | §4.5          | **Non applicabile in F4**: NFR di latenza riguarda transport (F6). F4 lascia il path API Java sotto-millisecondo per costruzione (no I/O).                                         |
| FR-COM-04 | §4.5, §7.5    | **Coperto pienamente**: log eventi append-only per match in `InMemoryMatchRepository`; sequence atomico via `AtomicLong` per match; `eventsSince(matchId, fromSeq)` per replay.    |
| FR-T-* (tornei) | §4.4    | **Solo skeleton**: interfaces e record domain compilano; logica generation/scheduling/standings deferred a F8/F9.                                                                  |
| FR-COMP-* (LAN/Internet match) | §4.2/§4.3 | **Indiretto**: il MatchManager di F4 sarà riusato da F6 (Internet) e F7 (LAN host) come dependency core. F4 ne valida solo l'API Java offline.                          |

### 3.2 Non funzionali (NFR)

| NFR ID   | SPEC ref       | Coperto in F4 come                                                                                                                                                                                  |
|----------|----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-M-01 | §5             | **Coperto pienamente**: gate JaCoCo `core-server` ≥ 80% line + branch. Vedi stop point §7.7 per il valore esatto.                                                                                    |
| NFR-M-04 | §5             | Spotless Google Java Style su tutto il codice F4.                                                                                                                                                    |
| NFR-S-04 | §5             | **Indiretto**: SpotBugs 0 High. Nessun input untrusted attraversa F4 (no transport).                                                                                                                 |
| NFR-P-04 | §5             | **Non applicabile in F4**: capacity 200 match concorrenti riguarda server JPA, F6+. F4 prova solo che `InMemoryMatchRepository` non corrompe stato sotto contesa con 16 thread (test `slow`).        |

### 3.3 Acceptance criteria globali (§17)

| AC §17  | Descrizione                                                                       | Coperto in F4 da                                                                                  |
|---------|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 17.1.2  | Partita Internet tra due client conclude correttamente con stato persistito.      | **Preview**: `MatchManager` API completa offline; persistenza in-memory. Persistenza MySQL → F6. |
| 17.1.3  | Partita LAN funziona con server centrale offline.                                 | **Preview**: `MatchManager` agnostico al transport; F7 lo cabla via Jetty embedded.              |
| 17.1.4  | Torneo eliminazione a 8 termina con bracket corretto.                              | **Non in F4**: solo skeleton. F8.                                                                |
| 17.1.5  | Campionato round-robin a 6 termina con classifica corretta.                        | **Non in F4**: solo skeleton. F9.                                                                |
| 17.1.10 | Autosave LAN host funziona: dopo crash, torneo riprende.                           | **Non in F4**: serve persistenza file + tournament logic. F7+F10.                                |
| 17.1.11 | Riconnessione Internet entro 2 minuti recupera match con replay eventi.            | **Preview parziale**: `eventsSince(matchId, fromSeq)` API esiste; transport STOMP arriva in F6.  |

---

## 4. Decomposizione in task

Ordine sequenziale; le dipendenze sono lineari salvo dove esplicitato.

### Task 4.1 — Module setup + sub-packages + remove smoke test

- Rimuovere `core-server/src/test/java/.../CoreServerSmokeTest.java` (vestigial F0).
- Creare 7 nuovi sotto-package con `package-info.java`:
  - `com.damaitaliana.core.match`
  - `com.damaitaliana.core.match.event`
  - `com.damaitaliana.core.tournament`
  - `com.damaitaliana.core.repository`
  - `com.damaitaliana.core.repository.inmemory`
  - `com.damaitaliana.core.eventbus`
  - `com.damaitaliana.core.stomp`
- Aggiungere `CoreServerConfiguration` `@Configuration @ComponentScan("com.damaitaliana.core")` per uso in `client` (host LAN) e `server` (Internet).
- **Moduli toccati**: `core-server`. **Test produttivo**: nessuno (solo scaffolding).
- **Dipendenze**: nessuna.

### Task 4.2 — Domain value types (records, enums) + MatchEvent sealed hierarchy

- Records: `MatchId(UUID)`, `UserRef(long id, String username, String displayName)` (id come long con valore `-1` per utenti LAN anonimi non ancora promossi a server-side), `TournamentId(UUID)`, `TournamentMatchRef(TournamentId, int roundNo, int matchIndex)`, `TimeControl(TimeControlPreset preset, long initialMillis, long incrementMillis)`.
- Enums: `MatchStatus { WAITING, ONGOING, FINISHED, ABORTED }`, `MatchResult { WHITE_WINS, BLACK_WINS, DRAW, UNFINISHED }`, `TournamentStatus { CREATED, IN_PROGRESS, FINISHED }`, `EndReason { CHECKMATE_LIKE, RESIGN, DRAW_AGREEMENT, DRAW_REPETITION, DRAW_FORTY_MOVES, FORFEIT_ANTI_CHEAT, FORFEIT_TIMEOUT }`, `TimeControlPreset { BLITZ_5_3, RAPID_15_10, CLASSICAL_30_30, UNLIMITED }`, `RejectionReason { NOT_YOUR_TURN, ILLEGAL_MOVE, MATCH_NOT_ONGOING, MATCH_NOT_FOUND }`.
- **MatchEvent sealed hierarchy** (SPEC §8.3):
  - `sealed interface MatchEvent permits MoveApplied, MoveRejected, DrawOffered, DrawAccepted, DrawDeclined, Resigned, MatchEnded, PlayerDisconnected, PlayerReconnected`
  - Common: `MatchId matchId(); long sequenceNo(); Instant timestamp();`
  - Records con i payload SPEC §8.3 (es. `MoveApplied(MatchId, long, Instant, Move, GameState newState)`).
- **Test**: `MatchEventTest` (sealed esaustività + Jackson roundtrip per ogni variant — anticipa FR-COM-02).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.1.

### Task 4.3 — Repository ports interfaces

- `MatchRepository`:
  - `Match save(Match match)`
  - `Optional<Match> findById(MatchId)`
  - `List<Match> findByStatus(MatchStatus)` (filtro lobby)
  - `List<Match> findByPlayer(UserRef)` (storia partite)
  - `long appendEvent(MatchEvent)` → ritorna sequenceNo assegnato
  - `List<MatchEvent> eventsSince(MatchId, long fromSeq)`
  - `long currentSequenceNo(MatchId)`
- `TournamentRepository` (skeleton): `save`, `findById`, `findByStatus`. Niente filtri elaborati in F4.
- `UserRepository` (skeleton): `findById(long)`, `findByUsername(String)`. F5 lo riempirà col DB user.
- **Test**: `MatchRepositoryContractTest` come abstract test parametrico riusabile per qualsiasi adapter (sarà ri-applicato a JPA in F6).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.2.

### Task 4.4 — In-memory adapters

- `InMemoryMatchRepository @Component`:
  - `ConcurrentHashMap<MatchId, Match>` per snapshot stato corrente.
  - `ConcurrentHashMap<MatchId, AtomicLong>` per sequence per match.
  - `ConcurrentHashMap<MatchId, List<MatchEvent>>` con `Collections.synchronizedList(new ArrayList<>())` per log eventi (sufficiente per scope F4: in F6 il JPA adapter usa append SQL atomico con `UNIQUE(match_id, sequence_no)`).
- `InMemoryTournamentRepository @Component`: stessa pattern.
- `InMemoryUserRepository @Component`: stessa pattern, popolato via setter visible-for-tests (no factory utenti in F4).
- **Test**: `InMemoryMatchRepositoryTest` (estende `MatchRepositoryContractTest` di 4.3) + `InMemoryRepositoryConcurrencyTest @Tag("slow")` (16 thread × 1000 op).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.3.

### Task 4.5 — Event bus interno

- `MatchEventBus` interface: `void publish(MatchEvent)`. Semantica: tutti i listener Spring `@EventListener(MatchEventEnvelope.class)` ricevono.
- `MatchEventEnvelope extends ApplicationEvent`: wrapper per Spring's event model (`source = MatchManager.class`, payload = MatchEvent).
- `SpringMatchEventBus @Component` impl: delega a `ApplicationEventPublisher` injected.
- **Test**: `SpringMatchEventBusTest` con `@SpringJUnitConfig(CoreServerConfiguration.class)` + listener bean che cumula gli eventi ricevuti, asserisce ordine FIFO per match.
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.2.

### Task 4.6 — STOMP-compatible publisher

- `StompCompatiblePublisher` interface: `void publishToTopic(String topic, Object payload)`. F4 non sa nulla di WebSocket; il contratto è solo "dato un topic, propaga il payload al transport".
- Impl `LoggingStompPublisher @Component`: log SLF4J `info` con topic + payload.toString(). Default impl quando nessun altro `@Component` registrato (`@ConditionalOnMissingBean(StompCompatiblePublisher.class)`).
- Impl `BufferingStompPublisher` (test-scope, in `core-server/src/test/java`): collect `(topic, payload)` in lista per asserzioni unit. Esposto come bean nei test via `@TestConfiguration`.
- F6 (server) e F7 (client LAN host) forniranno l'impl reale `WebSocketStompPublisher` che cabla `SimpMessagingTemplate`.
- **Test**: `LoggingStompPublisherTest` (smoke), `BufferingStompPublisherTest` (collect ordine).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.2.

### Task 4.7 — `Match` entity + state machine

- Class `Match` (mutable internamente al package, esposta read-only via getters):
  - `MatchId id; UserRef white, black; GameState state; long sequenceNo; MatchStatus status; TimeControl timeControl; Instant startedAt; Optional<TournamentMatchRef> tournamentRef;`
  - **Anti-cheat counter** (transient, non serializzato): `EnumMap<Color, Integer> consecutiveIllegalMoves` package-private, mutato da `MatchManager`.
  - Pending draw offer: `Optional<UserRef> pendingDrawOfferFrom`.
- State transitions:
  - WAITING → ONGOING (quando entrambi i giocatori sono settati).
  - ONGOING → FINISHED (mossa terminale, resign, draw accepted, anti-cheat forfait).
  - ONGOING → ABORTED (entrambi i giocatori disconnessi > timeout — F4 non implementa il timeout, lascia solo l'enum).
- **Test**: `MatchStateMachineTest` (transizioni valide + invalide).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.2.

### Task 4.8 — `MatchManager` service

- `MatchManager @Service`:
  - `Match createMatch(UserRef white, UserRef black, TimeControl)` → status WAITING o ONGOING (F4: ONGOING immediato, lobby management arriva in F6).
  - `Optional<Match> findById(MatchId)`.
  - `MatchEvent applyMove(MatchId, UserRef sender, Move)`:
    - Match non trovato → eccezione applicativa.
    - Match non ONGOING → MoveRejected(MATCH_NOT_ONGOING).
    - Sender ≠ side-to-move → MoveRejected(NOT_YOUR_TURN), counter avanza, antiCheatCheck.
    - `RuleEngine.applyMove` lancia → MoveRejected(ILLEGAL_MOVE), counter avanza, antiCheatCheck.
    - OK → MoveApplied + status update + reset counter sender.
    - Se status diventa terminal (vittoria/patta) → MatchEnded broadcast.
  - `MatchEvent resign(MatchId, UserRef who)`.
  - `MatchEvent offerDraw(MatchId, UserRef from)`.
  - `MatchEvent respondDraw(MatchId, UserRef responder, boolean accept)`.
  - `List<MatchEvent> eventsSince(MatchId, long fromSeq)` (delega al repository).
- Dependency injection: `MatchRepository`, `RuleEngine` (da `shared`), `MatchEventBus`, `StompCompatiblePublisher`.
- Ogni evento applicato:
  1. `repo.appendEvent(evt)` (atomic, sequence assegnato dentro).
  2. `bus.publish(evt)`.
  3. `stomp.publishToTopic("/topic/match/" + id, evt)`.
- Anti-cheat: dopo ogni MoveRejected, se counter sender ≥ 5 → genera MatchEnded(reason=FORFEIT_ANTI_CHEAT, winner=other) come evento successivo.
- **Test**: `MatchManagerTurnTest`, `MatchManagerValidationTest`, `ResignFlowTest`, `DrawFlowTest` (4 scenari).
- **Moduli toccati**: `core-server`, dipende da `shared` per `RuleEngine`/`Move`/`GameState`.
- **Dipendenze**: 4.4, 4.5, 4.6, 4.7.

### Task 4.9 — Anti-cheat tests + counter behavior

- `AntiCheatTest`:
  - `fiveConsecutiveIllegalMovesForfeitsThePlayer`: white prova 5 mosse illegali (es. da quadrato vuoto, o da pezzo nero); dopo la 5ª, MatchEnded(FORFEIT_ANTI_CHEAT, winner=BLACK) appare nel log.
  - `legalMoveResetsCounter`: white prova 4 illegali, poi 1 legal, poi 4 illegali → no forfait (counter resettato dopo legal).
  - `eachPlayerHasOwnCounter`: 4 illegal di white + 4 illegal di black + 1 illegal di white → forfait white sulla 5ª (di white).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.8.

### Task 4.10 — TournamentEngine skeleton

- `TournamentEngine` interface:
  - `Tournament createTournament(TournamentSpec spec)` (TournamentSpec record con format/timeControl/maxParticipants).
  - `Tournament registerParticipant(TournamentId, UserRef)`.
  - `Tournament startTournament(TournamentId)` → in F4 lancia `UnsupportedOperationException("deferred to F8")`.
  - `Optional<Tournament> findById(TournamentId)`.
- `BracketGenerator` interface: `BracketState generate(List<UserRef> seeds)` → stub `UnsupportedOperationException`.
- `RoundRobinScheduler` interface: `List<RoundRobinMatch> schedule(List<UserRef>)` → stub `UnsupportedOperationException`.
- `TieBreakerPolicy` interface: `List<UserRef> resolveTies(List<UserRef> tied, RoundRobinTournament)`.
- `NoOpTieBreakerPolicy @Component @ConditionalOnMissingBean` → ritorna l'input invariato (placeholder F4).
- `Tournament` sealed interface: `EliminationTournament` + `RoundRobinTournament` records con shape SPEC §8.3 (campi mutabili intenzionalmente lasciati vuoti/zero in F4).
- `TournamentEngineImpl @Service` cabla `createTournament` (genera id + persiste status CREATED) e `registerParticipant` (mutate participants list); `startTournament` lancia `UnsupportedOperationException`.
- **Test**: `TournamentEngineSkeletonTest`: createTournament → status CREATED + id non null; registerParticipant prima di start → ok; startTournament → throws.
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.4 (TournamentRepository).

### Task 4.11 — End-to-end Java API match test (acceptance autoritativa)

- `SoloMatchEndToEndTest`:
  - `playsRandomGameUntilTerminal`: crea match (white/black UserRef stub), poi loop `RuleEngine.legalMoves(state)` random pick (seed fisso per determinismo), invoca `MatchManager.applyMove` finché stato terminale (vittoria/patta-ripetizione/patta-40); asserisce che gli eventi sono in numero atteso, status FINISHED, ultimo evento è MatchEnded, ricostruzione di state da replay degli eventi == state corrente.
  - `replayFromEventsReconstructsCurrentState`: applica N mosse, poi `Match replay = applyAll(eventsSince(id, 0))`, `assertThat(replay.state()).isEqualTo(match.state())`.
  - `eventsBroadcastOnBusAndStomp`: stesso flusso di `playsRandomGameUntilTerminal` ma con `BufferingStompPublisher` + `@EventListener` test bean che collezionano; alla fine entrambe le liste contengono gli stessi eventi nello stesso ordine.
- **Moduli toccati**: `core-server`, dipende da `shared` (RuleEngine, RuleEngineCorpus per posizioni di partenza).
- **Dipendenze**: 4.8.

### Task 4.12 — ArchUnit anti-pattern check

- `CoreServerArchitectureTest` (ArchUnit):
  - **No JavaFX**: `noClasses().should().dependOnClassesThat().resideInAPackage("javafx..")`.
  - **No Spring Boot Web**: `noClasses().should().dependOnClassesThat().resideInAPackage("org.springframework.boot.web..")`.
  - **No Tomcat / Jetty**: `noClasses().should().dependOnClassesThat().resideInAPackage("org.apache.tomcat..", "org.eclipse.jetty..")`.
  - **No JPA / Hibernate**: `noClasses().should().dependOnClassesThat().resideInAPackage("jakarta.persistence..", "org.hibernate..")`.
  - **Sub-package layering**: `match` non dipende da `tournament` (decoupled), entrambi possono dipendere da `repository`/`eventbus`/`stomp`.
- Aggiungere dependency `<scope>test</scope>` `com.tngtech.archunit:archunit-junit5` (NUOVA dipendenza, vedi stop point §7.10).
- **Moduli toccati**: `core-server`.
- **Dipendenze**: 4.8 + 4.10 (le classi target esistono).

### Task 4.13 — Documentation + ADR + CHANGELOG + TRACEABILITY

- **Nuovi ADR** (in `ARCHITECTURE.md`):
  - **ADR-038**: ApplicationEventPublisher come backbone per `MatchEventBus`. Rationale: zero deps nuove (Spring già in DI), supporta `@TransactionalEventListener` per F6 quando arriverà JPA, ordine FIFO garantito per stesso publisher thread.
  - **ADR-039**: `StompCompatiblePublisher` come port lato `core-server`, impl reale lato transport (F6 server, F7 client). Rationale: rispetta CLAUDE.md §8.8 (`core-server` transport-agnostic).
  - **ADR-040**: Anti-cheat counter location in `MatchManager` (in-memory, transient). Rationale: è logica di stato del match, non di trasporto; persisterlo nel DB non aiuta (al recovery counter resetta — il giocatore comincia da zero, scelta benevola accettabile).
  - **ADR-041**: In-memory repository concurrency strategy. `ConcurrentHashMap` + `synchronizedList` + `AtomicLong` per sequence. Lock pessimistico per match (SPEC §7.5) sarà introdotto in F6 col JPA adapter; in F4 il workload single-threaded del LAN host non lo richiede.
- **CHANGELOG** sezione `[Unreleased]` con voci F4.
- **TRACEABILITY**: nuove righe per FR-COM-01, FR-COM-02 (preview), FR-COM-04, AC §17.1.2/3/11 (preview).
- **README**: nessun cambio (no UI, no installer, no comandi nuovi visibili all'utente).
- **AI_CONTEXT.md**: stato avanzato, sotto-fase TEST chiusa post-4.13, "Prossimo task" → REVIEW Fase 4.
- **Moduli toccati**: docs only.
- **Dipendenze**: 4.1..4.12.

---

## 5. Strategia di test

### 5.1 Composizione (CLAUDE.md §2.4.1)

| Tipo            | Target | Tooling                 | Cosa testa in F4                                                                  |
|-----------------|--------|-------------------------|-----------------------------------------------------------------------------------|
| **Unit**        | ~75%   | JUnit 5 + AssertJ + Mockito | Domain types, MatchEvent serialization, repo adapter contract, MatchManager flows, TournamentEngine stubs, anti-cheat counter, sequence monotonic |
| **Integration** | ~22%   | Spring `@SpringJUnitConfig` (no Spring Boot, solo `spring-context`) | Event bus end-to-end con listener Spring, MatchManager + repo + bus + STOMP publisher cabled via DI |
| **E2E API**     | ~3%    | Test Java custom (no UI, no rete) | `SoloMatchEndToEndTest`: partita random scriptata fino a stato terminale via API Java pura |

Niente TestFX (no UI), niente Testcontainers (no MySQL), niente JmDNS (no LAN). Tutto in-process JVM.

### 5.2 Coverage target (NFR-M-01)

| Modulo        | Coverage minima | Note                                                                                          |
|---------------|-----------------|-----------------------------------------------------------------------------------------------|
| `core-server` | ≥ 80% line + branch | Esclusi dal gate: package-info, record DTO senza logica (es. `MatchId`/`UserRef`), enum DTO. |

Vedi stop point §7.7.

### 5.3 Regression

- `RuleEngineCorpusTest` F1 (48 posizioni) DEVE continuare a passare.
- `AiTournamentSimulationTest` F2 (gating IA Campione ≥95/100) DEVE continuare a passare con `mvn clean verify` root.
- Tutti i test client F3+F3.5 (321 fast + slow/perf) DEVONO continuare a passare (F4 non tocca client).

### 5.4 Naming convention

Stile per modulo `core-server`: `should<Espressione>_when<Condizione>` (uniforme con `shared`/`core-server` test esistenti). Test method possono usare `<Feature><Scenario>` per scenari E2E (es. `playsRandomGameUntilTerminal`).

---

## 6. Rischi e mitigazioni

| ID  | Rischio                                                                                                    | Probabilità | Impatto | Mitigazione                                                                                                                                                                                            |
|-----|------------------------------------------------------------------------------------------------------------|-------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R-1 | Scope creep verso F6: tentazione di implementare già STOMP reale o JPA per "non doverci tornare".          | Media       | Alto    | PLAN F4 limita STOMP a interface + LoggingPublisher; ArchUnit (Task 4.12) blocca dependency su Tomcat/Jetty/JPA al build. Anti-pattern check parte del gate.                                          |
| R-2 | Scope creep verso F8/F9: tentazione di implementare bracket generator reale.                                | Media       | Alto    | TournamentEngine impl di F4 lancia esplicitamente `UnsupportedOperationException("deferred to F8")` per `startTournament`/`generate`/`schedule`. Test asserisce che la lancia.                          |
| R-3 | `InMemoryMatchRepository` corrompe stato sotto contesa.                                                     | Bassa       | Alto    | `InMemoryRepositoryConcurrencyTest @Tag("slow")` con 16 thread; uso `ConcurrentHashMap` + `synchronizedList` + `AtomicLong`. Lock pessimistico più forte arriva in F6 con JPA `@Lock(PESSIMISTIC_WRITE)`. |
| R-4 | Shape `MatchEvent` lock-in per F6 transport JSON: cambi futuri rompono compatibilità wire.                  | Media       | Medio   | F4 verifica Jackson roundtrip per ogni variant (`EventSerializationTest`); records con campi nominati (no positional in JSON). Eventuali rinomine verranno gestite con `@JsonAlias` in F6 senza breaking. |
| R-5 | Anti-cheat counter perso al recovery (F6): comportamento incoerente quando un giocatore disconnette/riconnette dopo 4 illegal moves. | Media | Medio | ADR-040 documenta la scelta benevola "counter resetta al recovery". Se in F6 si decide diverso, sarà un cambio di contratto tracciato.                              |
| R-6 | `RuleEngine` di `shared` ha API leggermente diverse da quanto assunto in PLAN.                              | Bassa       | Basso   | Verifica preliminare: `RuleEngine` interface esiste con `legalMoves`/`applyMove`/`computeStatus`; conferma SPEC §8.2.                                                                                  |
| R-7 | ArchUnit non disponibile o causa conflitti con dipendenze esistenti.                                        | Bassa       | Basso   | Stop point §7.10 chiede approvazione esplicita; in alternativa fallback a check Maven enforcer `bannedDependencies` (meno espressivo ma sufficiente).                                                  |
| R-8 | JaCoCo gate ≥80% non raggiungibile per via dell'high coverage richiesto su classi di configurazione Spring. | Media       | Medio   | Esclusioni esplicite in `pom.xml` per `package-info`, record DTO, enum DTO, `CoreServerConfiguration`. Pattern già usato in `client` per `JavaFxAudioService`/`JavaFxApp`.                              |

---

## 7. Stop points / decisioni utente da confermare

### 7.1 Bracket generator scope (decisione SPEC §16 lettura)

SPEC §16 Fase 4: "Tournament Engine **vuoto ma con interfacce**". Letteralmente: solo interfacce.

**Proposta**: solo interfacce + record domain compilanti + `TournamentEngine.createTournament` funzionante (per permettere a F5/F6 di linkare ai contratti); `BracketGenerator.generate`, `RoundRobinScheduler.schedule`, `TournamentEngine.startTournament` lanciano `UnsupportedOperationException("deferred to F8/F9")`.

**Alternativa**: implementare già single-elimination per N=2/4/8 (bye-free) in F4 per accorciare F8.

**Raccomandazione**: **Proposta** (rispetta letteralmente §16).

### 7.2 Anti-cheat counter location

SPEC §9.8.3: "5 mosse illegali consecutive da uno stesso giocatore → forfait automatico". Non specifica dove vive il counter.

**Proposta**: counter transient in `MatchManager`, mantenuto in-memory per la vita del match; non persistito; resetta al recovery (F6+ riconnessione).

**Alternativa**: persistere nel DB come campo `consecutive_illegal_moves` su `matches` table (F6).

**Raccomandazione**: **Proposta**. Rationale: è stato del match in corso, non audit trail. Comportamento "counter resetta al recovery" è benevolo, ammissibile.

### 7.3 Event bus implementation

**Proposta**: `ApplicationEventPublisher` Spring + `MatchEventEnvelope extends ApplicationEvent`. Zero deps nuove, supporta `@TransactionalEventListener` per F6.

**Alternativa**: bus custom basato su `BlockingQueue` + thread dispatcher (più controllo, più lavoro).

**Raccomandazione**: **Proposta**.

### 7.4 ChatMessage event

Chat è feature F7 (LAN match — SPEC §11.3 STOMP topic `/topic/match/{id}/chat`).

**Proposta**: nessun `ChatMessage` event in F4. Aggiunta in F7.

**Raccomandazione**: **Proposta**.

### 7.5 Tournament-level events

Eventi `TournamentStarted`, `BracketUpdated`, `StandingsUpdated`, `MatchAssigned`, `Challenge*` (SPEC §11.4) sono tutti F8/F9.

**Proposta**: nessun tournament event in F4.

**Raccomandazione**: **Proposta**.

### 7.6 TimeControl scope

**Proposta**: record `TimeControl` + enum `TimeControlPreset` (BLITZ_5_3, RAPID_15_10, CLASSICAL_30_30, UNLIMITED) come **metadato** del match. Niente clock running, niente timeout enforcement in F4.

**Alternativa**: implementare già clock + `MatchEnded(FORFEIT_TIMEOUT)` (richiede scheduler / `ScheduledExecutorService`).

**Raccomandazione**: **Proposta**. Clock attivo entra in F6 col transport (server-authoritative).

### 7.7 JaCoCo coverage gate

NFR-M-01: ≥ 80% per `core-server`.

**Proposta**: gate ≥ 80% line + branch su core-server, con esclusioni:
- `**/package-info.class`
- Record DTO senza logica (`MatchId`, `UserRef`, `TournamentId`, `TournamentMatchRef`, `TimeControl`, `TournamentSpec`).
- Enum DTO (`MatchStatus`, `MatchResult`, `TournamentStatus`, `EndReason`, `TimeControlPreset`, `RejectionReason`).
- `CoreServerConfiguration` (Spring config).

**Alternativa**: target più alto (≥85%) come per `shared` `RuleEngine`.

**Raccomandazione**: **Proposta** (≥80% in linea con SPEC; ulteriori +5% non necessari per modulo skeleton).

### 7.8 `Tournament` sealed interface — quanto popolare in F4?

SPEC §8.3 dice `EliminationTournament` ha `BracketState bracket` + `int currentRound`; `RoundRobinTournament` ha vari campi.

**Proposta**: i record domain hanno tutti i campi SPEC §8.3, ma con valori vuoti/zero in F4 (lista participants vuota a creazione, bracket null, schedule lista vuota). La logica per popolarli arriva in F8/F9.

**Raccomandazione**: **Proposta**.

### 7.9 `UserRef` shape

SPEC non fissa `UserRef` come record concreto. F5 introdurrà entità `User`.

**Proposta**: record `UserRef(long id, String username, String displayName)`. `id == -1` per utenti LAN anonimi (SPEC §11.1 "username + password opzionale al CONNECT"); `id > 0` per utenti server-side autenticati. F5 popola `id` da DB.

**Raccomandazione**: **Proposta**.

### 7.10 Nuova dipendenza ArchUnit

CLAUDE.md §8 anti-pattern #13: "Non aggiungere dipendenze non menzionate in `SPEC.md` sezione 6 senza approvazione esplicita".

ArchUnit NON è in SPEC §6. Richiesta approvazione.

- `com.tngtech.archunit:archunit-junit5:1.3.0` (~3 MB), `<scope>test</scope>`. Apache 2.0 license.
- Rationale: enforcement automatico di CLAUDE.md §8.7-8.8 (transport-agnostic), evita drift silenzioso.
- Alternativa zero-dep: Maven enforcer `bannedDependencies` (meno granulare, blocca solo a livello di Maven dependency, non di import Java).

**Raccomandazione**: **approvare ArchUnit test-scope**. Beneficio architetturale rilevante; alternativa enforcer-only è più debole.

### 7.11 Branch strategy

Branch: `feature/4-core-server-skeleton`, da staccarsi da `develop` **solo dopo approvazione di questo PLAN** (CLAUDE.md §2.1 stop point + §4.3).

---

## 8. Stima di completamento

**13 task** (4.1..4.13).

Dipendenze critiche path (in ordine non-parallelo):
- 4.1 → 4.2 → 4.3 → 4.4 → 4.7 → 4.8 → 4.9, 4.11
- 4.5 e 4.6 si possono parallelizzare con 4.7 (sono indipendenti).
- 4.10 (TournamentEngine) si può parallelizzare con 4.8 (dipende solo da 4.4).
- 4.12 (ArchUnit) e 4.13 (docs) sono terminal.

Non ci sono task che richiedono coordinamento esterno (no test manuali, no install MSI, no asset acquisition). Tutto Java + Maven, riproducibile.

---

## 9. Avvio sotto-fase IMPLEMENTA

A approvazione del PLAN:

1. Creare branch `feature/4-core-server-skeleton` da `develop`.
2. Aggiornare `AI_CONTEXT.md`: sotto-fase IMPLEMENTA Fase 4, prossimo task → Task 4.1.
3. Procedere con Task 4.1; al termine di ogni task, **chiedere conferma utente** prima di passare al successivo (memoria progetto: `feedback_ask_before_next_task.md`).
