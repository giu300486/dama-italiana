package com.damaitaliana.core.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.repository.inmemory.InMemoryTournamentRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Skeleton-coverage tests for {@link TournamentEngineImpl} (PLAN-fase-4 §4.10). Verifies that the
 * registration phase (createTournament + registerParticipant) is fully wired and that the start
 * phase plus the bracket / round-robin generators throw {@link UnsupportedOperationException}
 * (deferred to Fase 8/9, PLAN risk R-2). Also smoke-tests {@link NoOpTieBreakerPolicy} as the Fase
 * 4 fallback policy.
 */
class TournamentEngineSkeletonTest {

  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final UserRef CARLO = UserRef.anonymousLan("carlo");
  private static final TimeControl TC = TimeControl.unlimited();

  private InMemoryTournamentRepository repo;
  private TournamentEngineImpl engine;

  @BeforeEach
  void setUp() {
    repo = new InMemoryTournamentRepository();
    engine = new TournamentEngineImpl(repo);
  }

  // --- createTournament ----------------------------------------------------

  @Nested
  class CreateTournament {

    @Test
    void singleEliminationProducesEliminationTournamentWithStatusCreated() {
      Tournament tournament =
          engine.createTournament(
              new TournamentSpec("Cup 2026", TournamentFormat.SINGLE_ELIMINATION, TC, 8));

      assertThat(tournament).isInstanceOf(EliminationTournament.class);
      assertThat(tournament.id()).isNotNull();
      assertThat(tournament.id().value()).isNotNull();
      assertThat(tournament.name()).isEqualTo("Cup 2026");
      assertThat(tournament.status()).isEqualTo(TournamentStatus.CREATED);
      assertThat(tournament.participants()).isEmpty();
      assertThat(tournament.timeControl()).isEqualTo(TC);
    }

    @Test
    void roundRobinProducesRoundRobinTournamentWithStatusCreated() {
      Tournament tournament =
          engine.createTournament(
              new TournamentSpec("League 2026", TournamentFormat.ROUND_ROBIN, TC, 6));

      assertThat(tournament).isInstanceOf(RoundRobinTournament.class);
      assertThat(tournament.status()).isEqualTo(TournamentStatus.CREATED);
      assertThat(tournament.participants()).isEmpty();
    }

    @Test
    void persistsTheTournamentSnapshot() {
      Tournament tournament =
          engine.createTournament(
              new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 4));

      assertThat(repo.findById(tournament.id())).contains(tournament);
    }

    @Test
    void distinctCallsProduceDistinctIds() {
      Tournament a =
          engine.createTournament(
              new TournamentSpec("A", TournamentFormat.SINGLE_ELIMINATION, TC, 4));
      Tournament b =
          engine.createTournament(
              new TournamentSpec("B", TournamentFormat.SINGLE_ELIMINATION, TC, 4));

      assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void rejectsNullSpec() {
      assertThatNullPointerException().isThrownBy(() -> engine.createTournament(null));
    }
  }

  // --- registerParticipant -------------------------------------------------

  @Nested
  class RegisterParticipant {

    @Test
    void appendsParticipantAndPersistsUpdatedSnapshot() {
      Tournament created =
          engine.createTournament(
              new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 8));

      Tournament after = engine.registerParticipant(created.id(), ALICE);

      assertThat(after.participants()).containsExactly(ALICE);
      assertThat(after.id()).isEqualTo(created.id());
      assertThat(after.status()).isEqualTo(TournamentStatus.CREATED);
      assertThat(repo.findById(created.id())).contains(after);
    }

    @Test
    void multipleRegistrationsPreserveOrder() {
      Tournament created =
          engine.createTournament(
              new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 8));

      engine.registerParticipant(created.id(), ALICE);
      engine.registerParticipant(created.id(), BOB);
      Tournament after = engine.registerParticipant(created.id(), CARLO);

