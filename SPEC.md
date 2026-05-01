# SPEC — Dama Italiana Multiplayer (Desktop)

> **Documento di specifica per Spec-Driven Development con Claude Code**
> Versione: 2.2 · Data: 2026-04-30 · Linguaggio: Italiano
> Tipo progetto: Applicazione desktop Java + Server centrale Spring Boot
>
> Storico versioni: 2.0 (2026-04-26 — baseline post-Fase 0); 2.0 + CR-001 (2026-04-28 — repetition/40-mosse via reference incrementale, deferred F4); 2.1 (2026-04-30 — clarifiche post-REVIEW Fase 3: granularità undo/redo §4.1, deferral a11y i18n + daltonismo §13.5, deferral consci enumerati in Fase 11); **2.2 (2026-04-30 — pull-forward per demo cliente: §13.2 design system riscritto "videogame premium wood", §13.3 animazioni avanzate, §13.4 ambient music, nuova §16 Fase 3.5, modifica §16 Fase 11)**.

---

## 0. Come usare questo documento

Questo documento è la specifica autoritativa del progetto. Claude Code deve trattarlo come fonte di verità in pianificazione, progettazione e implementazione. Le sezioni sono ordinate per priorità di lettura:

1. Sezioni 1–9 → contesto, requisiti, dominio, architettura. Da leggere integralmente prima di scrivere codice.
2. Sezione 10 → flussi end-to-end di tutti gli scenari. È la parte più operativa: per ogni feature, descrive cosa accade dal click utente alla persistenza.
3. Sezione 11 → contratti REST e STOMP.
4. Sezioni 12–15 → IA, UI, persistenza, sicurezza.
5. Sezione 16 → roadmap a fasi. Ogni fase ha acceptance criteria espliciti (sezione 17).

Convenzioni:
- `MUST` / `DEVE` → requisito obbligatorio
- `SHOULD` / `DOVREBBE` → fortemente raccomandato
- `MAY` / `PUÒ` → opzionale

---

## 1. Visione e obiettivi

### 1.1 Vision statement

Realizzare un gioco di **Dama Italiana** in tecnologia Java come applicazione desktop, con esperienza utente moderna, giocabile in tre modalità: contro il computer (single-player), contro un avversario in LAN, e online tramite server centrale. La modalità multiplayer (sia LAN che Internet) supporta sia partite singole sia tornei in due formati: eliminazione diretta e campionato a punti.

### 1.2 Obiettivi

- **G1**: Implementare correttamente le regole della Dama Italiana FID.
- **G2**: Fornire un'interfaccia desktop moderna, fluida, accessibile.
- **G3**: Single-player con IA a tre livelli (Principiante, Esperto, Campione).
- **G4**: Multiplayer LAN peer-to-peer con discovery automatica.
- **G5**: Multiplayer Internet con server centrale.
- **G6**: Tornei multi-stanza con match concorrenti, sia in LAN sia su Internet.
- **G7**: Due formati di torneo: eliminazione diretta e campionato round-robin a punti.
- **G8**: Salvataggio multi-slot delle partite single-player.
- **G9**: Resilienza alla disconnessione (riconnessione automatica con replay degli eventi mancati).
- **G10**: Sezione "Regole" in-app con regolamento illustrato della Dama Italiana.

### 1.3 Non-obiettivi (out of scope)

- Versioni mobile (Android/iOS).
- Varianti del gioco diverse dalla Dama Italiana (rimosse dalla v2).
- Microtransazioni e monetizzazione.
- Stream/spectator mode broadcast verso pubblico esterno al torneo.
- Matchmaking ELO-based avanzato.

### 1.4 Stakeholder

| Ruolo | Responsabilità |
|---|---|
| Player guest | Gioca single-player, partecipa a LAN senza account |
| Player registrato | Gioca online, partecipa a tornei Internet, mantiene statistiche |
| Tournament admin (Internet) | Crea e modera tornei sul server centrale |
| LAN tournament host | Crea e gestisce tornei nella propria LAN (utente qualsiasi) |
| System admin | Mantiene il server centrale, monitora health |

---

## 2. Glossario

| Termine | Definizione |
|---|---|
| **Pedina** | Pezzo base, muove e cattura solo in avanti diagonale. |
| **Dama** | Pezzo promosso, muove di una sola casa in tutte e 4 le diagonali. |
| **Damiera** | Scacchiera 8×8 (32 case scure giocabili). |
| **Cattura** | Salto sopra una pedina avversaria adiacente. |
| **Presa multipla** | Sequenza di catture concatenate nello stesso turno. |
| **Match** | Singola partita 1v1 con vincitore. |
| **Lobby** | Sala d'attesa pre-partita per match singoli. |
| **Stanza torneo (room)** | Container logico di un sottogruppo del torneo (es. "tutti i match del round 1" oppure "tabellone A"). |
| **Bracket** | Tabellone di accoppiamenti del torneo a eliminazione. |
| **Round** | Insieme di match simultanei in un torneo. |
| **Campionato (round-robin)** | Torneo in cui ogni giocatore affronta tutti gli altri. Vince chi totalizza più punti. |
| **Host LAN** | Client che ospita una partita o un torneo nella propria rete locale. |
| **mDNS** | Multicast DNS, protocollo di scoperta servizi in LAN. |
| **STOMP** | Simple Text Oriented Messaging Protocol, protocollo applicativo su WebSocket. |
| **SDD** | Spec-Driven Development. |

---

## 3. Regole della Dama Italiana

> Riferimento normativo: regolamento ufficiale FID (Federazione Italiana Dama). Le regole della Dama Italiana sono diverse da quelle della Dama Internazionale e da quelle anglosassoni. Claude Code DEVE implementare la variante italiana e DEVE NOT confondere le regole.

### 3.1 Setup iniziale

- Damiera 8×8.
- Si gioca solo sulle 32 case scure.
- La casa scura DEVE trovarsi nell'angolo basso a sinistra rispetto al giocatore Bianco.
- 12 pedine per giocatore, disposte sulle prime 3 file.
- Bianco muove per primo.

### 3.2 Movimento

- **Pedina**: muove di una casa diagonalmente in avanti. Mai indietro.
- **Dama**: muove di una sola casa diagonalmente in qualsiasi direzione. Non è "volante".

### 3.3 Cattura

- Cattura per salto sopra un pezzo avversario adiacente in diagonale, atterrando sulla casa immediatamente successiva (vuota).
- **Pedina** cattura solo in avanti.
- **Dama** cattura in tutte e 4 le diagonali, ma di una sola casa per volta.
- La cattura è obbligatoria quando possibile.
- **La pedina NON può catturare la dama.** Regola distintiva della variante italiana.

### 3.4 Presa multipla e regole di precedenza

Quando più sequenze di cattura sono disponibili, il giocatore DEVE applicare in ordine le quattro leggi:

1. **Legge della quantità**: scegliere la sequenza che cattura il maggior numero di pezzi.
2. **Legge della qualità**: a parità di quantità, scegliere la sequenza che cattura il maggior numero di dame.
3. **Legge della precedenza della dama**: a parità di quantità e qualità, se si può catturare con dama o con pedina, catturare con la dama.
4. **Legge della prima dama**: a parità di quantità, qualità e tipo di pezzo, la dama DEVE catturare per prima la dama avversaria nella sequenza.

Implementazione: il motore di gioco genera tutte le sequenze legali di cattura, applica le quattro leggi in ordine, e fornisce al giocatore solo le mosse che superano tutti i filtri.

### 3.5 Promozione a Dama

- Una pedina che termina il movimento sull'ultima riga avversaria viene promossa a dama.
- **La promozione termina il turno**: una pedina che raggiunge l'ultima riga durante una sequenza di catture non continua a catturare come dama nello stesso turno.

### 3.6 Condizioni di fine partita

- **Vittoria**: l'avversario non ha pezzi, oppure non ha mosse legali (lo stallo è sconfitta in dama italiana).
- **Patta**: triplice ripetizione della stessa posizione con stesso giocatore al tratto; 40 mosse consecutive senza catture e senza spostamenti di pedina; accordo tra giocatori (online: pulsante "Offri patta").

### 3.7 Tempo di gioco (online)

- **Casual**: senza limite.
- **Classica**: 30 minuti per giocatore + 30 secondi di incremento per mossa (Fischer).
- **Rapid**: 10 minuti + 5 secondi.
- **Blitz**: 3 minuti + 2 secondi.
- I tornei DEVONO usare un controllo di tempo definito dall'admin/host.

### 3.8 Notazione

Notazione FID: caselle numerate da 1 a 32 (solo case scure).
- Mossa semplice: `12-16`.
- Cattura: `12x19`.
- Cattura multipla: `12x19x26`.

---

## 4. Requisiti funzionali

### 4.1 Single-player vs IA

| ID | Requisito |
|---|---|
| FR-SP-01 | Avvio partita contro CPU senza account né connessione. |
| FR-SP-02 | Tre livelli di difficoltà: **Principiante**, **Esperto**, **Campione**. |
| FR-SP-03 | Scelta del colore (Bianco / Nero / Casuale). |
| FR-SP-04 | Highlight delle mosse legali al click sulla pedina. |
| FR-SP-05 | Highlight rosso pulsante per cattura obbligatoria. |
| FR-SP-06 | Undo/redo illimitato. *Granularità: una coppia (mossa umana + risposta IA) annullata come unità singola; non è possibile annullare un solo half-ply. Profondità non vincolata da policy: limitata solo dalla memoria disponibile.* |
| FR-SP-07 | **Salvataggio multi-slot**: ogni partita salvata è un file dedicato; l'utente può nominarla; la lista delle partite salvate è visibile e ordinabile. |
| FR-SP-08 | Autosave continuo della partita corrente (slot speciale `_autosave`); alla riapertura del client, prompt "Riprendi partita interrotta?". |
| FR-SP-09 | Cronologia mosse in notazione FID nel pannello laterale. |

### 4.2 Multiplayer LAN

LAN supporta le **stesse modalità** di Internet (match singolo + tornei eliminazione e campionato), con limitazioni dovute all'assenza di server centrale (vedi 4.2.5).

| ID | Requisito |
|---|---|
| FR-LAN-01 | Hosting partita LAN singola: il client espone un endpoint locale e si pubblicizza via mDNS. |
| FR-LAN-02 | Discovery automatica via mDNS sui client della stessa subnet. |
| FR-LAN-03 | Inserimento manuale `host:port` come fallback. |
| FR-LAN-04 | Le partite LAN funzionano senza accesso a Internet. |
| FR-LAN-05 | Chat testuale tra giocatori dello stesso match. |
| FR-LAN-06 | In caso di drop di un client, l'host attende 60 secondi prima di assegnare forfait. |
| FR-LAN-07 | Hosting torneo LAN con scelta formato: **eliminazione diretta** o **campionato round-robin**. |
| FR-LAN-08 | Stanze multiple (rooms) e match simultanei dentro lo stesso round. |
| FR-LAN-09 | Spettatorialità read-only dei match della stessa stanza torneo. |
| FR-LAN-10 | Autosave dell'host: stato del torneo persistito su disco a ogni evento; se l'host crasha e riparte, prompt "Riprendi torneo interrotto?". |
| FR-LAN-11 | Export/import bracket su file JSON (backup di emergenza). |

