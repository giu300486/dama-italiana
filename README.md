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

| Tool                      | Versione minima | Note                                                              |
|---------------------------|-----------------|-------------------------------------------------------------------|
| JDK                       | 21 LTS          | Adoptium Temurin raccomandato                                     |
| Maven                     | 3.9             | Verificato dal plugin `maven-enforcer`                            |
| Docker + Docker Compose   | Recente         | Solo per ambiente dev locale (MySQL)                              |
| Git                       | 2.x             |                                                                   |

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

| Comando                                          | Scopo                                                              |
|--------------------------------------------------|--------------------------------------------------------------------|
| `mvn clean verify`                               | Build completo + test + coverage check su tutti i moduli           |
| `mvn -pl shared verify`                          | Verify modulo specifico                                            |
| `mvn -pl core-server verify`                     | Verify core-server                                                 |
| `mvn -pl client javafx:run`                      | Lancia il client desktop (da Fase 3)                               |
| `mvn -pl server spring-boot:run`                 | Lancia il server centrale (richiede MySQL attivo)                  |
| `mvn spotless:apply`                             | Applica il formato Google Java Style                                |
| `mvn spotless:check`                             | Verifica formato senza modificare                                   |
| `mvn jacoco:report`                              | Genera report HTML coverage in `target/site/jacoco/`                |
| `mvn dependency:tree`                            | Visualizza grafo dipendenze                                         |
| `docker compose up -d`                           | Avvia infrastruttura dev (MySQL + Adminer)                          |
| `docker compose down`                            | Ferma infrastruttura dev (volumi mantenuti)                         |
| `docker compose down -v`                         | Ferma e cancella i volumi (reset totale del DB dev)                 |

---

## Setup ambiente di sviluppo

### 1. Configurazione `.env`

```bash
cp .env.example .env
# Modifica i valori se necessario (default OK per dev locale)
```

### 2. Database MySQL

#### Opzione A — MySQL via Docker Compose (consigliata)

```bash
docker compose up -d
```

Servizi avviati:

- **MySQL 8.0** sulla porta host **3307** (mappata su 3306 del container).
  > La porta 3307 è scelta deliberatamente per non confliggere con un eventuale MySQL già in esecuzione localmente sulla 3306.
- **Adminer** su [http://localhost:8081](http://localhost:8081) per ispezionare il DB via browser (alternativa minimale a MySQL Workbench / DBeaver).

Connessione applicativa: `jdbc:mysql://localhost:3307/dama_italiana`.

#### Opzione B — MySQL locale già installato

Se preferisci usare un'istanza MySQL già installata sulla tua macchina (es. accessibile da MySQL Workbench / DBeaver sulla porta 3306):

1. Crea il database: `CREATE DATABASE dama_italiana CHARACTER SET utf8mb4;`.
2. Crea utente o usa root.
3. Esporta le variabili d'ambiente puntando alla 3306:
   ```bash
   export DB_URL=jdbc:mysql://localhost:3306/dama_italiana
   export DB_USERNAME=...
   export DB_PASSWORD=...
   ```

### 3. Build

```bash
mvn clean verify
```

Atteso: `BUILD SUCCESS` su tutti e 4 i moduli.

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
- Stile codice: Google Java Style Guide via Spotless (verificato in CI).
- Commit: [Conventional Commits](https://www.conventionalcommits.org/).
- Branch: trunk-based, feature branch `feature/<fase>-<topic>`.

Dettaglio: `CLAUDE.md` §4.

---

## Licenza

_(da definire prima del rilascio in Fase 11)_