      assertThat(after.participants()).containsExactly(ALICE, BOB, CARLO);
    }

    @Test
    void rejectsDoubleRegistration() {
      Tournament created =
          engine.createTournament(
              new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 8));
      engine.registerParticipant(created.id(), ALICE);

      assertThatIllegalArgumentException()
          .isThrownBy(() -> engine.registerParticipant(created.id(), ALICE))
          .withMessageContaining("already registered");
    }

    @Test
    void rejectsUnknownTournamentId() {
      TournamentId unknown = TournamentId.random();

      assertThatExceptionOfType(NoSuchElementException.class)
          .isThrownBy(() -> engine.registerParticipant(unknown, ALICE))
          .withMessageContaining(unknown.toString());
    }

    @Test
    void preservesFormatVariant() {
      Tournament created =
          engine.createTournament(
              new TournamentSpec("League", TournamentFormat.ROUND_ROBIN, TC, 6));

      Tournament after = engine.registerParticipant(created.id(), ALICE);

      assertThat(after).isInstanceOf(RoundRobinTournament.class);
    }

    @Test
    void rejectsNullArgs() {
      assertThatNullPointerException().isThrownBy(() -> engine.registerParticipant(null, ALICE));
      assertThatNullPointerException()
          .isThrownBy(() -> engine.registerParticipant(TournamentId.random(), null));
    }
  }

  // --- startTournament ------------------------------------------------------

  @Nested
  class StartTournament {

    @Test
    void throwsUnsupportedOperationDeferredToF8() {
      Tournament created =
          engine.createTournament(
              new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 8));

      assertThatThrownBy(() -> engine.startTournament(created.id()))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("deferred");
    }

    @Test
    void rejectsNullId() {
      assertThatNullPointerException().isThrownBy(() -> engine.startTournament(null));
    }
  }

  // --- findById -------------------------------------------------------------

  @Nested
  class FindById {

    @Test
    void returnsPersistedTournament() {
      Tournament created =
          engine.createTournament(
              new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 4));

      assertThat(engine.findById(created.id())).contains(created);
    }

    @Test
    void returnsEmptyForUnknownId() {
      assertThat(engine.findById(TournamentId.random())).isEqualTo(Optional.empty());
    }
  }

  // --- generators stubs -----------------------------------------------------

  @Nested
  class StubGenerators {

    @Test
    void bracketGeneratorThrowsDeferredToF8() {
      BracketGenerator generator = new StubBracketGenerator();

      assertThatThrownBy(() -> generator.generate(List.of(ALICE, BOB)))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("deferred to Fase 8");
    }

    @Test
    void roundRobinSchedulerThrowsDeferredToF9() {
      RoundRobinScheduler scheduler = new StubRoundRobinScheduler();

      assertThatThrownBy(() -> scheduler.schedule(List.of(ALICE, BOB, CARLO)))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("deferred to Fase 9");
    }
  }

  // --- NoOpTieBreakerPolicy --------------------------------------------------

  @Nested
  class NoOpPolicy {

    private final TieBreakerPolicy policy = new NoOpTieBreakerPolicy();

    @Test
    void returnsInputUnchanged() {
      RoundRobinTournament tournament =
          new RoundRobinTournament(
              TournamentId.random(),
              "League",
              TournamentStatus.CREATED,
              List.of(ALICE, BOB, CARLO),
              TC);

      List<UserRef> tied = List.of(ALICE, BOB, CARLO);
      List<UserRef> resolved = policy.resolveTies(tied, tournament);

      assertThat(resolved).containsExactlyElementsOf(tied);
    }

    @Test
    void returnsEmptyForEmptyInput() {
      RoundRobinTournament tournament =
          new RoundRobinTournament(
              TournamentId.random(), "League", TournamentStatus.CREATED, List.of(), TC);

      assertThat(policy.resolveTies(List.of(), tournament)).isEmpty();
    }

    @Test
    void rejectsNullArgs() {
      RoundRobinTournament tournament =
          new RoundRobinTournament(
              TournamentId.random(), "League", TournamentStatus.CREATED, List.of(), TC);

      assertThatNullPointerException().isThrownBy(() -> policy.resolveTies(null, tournament));
      assertThatNullPointerException().isThrownBy(() -> policy.resolveTies(List.of(), null));
    }
  }

  // --- TournamentSpec validation -------------------------------------------

  @Nested
  class TournamentSpecValidation {

    @Test
    void rejectsBlankName() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new TournamentSpec(" ", TournamentFormat.SINGLE_ELIMINATION, TC, 4))
          .withMessageContaining("name must not be blank");
    }

    @Test
    void rejectsTooFewParticipants() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 1))
          .withMessageContaining("maxParticipants must be >= 2");
    }

    @Test
    void rejectsNullFields() {
      assertThatNullPointerException()
          .isThrownBy(() -> new TournamentSpec(null, TournamentFormat.SINGLE_ELIMINATION, TC, 4));
      assertThatNullPointerException().isThrownBy(() -> new TournamentSpec("Cup", null, TC, 4));
      assertThatNullPointerException()
          .isThrownBy(
              () -> new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, null, 4));
    }
  }

  // --- registration-closed guard placed last so we don't pollute other tests --

  @Test
  void registrationClosedAfterStartIsTreatedAsIllegalState() {
    // F4 cannot transition status to IN_PROGRESS via the engine (startTournament throws). To
    // prove the status guard exists we persist a CREATED→IN_PROGRESS-shaped snapshot directly
    // via the repository, then attempt to register — same pattern Fase 8/9 will exercise.
    Tournament created =
        engine.createTournament(
            new TournamentSpec("Cup", TournamentFormat.SINGLE_ELIMINATION, TC, 4));
    Tournament inProgress =
        new EliminationTournament(
            created.id(),
            created.name(),
            TournamentStatus.IN_PROGRESS,
            created.participants(),
            created.timeControl());
    repo.save(inProgress);

    assertThatIllegalStateException()
        .isThrownBy(() -> engine.registerParticipant(created.id(), ALICE))
        .withMessageContaining("registration closed");
  }
}
