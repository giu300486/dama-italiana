package com.damaitaliana.core.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.damaitaliana.shared.domain.Board;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.GameStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Behavioural test for the {@link Match} state machine and anti-cheat counter introduced in Task
 * 4.7. Lives in package {@code com.damaitaliana.core.match} so it can exercise the package-private
 * mutators {@code status(MatchStatus)}, {@code recordIllegalMove(Color)}, and {@code
 * clearIllegalMoves(Color)} that {@link MatchManager} (Task 4.8) will own.
 */
class MatchStateMachineTest {

  private static final Instant NOW = Instant.parse("2026-05-05T12:00:00Z");
  private static final UserRef ALICE = UserRef.anonymousLan("alice");
  private static final UserRef BOB = UserRef.anonymousLan("bob");

  @Nested
  class StatusTransitions {

    @Test
    void waitingToOngoingIsValid() {
      Match match = newMatchWithStatus(MatchStatus.WAITING);
      match.status(MatchStatus.ONGOING);
      assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
    }

    @Test
    void ongoingToFinishedIsValid() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      match.status(MatchStatus.FINISHED);
      assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void ongoingToAbortedIsValid() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      match.status(MatchStatus.ABORTED);
      assertThat(match.status()).isEqualTo(MatchStatus.ABORTED);
    }

    @Test
    void waitingToFinishedThrows() {
      Match match = newMatchWithStatus(MatchStatus.WAITING);
      assertThatIllegalStateException()
          .isThrownBy(() -> match.status(MatchStatus.FINISHED))
          .withMessageContaining("WAITING -> FINISHED");
      assertThat(match.status()).isEqualTo(MatchStatus.WAITING);
    }

    @Test
    void waitingToAbortedThrows() {
      Match match = newMatchWithStatus(MatchStatus.WAITING);
      assertThatIllegalStateException()
          .isThrownBy(() -> match.status(MatchStatus.ABORTED))
          .withMessageContaining("WAITING -> ABORTED");
      assertThat(match.status()).isEqualTo(MatchStatus.WAITING);
    }

    @Test
    void ongoingToWaitingRegressionThrows() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      assertThatIllegalStateException()
          .isThrownBy(() -> match.status(MatchStatus.WAITING))
          .withMessageContaining("ONGOING -> WAITING");
      assertThat(match.status()).isEqualTo(MatchStatus.ONGOING);
    }

    @ParameterizedTest
    @EnumSource(MatchStatus.class)
    void anyTransitionFromFinishedThrows(MatchStatus to) {
      Match match = newMatchWithStatus(MatchStatus.FINISHED);
      assertThatIllegalStateException()
          .isThrownBy(() -> match.status(to))
          .withMessageContaining("FINISHED -> " + to);
      assertThat(match.status()).isEqualTo(MatchStatus.FINISHED);
    }

    @ParameterizedTest
    @EnumSource(MatchStatus.class)
    void anyTransitionFromAbortedThrows(MatchStatus to) {
      Match match = newMatchWithStatus(MatchStatus.ABORTED);
      assertThatIllegalStateException()
          .isThrownBy(() -> match.status(to))
          .withMessageContaining("ABORTED -> " + to);
      assertThat(match.status()).isEqualTo(MatchStatus.ABORTED);
    }

    @ParameterizedTest
    @EnumSource(MatchStatus.class)
    void identityTransitionsAreRejected(MatchStatus same) {
      Match match = newMatchWithStatus(same);
      assertThatIllegalStateException().isThrownBy(() -> match.status(same));
      assertThat(match.status()).isEqualTo(same);
    }

    @Test
    void statusRejectsNullArgument() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      assertThatNullPointerException()
          .isThrownBy(() -> match.status(null))
          .withMessageContaining("newStatus");
    }
  }

  @Nested
  class AntiCheatCounter {

    @Test
    void countersStartAtZeroForBothColors() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isZero();
      assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();
    }

    @Test
    void recordIllegalMoveIncrementsOneColorOnly() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      match.recordIllegalMove(Color.WHITE);
      match.recordIllegalMove(Color.WHITE);

      assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(2);
      assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isZero();
    }

    @Test
    void clearIllegalMovesResetsOnlyTheGivenColor() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      match.recordIllegalMove(Color.WHITE);
      match.recordIllegalMove(Color.WHITE);
      match.recordIllegalMove(Color.BLACK);

      match.clearIllegalMoves(Color.WHITE);

      assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isZero();
      assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isEqualTo(1);
    }

    @Test
    void recordsForBothColorsAreIndependent() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      match.recordIllegalMove(Color.WHITE);
      match.recordIllegalMove(Color.BLACK);
      match.recordIllegalMove(Color.WHITE);
      match.recordIllegalMove(Color.BLACK);
      match.recordIllegalMove(Color.WHITE);

      assertThat(match.consecutiveIllegalMoves(Color.WHITE)).isEqualTo(3);
      assertThat(match.consecutiveIllegalMoves(Color.BLACK)).isEqualTo(2);
    }

    @Test
    void readAccessorRejectsNullColor() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      assertThatNullPointerException().isThrownBy(() -> match.consecutiveIllegalMoves(null));
    }

    @Test
    void recordIllegalMoveRejectsNullColor() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      assertThatNullPointerException().isThrownBy(() -> match.recordIllegalMove(null));
    }

    @Test
    void clearIllegalMovesRejectsNullColor() {
      Match match = newMatchWithStatus(MatchStatus.ONGOING);
      assertThatNullPointerException().isThrownBy(() -> match.clearIllegalMoves(null));
    }
  }

  private static Match newMatchWithStatus(MatchStatus status) {
    GameState initial =
        new GameState(Board.initial(), Color.WHITE, 0, List.of(), GameStatus.ONGOING);
    return new Match(
        MatchId.random(),
        ALICE,
        BOB,
        TimeControl.unlimited(),
        NOW,
        null,
        initial,
        -1L,
        status,
        null);
  }
}
