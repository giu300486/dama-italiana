---
topic: pitfalls
verified: 2026-11-21
verifiedAt: { dama-italiana: 0d47711 }
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
  arrivano dallo stesso loopback; `RateLimitIntegrationTest` lo riattiva con `X-Forwarded-For` distinti.
- **`JwtProperties`:** usa `EnvironmentAware` + setter, non constructor injection di `Environment` (senza
  `-parameters` Spring tenta il value-object binding e fallisce).
- **CHECK su MySQL 8:** l'errore 3819 non è tradotto in `DataIntegrityViolationException`; i test asseriscono
  `DataAccessException` (`FlywayMigrationMysqlIntegrationTest`).
- **Niente Docker/Testcontainers nel server** (ADR-049): dev e it usano MySQL nativo. CLAUDE.md §5 elenca ancora
  `docker-compose up/down`, superati da ADR-018: non usarli.
- **Sandbox di Claude:** i test `@SpringBootTest(RANDOM_PORT)` falliscono con "Unable to establish loopback
  connection" se lanciati dalla sandbox dei comandi; lanciarli senza sandbox o dal terminale dell'utente prima
  di dichiarare la build verde o rossa.
- **Controller UI Spring nel client:** un solo costruttore (o `@Autowired` su quello di produzione) e almeno un
  test che risolve il bean dal context reale; i test con `new` + mock non vedono gli errori di wiring.
