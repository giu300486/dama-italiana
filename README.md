# Dama Italiana Multiplayer

Applicazione desktop Java per giocare a **Dama Italiana** in tre modalità: contro il computer, in LAN peer-to-peer, e online tramite server centrale. Supporta sia partite singole che tornei (eliminazione diretta e campionato round-robin).

> **Documentazione autoritativa**: [`SPEC.md`](SPEC.md). In caso di conflitto tra qualsiasi altro file e SPEC, prevale SPEC.
> **Workflow di sviluppo**: [`CLAUDE.md`](CLAUDE.md).

---

## Stato del progetto

Fase corrente: **Fase 0 — Setup infrastruttura** (vedi [`plans/PLAN-fase-0.md`](plans/PLAN-fase-0.md)).

Stato runtime corrente: vedi [`AI_CONTEXT.md`](AI_CONTEXT.md).

---

## Prerequisiti

| Tool      | Versione minima | Note                                                                                  |
|-----------|-----------------|---------------------------------------------------------------------------------------|
| JDK       | 21 LTS          | Adoptium Temurin raccomandato                                                          |
| Maven     | 3.9             | Verificato dal plugin `maven-enforcer`                                                 |
| MySQL     | 8.0+            | Istanza locale o di rete; richiesta dal `server` (Internet) a partire dalla Fase 5    |
| Git       | 2.x             |                                                                                        |

Per Windows è richiesto **Bash** (Git Bash o WSL) se si desidera replicare i comandi descritti qui letteralmente.

---

## Layout monorepo

```
dama-italiana/
├── shared/         # Modello di dominio puro + RuleEngine + IA (no framework)
├── core-server/    # Tournament engine + match manager (DI Spring, no transport)
├── client/         # Applicazione desktop JavaFX + Spring Boot non-web
└── server/         # Server centrale Spring Boot (REST + STOMP + JPA + MySQL)
```

Dettagli architetturali: [`ARCHITECTURE.md`](ARCHITECTURE.md) e `SPEC.md` sezioni 6-7.

---

## Comandi standard

| Comando                              | Scopo                                                              |
|--------------------------------------|--------------------------------------------------------------------|
| `mvn clean verify`                   | Build completo + test + coverage + Spotless + SpotBugs             |
| `mvn -pl shared verify`              | Verify modulo specifico                                            |
| `mvn -pl core-server verify`         | Verify core-server                                                 |
| `mvn -pl client javafx:run`          | Lancia il client desktop (da Fase 3)                               |
| `mvn -pl server spring-boot:run`     | Lancia il server centrale (richiede MySQL attivo)                  |
| `mvn spotless:apply`                 | Applica il formato Google Java Style                               |
| `mvn spotless:check`                 | Verifica formato senza modificare                                  |
| `mvn jacoco:report`                  | Genera report HTML coverage in `target/site/jacoco/`               |
| `mvn dependency:tree`                | Visualizza grafo dipendenze                                        |

---

## Setup ambiente di sviluppo

### 1. Database MySQL locale

Il `server` (modulo central server, Internet) si connette a un'istanza MySQL già installata sulla macchina, gestita con MySQL Workbench / DBeaver. Il client offline (single-player) e LAN **non** richiedono MySQL.

Setup una tantum:

```sql
-- Da Workbench / DBeaver / mysql CLI, come utente con privilegi sufficienti:
CREATE DATABASE dama_italiana CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dama'@'localhost' IDENTIFIED BY 'una-password-robusta';
GRANT ALL PRIVILEGES ON dama_italiana.* TO 'dama'@'localhost';
FLUSH PRIVILEGES;
```

> Lo schema delle tabelle è gestito da **Flyway** (vedi `server/src/main/resources/db/migration/`). Le migrazioni gireranno automaticamente all'avvio del server a partire dalla Fase 5.

Configurazione applicativa via env var (sovrascrivono i default in `server/src/main/resources/application.yml`):

