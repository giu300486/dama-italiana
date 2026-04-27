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

### ADR-019 — CI GitHub Actions predisposta ma disattivata fino a disponibilità repository remoto

- **Data**: 2026-04-27
- **Stato**: Accepted
- **Contesto**: Il workflow `.github/workflows/ci.yml` era stato creato in Fase 0 (Task 0.8). Il developer non dispone attualmente di una macchina remota né intende pushare il repository su un hosting Git pubblico/privato. GitHub Actions richiede repository remoto per girare; tenerlo "attivo" non produce alcun valore.
- **Decisione**: Rinominiamo `.github/workflows/ci.yml` → `.github/workflows/ci.yml.disabled`. GitHub Actions ignora workflow con estensione non `.yml`/`.yaml`, quindi non gira anche in caso di push futuro accidentale. Quando il developer avrà accesso a un repository remoto (GitHub, Gitea, Forgejo), basta un `git mv` per riattivarlo.
- **Conseguenze**:
  - I tre quality gate (build/lint/sast) restano completamente verificabili in locale via `mvn clean verify` (il parent POM lega Spotless e SpotBugs alla phase `verify`).
  - Acceptance A0.6 della Fase 0 resta tecnicamente "deferred" finché non c'è un repo remoto su cui validarla.
- **Alternative considerate**:
  - Eliminare `ci.yml`: meno attrito ma perde il lavoro fatto e l'allineamento del workflow con i comandi locali.
  - Spostarlo in `docs/`: meno scopribile, perde l'immediatezza di "rinomina e attiva".
  - Adottare subito un sistema CI self-hosted (Gitea Actions / Drone / Woodpecker): richiederebbe installazione e configurazione di nuova infrastruttura non strettamente necessaria per ora. Documentato nel README come proposta per il futuro.

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
