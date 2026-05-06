package com.damaitaliana.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit enforcement of the core-server transport-agnostic invariants (CLAUDE.md §8.7-8.8,
 * PLAN-fase-4 §4.12). The five rules below are the build-time gate that blocks the most likely
 * accidental scope creeps toward Fase 6/7 dependencies (UI, Spring Boot Web, embedded servlet
 * containers, JPA) and the bidirectional package-coupling between {@code match} and {@code
 * tournament} the SPEC explicitly forbids.
 *
 * <p>Why classpath import (not {@code @AnalyzeClasses}): the classpath at test time pulls in
 * Spring, Mockito, Jackson, JUnit, etc. — analyzing all of them would either need broad exclusions
 * or hang on huge graphs. Importing strictly from the {@code com.damaitaliana.core} package is
 * sharper, faster, and matches what the rules want to assert.
 */
class CoreServerArchitectureTest {

  private static final JavaClasses CORE_SERVER_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
          .importPackages("com.damaitaliana.core");

  // --- 1: no JavaFX ---------------------------------------------------------

  @Test
  void coreServerMustNotDependOnJavaFx() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("javafx..")
            .because(
                "core-server is transport-agnostic (CLAUDE.md §8.7-8.8) — JavaFX is the client UI"
                    + " concern, must not leak into core-server.");

    rule.check(CORE_SERVER_CLASSES);
  }

  // --- 2: no Spring Boot Web ------------------------------------------------

  @Test
  void coreServerMustNotDependOnSpringBootWeb() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.boot.web..")
            .because(
                "core-server provides the StompCompatiblePublisher port; the WebSocket transport"
                    + " (and any embedded HTTP container it implies) lives in server (F6) and"
                    + " client LAN host (F7).");

    rule.check(CORE_SERVER_CLASSES);
  }

  // --- 3: no Tomcat / Jetty -------------------------------------------------

  @Test
  void coreServerMustNotDependOnTomcatOrJetty() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.apache.tomcat..", "org.eclipse.jetty..")
            .because(
                "core-server has no embedded servlet container; the LAN host (client F7) uses"
                    + " Jetty and the Internet server (F6) uses Tomcat — both via their own"
                    + " spring-boot-starter dependencies.");

    rule.check(CORE_SERVER_CLASSES);
  }

  // --- 4: no JPA / Hibernate ------------------------------------------------

  @Test
  void coreServerMustNotDependOnJpaOrHibernate() {
    ArchRule rule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
            .because(
                "core-server defines plain repository ports (CLAUDE.md §8.8); the JPA adapter"
                    + " lives in server (F6) and reuses the contract tests written in F4.");

    rule.check(CORE_SERVER_CLASSES);
  }

  // --- 5: sub-package layering ---------------------------------------------

  @Test
  void matchPackageMustNotDependOnTournamentPackage() {
    // match -> tournament dependency would couple match logic to tournament concerns. The reverse
    // (tournament -> match) is currently allowed because Tournament records reference UserRef +
    // TimeControl from match (Task 4.3 deviation, documented in Tournament.java Javadoc). Once
    // F8/F9 land we may revisit and extract the common types into a third sub-package. This rule
    // pins down the half of the bidirectional coupling we are NOT willing to allow.
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.damaitaliana.core.match..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.damaitaliana.core.tournament..")
            .because(
                "Match logic must stay independent of tournament logic. The reverse direction"
                    + " (tournament -> match) is the documented Task 4.3 acceptance for shared"
                    + " value types; this test prevents it from becoming bidirectional.");

    rule.check(CORE_SERVER_CLASSES);
  }
}