```bash
export DB_URL=jdbc:mysql://localhost:3306/dama_italiana
export DB_USERNAME=dama
export DB_PASSWORD=una-password-robusta
```

I default `application.yml` puntano già a `localhost:3306`, db `dama_italiana`, utente `dama`. Solo la password è obbligatoria via env (vincolo NFR-S — niente secret committati).

### 2. Build

```bash
mvn clean verify
```

Atteso: `BUILD SUCCESS` su parent + 4 moduli (`shared`, `core-server`, `client`, `server`). Tempo indicativo primo run: 1–2 minuti (download dipendenze).

---

## Continuous Integration

Lo stato attuale del progetto **non prevede una macchina remota** né un repository Git remoto: il workflow GitHub Actions è quindi **disattivato** per default.

### Stato del file CI

Il workflow è preservato come [`.github/workflows/ci.yml.disabled`](.github/workflows/ci.yml.disabled). GitHub Actions ignora i file con estensione diversa da `.yml`/`.yaml`, quindi non viene eseguito anche se il repository fosse pushato. Per riattivarlo:

```bash
git mv .github/workflows/ci.yml.disabled .github/workflows/ci.yml
```

### Validazione locale equivalente al CI

Finché non c'è un runner remoto, i tre quality gate del workflow sono validabili in locale via Maven:

| Job CI                | Comando equivalente in locale                  |
|-----------------------|------------------------------------------------|
| `build` (mvn verify)  | `mvn -B clean verify`                          |
| `lint` (Spotless)     | `mvn spotless:check`                           |
| `sast` (SpotBugs)     | `mvn -DskipTests verify`                       |

Il comando `mvn clean verify` da solo copre tutti e tre, perché Spotless e SpotBugs sono legati alla phase `verify` nel parent POM.

### Proposte per CI/CD locale (futuro)

Quando avrai una macchina di staging/CI ma vuoi restare self-hosted senza dipendere da GitHub, ci sono due alternative pratiche:

1. **Gitea (o Forgejo) self-hosted + Gitea Actions**.
   - Server Git completo installabile come servizio Windows o standalone su una macchina Linux.
   - **Gitea Actions** è compatibile con la sintassi GitHub Actions: il file `ci.yml` esistente girerebbe quasi senza modifiche.
   - Ottimo trade-off: hai un repo remoto + CI in casa tua.
2. **Drone CI / Woodpecker CI** con runner installati direttamente sulla tua macchina.
   - Più leggeri di Jenkins.
   - Sintassi pipeline diversa rispetto a GitHub Actions (richiede riscrittura del workflow).

> Non installiamo niente di tutto questo ora. La proposta resta come riferimento per quando deciderai di aggiungere una macchina di build.

---

## Workflow Spec-Driven Development

Il progetto adotta un workflow **PIANIFICA → IMPLEMENTA → REVIEW → TEST** descritto in `CLAUDE.md` §2. Ogni fase della roadmap (`SPEC.md` §16) attraversa queste 4 sotto-fasi nell'ordine indicato.

Output di ogni sotto-fase:

- `plans/PLAN-fase-N.md` — output di PIANIFICA.
- `reviews/REVIEW-fase-N.md` — output di REVIEW.
- `tests/TEST-PLAN-fase-N.md` — output di TEST.
- `tests/TRACEABILITY.md` — matrice cumulativa requisito → test.

---

## Convenzioni

- Identificatori e Javadoc: **inglese**.
- Stringhe UI: solo via `ResourceBundle` (`messages_it.properties`, `messages_en.properties`).
- Stile codice: Google Java Style Guide via Spotless (verificato in `mvn verify`).
- Commit: [Conventional Commits](https://www.conventionalcommits.org/).
- Branch: trunk-based, feature branch `feature/<fase>-<topic>`.

Dettaglio: `CLAUDE.md` §4.

---

## Licenza

_(da definire prima del rilascio in Fase 11)_
