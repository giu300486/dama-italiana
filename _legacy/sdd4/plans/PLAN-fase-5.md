# PLAN — Fase 5: Server centrale skeleton

- **Riferimento roadmap**: SPEC §16 — "Fase 5 — Server centrale skeleton" (Bootstrap Spring Boot, Schema DB + Flyway V1, Auth registrazione/login/JWT, endpoint `/users/me`, test integrazione Testcontainers + MySQL, OpenAPI 3.1).
- **SPEC version**: 2.3 (post CR-F4.5-001). Questo PLAN propone **CR-F5-001** (vedi §11) che andrà approvata in Task 5.1 prima di scrivere `V1__init.sql`.
- **Data piano**: 2026-05-06 (v1.0 DRAFT) → 2026-05-10 (v1.1 APPROVED post §9 chiarimenti) → 2026-05-16 (**v1.2 AMENDMENT**, decisione 9.4 superseded da 9.4-bis: MySQL nativo locale per `dev`+`it`, Testcontainers rimosso).
- **Autore**: Claude Code.
- **Stato**: **AMENDMENT v1.2 PENDING USER APPROVAL** — il PLAN v1.1 era APPROVED e le sotto-fasi IMPLEMENTA 5.1/5.2/5.3 sono già chiuse (commit `3207b94`, `6640781`, `5d1fa06`). L'amendment v1.2 introduce ADR-049 + Task 5.3.bis di retrofit prima di Task 5.4. Stop point CLAUDE.md §2.1 attivo: nessun codice toccato finché v1.2 non riceve approvazione esplicita.
- **Branch**: `feature/5-server-skeleton` (staccato da `develop` HEAD `5ef6e8c` post-tag `v0.4.5`).
- **Tag finale previsto**: `v0.5.0`.
- **Predecessor closure**: F4.5 (`v0.4.5`) chiusa al 100% — utente ha confermato il **2026-05-06** che il software installato sul PC del cliente "funziona perfettamente"; A4.5.16 (manual demo run cliente) è **PASSED**, A4.5.11/A4.5.12 (DPI matrix + resize-jank) restano *deferred to manual* in TEST-PLAN-fase-4.5 §6 ma il cliente reale è OK quindi non bloccanti per F5.
- **v1.1 amendments** (post §9 user decisions, applicati 2026-05-10):
  - **9.1** JWT secret: `dev`=default hardcoded + warning, `test`/`it`=hardcoded yaml, `prod`=fail-fast env var. Documentato in **ADR-045**, NO SPEC CR (interpretazione di buona fede di SPEC §15.2).
  - **9.2** BCrypt cost 12 ovunque (dev, test, it, prod).
  - **9.3** `users.role` = `VARCHAR(32) + CHECK constraint` (NON ENUM MySQL puro). Richiede **CR-F5-001** a SPEC §8.4 — applicata in Task 5.1.
  - **9.4** ~~Profilo `dev` usa **Testcontainers Spring Boot dev mode**~~ → **SUPERSEDED da 9.4-bis (v1.2 amendment, 2026-05-16)**. Vedi §9.4-bis sotto.