#### 4.2.5 Limiti del torneo LAN (documentati)

- Il torneo LAN dipende dalla disponibilità del client host. Se l'host viene chiuso definitivamente, il torneo è perso (salvo export manuale già effettuato).
- Il **campionato round-robin** in LAN è praticabile in due scenari:
  1. Sessione singola: tutti i giocatori presenti per qualche ora, completano il girone in una serata.
  2. Multi-sessione: l'host mantiene lo stato persistito su disco e riapre il torneo in giorni successivi; i partecipanti devono essere nella stessa LAN. È supportato ma scoraggiato per affidabilità.

### 4.3 Multiplayer Internet (server centrale)

| ID | Requisito |
|---|---|
| FR-NET-01 | Registrazione account (username + email + password) o accesso come guest temporaneo. |
| FR-NET-02 | Lobby pubblica con elenco dei match aperti, paginata. |
| FR-NET-03 | Creazione match pubblico, privato (con codice invito), o invito a un amico per username. |
| FR-NET-04 | Riconnessione automatica entro 2 minuti di drop con replay degli eventi mancati. |
| FR-NET-05 | Stato del match persistito sul server in tempo reale. |
| FR-NET-06 | Profilo utente con statistiche (W/L/D, partite giocate, dame fatte, ecc.). |
| FR-NET-07 | Chat in-game con anti-spam server-side. |
| FR-NET-08 | Sistema di report giocatore. |
| FR-NET-09 | Storia partite scaricabile dal profilo, con replay viewer. |

### 4.4 Tornei (LAN e Internet)

#### 4.4.1 Eliminazione diretta

| ID | Requisito |
|---|---|
| FR-TRN-E-01 | Bracket generato automaticamente all'avvio del torneo. |
| FR-TRN-E-02 | Numero partecipanti: 4, 8, 16, 32 (potenze di 2). Se non è una potenza di 2, byes assegnati ai seed più alti. |
| FR-TRN-E-03 | Ogni round è una stanza separata con i suoi match simultanei. |
| FR-TRN-E-04 | Il vincitore di un match avanza al round successivo. |
| FR-TRN-E-05 | Forfait dopo 3 minuti di mancata presentazione (configurabile). |
| FR-TRN-E-06 | Pubblicazione classifica finale (1°, 2°, semifinalisti, ecc.). |

#### 4.4.2 Campionato round-robin a punti

