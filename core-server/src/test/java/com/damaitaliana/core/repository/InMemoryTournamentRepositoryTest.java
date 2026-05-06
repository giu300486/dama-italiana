package com.damaitaliana.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.core.match.TimeControl;
import com.damaitaliana.core.match.UserRef;
import com.damaitaliana.core.repository.inmemory.InMemoryTournamentRepository;
import com.damaitaliana.core.tournament.EliminationTournament;
import com.damaitaliana.core.tournament.RoundRobinTournament;
import com.damaitaliana.core.tournament.Tournament;
import com.damaitaliana.core.tournament.TournamentId;
import com.damaitaliana.core.tournament.TournamentStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Direct tests for {@link InMemoryTournamentRepository} covering save / findById / findByStatus —
 * the contract-test pattern of {@link MatchRepositoryContractTest} is overkill for a 3-method port,
 * so this class exercises the impl directly. Closes the F-002 coverage gap from REVIEW Fase 4.
 */
class InMemoryTournamentRepositoryTest {

  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");
  private static final TimeControl TC = TimeControl.unlimited();

  private InMemoryTournamentRepository repo;

  @BeforeEach
  void setUp() {
    repo = new InMemoryTournamentRepository();
  }

  @Test
  void saveThenFindByIdRoundtrips() {
    Tournament t =
        new EliminationTournament(
            TournamentId.random(), "Cup", TournamentStatus.CREATED, List.of(ALICE), TC);

    repo.save(t);

    assertThat(repo.findById(t.id())).contains(t);
  }

  @Test
  void saveOverwritesExistingTournamentSnapshot() {
    TournamentId id = TournamentId.random();
    Tournament before =
        new EliminationTournament(id, "Cup", TournamentStatus.CREATED, List.of(ALICE), TC);
    Tournament after =
        new EliminationTournament(id, "Cup", TournamentStatus.IN_PROGRESS, List.of(ALICE, BOB), TC);

    repo.save(before);
    repo.save(after);

    assertThat(repo.findById(id)).contains(after);
  }

  @Test
  void findByIdReturnsEmptyForUnknownId() {
    assertThat(repo.findById(TournamentId.random())).isEmpty();
  }

  @Test
  void findByStatusFiltersByExactStatusOnly() {
    Tournament created =
        new EliminationTournament(
            TournamentId.random(), "Cup-A", TournamentStatus.CREATED, List.of(), TC);
    Tournament inProgress =
        new RoundRobinTournament(
            TournamentId.random(), "League-B", TournamentStatus.IN_PROGRESS, List.of(ALICE), TC);
    Tournament finished =
        new EliminationTournament(
            TournamentId.random(), "Cup-C", TournamentStatus.FINISHED, List.of(ALICE, BOB), TC);
    repo.save(created);
    repo.save(inProgress);
    repo.save(finished);

    assertThat(repo.findByStatus(TournamentStatus.CREATED)).containsExactly(created);
    assertThat(repo.findByStatus(TournamentStatus.IN_PROGRESS)).containsExactly(inProgress);
    assertThat(repo.findByStatus(TournamentStatus.FINISHED)).containsExactly(finished);
  }

  @Test
  void findByStatusReturnsEmptyListForFreshlyCreatedRepository() {
    assertThat(repo.findByStatus(TournamentStatus.CREATED)).isEmpty();
    assertThat(repo.findByStatus(TournamentStatus.IN_PROGRESS)).isEmpty();
    assertThat(repo.findByStatus(TournamentStatus.FINISHED)).isEmpty();
  }

  @Test
  void findByStatusResultIsImmutableSnapshot() {
    Tournament a =
        new EliminationTournament(
            TournamentId.random(), "Cup-A", TournamentStatus.CREATED, List.of(), TC);
    repo.save(a);

    List<Tournament> snapshot = repo.findByStatus(TournamentStatus.CREATED);

    assertThat(snapshot).containsExactly(a);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> snapshot.add(a))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
