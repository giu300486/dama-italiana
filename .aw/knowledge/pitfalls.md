---
topic: pitfalls
verified: 2026-11-24
verifiedAt: { dama-italiana: d167e57 }
sources: [server/src/test/java/com/damaitaliana/server/config/SecurityConfigSliceTest.java, server/src/main/resources/application-test.yml, pom.xml, server/src/main/java/com/damaitaliana/server/config/JwtProperties.java, server/src/test/java/com/damaitaliana/server/FlywayMigrationMysqlIntegrationTest.java, CLAUDE.md]
---
# Pitfalls: do not reintroduce

- **Slice `@WebMvcTest`:** fa component scan anche di controller e `Filter` nuovi; ogni nuovo `@Component`
  web va aggiunto a `excludeFilters` di `SecurityConfigSliceTest` (oggi `AuthController`, `UserController`,
  `RateLimitFilter`), altrimenti lo slice fallisce. Candidato a refactor in review F5.
- **H2 condiviso tra classi di test:** `DB_CLOSE_DELAY=-1` in `application-test.yml` tiene vivo il DB e i
  `@SpringBootTest` non fanno rollback; usare prefissi univoci per classe (`flymig-`, `reg_`, `login_`, `me_`,
  `exh_`…) e query filtrate per prefisso.
- **springdoc vs Spring Boot:** springdoc 2.6.0 va in `NoSuchMethodError` con Boot 3.4.5 appena esiste un
  `@ControllerAdvice` (→ 500 su `/v3/api-docs`); serve la linea 2.8.x (`pom.xml`, `springdoc.version`).
- **Rate limit nei test:** disattivato nel profilo `test` (`dama.ratelimit.enabled: false`) perché tutti i test
  arrivano dallo stesso loopback; `RateLimitIntegrationTest` lo riattiva con `X-Forwarded-For` distinti, che
  valgono solo perché loopback è un proxy fidato (`server.tomcat.remoteip.internal-proxies`). Negli header
  usare IP pubblici di test (TEST-NET `192.0.2.x`, `198.51.100.x`, `203.0.113.x`): un IP che matcha la regex dei
  proxy fidati viene saltato dal `RemoteIpValve`. `RateLimitFilter` non deve leggere l'header da sé (ADR-047).
- **`JwtProperties`:** usa `EnvironmentAware` + setter, non constructor injection di `Environment` (senza
  `-parameters` Spring tenta il value-object binding e fallisce).
- **CHECK su MySQL 8:** l'errore 3819 non è tradotto in `DataIntegrityViolationException`; i test asseriscono
  `DataAccessException` (`FlywayMigrationMysqlIntegrationTest`).
- **Niente Docker/Testcontainers nel server** (ADR-049): dev e it usano MySQL nativo. CLAUDE.md §5 elenca ancora
  `docker-compose up/down`, superati da ADR-018: non usarli.
- **Sandbox di Claude:** i test `@SpringBootTest(RANDOM_PORT)` falliscono con "Unable to establish loopback
  connection" se lanciati dalla sandbox dei comandi, e (verificato il 2026-11-22) anche da Bash o PowerShell con
  `dangerouslyDisableSandbox`. Lanciarli dal pannello Terminale (`run_in_terminal`) o dal terminale dell'utente
  prima di dichiarare la build verde o rossa.
- **Metriche nei test:** `@SpringBootTest` disattiva l'export delle metriche, quindi `/actuator/prometheus` dà
  404 nei test; serve `@AutoConfigureObservability(tracing = false)` sulla classe (`MetricsIntegrationTest`).
  In produzione l'endpoint è esposto normalmente.
- **Controller UI Spring nel client:** un solo costruttore (o `@Autowired` su quello di produzione) e almeno un
  test che risolve il bean dal context reale; i test con `new` + mock non vedono gli errori di wiring.