| ID | Requisito |
|---|---|
| FR-TRN-R-01 | Ogni partecipante affronta tutti gli altri in match 1v1. |
| FR-TRN-R-02 | Sistema di punteggio: **vittoria 2 punti, patta 1 punto, sconfitta 0 punti** (modificabile dall'admin in fase di creazione). |
| FR-TRN-R-03 | I match possono essere giocati in giornate diverse: il torneo non vincola tutti i giocatori a essere online contemporaneamente. |
| FR-TRN-R-04 | Per ogni partecipante è disponibile la lista dei match da giocare e di quelli completati. |
| FR-TRN-R-05 | Calendario opzionale: l'admin PUÒ definire scadenze per round (es. "tutti i match del girone A entro il 30 aprile"). |
| FR-TRN-R-06 | Classifica live aggiornata a ogni match concluso. **Tie-breaker** in ordine: (1) scontro diretto, (2) Sonneborn-Berger, (3) numero di vittorie, (4) sorteggio. La policy DEVE essere implementata come strategia configurabile (`TieBreakerPolicy` interfaccia con implementazioni intercambiabili) per consentire futuri allineamenti al regolamento FID. |
| FR-TRN-R-07 | Il torneo si chiude automaticamente quando tutti i match sono giocati (o per forfait gestito dall'admin). |
| FR-TRN-R-08 | In Internet il campionato PUÒ durare giorni o settimane. In LAN si raccomanda sessione singola (vedi 4.2.5). |

### 4.5 Comuni a tutte le modalità multiplayer

| ID | Requisito |
|---|---|
| FR-COM-01 | Validazione mosse server-side (anti-cheat). Il client è untrusted. |
| FR-COM-02 | Messaggi serializzati in JSON. |
| FR-COM-03 | Latenza percepita mossa→broadcast < 300 ms in condizioni normali. |
| FR-COM-04 | **Log mosse append-only con sequence number monotonico per match**: ogni mossa applicata ha un numero progressivo, scritto nel log; la riconnessione e il replay si basano su questo. |

### 4.6 Sezione regole in-app

| ID | Requisito |
|---|---|
| FR-RUL-01 | Schermata "Regole" accessibile dal menu principale e dall'icona ⓘ in-game. |
| FR-RUL-02 | Layout a sezioni: setup, movimento, cattura, leggi di precedenza, promozione, fine partita, notazione. |
| FR-RUL-03 | Diagrammi statici della damiera per illustrare le regole, generati con lo stesso renderer del board view. |
| FR-RUL-04 | Mini-animazioni dimostrative (opzionali) per cattura semplice, presa multipla, promozione. |
| FR-RUL-05 | Localizzazione IT/EN. |

---

## 5. Requisiti non funzionali

| ID | Requisito |
|---|---|
| NFR-P-01 | Client a 60 FPS su hardware con GPU integrata Intel UHD 620 o superiore. |
| NFR-P-02 | IA "Campione" risponde entro 5 secondi in posizioni di metà partita. |
| NFR-P-03 | Server centrale sostiene almeno 200 match concorrenti su istanza 4 vCPU / 8 GB RAM. |
| NFR-P-04 | Throughput WebSocket ≥ 1000 messaggi/sec aggregati. |
| NFR-R-01 | Stato match persistito a ogni mossa (write-through). |
| NFR-R-02 | Crash client non causa perdita partita. |
| NFR-R-03 | Restart server permette ripristino match in corso da DB. |
| NFR-S-01 | Password hashate con BCrypt (cost ≥ 12). |
| NFR-S-02 | JWT con access token 15 min e refresh token 7 giorni. |
| NFR-S-03 | TLS 1.3 obbligatorio per tutto il traffico Internet. |
| NFR-S-04 | Rate-limit login (5/min) e creazione account (3/ora). |
| NFR-S-05 | Sequence number per protezione da replay attack. |
| NFR-U-01 | Localizzazione IT/EN. |
| NFR-U-02 | Dark mode e light mode. |
| NFR-U-03 | Accessibilità tastiera per tutte le azioni principali. |
| NFR-U-04 | Contrasto WCAG AA. |
| NFR-M-01 | Coverage test ≥ 80% sul motore di gioco (modulo `shared`). |
| NFR-M-02 | Coverage test ≥ 60% su rete e UI. |
| NFR-M-03 | API documentate via OpenAPI 3.1. |
| NFR-M-04 | Stile codice Google Java Style Guide (verifica via Spotless). |
| NFR-O-01 | Server espone metriche Prometheus su `/actuator/prometheus`. |
| NFR-O-02 | Logging strutturato JSON. |
| NFR-O-03 | Correlation-id end-to-end. |

---

## 6. Stack tecnologico

### 6.1 Linguaggio e build

- **Java 21 LTS** (client, server, shared, core-server).
- **Maven 3.9+** con monorepo multi-modulo (parent `pom.xml` + 4 sotto-moduli).

### 6.2 Modulo `shared`

Modello di dominio puro, senza dipendenze framework.

| Componente | Tecnologia |
|---|---|
| Linguaggio | Java 21 |
| Serializzazione | Jackson Databind (per i DTO) |
| Test | JUnit 5, AssertJ |

Vincolo: `shared` non DEVE dipendere da JavaFX, Spring, JPA, o WebSocket library.

### 6.3 Modulo `core-server`

Logica di matchmaking, tournament engine, broker eventi, indipendente dal transport.

| Componente | Tecnologia |
|---|---|
| Linguaggio | Java 21 |
| DI | Spring Framework (`spring-context`) |
| Storage astratto | Repository ports (interfacce); implementazioni in-memory built-in |
| Eventi | Listener pattern interno + adattatore STOMP |
| Test | JUnit 5, Mockito |

### 6.4 Modulo `client`

Applicazione desktop JavaFX.

| Componente | Tecnologia |
|---|---|
| UI framework | **JavaFX 21+** (no AtlantaFX, CSS custom) |
| DI / config | **Spring Boot starter** non-web (`spring-boot-starter` senza `-web`) per il singleton container e `application.yml` |
| Container embedded HOST | **Spring Boot WebSocket starter con Jetty** (non Tomcat); attivato solo in modalità host LAN |
| HTTP client | `java.net.http.HttpClient` |
| WebSocket client | `java.net.http.WebSocket` + libreria STOMP client (es. `spring-messaging` STOMP client) |
| JSON | Jackson |
| mDNS | **JmDNS 3.5+** |
| Iconografia | **Ikonli** (Material + FontAwesome pack) |
| Audio | JavaFX Media |
| Logging | SLF4J + Logback |
| Test | JUnit 5, AssertJ, TestFX, Mockito |
| Packaging | **jpackage** (installer .msi/.dmg/.deb) |

> **Decisione DI client**: Spring Boot starter senza `-web`. Ottieni `application.yml`, profili (dev/prod), `@ConfigurationProperties`, `@Component` scan, senza embedded Tomcat. Bootstrap circa +200ms rispetto a `spring-context` puro, ma tooling familiare e meno boilerplate.

> **Decisione embedded HOST**: quando il client deve esporre un endpoint WebSocket (host LAN match o torneo), si attiva `spring-boot-starter-websocket` con **Jetty** invece di Tomcat (Tomcat escluso esplicitamente, Jetty incluso). Jetty è ~2 MB, parte in <100ms, ed è progettato anche per uso embedded leggero. Il container Jetty NON parte se l'utente non avvia un hosting.

> **Decisione UI**: JavaFX puro con un **design system documentato** (sezione 13). Niente AtlantaFX. Si scrive un `theme.css` custom basato su design token (palette, tipografia, spacing, radius, shadow) per ottenere estetica moderna controllata e coerente.

### 6.5 Modulo `server` (centrale Internet)

Spring Boot completo che incorpora `core-server`.

| Componente | Tecnologia |
|---|---|
| Framework | Spring Boot 3.4+ |
| Web layer | Spring Web MVC + Spring WebSocket + STOMP |
| Sicurezza | Spring Security + JJWT |
| Persistenza | Spring Data JPA + Hibernate |
| Database | **MySQL 8.0+** (prod) / H2 (test) |
| Driver | `mysql-connector-j` |
| Migrations | Flyway |
| Cache | Caffeine (locale) |
| API doc | springdoc-openapi |
| Metriche | Micrometer + Prometheus |
| Logging | Logback + Logstash JSON encoder |
| Test | JUnit 5, Spring Boot Test, Testcontainers (con MySQL) |
| Deploy | Docker image (Spring Boot Buildpacks) |

### 6.6 Layout monorepo Maven

```
dama-italiana/
├── pom.xml                          # parent, packaging=pom
├── shared/
│   ├── pom.xml
│   └── src/main/java/com/damaitaliana/shared/
│       ├── domain/
│       ├── rules/
│       ├── ai/
│       └── dto/
├── core-server/
│   ├── pom.xml                      # depends on: shared
│   └── src/main/java/com/damaitaliana/core/
│       ├── match/
│       ├── tournament/
│       ├── repository/              # ports (interfacce)
│       ├── repository/inmemory/     # adapter in-memory
│       └── stomp/
├── client/
│   ├── pom.xml                      # depends on: shared, core-server
│   └── src/main/java/com/damaitaliana/client/
│       ├── ui/
│       ├── controller/
│       ├── network/
│       ├── lan/
│       │   ├── discovery/           # JmDNS
│       │   └── host/                # Jetty embedded
│       └── persistence/
└── server/
    ├── pom.xml                      # depends on: shared, core-server
    └── src/main/
        ├── java/com/damaitaliana/server/
        │   ├── auth/
        │   ├── user/
        │   ├── match/
        │   ├── tournament/
        │   ├── repository/jpa/      # adapter JPA per le port di core-server
        │   └── config/
        └── resources/
            ├── application.yml
            └── db/migration/        # Flyway scripts MySQL
```

---

## 7. Architettura di sistema

### 7.1 Visione alto livello

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENT DESKTOP (JavaFX)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐   ┌─────────────────┐ │
│  │   UI     │  │ Game Eng │  │   AI     │   │  Network        │ │
│  │ (JavaFX) │←→│ (shared) │  │ (shared) │   │  Adapters       │ │
│  └──────────┘  └──────────┘  └──────────┘   │  - REST/STOMP   │ │
│                                             │  - LAN client   │ │
│                                             │  - LAN host *   │ │
│                                             └────────┬────────┘ │
│  * LAN host wraps core-server with Jetty embedded    │          │
└──────────────────────────────────────────────────────┼──────────┘
                                                      │
                          ┌───────────────────────────┴──────────┐
                          │                                      │
                  HTTPS / WSS                              WS in LAN
                  (Internet)                            (mDNS discovery)
                          │                                      │
                          ▼                                      ▼
              ┌────────────────────────────┐    ┌─────────────────────────┐
              │   SERVER CENTRALE          │    │  ALTRO CLIENT (in HOST) │
              │  Spring Boot + MySQL       │    │  che espone core-server │
              │  ┌──────────────────────┐  │    │  via Jetty embedded     │
              │  │   core-server        │  │    │  ┌───────────────────┐  │
              │  │  (match, tournament) │  │    │  │   core-server     │  │
              │  └──────────────────────┘  │    │  │  (in-memory)      │  │
              │  + Auth (JWT), JPA, etc.   │    │  └───────────────────┘  │
              └────────────────────────────┘    └─────────────────────────┘
```

### 7.2 Modulo `core-server` come libreria riusabile

Il modulo `core-server` contiene:

- **Tournament Engine**: bracket generation (single elimination), round-robin scheduling, classifica, tie-breaker.
- **Match Manager**: ciclo di vita di un match, coordinamento turni, validazione tramite `RuleEngine`.
- **Event Bus**: pubblicazione di eventi su topic STOMP-compatibili.
- **Repository Ports**: interfacce `MatchRepository`, `TournamentRepository`, `UserRepository`, ecc.
- **Adapter in-memory** built-in (per LAN host).

Adapter forniti da chi lo incorpora:
- `client` (in modalità host LAN): in-memory + persistenza autosave su file.
- `server` (Internet): JPA + MySQL.

### 7.3 Architettura del client (esagonale)

Il client segue ports & adapters:

- **Domain layer**: `shared` (regole, modello, IA), `core-server` (quando attivo come host).
- **Application layer**: use case (orchestrazione), porte (interfacce).
- **Infrastructure layer**:
  - UI adapter (JavaFX).
  - REST adapter (HTTP client → server centrale).
  - STOMP adapter (WebSocket client → server centrale o → host LAN).
  - LAN discovery adapter (JmDNS).
  - LAN host adapter (Jetty + STOMP server, attivato on-demand).
  - Persistence adapter (file JSON locali).

### 7.4 Architettura del server centrale

Modularizzato per package:

```
server/
├── auth/          # JWT, login, registrazione, refresh
├── user/          # Profilo, statistiche
├── match/         # API REST + STOMP per match singoli
├── tournament/    # API REST + STOMP per tornei
├── repository/    # Adapter JPA delle port di core-server
└── config/        # Spring Security, OpenAPI, WebSocket, MySQL
```

### 7.5 State management dei match

Strategia comune (LAN e Internet):

- **Stato canonico** = log mosse append-only con sequence number monotonico per match.
- **Stato corrente** = materializzato (board, side-to-move, status) per query veloci.
- **Cache in-memory** (Caffeine sul server, mappe nel client host) per performance.
- Ogni mossa: validazione → applicazione → incremento sequence → persist → broadcast STOMP. Singola transazione applicativa con lock pessimistico per match.

### 7.6 Scalabilità (informativo)

V1 server monolitico. Per scalare:
- Match Manager partizionabile per `matchId`.
- Eventi cross-istanza via Redis Pub/Sub.
- Match identity persistente su MySQL (già v1).

---

## 8. Modello di dominio

### 8.1 Entità modulo `shared`

```java
// Coordinata 0..7 / 0..7 sulla damiera
public record Square(int file, int rank) { }

// Colore giocatore
public enum Color { WHITE, BLACK }

// Tipo di pezzo
public enum PieceKind { MAN, KING }   // Pedina, Dama

// Pezzo
public record Piece(Color color, PieceKind kind) { }

// Damiera immutabile
public final class Board {
    private final Piece[] squares;          // 64 caselle, 32 valide
    public Optional<Piece> at(Square s);
    public Board apply(Move m);
}

// Mossa (semplice o multipla)
public sealed interface Move permits SimpleMove, CaptureSequence {
    Square from();
    Square to();
    List<Square> capturedSquares();
}

// Stato completo della partita
public record GameState(
    Board board,
    Color sideToMove,
    int halfmoveClock,           // per regola 40 mosse
    List<Move> history,
    GameStatus status
) { }

// Status della partita: il ramo DRAW è materializzato in tre voci distinte
// così che UI, log e replay possano mostrare il motivo della patta.
public enum GameStatus {
    ONGOING,
    WHITE_WINS,
    BLACK_WINS,
    DRAW_REPETITION,        // triplice ripetizione
    DRAW_FORTY_MOVES,       // 40 mosse senza catture e senza spostamenti di pedina
    DRAW_AGREEMENT;         // accordo fra giocatori (online)

    public boolean isOngoing() { return this == ONGOING; }
    public boolean isWin()     { return this == WHITE_WINS || this == BLACK_WINS; }
    public boolean isDraw()    { return this == DRAW_REPETITION || this == DRAW_FORTY_MOVES || this == DRAW_AGREEMENT; }
}
```

> Nota: in Dama Italiana lo stallo (assenza di mosse legali per chi è al tratto)
> è una **sconfitta** del bloccato, NON una patta come negli scacchi (§3.6). Per
> questo non esiste una voce di stallo-patta in `GameStatus`.

### 8.2 Servizi di dominio

```java
public interface RuleEngine {
    List<Move> legalMoves(GameState state);
    GameState applyMove(GameState state, Move move) throws IllegalMoveException;
    GameStatus computeStatus(GameState state);
}
```

Algoritmo `legalMoves`:
1. DFS su tutte le sequenze di cattura possibili.
2. Se almeno una esiste, applica le 4 leggi italiane in ordine.
3. Se nessuna cattura, genera tutti i movimenti semplici.

### 8.3 Entità modulo `core-server`

```java
public record MatchId(String value) { }

public final class Match {
    MatchId id;
    UserRef white;
    UserRef black;
    GameState state;
    long sequenceNo;
    MatchStatus status;
    TimeControl timeControl;
    Instant startedAt;
    Optional<TournamentMatchRef> tournamentRef;   // null se match libero
}

public sealed interface MatchEvent {
    MatchId matchId();
    long sequenceNo();
    Instant timestamp();
}
record MoveApplied(MatchId, long, Instant, Move, GameState newState) implements MatchEvent {}
record MoveRejected(MatchId, long, Instant, String reason) implements MatchEvent {}
record DrawOffered(MatchId, long, Instant, UserRef from) implements MatchEvent {}
record DrawAccepted(MatchId, long, Instant) implements MatchEvent {}
record Resigned(MatchId, long, Instant, UserRef who) implements MatchEvent {}
record MatchEnded(MatchId, long, Instant, MatchResult result) implements MatchEvent {}
record PlayerDisconnected(MatchId, long, Instant, UserRef who) implements MatchEvent {}
record PlayerReconnected(MatchId, long, Instant, UserRef who) implements MatchEvent {}
```

```java
public sealed interface Tournament {
    TournamentId id();
    String name();
    TournamentStatus status();
    List<UserRef> participants();
    TimeControl timeControl();
}
record EliminationTournament(...) implements Tournament {
    BracketState bracket;
    int currentRound;
}
record RoundRobinTournament(...) implements Tournament {
    int pointsForWin;       // default 2
    int pointsForDraw;      // default 1
    int pointsForLoss;      // default 0
    Optional<LocalDate> deadline;
    List<RoundRobinMatch> schedule;     // tutti i match (giocati o no)
    Standings standings;                // classifica live
    TieBreakerPolicy tieBreakerPolicy;  // strategia tie-breaker
}

// Strategia configurabile per la risoluzione dei pari punti
public interface TieBreakerPolicy {
    /** Ordina i partecipanti a parità di punti. */
    List<UserRef> resolveTies(List<UserRef> tied, RoundRobinTournament tournament);
}
// Implementazione default: scontro diretto → Sonneborn-Berger → vittorie → sorteggio.
public final class StandardTieBreakerPolicy implements TieBreakerPolicy { ... }
```

### 8.4 Schema dati MySQL (server centrale)

Tabelle principali:

```sql
users (
  id BIGINT PK AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN'),
  created_at TIMESTAMP,
  last_login_at TIMESTAMP
);

matches (
  id BIGINT PK AUTO_INCREMENT,
  external_id VARCHAR(36) UNIQUE NOT NULL,    -- UUID per API
  white_user_id BIGINT FK,
  black_user_id BIGINT FK,
  status ENUM('WAITING','ONGOING','FINISHED','ABORTED'),
  time_control VARCHAR(50),
  current_state_json JSON,                    -- materializzato
  current_sequence_no BIGINT,
  result ENUM('WHITE_WINS','BLACK_WINS','DRAW','UNFINISHED'),
  tournament_match_id BIGINT FK NULL,
  started_at TIMESTAMP,
  finished_at TIMESTAMP NULL,
  INDEX(white_user_id), INDEX(black_user_id), INDEX(tournament_match_id)
);

match_events (
  id BIGINT PK AUTO_INCREMENT,
  match_id BIGINT FK NOT NULL,
  sequence_no BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  payload_json JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_match_seq (match_id, sequence_no)
);

tournaments (
  id BIGINT PK AUTO_INCREMENT,
  external_id VARCHAR(36) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  format ENUM('SINGLE_ELIMINATION','ROUND_ROBIN'),
  status ENUM('CREATED','IN_PROGRESS','FINISHED'),
  max_participants INT,
  time_control VARCHAR(50),
  points_for_win INT DEFAULT 2,
  points_for_draw INT DEFAULT 1,
  points_for_loss INT DEFAULT 0,
  deadline DATE NULL,
  created_by BIGINT FK,
  started_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL
);

tournament_rooms (
  id BIGINT PK AUTO_INCREMENT,
  tournament_id BIGINT FK,
  name VARCHAR(100),
  capacity INT
);

tournament_participants (
  id BIGINT PK AUTO_INCREMENT,
  tournament_id BIGINT FK,
  user_id BIGINT FK,
  room_id BIGINT FK NULL,
  seed INT,
  eliminated BOOLEAN DEFAULT FALSE,
  points INT DEFAULT 0,                       -- usato in round-robin
  UNIQUE KEY uk_t_u (tournament_id, user_id)
);

tournament_matches (
  id BIGINT PK AUTO_INCREMENT,
  tournament_id BIGINT FK,
  round_no INT NOT NULL,
  room_id BIGINT FK NULL,
  white_participant_id BIGINT FK,
  black_participant_id BIGINT FK,
  match_id BIGINT FK NULL,                    -- linka a matches quando il match è creato
  status ENUM('SCHEDULED','READY','IN_PROGRESS','FINISHED','FORFEITED'),
  scheduled_for TIMESTAMP NULL
);
```

---

## 9. Flussi end-to-end

> Questa sezione descrive nel dettaglio cosa succede in ogni scenario, dal click utente alla persistenza. È la sezione operativa di riferimento.

### 9.1 Avvio applicazione

**Cosa succede al lancio del client:**

1. Spring Boot non-web inizializza il container DI: legge `application.yml`, attiva i `@Component`, prepara i bean.
2. JavaFX `Application.start()` carica lo stylesheet `theme.css` (light o dark in base alla preferenza salvata).
3. Il `PreferencesService` carica `~/.dama-italiana/config.json`: lingua, tema, eventuale token JWT cifrato.
4. La schermata splash mostra logo per 1-2 secondi mentre completa il bootstrap.
5. Se è presente un token JWT valido → richiesta `/users/me` al server centrale; in caso di successo, si va alla **Home autenticata**. Altrimenti **Home guest**.
6. Se è presente l'autosave single-player (`~/.dama-italiana/saves/_autosave.json`), prompt "Riprendi partita interrotta?".

**Home (main menu):**
- Card 1: "Single Player" (sempre disponibile, anche offline).
- Card 2: "LAN" (sempre disponibile, anche offline).
- Card 3: "Online" (richiede login; se guest, prompt registrazione/login).
- Card 4: "Regole" (apre la sezione regole in-app).
- Card 5: "Profilo" (visibile se loggato; statistiche, storia partite).
- Card 6: "Impostazioni".

### 9.2 Single-player vs IA

#### 9.2.1 Nuova partita

1. Utente clicca "Single Player" → "Nuova partita".
2. Modal di setup: livello difficoltà (Principiante / Esperto / Campione), colore (Bianco / Nero / Casuale), nome partita (default: "Partita del DD MMM HH:MM").
3. Conferma → il client crea un nuovo `GameState` con board iniziale.
4. Si entra nel **Board view**.
5. Il Bianco muove per primo. Se il giocatore è Bianco, il client attende click su una pedina propria. Altrimenti, l'IA pianifica la prima mossa.
6. Quando il giocatore clicca su una propria pedina:
   - Il client invoca `RuleEngine.legalMoves()` filtrato sul pezzo selezionato.
   - Le case di destinazione sono evidenziate in giallo. Le sequenze di cattura obbligatorie sono evidenziate in rosso pulsante.
7. Click sulla casa di destinazione → `RuleEngine.applyMove()` → animazione 250 ms → autosave → IA inizia a calcolare.
8. L'IA gira su un **virtual thread**, è cancellabile, deve completare entro il timeout del livello (vedi 12.2).
9. La cronologia mosse nel pannello laterale si aggiorna con la notazione FID.

#### 9.2.2 Salvataggio multi-slot

**Salva manuale:**
1. Utente apre menu "Partita" → "Salva con nome".
2. Modal con campo nome (precompilato col nome corrente).
3. Click "Salva" → serializzazione `GameState` + metadata in `~/.dama-italiana/saves/<slug>.json`.
4. Toast "Partita salvata".

**Carica:**
1. Utente apre menu "Partita" → "Carica".
2. Schermata con lista partite salvate, ordinabile per nome / data / livello.
3. Ogni voce mostra: nome, data ultima modifica, livello IA, colore giocatore, mossa corrente, miniatura board.
4. Click → conferma "Carica `<nome>`?" → deserializzazione → ingresso in Board view nello stato salvato.

**Autosave:**
- Ad ogni mossa applicata, il client scrive `~/.dama-italiana/saves/_autosave.json`.
- Quando l'utente esce esplicitamente con "Termina partita", l'autosave viene cancellato.
- Se invece chiude la finestra senza terminare, l'autosave persiste e al prossimo avvio appare il prompt di ripresa.

**Formato file:**
```json
{
  "schemaVersion": 1,
  "kind": "SINGLE_PLAYER_GAME",
  "name": "Partita del 12 aprile vs Esperto",
  "createdAt": "2026-04-12T20:15:00",
  "updatedAt": "2026-04-12T20:42:13",
  "aiLevel": "ESPERTO",
  "humanColor": "WHITE",
  "moves": [ "12-16", "21-17", "..." ],
  "currentState": { "board": "...", "sideToMove": "BLACK", "halfmoveClock": 5 }
}
```

#### 9.2.3 Visualizzazione regole

1. Click su icona ⓘ in-game o card "Regole" dalla home.
2. Schermata regole: navigazione laterale per sezione (setup, movimento, cattura, leggi, promozione, fine partita, notazione).
3. Ogni sezione contiene testo + diagrammi statici della damiera (renderizzati con lo stesso renderer del board view, configurato in modalità "snapshot").
4. Per cattura semplice, presa multipla, promozione: animazione dimostrativa che ricicla l'animation engine del board.

### 9.3 LAN — partita singola

#### 9.3.1 Hosting

1. Utente clicca "LAN" → "Ospita partita".
2. Modal: nome host (default = nome utente sistema), time control, colore preferito, eventuale password.
3. Conferma → il client:
   - Avvia il container Jetty embedded (se non già attivo) su una porta libera (es. 9876).
   - Avvia un'istanza locale di `core-server` con repository in-memory.
   - Crea un `Match` in stato `WAITING`.
   - Pubblica via JmDNS il servizio `_dama-match._tcp.local` con metadata: nome host, porta, time control, password-required (boolean), `matchId`.
4. UI mostra "In attesa di un avversario..." con possibilità di copiare `host:port` per condivisione manuale.

#### 9.3.2 Discovery e join

1. Utente clicca "LAN" → "Cerca partita".
2. Il client lancia una query JmDNS per `_dama-match._tcp.local`.
3. Lista degli host scoperti (refresh ogni 3 secondi). Ogni voce: nome host, time control, password? sì/no.
4. Inserimento manuale `host:port` come fallback se mDNS è bloccato dalla rete.
5. Click su un host → connessione WebSocket a `ws://<host>:<port>/ws`.
6. Handshake STOMP `CONNECT` con header custom `username` (e `password` se richiesta).
7. Server (host) genera un **session token** in memoria, valido per la durata della connessione.
8. Server emette `MatchReady` su `/topic/match/{id}` → entrambi i client passano al Board view.

#### 9.3.3 Gioco

Identico al flusso single-player nella UI, con queste differenze:
- Le mosse del giocatore sono inviate via STOMP a `/app/match/{id}/move`.
- Il Match Manager dell'host valida (RuleEngine), applica, scrive nel log mosse, incrementa sequence, broadcasta `MoveApplied` su `/topic/match/{id}`.
- Entrambi i client ricevono l'evento e aggiornano la UI.
- Chat su `/topic/match/{id}/chat`, send su `/app/match/{id}/chat`.
- Resign, draw offer, draw response come Internet (vedi 9.5).

#### 9.3.4 Disconnessione e riconnessione

- Heartbeat STOMP ogni 10 secondi. Se il server non riceve heartbeat per 30 secondi, marca il client come disconnesso ed emette `PlayerDisconnected`.
- Il client che si disconnette tenta riconnessione automatica per 60 secondi (intervalli con backoff).
- Alla riconnessione: rinegoziazione handshake, recupero eventi mancati via `GET /matches/{id}/events?since={lastSeq}` (endpoint REST esposto anche dall'host LAN).
- Dopo 60 secondi di mancata riconnessione: forfait automatico, `MatchEnded` con risultato per l'altro giocatore.

#### 9.3.5 Salvataggio LAN match

L'host scrive lo snapshot del match in `~/.dama-italiana/lan-host/<matchId>.json` ad ogni evento. Se l'host crasha:
- Alla riapertura, prompt "Trovato match LAN interrotto, riprendere?".
- Se accetta, il `core-server` viene reidratato dallo snapshot e dal log mosse.
- L'host ripubblica il servizio mDNS con lo stesso `matchId`.
- Il client ospite (se ancora attivo o riaperto) può riscoprire l'host e riconnettersi.

### 9.4 LAN — torneo

#### 9.4.1 Creazione torneo

1. Utente clicca "LAN" → "Ospita torneo".
2. Setup wizard:
   - Step 1: nome torneo, formato (eliminazione diretta / round-robin).
   - Step 2 (eliminazione): numero partecipanti attesi (4/8/16/32).
   - Step 2 (round-robin): numero partecipanti, sistema di punteggio (default 2-1-0).
   - Step 3: time control per i match.
   - Step 4: password opzionale.
3. Conferma → il client:
   - Avvia Jetty embedded.
   - Avvia `core-server` con `TournamentEngine` in-memory.
   - Crea il torneo in stato `CREATED`.
   - Pubblica via JmDNS `_dama-tournament._tcp.local` con metadata: nome, formato, slot, password.
4. L'host stesso si registra come primo partecipante (se vuole giocare).

#### 9.4.2 Iscrizione partecipanti

1. Altri client cercano `_dama-tournament._tcp.local`, vedono il torneo, cliccano "Iscriviti".
2. Connessione WebSocket → handshake con username.
3. Server (host) registra il partecipante. UI dell'host mostra in tempo reale la lista che si popola.
4. Quando l'host clicca "Avvia torneo":
   - **Eliminazione diretta**: bracket generato per il numero attuale di partecipanti (potenze di 2 con byes ai seed più alti se necessario). Stanze create per ogni round (Round 1, Round 2, ecc.). Match del Round 1 creati in stato `READY`.
   - **Round-robin**: schedule di tutti gli accoppiamenti generato. Una sola "stanza" logica con tutti i match. Match in stato `SCHEDULED` (giocabili in qualsiasi ordine).
5. Broadcast `TournamentStarted` su `/topic/tournament/{id}`.

#### 9.4.3 Gioco match in torneo (eliminazione)

1. Tournament Engine notifica i due partecipanti del Round 1 con messaggio personalizzato su `/user/queue/notifications`: "Il tuo match è pronto, Bianco/Nero, controparte X".
2. Il client mostra modal "Match in arrivo - Inizia". Timer di 3 minuti per accettazione (configurabile). Se uno dei due non si presenta entro il timeout, l'altro vince per forfait.
3. All'accettazione: il match passa a `IN_PROGRESS`, il flusso è identico al match singolo LAN.
4. Alla conclusione: `MatchEnded` → Tournament Engine aggiorna il bracket → se tutti i match del round sono finiti, avanza al round successivo creandolo come nuova stanza.
5. Quando rimane un solo partecipante, `TournamentEnded` con classifica.

#### 9.4.4 Gioco match in torneo (round-robin)

1. Ogni partecipante vede una lista personale dei propri match: "Da giocare" (con avversari ancora da affrontare) e "Giocati".
2. Per giocare un match: il partecipante clicca "Sfida" su un avversario. Il sistema invia notifica all'avversario.
3. Se entrambi sono online e accettano, il match viene creato in stato `IN_PROGRESS`. Altrimenti la sfida resta pending.
4. In LAN, considerando che tipicamente i giocatori sono presenti contemporaneamente, il flusso si traduce in: scelta libera dell'ordine dei match, esecuzione in qualsiasi sequenza.
5. Alla conclusione di ogni match: punteggio aggiornato, classifica `Standings` ricalcolata, broadcast `StandingsUpdated` su `/topic/tournament/{id}`.
6. Quando tutti i match sono completati, `TournamentEnded`.

#### 9.4.5 Spettatori

- Ogni partecipante (e l'host) può aprire un altro match della stessa stanza per osservarlo.
- Subscription a `/topic/match/{matchId}` come read-only: il client riceve gli eventi mossa ma il pannello azioni è nascosto.
- Multipli match osservabili in tab.

#### 9.4.6 Salvataggio host del torneo

A ogni evento significativo (match concluso, round avanzato, partecipante iscritto), l'host scrive in `~/.dama-italiana/lan-host/<tournamentId>.json` lo snapshot completo:

```json
{
  "schemaVersion": 1,
  "kind": "LAN_TOURNAMENT",
  "tournament": { "id": "...", "name": "Domenica sera", "format": "ROUND_ROBIN", ... },
  "participants": [ ... ],
  "matches": [
    { "id": "m-001", "white": "...", "black": "...", "status": "FINISHED",
      "result": "WHITE_WINS", "moves": [ ... ] },
    { "id": "m-002", "white": "...", "black": "...", "status": "IN_PROGRESS",
      "moves": [ ... ], "currentSequence": 17 }
  ],
  "standings": [ ... ]
}
```

Alla riapertura del client: scansione di `~/.dama-italiana/lan-host/`. Se ci sono tornei `IN_PROGRESS`, prompt "Riprendi torneo `<nome>`?". I match in corso vengono reidratati e i partecipanti possono riconnettersi.

#### 9.4.7 Export/import bracket (backup)

- Comando dell'host: "Esporta torneo". Crea un file scaricabile dal disco con lo stesso JSON dello snapshot.
- Comando di un altro client: "Importa torneo". Carica un torneo da file e diventa il nuovo host. I partecipanti devono manualmente riscoprire il nuovo host via mDNS. È una funzione di emergenza, non un flusso primario.

### 9.5 Internet — partita singola

#### 9.5.1 Registrazione e login

1. Schermata login: campi username/email + password, oppure "Continua come guest", oppure "Crea account".
2. Registrazione: form con username, email, password, conferma password. Validazione client-side, poi `POST /v1/auth/register`. Server crea utente con BCrypt hash. Risposta con access + refresh token.
3. Login: `POST /v1/auth/login`, riceve access (15min) e refresh (7gg). Token salvati in `config.json` cifrati con la machine key.
4. Refresh automatico: il client intercetta 401, chiama `POST /v1/auth/refresh`, riprova la richiesta originale.

#### 9.5.2 Lobby e match singolo

1. Utente clicca "Online" → entra nella lobby pubblica.
2. UI mostra:
   - Lista match aperti (paginata, refresh ogni 5 secondi via REST oppure WebSocket subscription a `/topic/lobby` per push real-time).
   - Pulsanti "Crea match", "Match privato (codice)", "Sfida amico".
   - Tab "Le mie partite in corso" se ci sono match attivi non completati.
3. **Creazione match pubblico**:
   - `POST /v1/matches` con time control, color preference, visibility=public.
   - Match creato in stato `WAITING`. Cliente sottoscrive `/topic/match/{id}`.
   - Quando un secondo giocatore fa join, il server emette `MatchReady` ed entrambi entrano nel Board view.
4. **Match privato**:
   - Stesso flusso ma visibility=private; il server genera un codice 6 caratteri.
   - L'invitato inserisce il codice in lobby, fa `POST /v1/matches/{id}/join?code=XXX`.
5. **Sfida amico**: se conosci lo username, `POST /v1/matches?invite_username=...`. L'invitato riceve notifica push via `/user/queue/notifications`.

Il gioco si svolge come in LAN, ma la connessione è verso `wss://api.dama-italiana.example/ws`.

#### 9.5.3 Riconnessione

- Se il client perde connessione (rete instabile, app chiusa), il server mantiene il match attivo per 5 minuti.
- All'evento `PlayerDisconnected` viene avviato un timer server-side. L'altro giocatore vede notifica "Avversario disconnesso, attesa riconnessione".
- Riconnessione automatica del client per 2 minuti con backoff esponenziale.
- Alla riconnessione: nuovo handshake, `GET /v1/matches/{id}/events?since={lastSeq}`, replay degli eventi mancati, ripresa partita.
- Dopo 5 minuti senza riconnessione: forfait automatico server-side.

#### 9.5.4 Storia partite e replay

- `GET /v1/users/me/matches?status=finished` paginato.
- Click su un match → schermata replay con board view in modalità "navigazione". Pulsanti: prima mossa, indietro, avanti, ultima mossa, autoplay.
- Il replay carica il log mosse via `GET /v1/matches/{id}/events` e ricostruisce gli stati intermedi.

### 9.6 Internet — torneo eliminazione diretta

#### 9.6.1 Creazione

1. Solo utenti con ruolo `TOURNAMENT_ADMIN` (o creatore del torneo nel modello permissive). `POST /v1/tournaments` con `format=SINGLE_ELIMINATION`, max_participants, time_control, ecc.
2. Torneo creato in stato `CREATED`. Visibile in `GET /v1/tournaments`.
3. Gli utenti si iscrivono con `POST /v1/tournaments/{id}/join`.
4. L'admin avvia con `POST /v1/tournaments/{id}/start`. Bracket generato (con byes se serve), Round 1 creato come stanza con i match.

#### 9.6.2 Esecuzione

- Al start, il sistema notifica i partecipanti del Round 1 via `/user/queue/notifications`.
- Match creati con tutti i metadata. Quando entrambi i giocatori sono online e accettano, il match passa `IN_PROGRESS`.
- Forfeit dopo 3 minuti di mancata accettazione.
- Conclusione match → bracket aggiornato → Round 2 creato quando tutti i match del Round 1 sono terminati.
- Spettatorialità: chi non è impegnato in un match attivo può guardare gli altri della stessa stanza.

### 9.7 Internet — campionato round-robin

Il flusso più articolato perché può durare giorni o settimane.

#### 9.7.1 Creazione

1. `POST /v1/tournaments` con `format=ROUND_ROBIN`, `points_for_win=2`, `points_for_draw=1`, `points_for_loss=0`, opzionalmente `deadline=YYYY-MM-DD`.
2. Iscrizione partecipanti.
3. Quando il torneo viene avviato, il **Tournament Engine** genera l'intero schedule:
   - Per N partecipanti, ogni partecipante affronta tutti gli altri → N×(N-1)/2 match.
   - I match vengono creati in stato `SCHEDULED` (non `READY`): non hanno una data prefissata, sono giocabili appena entrambi i partecipanti sono online e accettano.
   - Opzionale: se l'admin definisce una `deadline`, i match non giocati entro la deadline vengono dichiarati `FORFEITED` con regole configurabili (es. doppia patta, sconfitta per entrambi, ecc.).

#### 9.7.2 Visualizzazione del torneo

Il partecipante apre la schermata torneo e vede:
- **Tab "Classifica"**: lista partecipanti ordinata per punti, con tie-breaker visibili.
- **Tab "I miei match"**: tabella personale con tutti i match (giocati, in corso, ancora da giocare). Per ogni match: avversario, status, risultato (se concluso), data (se concluso), pulsante "Sfida" (se da giocare e l'avversario è online).
- **Tab "Tutti i match"**: vista globale con filtro per partecipante e per status.
- **Tab "Calendario"** (opzionale): se l'admin ha definito deadline o calendario per round, qui appare.

#### 9.7.3 Iniziare un match round-robin

1. Partecipante A clicca "Sfida" sul partecipante B nella tab "I miei match".
2. Sistema verifica:
   - B è online (presence tracking via WebSocket session).
   - Il match A-B esiste e ha status `SCHEDULED`.
3. Notifica push a B: "A vuole giocare il vostro match del torneo X. Accetti?".
4. B accetta → match passa a `READY` → entrambi entrano nel Board view → match parte come da flusso standard.
5. Se B rifiuta o non risponde entro 60 secondi, l'invito decade. Il match resta `SCHEDULED`.

#### 9.7.4 Conclusione di un match round-robin

1. Match concluso → MatchEnded.
2. Tournament Engine aggiorna i punti dei due partecipanti secondo il sistema (default vittoria=2, patta=1, sconfitta=0).
3. Ricalcolo classifica con tie-breaker applicati nell'ordine:
   1. **Scontro diretto** tra i giocatori a pari punti (chi ha vinto il match diretto, se applicabile).
   2. **Sonneborn-Berger**: somma pesata dei punti degli avversari battuti (peso 1.0) e di quelli pattati (peso 0.5).
   3. **Numero di vittorie totali**.
   4. **Sorteggio**.
4. Broadcast `StandingsUpdated` su `/topic/tournament/{id}`.
5. Se tutti i match sono completati → `TournamentEnded` con classifica finale archiviata.

#### 9.7.5 Gestione deadline e forfeit

- Job schedulato server-side (Spring Scheduler, cron giornaliero) che controlla i tornei con `deadline` superata.
- Match `SCHEDULED` non giocati alla deadline:
  - Se l'admin ha configurato "forfait reciproco": sconfitta a entrambi (0-0).
  - Se l'admin ha configurato "patta automatica": 1-1 ai partecipanti.
  - Default: l'admin viene notificato e decide manualmente.

### 9.8 Funzionalità trasversali

#### 9.8.1 Resign e draw offer

- **Resign**: pulsante in board view → conferma → `SEND /app/match/{id}/resign` → `MatchEnded` con vittoria avversaria.
- **Draw offer**: pulsante "Offri patta" → `SEND /app/match/{id}/draw-offer` → `DrawOffered` broadcastato. L'avversario riceve modal "Patta offerta, accetti?".
- Se accetta: `SEND /app/match/{id}/draw-response {accept:true}` → `DrawAccepted` → `MatchEnded` con `DRAW`.
- Se rifiuta o ignora: il gioco continua. Si può offrire patta al massimo una volta ogni 10 mosse (anti-spam).

#### 9.8.2 Chat

- Chat per match: send su `/app/match/{id}/chat`, ricevuti su `/topic/match/{id}/chat`.
- Anti-spam server-side: max 5 messaggi in 10 secondi per utente per match.
- Filtro parolacce server-side (lista configurabile).
- Storage: i messaggi chat NON sono archiviati persistentemente (conservati in memoria per la durata del match, poi eliminati).

#### 9.8.3 Anti-cheat

- Tutte le mosse passate al `RuleEngine` server-side.
- Mossa illegale → `MoveRejected` evento, stato non modificato.
- 5 mosse illegali consecutive da uno stesso giocatore → forfait automatico.
- Tentativi di alterazione del client (modifica del JS in caso di future versioni web): out of scope per v1.

#### 9.8.4 Notifiche

- Le notifiche personali (`/user/queue/notifications`) coprono: invito a match, match torneo pronto, tua sfida accettata, classifica torneo aggiornata, ecc.
- Il client mostra un badge sulla campanella + pannello notifiche.
- In Internet, le notifiche sono persistite server-side finché non lette.
- In LAN, le notifiche sono volatili (se il client è chiuso quando l'evento è generato, è perso).

---

## 10. (sezione fusa con la 9 — flussi)

---

## 11. Specifiche di rete

### 11.1 LAN — protocollo

- **Discovery**: mDNS su porta 5353 multicast 224.0.0.251. Tipi servizio: `_dama-match._tcp.local`, `_dama-tournament._tcp.local`. JmDNS.
- **Transport**: WebSocket (cleartext WS in LAN, accettabile considerando la rete privata).
- **Protocollo applicativo**: STOMP 1.2 — identico a Internet.
- **Auth**: username + password opzionale al CONNECT; session token volatile.
- **Endpoint**: `ws://<host>:<port>/ws`.
- **REST companion endpoint** sull'host (per replay eventi): `http://<host>:<port>/v1/matches/{id}/events?since=N`.

### 11.2 Internet — REST API

Base URL: `https://api.dama-italiana.example/v1`. Bearer JWT in `Authorization`. Documentazione OpenAPI 3.1 esposta su `/v3/api-docs` e UI Swagger su `/swagger-ui`.

**Auth & utenti**

| Metodo | Path | Descrizione |
|---|---|---|
| POST | `/auth/register` | Registrazione |
| POST | `/auth/login` | Login → access + refresh |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/logout` | Invalida refresh token |
| GET | `/users/me` | Profilo |
| GET | `/users/{id}` | Profilo pubblico |
| GET | `/users/me/stats` | Statistiche personali |
| GET | `/users/me/matches` | Storia partite |

**Match**

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/matches` | Lista lobby (filtri: status, time_control) |
| POST | `/matches` | Crea match |
| GET | `/matches/{id}` | Dettaglio |
| GET | `/matches/{id}/events?since=N` | Replay eventi |
| POST | `/matches/{id}/join` | Entra come secondo giocatore |
| POST | `/matches/{id}/resign` | Abbandona |
| POST | `/matches/{id}/draw-offer` | Offre patta |
| POST | `/matches/{id}/draw-response` | Risposta patta |

**Tornei**

| Metodo | Path | Descrizione |
|---|---|---|
| GET | `/tournaments` | Lista tornei |
| POST | `/tournaments` | Crea torneo |
| GET | `/tournaments/{id}` | Dettaglio + bracket/standings |
| POST | `/tournaments/{id}/join` | Iscrizione |
| POST | `/tournaments/{id}/start` | Avvia (admin) |
| GET | `/tournaments/{id}/rooms` | Stanze |
| GET | `/tournaments/{id}/standings` | Classifica (round-robin) |
| GET | `/tournaments/{id}/matches` | Tutti i match del torneo |
| POST | `/tournaments/{id}/matches/{mid}/challenge` | Avvia sfida (round-robin) |

### 11.3 STOMP topics

**Subscriptions (server → client)**

| Topic | Descrizione |
|---|---|
| `/topic/lobby` | Aggiornamenti lobby pubblica (match aperti) |
| `/topic/match/{id}` | Eventi match (mossa, fine, resign, ecc.) |
| `/topic/match/{id}/chat` | Chat |
| `/topic/tournament/{id}` | Eventi torneo (start, end, standings update) |
| `/topic/tournament/{id}/room/{rid}` | Eventi stanza (match assigned, ecc.) |
| `/user/queue/notifications` | Notifiche personali |

**Destinations (client → server)**

| Destination | Payload |
|---|---|
| `/app/match/{id}/move` | `{ from, to, capturedSquares[] }` |
| `/app/match/{id}/resign` | `{}` |
| `/app/match/{id}/draw-offer` | `{}` |
| `/app/match/{id}/draw-response` | `{ accept: bool }` |
| `/app/match/{id}/chat` | `{ text }` |
| `/app/tournament/{id}/match/{mid}/challenge` | `{}` |
| `/app/tournament/{id}/match/{mid}/challenge-response` | `{ accept: bool }` |

### 11.4 Schema eventi

Formato uniforme:

```json
{
  "type": "MOVE_APPLIED",
  "matchId": "uuid",
  "sequenceNo": 42,
  "timestamp": "2026-04-26T10:00:00Z",
  "payload": { "move": { "from": 12, "to": 19, "captured": [16] }, "newState": { ... } }
}
```

Tipi: `MOVE_APPLIED`, `MOVE_REJECTED`, `DRAW_OFFERED`, `DRAW_ACCEPTED`, `DRAW_DECLINED`, `RESIGNED`, `MATCH_ENDED`, `PLAYER_DISCONNECTED`, `PLAYER_RECONNECTED`, `CHAT`, `TOURNAMENT_STARTED`, `TOURNAMENT_ENDED`, `STANDINGS_UPDATED`, `BRACKET_UPDATED`, `MATCH_ASSIGNED`, `CHALLENGE_REQUESTED`, `CHALLENGE_ACCEPTED`, `CHALLENGE_DECLINED`.

---

## 12. Motore IA

### 12.1 Algoritmo

Base: **Minimax con alpha-beta pruning**. Ottimizzazioni: iterative deepening, transposition table (Zobrist hashing), move ordering (cattura > mossa centrale > altre).

Funzione di valutazione (heuristic):
- Materiale: pedina = 100, dama = 300.
- Mobilità: numero mosse legali.
- Posizione: avanzamento verso l'ultima riga.
- Sicurezza: pezzi sui bordi protetti.
- Centro: bonus controllo case centrali.

### 12.2 Livelli

| Livello | Profondità | Tempo max per mossa | Caratteristiche |
|---|---|---|---|
| Principiante | 2 ply | 500 ms | Probabilità 25% di scegliere mossa subottima (rumore) |
| Esperto | 5 ply | 2000 ms | Move ordering, alpha-beta, mossa sempre ottimale entro la profondità |
| Campione | 8 ply | 5000 ms | + iterative deepening + transposition table |

L'IA gira su un **virtual thread** (Java 21), cancellabile, con timeout. Non blocca la UI.

---

## 13. Interfaccia utente

### 13.1 Schermate

1. Splash & login.
2. Home (main menu) — quattro card.
3. Single-player setup.
4. Board view — damiera centrale, pannello laterale (giocatori, tempi, cronologia, chat).
5. Lobby online.
6. Tournament hub.
7. Tournament room (con tab classifica / bracket / match list).
8. Profilo & statistiche.
9. Storia partite + replay viewer.
10. Sezione regole.
11. Settings.
12. LAN host setup.
13. LAN tournament admin view.

### 13.2 Design system custom

JavaFX puro, senza temi esterni. Si scrive un `theme.css` in `src/main/resources/css/` parametrico tramite CSS variables JavaFX (`-fx-*`).

**Direzione estetica (da Fase 3.5)**: **"videogame premium wood"**. La UI deve evocare un gioco da tavolo classico premium con materiali "fisici" simulati (legno, ottone/oro, feltro) — non un software gestionale moderno e neutro. Token color, tipografia, ombre e motion sono allineati a questa direzione.

**Design token** definiti come CSS variables:

```css
.root {
    /* Palette light "wood premium" (da Fase 3.5) */
    -color-bg-primary: #2A1F15;        /* dark roast — sfondo principale */
    -color-bg-surface: #3D2E20;        /* deep walnut — superfici secondarie */
    -color-bg-elevated: #F5E6C8;       /* cream parchment — card, popover */
    -color-border-subtle: #6B4423;     /* warm brown — bordi sottili */
    -color-border-frame: #4A2E18;      /* dark frame — cornici e separatori */
    -color-text-on-dark: #F0E0C4;      /* cream — testo su sfondo scuro */
    -color-text-on-light: #2A1F15;     /* dark roast — testo su sfondo chiaro */
    -color-text-secondary: #8B6F4E;    /* muted brown — testo secondario */
    -color-accent-gold: #C9A45C;       /* antique gold — accent primario */
    -color-accent-gold-hover: #DDB874; /* gold hover */
    -color-accent-deep-red: #8B3A3A;   /* deep red — accent secondario, dame */
    -color-success: #6B8E4E;           /* moss green — toni naturali */
    -color-warning: #C9A45C;           /* gold — coerente con palette */
    -color-danger: #8B3A3A;            /* deep red — coerente */

    /* Damiera (texture-driven, vedi 13.2.1) */
    -color-board-light: #E8C99A;       /* fallback prima del caricamento texture */
    -color-board-dark: #6B4423;        /* fallback */
    -color-piece-white: #F0E0C4;       /* cream */
    -color-piece-black: #2A1F15;       /* dark roast */
    -color-piece-king-marker-white: #C9A45C;  /* gold marker su pezzo bianco */
    -color-piece-king-marker-black: #8B3A3A;  /* deep red marker su pezzo nero */
    -color-highlight-legal: #C9A45C;          /* gold glow */
    -color-highlight-mandatory: #C9A45C;      /* gold halo (vedi 13.3) */

    /* Tipografia: display serif + UI sans */
    -font-family-display: "Playfair Display", "Cormorant Garamond", "Georgia", serif;
    -font-family-base: "Inter", "Segoe UI", "Helvetica Neue", sans-serif;
    -font-size-xs: 12px;
    -font-size-sm: 14px;
    -font-size-md: 16px;
    -font-size-lg: 20px;
    -font-size-xl: 24px;
    -font-size-display-md: 32px;       /* titoli card */
    -font-size-display-lg: 48px;       /* titoli schermata */

    /* Spacing (multiples of 4) */
    -spacing-1: 4px; -spacing-2: 8px; -spacing-3: 12px;
    -spacing-4: 16px; -spacing-5: 24px; -spacing-6: 32px;

    /* Radius */
    -radius-sm: 4px; -radius-md: 8px; -radius-lg: 12px;

    /* Motion (da Fase 3.5) */
    -easing-out-quad: cubic-bezier(0.25, 0.46, 0.45, 0.94);     /* default ease-out */
    -easing-out-back: cubic-bezier(0.34, 1.56, 0.64, 1);        /* juicy overshoot per atterraggio mossa */
}

.root.dark {
    /* Variant dark — riservato a Fase 11 (toggle runtime); placeholder. */
    -color-bg-primary: #1A120A;
    -color-bg-surface: #2A1F15;
}
```

**13.2.1 Asset visuali della tavola**: la tavola usa **texture legno** (warm brown chiaro/scuro alternati con venature visibili) caricate da `client/src/main/resources/assets/textures/` come `BackgroundImage`, **non** colori piatti. Cornice (`-color-border-frame`) attorno alla griglia 8×8 per evocare un tavoliere fisico. Asset solo CC0 o CC-BY (vedi 13.2.3).

**13.2.2 Stati interattivi**: default, `:hover` (glow gold + lift -2px), `:pressed`, `:focused`, `:disabled`. Transizioni 200-250 ms ease-out di default; mossa pezzo usa `out-back` (vedi §13.3).

**13.2.3 Asset licensing**: tutti gli asset visuali e audio sono **CC0** preferibilmente, o CC-BY con attribution rigorosa. Inventario in `client/src/main/resources/assets/CREDITS.md`. Vietati asset senza licenza esplicita o CC-BY-NC (non utilizzabili commercialmente).

**Font**: `Inter` (UI) caricato via `Font.loadFont()` da `client/src/main/resources/fonts/`; `Playfair Display` (display) aggiunto in Fase 3.5, stessa modalità di caricamento.

**Ombre**: bevel multilivello per bottoni (`inset 0 1px 0 rgba(255,255,255,0.2), 0 2px 4px rgba(0,0,0,0.3)`) + dropshadow soft per card.

### 13.3 Animazioni

**Animazioni base (Fase 3)**:
- Mossa pezzo: `TranslateTransition` 250 ms easing OUT_QUAD.
- Cattura: `ScaleTransition` 200 ms scale-out + `FadeTransition` parallela.
- Promozione: flash dorato + `RotateTransition` 500 ms.
- Highlight cattura obbligatoria: pulsazione `FillTransition` ciclica 800 ms.

**Animazioni avanzate (da Fase 3.5)**:
- **Mossa pezzo "juicy"**: l'easing OUT_QUAD è sostituito da **OUT_BACK** (`cubic-bezier(0.34, 1.56, 0.64, 1)`) per atterraggio con leggero overshoot, che dà sensazione di peso e impatto fisico. Durata invariata 250 ms.
- **Particle puff su cattura**: 8-12 piccole particle (`Circle` color marrone/grigio polvere, raggio 2-4px) emesse dal centro del pezzo catturato, con `ParallelTransition(TranslateTransition radiale + FadeTransition out + ScaleTransition shrink)` durata ~350 ms. Sovrapposto allo `ScaleTransition`+`FadeTransition` esistenti. Pulizia automatica a fine animazione.
- **Raggi dorati su promozione**: 8-12 `Line` o `Polygon` triangolari (color `#C9A45C` accent gold + `#DDB874`) irradianti dal centro del pezzo promosso, con `ParallelTransition(ScaleTransition expand + FadeTransition out)` 600 ms. Sovrapposto al flash dorato esistente + `RotateTransition`.
- **Glow halo cattura obbligatoria**: il `FillTransition` 800 ms è **affiancato/sostituito** da un `DropShadow` esterno animato (`Timeline` cycle infinito 1200 ms, radius 12→24→12, color accent gold) che evoca un alone di luce attorno al pezzo. Più appariscente del solo pulsare interno.
- **No camera shake**: deliberatamente escluso per non distrarre dal piano di gioco classico.

I parametri esatti (count particle, durata ms, curve di easing) sono finalizzati nel `PLAN-fase-3.5.md` e nelle ADR-034/035.

### 13.4 Audio

**SFX di gameplay (Fase 3 + 3.5)**: JavaFX Media. Suoni discreti: click mossa, tonfo legno cattura, chime promozione, fanfara breve vittoria. Mutabili separatamente. Default volume SFX 70%.

**Music ambient (da Fase 3.5)**: 3-5 tracce **orchestrali soft / chamber** in playlist random shuffle, loop continuo a fine playlist. Una sola traccia in playback alla volta (no overlap). Default volume musica **30%**, mutabile via Settings. Persistence dei volumi e dello stato muted in `~/.dama-italiana/config.json` (campi `musicVolumePercent`, `sfxVolumePercent`, `musicMuted`, `sfxMuted`). Asset CC0/CC-BY (vedi §13.2.3).

Stile musicale: tranquillo, da concentrazione (no battle/heroic). Implementazione tramite `client/audio/AudioService` (vedi ADR-035).

### 13.5 Accessibilità

- `accessibleText` su ogni pezzo per screen reader.
- Modalità daltonismo: pattern (linee, puntini) sui pezzi oltre al colore.
- Scaling UI 100% / 125% / 150% (preferenza salvata).

> **Implementazione per fase**: Fase 3 implementa `accessibleText` (formato univoco screen-reader-friendly inglese, stabile e indipendente dal locale attivo) + scaling UI 100/125/150 + keyboard navigation board. **Deferred a Fase 11** (vedi §16): (a) localizzazione IT/EN dei testi `accessibleText` (NFR-U-01 — il deferral è giustificato dal fatto che ogni cell read screen-reader riprodurrebbe la stessa frase da ~30 chiavi i18n aggiunte; valutazione costi/benefici a F11), (b) pattern daltonismo sui pezzi (feature di a11y avanzata).

### 13.6 Localizzazione

- ResourceBundle `messages_it.properties` / `messages_en.properties`.
- Tutti i testi UI passano da `MessageSource` Spring.

---

## 14. Persistenza e dati

### 14.1 Lato client (file system)

```
~/.dama-italiana/
├── config.json                          # preferenze utente, token JWT cifrato
├── saves/                               # single-player saves (multi-slot)
│   ├── _autosave.json
│   ├── partita-12-aprile-vs-esperto.json
│   └── ...
├── lan-host/                            # snapshot tornei e match LAN ospitati
│   ├── tournament-<uuid>.json
│   └── match-<uuid>.json
└── cache/                               # avatar, statiche
```

Cifratura del token JWT: AES-GCM con chiave derivata dalla machine-id (best-effort, non security-critical).

### 14.2 Lato server (MySQL)

- Vedi schema sezione 8.4.
- Tutte le scritture in transazione.
- `match_events` append-only.
- `current_state_json` materializzato per query veloci.
- Retention: tornei e match conclusi archiviati dopo 90 giorni in tabella `*_archive` (migrazione notturna via Spring Scheduler).
- Migration via Flyway (`db/migration/V1__init.sql`, `V2__add_round_robin.sql`, ...).

### 14.3 Cache

- Caffeine in-memory sul server per match attivi (`Cache<MatchId, MatchState>`).
- Cache invalidata al `MatchEnded`.
- Per scaling multi-istanza futuro: spostare su Redis.

---

## 15. Sicurezza

### 15.1 Threat model

| Minaccia | Mitigazione |
|---|---|
| Brute force login | Rate limit 5/min + lock account dopo 10 tentativi/ora |
| Cheating | Validazione mosse server-side via `RuleEngine` |
| Account takeover | BCrypt cost 12, JWT short-lived, refresh rotation |
| MITM | TLS 1.3 in produzione, certificato Let's Encrypt |
| Replay attack | Sequence number monotonico per match |
| Spam chat | Rate limit + filtro |
| DOS WebSocket | Connection rate-limit per IP, heartbeat enforcement |
| SQL injection | JPA parametrizzato, mai SQL concatenato |

### 15.2 Gestione segreti

- Secrets via env variable (`SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`).
- Mai in `application.yml` committato.
- JWT secret ruotato ogni 90 giorni, grace period 24h.

---

## 16. Roadmap implementativa

Fasi sequenziali. Ogni fase è autonomamente verificabile. Claude Code DEVE completare e validare una fase prima di passare alla successiva.

### Fase 0 — Setup infrastruttura

- Monorepo Maven multi-modulo (`shared/`, `core-server/`, `client/`, `server/`).
- Parent POM con dependency management, plugin Spotless, JaCoCo, Surefire.
- CI GitHub Actions: build, test, lint, SAST (SpotBugs).
- File di progetto: `README.md`, `CLAUDE.md`, `AI_CONTEXT.md`, `ARCHITECTURE.md`, `CHANGELOG.md`.
- Docker Compose dev: MySQL.

**Acceptance**: `mvn clean verify` completa con successo a tutti i moduli.

### Fase 1 — Dominio e regole (`shared`)

- Modello: `Square`, `Piece`, `Board`, `Move`, `GameState`.
- `RuleEngine` con generazione legali e applicazione delle 4 leggi italiane.
- Regola promozione + stop alla riga di promozione.
- Calcolo status (in corso, vittoria, patta).
- Coverage ≥ 90%.

**Acceptance**: tutti i test FID di riferimento passano. Partita end-to-end via API Java pura senza UI né rete.

### Fase 2 — IA

- Minimax + alpha-beta nel modulo `shared`.
- Funzione di valutazione modulare.
- 3 livelli (Principiante / Esperto / Campione).
- Cancellabilità (`Future#cancel`) e timeout per mossa.

**Acceptance**: Campione vince contro Principiante in ≥ 95% delle 100 partite simulate.

### Fase 3 — Client UI single-player

- Setup JavaFX + design system CSS custom.
- Schermate: splash, main menu, board view, settings.
- Rendering damiera + pezzi + animazioni.
- Highlight legali + cattura obbligatoria.
- Cronologia mosse, salva/carica multi-slot, autosave.
- Localizzazione IT/EN.
- Schermata regole in-app.

**Acceptance**: utente gioca partita completa contro IA, salva, ricarica, riapre dopo crash con autosave.

### Fase 3.5 — Visual polish + Audio + Demo release Windows

> Mini-fase intermedia tra Fase 3 e Fase 4, **pull-forward** parziale di Fase 11 introdotta per esigenza di **demo cliente** Win 10/11. Non era prevista nella roadmap originale (v2.0/v2.1).

- Visual rework completo delle 8 schermate F3 con design system v2 (palette wood premium, vedi §13.2): splash, main menu, sp setup, board, save dialog, load screen, settings, rules.
- Tavola con texture legno e cornice frame; pezzi 3D-look (gradient + scanalatura simulata + dame con marker oro/rosso).
- Animazioni "juicy" (vedi §13.3): mossa con `OUT_BACK`, particle puff su cattura, raggi dorati su promozione, glow halo esterno cattura obbligatoria.
- Audio: ambient music orchestrale soft (playlist random 3-5 tracce, default 30%, mutabile) + SFX su mossa/cattura/promozione/vittoria (vedi §13.4).
- Settings: nuovi controlli volume musica/SFX, persistenza in `config.json`.
- Asset solo CC0 / CC-BY con `CREDITS.md` di inventario (vedi §13.2.3).
- Packaging Windows: `jpackage` `.msi` con JRE bundled, shortcut Start menu, icon dedicata. Mac/Linux installer restano a Fase 11.
- Estinzione contestuale del debito TEST sotto-fase Fase 3 (deferred al tag `v0.3.0`).

**Acceptance**: cliente apre l'installer Windows `.msi` su Win 10 o Win 11, completa l'installazione senza prerequisiti manuali, lancia il gioco, naviga splash → main menu → setup → board, gioca una partita completa vs IA al livello scelto fino a stato terminale, salva, ricarica, sente musica orchestrale soft di sottofondo + SFX coerenti su ogni evento di gameplay, e percepisce uno stile "videogame premium wood" coerente in tutte e 8 le schermate.

### Fase 4 — `core-server` skeleton

- Tournament Engine (vuoto ma con interfacce).
- Match Manager con repository ports.
- Adapter in-memory.
- Event bus interno + STOMP-compatible publisher.

**Acceptance**: test di unità del core-server con repository in-memory; un match singolo funziona end-to-end via API Java.

### Fase 5 — Server centrale skeleton

- Bootstrap Spring Boot.
- Schema DB + Flyway V1.
- Auth (registrazione, login, JWT).
- Endpoint `/users/me`.
- Test integrazione con Testcontainers + MySQL.
- OpenAPI 3.1.

**Acceptance**: utente registra/logga via curl. Token JWT valido emesso.

### Fase 6 — Match singoli online (Internet)

- Adapter JPA per le port di `core-server`.
- REST: create / list / join / get match.
- WebSocket STOMP: topic `/topic/match/{id}`, destination `/app/match/{id}/move`.
- Match Manager con cache Caffeine + write-through MySQL.
- Validazione mosse server-side.
- Riconnessione con replay eventi.
- Resign + draw offer.
- Client adapter STOMP collegato.

**Acceptance**: due client giocano partita Internet completa con drop di rete simulato e recovery.

### Fase 7 — Match LAN singolo

- LAN host: Jetty embedded + core-server in-memory.
- Discovery JmDNS.
- Client UI: ospita / cerca / inserisci IP.
- Chat in-game.
- Snapshot autosave dell'host.

**Acceptance**: due client su LAN giocano senza Internet. Host crasha e riprende dal snapshot.

### Fase 8 — Tornei eliminazione diretta (Internet + LAN)

- Bracket generator.
- Stanze e match concorrenti.
- Topic `/topic/tournament/{id}` e room.
- Forfeit timer.
- Spettatorialità.
- Sia in modalità Internet (server) sia in modalità LAN (host client).

**Acceptance**: torneo a 8 con eliminazione diretta termina con bracket corretto e classifica, sia online sia LAN.

### Fase 9 — Tornei round-robin (Internet)

- Schedule generator round-robin.
- Sistema punti configurabile.
- Tab classifica live con tie-breaker.
- Sfida diretta tra partecipanti.
- Gestione deadline e forfeit automatico (job schedulato).

**Acceptance**: campionato a 6 partecipanti completa tutti i match, classifica corretta con tie-breaker, deadline applicata se configurata.

### Fase 10 — Tornei round-robin LAN

- Stessa logica, in-memory + autosave host.
- Gestione multi-sessione (host che chiude e riapre).

**Acceptance**: torneo round-robin LAN a 4 partecipanti completa in sessione singola.

### Fase 11 — Polish & rilascio

- ~~Audio + animazioni avanzate~~ → *anticipato a Fase 3.5* per esigenza demo cliente.
- Dark mode con toggle runtime.
- Profilo + statistiche complete.
- Replay viewer.
- Sistema report.
- Packaging jpackage Mac (`.dmg`) + Linux (`.deb`/`.rpm`). *Windows `.msi` anticipato a Fase 3.5.*
- Docker image server.
- Monitoring stack (Prometheus + Grafana dashboard).
- Documentazione utente.
- **Deferral consci da Fase 3** (formalizzati nella REVIEW-fase-3 chiusa il 2026-04-30):
  - Localizzazione IT/EN dei testi `accessibleText` su pezzi e celle (NFR-U-01, vedi §13.5).
  - Pattern daltonismo sui pezzi (§13.5).
  - Misura formale del target 60 FPS durante animazioni (NFR-P-01, AC §17.2.3) — valutazione tool-based su hardware target (Intel UHD 620+).
  - Verifica tool-based del contrasto WCAG AA su entrambi i temi (AC §17.2.7) — light implementato in F3 by-design, dark e check automatico in F11.

**Acceptance**: utente scarica installer, registra, gioca in tutte le modalità, accede a profilo e replay.

---

## 17. Acceptance criteria globali

### 17.1 Funzionali

1. Partita single-player vs IA Campione si conclude entro 30 minuti senza crash.
2. Partita Internet tra due client si conclude correttamente con stato persistito su MySQL.
3. Partita LAN funziona con server centrale offline.
4. Torneo eliminazione a 8 termina con bracket corretto sia online sia LAN.
5. Campionato round-robin a 6 termina con classifica corretta e tie-breaker funzionanti.
6. Tutte le 4 leggi della Dama Italiana sono applicate (test specifici nel modulo `shared`).
7. Pedina che raggiunge la promozione durante una sequenza di catture **non continua** la sequenza.
8. Pedina **non può** catturare la dama.
9. Salvataggio multi-slot single-player funziona: l'utente vede la lista, carica, riprende.
10. Autosave LAN host funziona: dopo crash, il torneo riprende.
11. Riconnessione Internet entro 2 minuti recupera il match con replay eventi.
12. Sezione regole in-app accessibile e completa.

### 17.2 Non funzionali

1. Server gestisce 200 match concorrenti CPU < 70%, RAM < 6 GB.
2. Latenza p95 mossa→broadcast < 200 ms.
3. Client a 60 FPS durante animazioni.
4. Coverage test ≥ 80% sul modulo `shared`.
5. SAST (SpotBugs) senza warning High.
6. Password hashate (verificabile DB).
7. Dark mode e light mode entrambi WCAG AA.

---

## 18. Linee guida per Claude Code

### 18.1 Workflow

1. All'inizio di ogni fase: rileggere SPEC + `CLAUDE.md` + `AI_CONTEXT.md` + `ARCHITECTURE.md`.
2. Produrre un piano dettagliato per la fase prima di scrivere codice.
3. Test prima dell'implementazione (TDD dove sensato).
4. Eseguire `mvn verify` dopo ogni gruppo di modifiche.
5. Aggiornare `ARCHITECTURE.md` quando si introducono decisioni nuove.
6. Aggiornare `CHANGELOG.md` ad ogni milestone.

### 18.2 Convenzioni

- Identificatori e Javadoc in **inglese**.
- Messaggi UI via `ResourceBundle` (it_IT, en_US).
- Branch: `feature/<fase>-<topic>` su trunk.
- Commit: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- PR: ogni PR DEVE chiudere almeno un acceptance criterion citato esplicitamente.

### 18.3 Definition of Done

- Codice compila e test passano (`mvn verify`).
- Coverage non scende sotto le soglie (NFR-M-01, NFR-M-02).
- Spotless OK.
- Javadoc su API pubbliche, OpenAPI per REST aggiornati.
- Acceptance criteria della fase soddisfatti.
- Nessun TODO/FIXME senza issue tracciata.

---

## Appendice A — Riferimenti normativi

- Regolamento ufficiale FID — Federazione Italiana Dama: <https://www.federdama.it>.
- Notazione FID: numerazione case 1–32.

## Appendice B — Architectural Decision Records (ADR)

| ADR | Titolo | Decisione |
|---|---|---|
| ADR-001 | Linguaggio | Java 21 LTS |
| ADR-002 | Build tool | Maven multi-modulo |
| ADR-003 | UI framework | JavaFX 21+ con CSS custom design system |
| ADR-004 | DI client | Spring Boot starter (non-web) |
| ADR-005 | Container LAN host | Jetty embedded via Spring Boot WebSocket starter, on-demand |
| ADR-006 | Server framework | Spring Boot 3.4+ |
| ADR-007 | DB Internet | MySQL 8 (vs PostgreSQL) |
| ADR-008 | Real-time protocol | STOMP su WebSocket |
| ADR-009 | Discovery LAN | mDNS via JmDNS |
| ADR-010 | Architettura | Monorepo Maven 4 moduli (shared, core-server, client, server) |
| ADR-011 | Modulo `core-server` | Libreria condivisa client (host LAN) ↔ server |
| ADR-012 | Storage match | Log mosse append-only con sequence number monotonico + stato corrente materializzato |
| ADR-013 | Variante gioco | Solo Dama Italiana FID (rimossa Internazionale) |
| ADR-014 | Tornei | Single elimination + round-robin con punti |
| ADR-015 | IA livelli | 3 (Principiante, Esperto, Campione) |
| ADR-016 | Salvataggi single-player | Multi-slot + autosave |
| ADR-017 | Tie-breaker round-robin | Scontro diretto → Sonneborn-Berger → numero vittorie → sorteggio. Implementato come strategia configurabile (`TieBreakerPolicy`) per futuri allineamenti FID. |

## Appendice C — Glossario tecnico breve

- **mDNS** (Multicast DNS): protocollo di scoperta servizi in LAN senza autorità centrale. Risolve nomi `.local` via multicast UDP.
- **STOMP** (Simple Text Oriented Messaging Protocol): protocollo applicativo testuale su WebSocket, supporta pub/sub via topic.
- **JWT** (JSON Web Token): token firmato per autenticazione stateless.
- **Sequence number monotonico**: contatore intero crescente per evento, garantisce ordinamento e riconciliazione.
- **BCrypt**: algoritmo di hashing password con salt e cost factor configurabile.
- **Zobrist hashing**: tecnica per identificare posizioni di gioco con un hash, usata in transposition table.
- **Sonneborn-Berger**: tie-breaker per tornei round-robin. Per ogni partecipante, somma i punti totalizzati nel torneo dagli avversari **battuti** (peso 1.0) più la metà dei punti degli avversari con cui ha **pattato** (peso 0.5). Premia chi ha ottenuto i risultati positivi contro gli avversari più forti.

---

**FINE DOCUMENTO**