- **v1.2 amendment** (2026-05-16, decisione retroattiva dell'utente in apertura sessione post chiusura Task 5.3):
  - **9.4-bis** Profili `dev` e `it` puntano a **MySQL nativo locale** `localhost:3306` (DB `dama_italiana_dev` e `dama_italiana_it`). Testcontainers rimosso completamente dal modulo `server`. Comando dev: `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev`. Razionale utente: replicare già da F5 lo stack di produzione (MySQL nativo) eliminando il prerequisito Docker. Documentato in **ADR-049** che supersedes parts di ADR-044 §dev/it + ADR-048 §full-integration-MySQL. NO SPEC CR (SPEC non vincola HOW dev accede a MySQL; SPEC §6.5 già lista MySQL 8 come DB). **Task 5.3.bis** (nuovo, vedi §4) esegue il retrofit di codice/config/docs già committati in Task 5.2 e 5.3 prima di proseguire con Task 5.4.

---

## 1. Scopo della fase

Mettere in piedi il **modulo `server`** come applicazione Spring Boot autonoma, con autenticazione username+password, emissione e refresh di token JWT, profilo utente accessibile via `/users/me`, persistenza MySQL via JPA + Flyway, e tutta l'observability di base (Prometheus, JSON logging, correlation-id, OpenAPI 3.1).

Al termine della fase un utente DEVE poter:

> 1. Avviare il server con `mvn -pl server spring-boot:run` (profilo `dev`, MySQL locale via Docker Compose), 2. registrarsi via `curl POST /v1/auth/register` ricevendo HTTP 201 + `accessToken` + `refreshToken`, 3. effettuare login via `curl POST /v1/auth/login` ricevendo gli stessi token, 4. interrogare `curl GET /v1/users/me -H "Authorization: Bearer <accessToken>"` ricevendo il proprio profilo, 5. ottenere un nuovo access token via `curl POST /v1/auth/refresh -H "Authorization: Bearer <refreshToken>"`, 6. invalidare il refresh token via `curl POST /v1/auth/logout`, 7. consultare la documentazione interattiva su `http://localhost:8080/swagger-ui.html`, 8. visualizzare metriche Prometheus su `http://localhost:8080/actuator/prometheus`.

### 1.1 Out of scope (rinviato a F6+)

- **Match REST/STOMP** (creazione, join, mosse, eventi, replay): tutto F6. Lo schema `matches` / `match_events` NON viene creato in F5 (no Flyway V2 in questa fase).
- **Tornei** (lobby, bracket, round-robin, partecipanti): F8.
- **WebSocket / STOMP broker**: F6 (lo starter è già nel pom ma non si configura un endpoint STOMP attivo in F5).
- **Caffeine cache di match**: F6 (non serve cachare nulla in F5 perché non ci sono entità "calde" oltre all'utente loggato).
- **`/users/{id}` profilo pubblico, `/users/me/stats`, `/users/me/matches`**: dipendono da `matches` ⇒ rinviati a F6/F11.
- **Email verification, password reset**: non in SPEC, fuori scope.
- **TLS 1.3 (NFR-S-03)**: concern di deploy (Let's Encrypt + reverse proxy), documentato in ADR-044 ma non implementato come configurazione Spring in F5.
- **Account lockout dopo 10 tentativi/ora** (SPEC §15.1 mitigazione brute force, parte 2): in F5 implementiamo solo il rate-limit "5/min" (NFR-S-04 parte 1) — il lockout per account è F6 quando ci sono utenti reali a rischio.
- **Rotazione automatica del JWT secret ogni 90 giorni con grace 24h** (SPEC §15.2): meccanismo deploy/ops, NON in scope F5 — il secret è caricato da env var e basta.
- **Docker image deploy via Spring Boot Buildpacks** (SPEC §6.5): fuori scope F5 (deploy concern).

### 1.2 Note operative

- Il cliente del modulo `server` non è ancora `client` (l'integrazione client→server è F6). F5 si verifica esclusivamente via curl, integration test e Swagger UI.
- **Profilo `dev` con MySQL nativo locale** (decisione utente §9.4-bis — 2026-05-16, v1.2 amendment, ADR-049): l'avvio di sviluppo è `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev`. Datasource default `jdbc:mysql://localhost:3306/dama_italiana_dev`, username `dama` / password `dama` (overridabili via env var `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`). Setup user-side una tantum via Workbench: `CREATE DATABASE dama_italiana_dev` + `CREATE USER 'dama'@'localhost'` + `GRANT ALL`. **Pro**: parità prod-equivalent (stesso stack MySQL nativo di prod); ispezione DB stabile via Workbench su porta fissa 3306; nessun prerequisito Docker. **Con**: setup MySQL nativo una tantum richiesto (mitigato da script SQL nel README).
- **Profilo `prod`**: MySQL installato nativamente sul server di deploy. Configurazione via env var `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, fail-fast se assenti.
- **Profilo `test` (fast loop)**: H2 in-memory MySQL-compat; nessun MySQL, nessun Docker richiesto. Garantisce `mvn -pl server verify -DexcludedGroups=slow` rapido.
- **Profilo `it` (slow integration)**: MySQL nativo locale come `dev` ma su DB dedicato `dama_italiana_it`. Isolamento test-run via `flyway.clean() + flyway.migrate()` in `AbstractMysqlIntegrationTest`. `@Tag("slow")` per escludere dal fast loop. Eseguito da `mvn clean verify` root.
- **Niente Testcontainers, niente Docker** nel modulo `server` da F5+ (ADR-049). ADR-018 (Docker Compose rimosso) **resta valido e rafforzato**: Docker non è più prerequisito di alcun layer del server.

---

## 2. Acceptance criteria

### 2.1 Acceptance globale F5

> Il maintainer avvia `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev`, con MySQL nativo 8 raggiungibile su `localhost:3306` (DB `dama_italiana_dev` pre-creato — vedi README §1.a). Esegue la sequenza curl di §1 (register → login → me → refresh → logout) e ottiene risposte 200/201 con payload conformi a OpenAPI. Riavvia il server: l'utente registrato persiste in DB (stato preservato — niente container effimero), il refresh token usato per il logout NON funziona più, le credenziali permettono ancora un nuovo login. La build root `mvn clean verify` passa pulita includendo i nuovi test integrazione MySQL nativo (`@Tag("slow")`, DB `dama_italiana_it` pre-creato).

### 2.2 Criteri operativi (A5.x)

| ID | Criterio | Verifica |
|---|---|---|
| **A5.1** | `mvn -pl server verify -DexcludedGroups=slow` BUILD SUCCESS (fast loop) | output Maven |
| **A5.2** | `mvn clean verify` (root, slow inclusi, MySQL nativo localhost:3306 on) BUILD SUCCESS | output Maven |
| **A5.3** | Coverage JaCoCo `server` ≥ 70% line+branch (NFR-M-02 + delta server) | `target/site/jacoco/jacoco.csv` |
| **A5.4** | SpotBugs 0 High su `server`, Spotless OK | output Maven |
| **A5.5** | **`ServerApplication` Spring Boot bootstrap funzionante**: classe `@SpringBootApplication`, profilo `dev` carica MySQL nativo `localhost:3306/dama_italiana_dev`, profilo `test` carica H2 in-memory, profilo `it` carica MySQL nativo `localhost:3306/dama_italiana_it` (ADR-049) | `ServerApplicationContextLoadsTest` |
| **A5.6** | **Flyway V1 init schema**: `db/migration/V1__init_users_and_refresh_tokens.sql` crea tabelle `users` (id, username UNIQUE, email UNIQUE, password_hash, **role VARCHAR(32) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN'))**, created_at, last_login_at) + `refresh_tokens` (id, user_id FK, token_hash UNIQUE, expires_at, revoked_at NULL, created_at). Migration applicata sia su H2 (test profile) sia su MySQL (it/dev profile) senza warning di compatibilità — il CHECK constraint è SQL standard, supportato da entrambi (MySQL 8.0.16+ via `enforced`, H2 nativo). Decisione 9.3 strada α — vedi CR-F5-001 §11. | Flyway info + `FlywayMigrationIntegrationTest` |
| **A5.7** | **`POST /v1/auth/register`**: input `{username, email, password}` → 201 con `{user: {id, username, email, role, createdAt}, accessToken, refreshToken}`. Validation: username 3-50 chars `[a-zA-Z0-9_-]`, email RFC 5322, password ≥ 8 chars (almeno 1 lettera + 1 cifra). Conflict 409 se username/email già esistente. Password salvata BCrypt cost 12 (NFR-S-01) | `AuthRegisterIntegrationTest` |
| **A5.8** | **`POST /v1/auth/login`**: input `{username, password}` → 200 con `{accessToken (15min, NFR-S-02), refreshToken (7gg, NFR-S-02), tokenType: "Bearer", expiresIn: 900}`. 401 se credenziali invalide. `last_login_at` aggiornato al login OK. Risposta 401 NON rivela se username o password è errato | `AuthLoginIntegrationTest` |
| **A5.9** | **`POST /v1/auth/refresh`**: input header `Authorization: Bearer <refreshToken>` → 200 con nuova coppia `{accessToken, refreshToken}` (rotation: vecchio refresh marked `revoked_at=now()`). 401 se token scaduto/revocato/inesistente | `AuthRefreshIntegrationTest` |
| **A5.10** | **`POST /v1/auth/logout`**: input header `Authorization: Bearer <refreshToken>` → 204 + token marked `revoked_at=now()`. Idempotente: secondo logout su stesso token → 401 (token già revocato) | `AuthLogoutIntegrationTest` |
| **A5.11** | **`GET /v1/users/me`**: header `Authorization: Bearer <accessToken>` → 200 con `{id, username, email, role, createdAt, lastLoginAt}`. 401 se token mancante/scaduto/firma invalida | `UsersMeIntegrationTest` |
| **A5.12** | **JWT crittografia**: HS256 con secret ≥ 256 bit. Strategia per profilo (decisione 9.1): `prod` = secret da `JWT_SECRET` env var, **fail-fast** se missing; `dev` = default hardcoded `dev-only-secret-change-me-32bytes-min-padding-xxxxxx` con **warning log** al boot ("⚠️ Using DEV-ONLY JWT secret — NEVER use in production"); `test`/`it` = hardcoded in `application-test.yml` / `application-it.yml`. Claims: `sub=userId`, `username`, `role`, `iat`, `exp`, `type=ACCESS|REFRESH`. Niente PII oltre username | inspection `JwtService` + `JwtServiceTest` + `JwtBootValidationTest` |
| **A5.13** | **Rate limit (NFR-S-04 parte 1)**: `/v1/auth/login` ≤ 5 tentativi/min per IP, `/v1/auth/register` ≤ 3 tentativi/ora per IP. 429 Too Many Requests con header `Retry-After` quando superato. Implementazione in-memory via Caffeine (no nuova dipendenza) — single-instance OK per v1 | `RateLimitIntegrationTest` |
| **A5.14** | **Stateless Spring Security**: `SessionCreationPolicy.STATELESS`, CSRF disabled (REST-only), CORS configurato con whitelist `http://localhost:8080` (futuri origin client da configurare in F6 via env). `JwtAuthenticationFilter` su tutti gli endpoint esclusi `/v1/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/actuator/prometheus` | inspection `SecurityConfig` + `SecurityWiringIntegrationTest` |
| **A5.15** | **GlobalExceptionHandler**: tutte le 4xx/5xx ritornano `application/problem+json` (RFC 7807) con `{type, title, status, detail, instance, errors[]}` per validation errors. NESSUNO stack trace nel body, NESSUN leak di dettagli interni | `ExceptionHandlingIntegrationTest` |
| **A5.16** | **OpenAPI 3.1 (NFR-M-03)**: `springdoc-openapi` espone `/v3/api-docs` (JSON) e `/swagger-ui/index.html` (UI). Tutti i 5 endpoint REST F5 documentati con request/response schema, security scheme `bearerAuth` (JWT), tag `auth`/`users`. `info.version` allineato a `0.5.0` | curl `/v3/api-docs` + manual `/swagger-ui` |
| **A5.17** | **Logging strutturato JSON (NFR-O-02)**: Logback con `logstash-logback-encoder`, layout JSON in `logback-spring.xml` profilo `prod`/`it`, console plain in `dev`/`test`. MDC `correlationId` propagato (NFR-O-03) via filter che genera/legge header `X-Correlation-Id` su ogni request | inspection + `CorrelationIdFilterIntegrationTest` |
| **A5.18** | **Metriche Prometheus (NFR-O-01)**: `/actuator/prometheus` espone metriche standard JVM + custom counter `auth_login_attempts_total{result=success\|failure}`, `auth_register_attempts_total`, gauge `users_total` | curl `/actuator/prometheus` + `MetricsIntegrationTest` |
| **A5.19** | **Health check**: `/actuator/health` ritorna `{status: UP, components: {db: {status: UP}, ...}}`. Endpoint pubblico (no auth) per liveness/readiness probe Kubernetes-ready | curl + `HealthIntegrationTest` |
| **A5.20** | **TRACEABILITY**: nuove righe per FR-INT-01/02/03 (registrazione, login, refresh — vedi §3.1), NFR-S-01/02/04, NFR-O-01/02/03, NFR-M-03. Sezione "Acceptance criteria di Fase 5" con A5.1..A5.21 | inspection `tests/TRACEABILITY.md` |
| **A5.21** | **Nessun TODO/FIXME pending** introdotto in `server/src/main/java/` da F5 (eccetto eventuali `// TODO F6:` espliciti, motivati e tracciati in `AI_CONTEXT.md`) | grep |
| **A5.22** | **Manual smoke run end-to-end**: maintainer esegue la sequenza curl di §1 con MySQL nativo locale (`dama_italiana_dev` pre-creato), conferma tutti gli step in `TEST-PLAN-fase-5.md §6` (sign-off table) | log riempito |

---

## 3. Requisiti SPEC coperti / aggiornati

### 3.1 FR coperti

SPEC §4 non ha una sezione FR esplicita per la registrazione/login Internet (è solo descritta nei flussi §9.5.1). Per allineare la TRACEABILITY proporremo (con eventuale CR a fine fase, vedi §11) le seguenti voci:

| FR ID | Descrizione (proposta) | Test |
|---|---|---|
| FR-INT-01 | Registrazione utente con username + email + password | `AuthRegisterIntegrationTest` |
| FR-INT-02 | Login con credenziali → emissione JWT access (15min) + refresh (7gg) | `AuthLoginIntegrationTest` |
| FR-INT-03 | Refresh access token via refresh token + rotazione | `AuthRefreshIntegrationTest` |
| FR-INT-04 | Logout = revoca refresh token | `AuthLogoutIntegrationTest` |
| FR-INT-05 | Endpoint profilo `/users/me` | `UsersMeIntegrationTest` |

> Decisione: queste FR-INT-* possono restare implicite in F5 (il flusso §9.5.1 è già la fonte di verità). Se a fine fase sembra utile codificarle in §4.3 SPEC, lo faremo via CR. **Default**: niente CR di scope F5 — la TRACEABILITY referenzia §9.5.1 + §11.2 SPEC.

### 3.2 NFR coperti / pull-forward

| NFR | Descrizione SPEC | Stato corrente | F5 obiettivo |
|---|---|---|---|
| NFR-P-03 | Server 200 match concorrenti CPU < 70%, 4 vCPU/8GB | Out of scope F5 (no match yet) | N/A — verifica F11 perf |
| NFR-P-04 | WebSocket ≥ 1000 msg/s | Out of scope F5 (no STOMP attivo) | N/A — F6 |
| NFR-R-01 | Stato match write-through | Out of scope F5 | N/A — F6 |
| NFR-R-03 | Restart server ripristina match | Out of scope F5 | N/A — F6 |
| **NFR-S-01** | **BCrypt cost ≥ 12** | Mancante | **Coperto pienamente** |
| **NFR-S-02** | **JWT access 15min + refresh 7gg** | Mancante | **Coperto pienamente** |
| NFR-S-03 | TLS 1.3 obbligatorio | Mancante | **Documentato** in ADR-044, deploy concern |
| **NFR-S-04** | **Rate limit login 5/min + register 3/ora** | Mancante | **Coperto pienamente** (parte lockout 10/h deferred F6) |
| NFR-S-05 | Sequence number anti-replay | Out of scope F5 (no match) | N/A — F6 |
| NFR-M-01 | Coverage `shared` ≥ 80% | ✅ F1 | Preservato (no modifica) |
| NFR-M-02 | Coverage rete/UI ≥ 60% | ✅ F4.5 client | Esteso a `server` ≥ 70% (target nostro) |
| **NFR-M-03** | **OpenAPI 3.1** | Mancante | **Coperto pienamente** (springdoc) |
| NFR-M-04 | Spotless | ✅ continuo | Preservato |
| **NFR-O-01** | **Prometheus `/actuator/prometheus`** | Mancante | **Coperto pienamente** |
| **NFR-O-02** | **Logging JSON strutturato** | Mancante | **Coperto pienamente** |
| **NFR-O-03** | **Correlation-id end-to-end** | Mancante | **Coperto pienamente** lato server (lato client F6) |

### 3.3 Acceptance criteria globali (§17) coperti

| AC | Descrizione | Status post-F5 |
|---|---|---|
| §17.1.2 | Partita Internet conclusa con stato persistito su MySQL | **PARZIALE** — l'infrastruttura (Spring Boot + MySQL + JPA + Flyway + auth) è in piedi; il match vero è F6 |
| §17.2.5 | SAST 0 High | ✅ Esteso a `server` |
| §17.2.6 | Password hashate (verificabile DB) | ✅ Coperto |

---

## 4. Decomposizione in task

Ordine sequenziale; dipendenze lineari salvo dove esplicitato. **Stima**: 14 task.

### Task 5.1 — CR-F5-001 + ADR baseline server + scelte di design

**Sotto-task 5.1.a — SPEC change request CR-F5-001** (vedi §11): aprire la CR a SPEC §8.4 (`role` ENUM → VARCHAR + CHECK). **Stop point obbligatorio**: attendere approvazione utente prima di procedere con 5.1.b. Una volta approvata: applicare diff a `SPEC.md` §8.4 + annotare CR in `CHANGELOG.md` come `docs(spec): CR-F5-001 users.role ENUM → VARCHAR + CHECK constraint`.

**Sotto-task 5.1.b — 5 nuovi ADR in `ARCHITECTURE.md`** (procedere solo dopo CR approvata):

- **ADR-044 — Server bootstrap & profile strategy**: 4 profili (`dev` Testcontainers MySQL via Spring Boot test-and-develop mode, `test` H2 in-memory, `it` Testcontainers MySQL `@Tag("slow")`, `prod` MySQL nativo installato sul server); secrets esclusivamente da env var in `prod` (`SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`); fail-fast in `prod` se env var assente. TLS gestito da reverse proxy (out of scope F5 codice). **Niente Docker Compose** — Testcontainers gestisce il container MySQL (decisione utente §9.4); ADR-018 resta valido. Conferma rafforzata: nessun `docker-compose.yml` in repo.
- **ADR-045 — JWT signing & secret strategy**: HS256 (HMAC-SHA256), secret ≥ 256 bit. Strategia per profilo (decisione utente §9.1): `prod` = env var fail-fast; `dev` = default hardcoded `dev-only-secret-change-me-32bytes-min-padding-xxxxxx` con warning log al boot; `test`/`it` = hardcoded in YAML. **Razionale conformità SPEC §15.2**: §15.2 vincola "secrets di produzione mai in repo committato" — il default `dev-only-*` non è un secret di produzione (è marcato esplicitamente, non è mai usato per emettere token spesi da utenti reali); spirito di §15.2 preservato. Claims minimali (`sub`, `username`, `role`, `iat`, `exp`, `type`); refresh tokens persistiti in DB con hash SHA-256 del token raw (mai il token in chiaro); rotation su ogni refresh. Niente RS256/EdDSA in v1 (single-instance, no JWK distribution needed).
- **ADR-046 — Flyway baseline strategy**: V1 = `users` + `refresh_tokens` (auth core). V2 (F6) = `matches` + `match_events`. V3 (F8) = `tournaments` + relations. Mai modificare migration applicate; ogni modifica = nuova migration. `clean` proibito in profilo `prod` (`spring.flyway.clean-disabled=true`). Schema `users.role` = `VARCHAR(32) + CHECK constraint` (decisione utente §9.3 strada α + CR-F5-001 a SPEC §8.4): SQL standard portabile H2/MySQL con vincolo DB-level preservato. Workbench-friendly (l'utente vede esplicitamente quali sono i 3 valori validi nel CHECK clause).
- **ADR-047 — Rate limiter implementation**: in-memory Caffeine `Cache<String, Bucket>` con sliding window per IP. Fallback dopo restart = perdita stato (accettabile in v1). Migrazione futura a Redis quando si scala multi-istanza.
- **ADR-048 — Test strategy & isolation**: 3 layer = (a) **unit** Mockito + JUnit (logica pura — JwtService, RateLimiter, validators); (b) **slice** Spring Boot `@WebMvcTest` / `@DataJpaTest` con H2 (controller layer, repository layer); (c) **full integration** `@SpringBootTest` `webEnvironment=RANDOM_PORT` con **Testcontainers MySQL 8** (`@Tag("slow")`) per E2E auth flow. H2 vs MySQL: tutti gli statement SQL Flyway compatibili con entrambi (CHECK constraint incluso); BCrypt cost 12 mantenuto in tutti i profili (decisione 9.2). Profilo `dev` riusa Testcontainers via Spring Boot dev mode (ADR-044) — assicura identità tra dev e it.

**Moduli toccati**: `SPEC.md` (CR-F5-001) + `ARCHITECTURE.md` (5 ADR) + `CHANGELOG.md`.
**Dipendenze**: nessuna.
**Output stop point**: §11 CR-F5-001 (sotto-task 5.1.a).

### Task 5.2 — `ServerApplication` bootstrap + 4 profili + Testcontainers Spring Boot dev mode + smoke context test

- Creare `server/src/main/java/com/damaitaliana/server/ServerApplication.java` con `@SpringBootApplication`, `main()`. Component-scan default sul package `com.damaitaliana.server`.
- `server/src/main/resources/application.yml` con 4 profili:
  - `default` (no profile attivo) → fail-fast con messaggio "must specify SPRING_PROFILES_ACTIVE".
  - `dev`: NO `spring.datasource.url` (viene dal Testcontainers `@ServiceConnection`); `spring.flyway.enabled=true`; `logging.level.com.damaitaliana=DEBUG`; console plain; JWT secret default hardcoded + warning log; BCrypt cost 12.
  - `test`: `spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE` (modalità MySQL-compat per CHECK constraint); `spring.jpa.hibernate.ddl-auto=validate`; JWT_SECRET hardcoded `test-secret-not-for-prod-32bytes-padding-xxxx`; BCrypt cost 12; Flyway abilitato per coerenza con prod.
  - `it`: estende `test` overridando datasource via Testcontainers (programmatic in `AbstractMysqlIntegrationTest`).
  - `prod`: tutto da env var (`SPRING_DATASOURCE_*`, `JWT_SECRET`); `spring.flyway.clean-disabled=true`; logging JSON via Logback; **fail-fast** se `JWT_SECRET` env var assente.
- **Spring Boot dev mode setup**:
  - Aggiungere dependency `org.springframework.boot:spring-boot-testcontainers` (test scope).
  - Creare `server/src/test/java/com/damaitaliana/server/TestServerApplication.java` con `@TestConfiguration` + `@Bean @ServiceConnection MySQLContainer<?> mysqlContainer()` (immagine `mysql:8.0`). Main method: `SpringApplication.from(ServerApplication::main).with(TestServerApplication.class).run(args)`.
  - Comando dev: `mvn -pl server spring-boot:test-run` (Spring Boot Maven plugin 3.x supporta nativamente `test-run` per usare la classe `TestServerApplication`).
  - README aggiornato con istruzioni "How to develop locally": `mvn -pl server spring-boot:test-run` + tip su come trovare la porta MySQL nel log per Workbench connection.
- Sostituire `ServerSmokeTest` con `ServerApplicationContextLoadsTest` (`@SpringBootTest @ActiveProfiles("test")`, asserisce solo che il context si carica).
- Cancellare il vecchio `ServerSmokeTest`.
- `mvn -pl server verify -DexcludedGroups=slow` BUILD SUCCESS.

**Moduli toccati**: `server` (prod + test + resources) + `README.md` (sezione dev setup).
**Dipendenze**: 5.1.

> **Stato post-implementazione (commit `6640781`, 2026-05-10)**: task chiuso secondo PLAN v1.1. File creati: `ServerApplication.java`, `TestServerApplication.java`, `ServerApplicationContextLoadsTest.java`, 5 yaml (`application{,-dev,-test,-it,-prod}.yml`), `pom.xml` con dep `spring-boot-testcontainers`. **Retrofit pianificato in Task 5.3.bis (v1.2)**: cancellare `TestServerApplication.java`; rescrivere `application-dev.yml` e `application-it.yml` per MySQL nativo; rimuovere dep `spring-boot-testcontainers` + `testcontainers/mysql` + `testcontainers/junit-jupiter` da `server/pom.xml`. Vedi ADR-049.

### Task 5.3 — Flyway V1 schema (users + refresh_tokens) + integration test

- `server/src/main/resources/db/migration/V1__init_users_and_refresh_tokens.sql`:
  ```sql
  CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN'))
  );
  CREATE INDEX idx_users_email ON users(email);

  CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );
  CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
  CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
  ```
  > Note: `role` = `VARCHAR(32)` + `CHECK constraint` SQL standard (decisione utente §9.3 strada α + CR-F5-001). Funziona identico su MySQL 8.0.16+ (CHECK enforced) e su H2. Workbench-friendly: l'utente vede esplicitamente i 3 valori validi nel DDL e non può inserire altri valori a mano. Documentato in ADR-046.
- `FlywayMigrationIntegrationTest` (`@SpringBootTest` + profilo `test` H2): asserisce che le 2 tabelle esistono con le colonne attese dopo bootstrap.
- `FlywayMigrationMysqlIntegrationTest` (`@SpringBootTest` + profilo `it` MySQL nativo locale, `@Tag("slow")`): stessa asserzione su MySQL reale.

**Moduli toccati**: `server` (resources + test).
**Dipendenze**: 5.2.

> **Stato post-implementazione (commit `5d1fa06`, 2026-05-10)**: task chiuso secondo PLAN v1.1. File creati: `V1__init_users_and_refresh_tokens.sql`, `AbstractMysqlIntegrationTest.java` (con Testcontainers `@Container @ServiceConnection MySQLContainer<>("mysql:8.0")`), `FlywayMigrationIntegrationTest.java` (H2, 6/6 verdi), `FlywayMigrationMysqlIntegrationTest.java` (slow, non eseguito per assenza Docker daemon). Fast loop verde. **Retrofit pianificato in Task 5.3.bis (v1.2)**: refattorizzare `AbstractMysqlIntegrationTest` per puntare al MySQL nativo (`application-it.yml` già configurato), rimuovere `@Testcontainers`/`@Container`/`MySQLContainer`, aggiungere `flyway.clean() + flyway.migrate()` in `@BeforeEach` per isolamento stato. `FlywayMigrationMysqlIntegrationTest` resta invariato. Test slow eseguito per la prima volta dopo retrofit e setup user-side dei DB locali. Vedi ADR-049.

### Task 5.3.bis — Retrofit Testcontainers → MySQL nativo locale (v1.2 amendment)

**Razionale**: applicare ADR-049 (decisione utente 9.4-bis del 2026-05-16) al codice/config già committato nei Task 5.2 e 5.3 prima di proseguire con Task 5.4. Mantiene lineare la storia di `feature/5-server-skeleton` (un singolo commit retro che applica la nuova baseline) ed evita che Task 5.4+ erediti il pattern Testcontainers ormai superseded.

**Pre-requisito setup DB** (una tantum, su MySQL nativo già installato `localhost:3306`):

- **Script committati nel repo**:
  - `scripts/db/setup-local-mysql.sql` — idempotente (`CREATE DATABASE IF NOT EXISTS`, `CREATE USER IF NOT EXISTS`, `GRANT`, `FLUSH PRIVILEGES`) — crea i DB `dama_italiana_dev` + `dama_italiana_it` + utente `dama`@`localhost` con password `dama` (override via variabili SQL `@db_dev`, `@db_it`, `@app_user`, `@app_password`).
  - `scripts/db/teardown-local-mysql.sql` — reversibile (`DROP DATABASE IF EXISTS`, `DROP USER IF EXISTS`) per ricreare lo stato pulito quando serve.
- **Esecuzione**: Claude esegue lo script direttamente via `mysql.exe` (path `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`) con credenziali admin fornite dall'utente in una tantum (es. `root` + password). L'utente verifica il risultato in MySQL Workbench (DB `dama_italiana_dev` e `dama_italiana_it` visibili in tree-view, utente `dama` in Users and Privileges).
- **Modalità future** (post Task 5.3.bis): qualsiasi utente che cloni il repo potrà eseguire lo script manualmente con `mysql -u root -p < scripts/db/setup-local-mysql.sql` (documentato in README §1.a).

> Username/password app default `dama`/`dama` per minimizzare frizioni single-developer. Override via variabili SQL inline (`mysql --execute "SET @app_password='altropwd'; SOURCE setup-local-mysql.sql;"`) o tramite env var Spring (`SPRING_DATASOURCE_PASSWORD`) ai test/run-time.

**Lavoro Task 5.3.bis** (5 categorie):

0. **Script DB setup/teardown** (committati nel repo):
   - **Creare** `scripts/db/setup-local-mysql.sql` (idempotente, `IF NOT EXISTS` + `FLUSH PRIVILEGES`).
   - **Creare** `scripts/db/teardown-local-mysql.sql` (reversibile, `DROP ... IF EXISTS`).
   - **Eseguire** `setup-local-mysql.sql` via `mysql.exe -u <admin> -p < scripts/db/setup-local-mysql.sql` (credenziali admin fornite dall'utente).
   - **Verifica** post-esecuzione: `mysql -u dama -p<dama-pwd> -e "SHOW DATABASES;"` deve listare `dama_italiana_dev` e `dama_italiana_it`; utente Workbench-visible.

1. **Codice test**:
   - **Refactor** `server/src/test/java/com/damaitaliana/server/AbstractMysqlIntegrationTest.java`: rimuovere `@Testcontainers`, `@Container @ServiceConnection static MySQLContainer<>("mysql:8.0")` field; aggiungere `@Autowired Flyway flyway` + `@BeforeEach void resetSchema() { flyway.clean(); flyway.migrate(); }` per isolamento test-run. Annotazioni residue: `@SpringBootTest`, `@ActiveProfiles("it")`, `@Tag("slow")`. Javadoc aggiornato.
   - **Cancellare** `server/src/test/java/com/damaitaliana/server/TestServerApplication.java` (non più necessario).
   - `FlywayMigrationMysqlIntegrationTest.java` resta invariato (estende Abstract refattorizzato).
   - `ServerApplicationContextLoadsTest.java` resta invariato (usa profilo `test` H2).
   - `FlywayMigrationIntegrationTest.java` resta invariato (H2 fast loop).

2. **Configurazione**:
   - **Rescrivere** `server/src/main/resources/application-dev.yml`: datasource `jdbc:mysql://localhost:3306/dama_italiana_dev` (default, override via `SPRING_DATASOURCE_URL`), username/password default `dama`/`dama` overridabili, `spring.flyway.clean-disabled=false`. JWT secret default invariato (decisione 9.1).
   - **Rescrivere** `server/src/main/resources/application-it.yml`: datasource analogo su `dama_italiana_it`, `spring.flyway.clean-disabled=false` (necessario per `@BeforeEach` clean). JWT secret hardcoded invariato.
   - **Aggiornare** `server/src/main/resources/application.yml`: rimuovere commenti su Testcontainers; aggiornare riferimento ADR-044 + ADR-049.

3. **Build**:
   - **Modificare** `server/pom.xml`: rimuovere 3 dep test-scope: `org.springframework.boot:spring-boot-testcontainers`, `org.testcontainers:mysql`, `org.testcontainers:junit-jupiter`. Driver `com.mysql:mysql-connector-j` runtime resta. Flyway dep restano.

4. **Documentation**:
   - **Rescrivere** sezione README.md "Setup ambiente di sviluppo" §1 e §1.a: rimuovere riferimento Testcontainers + Docker prerequisito; aggiungere prerequisite SQL setup + comando dev `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev`.
   - **Aggiornare** tabella Prerequisiti README: Docker da "richiesto" a "non richiesto da F5+ per server" (resta come opzione futura).
   - **Aggiornare** sezione "Comandi standard" README: riga `spring-boot:test-run` rimossa o riadattata.

**Closure Task 5.3.bis**:
- DB locali `dama_italiana_dev` + `dama_italiana_it` creati con successo + utente `dama`@`localhost` configurato (verifica via Workbench).
- `mvn -pl server verify -DexcludedGroups=slow` BUILD SUCCESS (fast loop H2 invariato).
- `mvn -pl server verify` (slow inclusi) BUILD SUCCESS: `FlywayMigrationMysqlIntegrationTest` verde su MySQL nativo locale (prima esecuzione).
- 1 commit `refactor(server): retrofit Testcontainers → MySQL nativo locale (ADR-049, PLAN-fase-5 v1.2)` o split in 2 commit (`feat(scripts): add MySQL local setup/teardown scripts` + `refactor(server): remove Testcontainers, point dev+it to local MySQL`) se l'utente preferisce.
- `AI_CONTEXT.md` aggiornato: sotto-fase IMPLEMENTA continua, Task 5.3.bis chiuso, prossimo Task 5.4.

**Moduli toccati**: `scripts/db/` (nuovo) + `server` (test + resources + pom) + `README.md`.
**Dipendenze**: 5.3 (refattorizza file da esso creati) + credenziali admin MySQL fornite dall'utente per esecuzione setup script.

### Task 5.4 — `User` entity + repository + service + DTO + validation

- `entity/UserEntity.java` (`@Entity @Table(name="users")`, campi `id, username, email, passwordHash, role, createdAt, lastLoginAt`); `Role` enum `{USER, TOURNAMENT_ADMIN, SYSTEM_ADMIN}` mappato `@Enumerated(EnumType.STRING)`.
- `repository/UserRepository extends JpaRepository<UserEntity, Long>` con `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`.
- `service/UserService`: `register(RegisterRequest) → UserEntity` (controlla duplicati, hash password, save), `findByUsername`, `findById`, `updateLastLogin(userId)`.
- `dto/RegisterRequest`, `dto/LoginRequest`, `dto/UserResponse`, `dto/AuthResponse` (records con Bean Validation: `@NotBlank @Size(min=3, max=50) @Pattern("[a-zA-Z0-9_-]+")`, `@Email`, `@NotBlank @Size(min=8, max=72) @Pattern(".*[A-Za-z].*") @Pattern(".*\\d.*")`).
- `mapper/UserMapper`: static toResponse(entity).
- Unit test `UserServiceTest` (Mockito) + slice test `UserRepositorySliceTest` (`@DataJpaTest` H2).

**Moduli toccati**: `server`.
**Dipendenze**: 5.3.

### Task 5.5 — `JwtService` + `JwtProperties`

- `config/JwtProperties` (`@ConfigurationProperties("dama.jwt")`): `secret`, `accessTtlSeconds=900`, `refreshTtlSeconds=604800`. Validation: `secret` ≥ 32 bytes (HS256 minimum). Strategia per profilo (decisione 9.1): `prod` = throw on bootstrap se `${JWT_SECRET}` env var assente; `dev` = se assente, fallback a default hardcoded `dev-only-secret-change-me-32bytes-min-padding-xxxxxx` + `log.warn("⚠️ Using DEV-ONLY JWT secret — NEVER use in production")`; `test`/`it` = secret hardcoded letto da `application-{test,it}.yml`. Implementazione: `@PostConstruct` validator che ispeziona `Environment.getActiveProfiles()` per applicare la regola corretta.
- `auth/JwtService`: `issueAccess(UserEntity) → String`, `issueRefresh(UserEntity) → String`, `parse(String) → JwtClaims` (record con userId, username, role, type, expiresAt). Throws `JwtVerificationException` su parse/verify failure (signature, expiration, malformed).
- Claims: `sub=String.valueOf(userId)`, `username`, `role`, `iat`, `exp`, `type=ACCESS|REFRESH`.
- Unit test `JwtServiceTest`: issue → parse roundtrip, expiration, tampered signature, type mismatch (access usato come refresh), wrong secret.

**Moduli toccati**: `server`.
**Dipendenze**: 5.4 (per `UserEntity`).

### Task 5.6 — `RefreshTokenService` + entity + repository

- `entity/RefreshTokenEntity` (`@Entity @Table(name="refresh_tokens")`).
- `repository/RefreshTokenRepository`: `findByTokenHash(String)`, `revokeAllByUserId(Long)` (per logout-all futuro).
- `auth/RefreshTokenService`: `issue(UserEntity) → {String rawToken, RefreshTokenEntity persisted}` (genera raw via JwtService.issueRefresh, hash SHA-256 → persist); `verifyAndRotate(String rawToken) → {UserEntity, String newAccess, String newRefresh}` (parse JWT, lookup hash, check `revoked_at IS NULL && expires_at > now()`, mark old revoked, issue new); `revoke(String rawToken)` (idempotent).
- Unit test `RefreshTokenServiceTest` con `@DataJpaTest` H2.

**Moduli toccati**: `server`.
**Dipendenze**: 5.5.

### Task 5.7 — `SecurityConfig` + `JwtAuthenticationFilter` + `BCryptPasswordEncoder`

- `config/SecurityConfig`: `@EnableWebSecurity`, `SecurityFilterChain` con `csrf().disable()`, `sessionManagement(STATELESS)`, `authorizeHttpRequests` con whitelist `/v1/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/actuator/prometheus`; tutto il resto autenticato. Aggiungere `JwtAuthenticationFilter` prima di `UsernamePasswordAuthenticationFilter`.
- `BCryptPasswordEncoder` con `strength=12` (NFR-S-01).
- `auth/JwtAuthenticationFilter` (`OncePerRequestFilter`): legge `Authorization: Bearer <jwt>`, parse via `JwtService`, popola `SecurityContext` con `UsernamePasswordAuthenticationToken(userId, null, [Role.toAuthority()])`. Su parse failure → 401 (delegato a `AuthenticationEntryPoint` → `ProblemDetail`).
- CORS: `CorsConfigurationSource` con allowed origins `${dama.cors.allowed-origins:http://localhost:8080}` (env-driven).
- Slice test `SecurityConfigSliceTest`: `@WebMvcTest` con un controller dummy verifica 401 su missing token, 200 con token valido (mock `JwtService`).

**Moduli toccati**: `server`.
**Dipendenze**: 5.5 (JwtService).

### Task 5.8 — `AuthController`: `/auth/register` + `/auth/login`

- `auth/AuthController` (`@RestController @RequestMapping("/v1/auth")`):
  - `POST /register`: `@Valid RegisterRequest` → 201 + `AuthResponse`. 409 se duplicato (custom exception `UserAlreadyExistsException` → 409 in `GlobalExceptionHandler`).
  - `POST /login`: `@Valid LoginRequest` → 200 + `AuthResponse`. 401 se credenziali invalide (`BadCredentialsException` → 401, message generico "Invalid username or password").
  - `last_login_at` aggiornato in `UserService` post-login OK.
- Integration test `AuthRegisterIntegrationTest` (`@SpringBootTest` `RANDOM_PORT` + `TestRestTemplate` o `WebTestClient`, profilo `test` H2): casi happy path + duplicate username + duplicate email + validation errors (username troppo corto, email malformato, password debole) + risposta non rivela dettagli.
- Integration test `AuthLoginIntegrationTest`: register-then-login OK, wrong password → 401, unknown user → 401 (stesso messaggio), `last_login_at` aggiornato in DB.

**Moduli toccati**: `server`.
**Dipendenze**: 5.6 (RefreshTokenService) + 5.7 (Security).

### Task 5.9 — `/auth/refresh` + `/auth/logout`

- `AuthController` esteso:
  - `POST /refresh`: legge refresh token da header `Authorization: Bearer ...` (NON dal body — convenzione consolidata). Chiama `RefreshTokenService.verifyAndRotate` → 200 + `AuthResponse`. 401 se token invalido/scaduto/revocato.
  - `POST /logout`: legge refresh token da header. Chiama `RefreshTokenService.revoke` → 204. 401 se token già revocato/inesistente (NON 204 idempotente per chiarezza — il client che chiama logout 2 volte ha un bug e deve saperlo).
- Integration test `AuthRefreshIntegrationTest`: register → estrarre refresh → refresh → verifica nuovo access funziona + vecchio refresh non funziona più (rotation). Refresh scaduto → 401. Refresh con type=ACCESS (mismatch) → 401.
- Integration test `AuthLogoutIntegrationTest`: register → logout → verifica refresh non funziona più. Doppio logout → 401.

**Moduli toccati**: `server`.
**Dipendenze**: 5.8.

### Task 5.10 — `UserController`: `/users/me`

- `user/UserController` (`@RestController @RequestMapping("/v1/users")`):
  - `GET /me`: legge `userId` da `SecurityContext` (popolato da `JwtAuthenticationFilter`), carica `UserEntity` via `UserService.findById`, ritorna `UserResponse`. 404 se utente cancellato (edge case, normalmente impossibile se token valido).
- Integration test `UsersMeIntegrationTest`: register → me con access token → 200 con payload corretto (no password_hash, no internal fields). Senza token → 401. Token scaduto → 401. Token con userId inesistente → 404.

**Moduli toccati**: `server`.
**Dipendenze**: 5.7 (Security ⇒ SecurityContext popolato).

### Task 5.11 — Rate limiter (Caffeine) per `/auth/login` + `/auth/register`

- `config/RateLimitConfig`: 2 `Cache<String, AtomicInteger>` (key=client IP, value=counter); window 60s per login, 3600s per register.
- `auth/RateLimitFilter` (`OncePerRequestFilter` o interceptor MVC): applicato selettivamente. Su superamento → 429 + header `Retry-After: <seconds>`. Implementazione: sliding window con Caffeine `expireAfterWrite`.
- IP detection: header `X-Forwarded-For` first IP se presente (proxy-aware), altrimenti `request.getRemoteAddr()`.
- Integration test `RateLimitIntegrationTest`: 6 login consecutivi rapidi → 6° è 429. 4 register consecutivi → 4° è 429. Reset dopo TTL (test usa `@TestConfiguration` con TTL 2 secondi per rapidità).

**Moduli toccati**: `server`.
**Dipendenze**: 5.8 (controller esistenti).

### Task 5.12 — `GlobalExceptionHandler` + `ProblemDetail` (RFC 7807)

- `web/GlobalExceptionHandler` (`@RestControllerAdvice`):
  - `MethodArgumentNotValidException` → 400 ProblemDetail con `errors[]` (field, message).
  - `UserAlreadyExistsException` → 409.
  - `BadCredentialsException` → 401 (no detail leak).
  - `JwtVerificationException` → 401.
  - `RateLimitExceededException` → 429.
  - `EntityNotFoundException` → 404.
  - `Exception` (catch-all) → 500 senza stack trace nel body, log a `WARN` lato server.
- `application/problem+json` content-type su tutte le risposte di errore.
- Integration test `ExceptionHandlingIntegrationTest`: ogni caso d'errore restituisce ProblemDetail valido (asserisce shape + content-type + assenza di stack trace nel body).

**Moduli toccati**: `server`.
**Dipendenze**: 5.10 (necessario per coprire tutti i casi REST).

### Task 5.13 — OpenAPI 3.1 (springdoc) + `/swagger-ui` + security scheme bearer

- `config/OpenApiConfig` (`@Configuration`): `@Bean OpenAPI customOpenAPI()` con `info.title="Dama Italiana Server API"`, `info.version=@Value("${dama.version:0.5.0}")`, `securityScheme bearerAuth = type=HTTP scheme=bearer bearerFormat=JWT`.
- Annotare i 5 endpoint con `@Operation`, `@ApiResponses`, `@SecurityRequirement(name="bearerAuth")` dove applicabile.
- Verifica manuale: `curl http://localhost:8080/v3/api-docs | jq` ⇒ JSON OpenAPI 3.1 con tutti i 5 endpoint; visita `http://localhost:8080/swagger-ui/index.html` ⇒ UI funzionante con bottone "Authorize" per JWT.
- Test `OpenApiIntegrationTest`: GET `/v3/api-docs` → 200 + content-type `application/json` + parsing valido + presenza dei 5 path.

**Moduli toccati**: `server`.
**Dipendenze**: 5.10.

### Task 5.14 — Observability: Logback JSON + Correlation-Id filter + Prometheus + Health

- `server/src/main/resources/logback-spring.xml`:
  - profilo `prod` / `it`: JSON encoder via `LoggingEventCompositeJsonEncoder` con providers `mdc`, `loggerName`, `level`, `message`, `stackTrace`.
  - profilo `dev` / `test`: console pattern colorato standard.
- `web/CorrelationIdFilter` (`OncePerRequestFilter`, ordine PRIMA di tutto): legge header `X-Correlation-Id`, se assente genera UUID; setta `MDC.put("correlationId", ...)`; aggiunge response header echo. Cleanup `MDC.clear()` in finally.
- `application.yml`:
  - `management.endpoints.web.exposure.include=health,prometheus,info`
  - `management.endpoint.health.show-details=when-authorized`
  - `management.metrics.distribution.percentiles-histogram.http.server.requests=true`
- Custom metrics via `MeterRegistry` (auto-injected) in `UserService` (counter `auth_register_attempts_total`, gauge `users_total`) e `AuthController` (counter `auth_login_attempts_total{result}`).
- Integration test `CorrelationIdFilterIntegrationTest`: request senza header → response ha header generato; request con header → response echoa lo stesso valore; MDC propagation (verificato indirettamente via log capture o direct MDC inspection).
- Integration test `MetricsIntegrationTest`: dopo register+login, GET `/actuator/prometheus` contiene `auth_register_attempts_total` e `auth_login_attempts_total` con valori attesi.
- Integration test `HealthIntegrationTest`: GET `/actuator/health` → 200 + `{status: UP}` con component DB.

**Moduli toccati**: `server`.
**Dipendenze**: 5.13.

### Task 5.15 — Documentation finalization + TRACEABILITY + AI_CONTEXT + CHANGELOG

- ADR-044..048 finalizzati con sezione "Stato post-implementazione F5" (delta tra plan e realtà se presenti).
- Nuovo `package-info.java` per i sotto-package introdotti: `server.auth`, `server.user`, `server.config`, `server.web`, `server.entity`, `server.repository`, `server.service`, `server.dto`, `server.mapper`.
- `tests/TRACEABILITY.md`:
  - Nuova sezione "Acceptance criteria di Fase 5 (PLAN-fase-5.md §2.2)" con A5.1..A5.22 + status.
  - Nuove righe NFR: NFR-S-01, NFR-S-02, NFR-S-04, NFR-O-01, NFR-O-02, NFR-O-03, NFR-M-03.
  - Nuove righe FR-INT-01..05 (riferimento SPEC §9.5.1 + §11.2).
- `AI_CONTEXT.md`: chiusura sotto-fase IMPLEMENTA F5, transizione a REVIEW.
- `CHANGELOG.md` `[Unreleased]`: voce sintetica F5 con elenco endpoint, ADR, file count delta.

**Moduli toccati**: docs only.
**Dipendenze**: 5.14 (chiusura tutti i task code).

---

## 5. Strategia di test

Composizione (CLAUDE.md §2.4.1):

| Tipo | Tecnologia | Cosa testa |
|---|---|---|
| **Unit** (~40%) | JUnit 5 + Mockito + AssertJ | `JwtService`, `RateLimiter`, `UserMapper`, validators, exception mapping |
| **Slice integration** (~30%) | `@WebMvcTest` (controller), `@DataJpaTest` (repository) con H2 | Singolo layer isolato |
| **Full integration H2** (~25%) | `@SpringBootTest webEnvironment=RANDOM_PORT` + H2 | E2E auth flow su context completo, fast loop |
| **Full integration MySQL** (~5%) | `@SpringBootTest` + MySQL nativo locale `dama_italiana_it` + `@Tag("slow")` (ADR-049) | E2E auth flow su MySQL reale (compatibility, Flyway migration) |

### 5.1 Coverage target

| Modulo | Target | Note |
|---|---|---|
| `server` | ≥ 70% line+branch | NFR-M-02 ≥ 60% + delta nostro |

JaCoCo gate `haltOnFailure=true` su `server` come per `client`.

### 5.2 Integration MySQL nativo locale (v1.2 amendment, ADR-049)

- `AbstractMysqlIntegrationTest` base class: `@SpringBootTest @ActiveProfiles("it") @Tag("slow")`. **Niente `@Testcontainers`, niente `@Container`**. Datasource configurato da `application-it.yml` (default `jdbc:mysql://localhost:3306/dama_italiana_it`). Isolamento test-run via `@Autowired Flyway` + `@BeforeEach { flyway.clean(); flyway.migrate(); }`.
- Tag `slow` per escludere dal fast loop locale.
- `mvn -pl server verify -DexcludedGroups=slow` rimane veloce (H2 in-memory, no MySQL nativo richiesto).
- `mvn clean verify` (root, slow inclusi) richiede MySQL nativo localhost:3306 con DB `dama_italiana_it` + utente `dama` pre-configurati (setup user-side una tantum, vedi Task 5.3.bis).

### 5.3 Smoke e2e manuale (TEST-PLAN-fase-5.md §6)

Sequenza curl documentata di §1 con sign-off table per il maintainer:
- Step 0 (una tantum): setup DB `dama_italiana_dev` + utente `dama` via Workbench (vedi Task 5.3.bis + README).
- Step 1: `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev`
- Step 2-7: 6 curl call (register → login → me → refresh → logout + verify).
- Step 8: restart server, verify persistence (utente in `dama_italiana_dev` resta, refresh token revocato non funziona).

---

## 6. Rischi identificati

| # | Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|---|
| R1 | Discrepancy H2 vs MySQL su Flyway SQL | Media | Alta (test passano in fast loop, falliscono in CI/prod) | Test Flyway esplicito con MySQL container (Task 5.3); SQL standard ANSI when possible |
| R2 | BCrypt cost 12 rallenta test (~250ms/hash × N) | Alta | Media (suite time +20s) | Profilo `test` può overridare a cost 4 via `@TestConfiguration` se necessario; documentato nel commit |
| R3 | ~~Testcontainers richiede Docker~~ → **superseded da v1.2**. **Nuovo R3**: MySQL nativo richiesto come prerequisito dev/it (ADR-049) | Alta (è il design) | Bassa | Script SQL fornito in README per setup una tantum (≤ 60s di lavoro user); profilo `test` (H2) garantisce fast loop senza MySQL nativo; Docker non più richiesto. Vincolo cross-contamination test paralleli mitigato da convenzione single-runner (single-developer ok) |
| R4 | JWT secret default in `dev` accidentalmente usato in `prod` | Bassa (profili distinti) | Alta (catastrofico: token leakable) | (a) warning log al boot in `dev` molto visibile; (b) `prod` fail-fast se env var assente — impossibile partire prod senza secret valido; (c) test `JwtBootValidationTest` che asserisce fail-fast in profilo `prod` simulato |
| R5 | Rate limiter in-memory perde stato a restart | Bassa | Bassa (accettato in v1, ADR-047) | Documentato; futura migrazione Redis |
| R6 | OpenAPI generation espone endpoint senza protezione → leak schema | Bassa | Bassa (schema OpenAPI è di per sé info pubblica per API client) | Endpoint `/v3/api-docs` whitelist esplicita ma documentata; in produzione si può chiudere via env var |
| R7 | Refresh token storage hash collision | Estremamente bassa | Bassa (SHA-256 raw token, ~256 bit entropy) | Nessuna mitigazione necessaria — il raw token JWT ha già entropia alta |
| R8 | Race condition su rotation refresh token (doppio refresh concomitante) | Bassa | Media (entrambi i client perdono il token) | Transazione + lock pessimistico su `refresh_tokens.id` durante `verifyAndRotate`. Documentato nel codice |
| R9 | CORS troppo permissivo in dev finisce in prod | Media | Media | `dama.cors.allowed-origins` da env var; default `localhost:8080`; in prod DEVE essere overridato — documentato in ADR-044 |
| R10 | Schema `users.role` come VARCHAR vs ENUM (SPEC §8.4 dice ENUM) | Bassa | Bassa (logica equivalente) | Decisione documentata in ADR-046 — VARCHAR per portabilità H2/MySQL; vincolo applicativo via `@Enumerated(STRING)` JPA |

---

## 7. Stima

- **14 task** (Task 5.1 → 5.15, no 5.0).
- **Output prod stimato**: ~25-30 classi Java (entity, repository, service, controller, config, filter, exception, dto, mapper).
- **Output test stimato**: ~12-15 classi test (~80-100 test methods totali), di cui 2 `@Tag("slow")` Testcontainers MySQL.
- **Output docs**: 5 ADR nuovi (044-048), TRACEABILITY esteso (~10-15 righe nuove), CHANGELOG entry, package-info per ~9 nuovi sotto-package.
- **Resources**: 1 `application.yml`, 1 `logback-spring.xml`, 1 Flyway V1 SQL.
- **Tempi indicativi** (riferimento PLAN-fase-4.5: 12 task in ~10 sessioni): ~12-15 sessioni di lavoro.

---

## 8. Stop point sotto-fase PIANIFICA

Stop point richiesto da CLAUDE.md §2.1 prima di passare a IMPLEMENTA. L'utente è invitato a:

1. **Confermare lo scope F5** come definito in §1 (incluso §1.1 out of scope).
2. **Confermare i 22 acceptance criteria** A5.x di §2.2 (o richiedere modifiche).
3. **Confermare la decomposizione in 14 task** di §4 (ordine, granularità, dipendenze).
4. **Confermare la strategia di test** §5 (in particolare il target coverage 70% server e l'uso di Testcontainers MySQL `@Tag("slow")`).
5. **Confermare i 5 ADR proposti** (044-048) come unica modifica strutturale a `ARCHITECTURE.md` in F5.
6. **Decidere su scelte aperte** (vedi §9).

Nessuna SPEC change request emerge da F5 (segue verbatim §16/§17/§5/§9.5.1/§11.2). Se a fine fase emerge l'utilità di codificare FR-INT-01..05 nella SPEC §4.3, lo si farà come CR a closure REVIEW.

---

## 9. Decisioni risolte (post user approval 2026-05-10)

Tutte le 4 decisioni aperte sono state confermate dall'utente. Tracciato qui per audit + riferimento durante REVIEW.

### 9.1 Default JWT secret in profilo `dev` — **RESOLVED: B (default dev + warning), A in test/it/prod**

- **Risolta**: 2026-05-10.
- **Decisione**: `prod` = fail-fast env var (A); `dev` = default hardcoded `dev-only-secret-change-me-32bytes-min-padding-xxxxxx` con warning log (B); `test`/`it` = hardcoded YAML.
- **Razionale**: DX su `dev` (single-developer), sicurezza preservata su `prod` (impossibile partire senza env var valida).
- **Implementazione**: `JwtProperties.@PostConstruct` ispeziona `Environment.getActiveProfiles()`. Verificato da `JwtBootValidationTest`.
- **NO SPEC CR**: documentato in **ADR-045** con citazione esplicita SPEC §15.2 (spirito preservato — il default `dev-only-*` non è "secret di produzione").

### 9.2 BCrypt cost — **RESOLVED: A (cost 12 ovunque)**

- **Risolta**: 2026-05-10.
- **Decisione**: `cost=12` in tutti i profili (`dev`, `test`, `it`, `prod`).
- **Razionale**: aderenza letterale a NFR-S-01 SPEC; +20s sulla suite tollerabile finché < 60s totali.
- **Implementazione**: `BCryptPasswordEncoder(12)` bean unico per tutti i profili.

### 9.3 Schema `users.role` — **RESOLVED: strada α (CR-F5-001 a SPEC §8.4)**

- **Risolta**: 2026-05-10.
- **Decisione**: VARCHAR(32) + CHECK constraint SQL standard. Richiede modifica a SPEC §8.4 — vedi **CR-F5-001 §11**.
- **Razionale**: portabilità H2/MySQL + vincolo DB-level preservato (Workbench-friendly: utente vede i 3 valori nel DDL).
- **Implementazione**: `V1__init_users_and_refresh_tokens.sql` con `CONSTRAINT chk_users_role CHECK (role IN (...))`.
- **SPEC CR**: aperta come **CR-F5-001**, applicata in Task 5.1.a con stop point.

### 9.4 Infrastruttura MySQL dev — ~~**RESOLVED: Testcontainers**~~ → **SUPERSEDED da 9.4-bis (v1.2)**

- **Risolta originariamente**: 2026-05-10.
- **Decisione originaria**: profilo `dev` usa Testcontainers via `mvn spring-boot:test-run` con `TestServerApplication` + `@ServiceConnection MySQLContainer`. Profilo `it` stesso pattern con `@Tag("slow")`. Profilo `test` H2. Profilo `prod` MySQL nativo installato sul server (no Docker).
- **Stato (v1.2)**: **SUPERSEDED** il 2026-05-16. Implementata fedelmente nei Task 5.2 e 5.3 (commit `6640781`, `5d1fa06`); poi rivalutata all'apertura sessione successiva per vincoli risorse Docker Desktop + richiesta esplicita utente di parità prod. Vedi §9.4-bis.

### 9.4-bis Infrastruttura MySQL dev — **RESOLVED v1.2: MySQL nativo locale per dev+it (ADR-049)**

- **Risolta**: 2026-05-16.
- **Decisione**: profili `dev` e `it` puntano a MySQL nativo locale `localhost:3306` (DB `dama_italiana_dev` e `dama_italiana_it` rispettivamente, utente `dama`@`localhost` default). Comando dev: `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev`. Testcontainers rimosso completamente dal modulo `server`. Profilo `test` H2 in-memory (invariato). Profilo `prod` MySQL nativo env var (invariato).
- **Razionale utente**:
  1. **Parità prod-equivalent già da F5**: dev/it/prod usano stesso stack (MySQL nativo, stesso driver JDBC, stessa migration Flyway, stesso dialect Hibernate). Solo i nomi DB cambiano per isolamento ambiente.
  2. **Eliminazione prerequisito Docker**: Docker Desktop non praticabile sulla macchina dev utente (vincoli risorse disco).
  3. **MySQL nativo + Workbench già installati e in uso quotidiano** dall'utente: zero setup aggiuntivo lato stack, solo creazione 2 DB + 1 utente via script SQL.
  4. **Workbench experience stabile**: porta fissa 3306, ispezione DB sempre disponibile (vs porta random ad ogni boot Testcontainers).
- **Implementazione**: nuovo **Task 5.3.bis** (vedi §4) esegue il retrofit del codice/config già committato nei Task 5.2 e 5.3 + crea 2 nuovi script SQL (`scripts/db/setup-local-mysql.sql`, `scripts/db/teardown-local-mysql.sql`). Documentata in **ADR-049** (supersedes parts di ADR-044 §dev/it + ADR-048 §full-integration-MySQL).
- **NO SPEC CR**: SPEC §6.5 già lista MySQL 8 come DB Internet; SPEC non vincola HOW dev/it accedano a MySQL. ADR-018 (Docker Compose rimosso) **rafforzato** — Docker non è più prerequisito di alcun layer F5+ del modulo `server`.

---

## 10. Piano di chiusura fase (workflow CLAUDE.md §11)

1. Tutti i 14 task IMPLEMENTA chiusi → commit + push su `feature/5-server-skeleton`.
2. Sotto-fase REVIEW → `reviews/REVIEW-fase-5.md` con findings → fix → closure.
3. Sotto-fase TEST → `tests/TEST-PLAN-fase-5.md` con sign-off table manual smoke run.
4. Merge `feature/5-server-skeleton` → `develop` (`git merge --no-ff`).
5. Merge `develop` → `main` con commit `release: chiusura Fase 5`.
6. Tag `v0.5.0` su commit di merge in `main`.
7. Ritorno su `develop`.

---

## 11. SPEC change requests

### CR-F5-001 — `users.role` ENUM → VARCHAR(32) + CHECK constraint

- **Stato**: PENDING USER APPROVAL (stop point Task 5.1.a).
- **Sezione SPEC impattata**: §8.4 Schema dati MySQL (server centrale), tabella `users`.
- **Contesto**: la decisione utente §9.3 (strada α, 2026-05-10) richiede di rappresentare la colonna `role` come `VARCHAR(32) + CHECK constraint` invece di `ENUM` MySQL. Questo per:
  1. **Portabilità**: H2 (profilo `test` fast loop) gestisce ENUM in modo non standard; il CHECK constraint è SQL standard supportato identico da MySQL 8.0.16+ e H2.
  2. **Workbench-friendly**: l'utente vuole usare MySQL Workbench attivamente per ispezionare e (occasionalmente) editare il DB; con CHECK constraint il vincolo è applicato anche su scritture manuali in Workbench, evitando l'errore "scrivo `'admin'` invece di `'SYSTEM_ADMIN'`".
  3. **Behavior identico**: comportamento osservabile invariato — solo i 3 valori `'USER'`, `'TOURNAMENT_ADMIN'`, `'SYSTEM_ADMIN'` accettati a livello DB.
- **Diff proposto a SPEC §8.4**:
  ```diff
    users (
      id BIGINT PK AUTO_INCREMENT,
      username VARCHAR(50) UNIQUE NOT NULL,
      email VARCHAR(255) UNIQUE NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
  -   role ENUM('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN'),
  +   role VARCHAR(32) NOT NULL DEFAULT 'USER'
  +     CHECK (role IN ('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN')),
      created_at TIMESTAMP,
      last_login_at TIMESTAMP
    );
  ```
- **Decisione utente**: **APPROVED** in linea di principio (decisione §9.3 confermata 2026-05-10); diff applicato in Task 5.1.a con commit dedicato `docs(spec): CR-F5-001 users.role ENUM → VARCHAR + CHECK constraint`.
- **Annotazione `CHANGELOG.md`**: voce sotto `[Unreleased]` come "**SPEC v2.4** — CR-F5-001 ...".

> Nessuna altra CR in F5. La decisione 9.1 (JWT default in dev) è stata documentata come ADR-045 senza modifiche a SPEC §15.2 (interpretazione di buona fede dello "spirito" della clausola — il default `dev-only-*` non è un secret di produzione e il warning runtime esplicita la cosa).
>
> **v1.2 amendment (2026-05-16)**: la decisione 9.4-bis (MySQL nativo locale per dev+it, ADR-049) NON richiede CR a SPEC. SPEC §6.5 già vincola MySQL 8 come DB Internet; SPEC non vincola HOW dev/it accedano a MySQL. Documentazione: ADR-049 in `ARCHITECTURE.md`, voce dedicata in `CHANGELOG.md [Unreleased]`.

---

**FINE PIANO**
