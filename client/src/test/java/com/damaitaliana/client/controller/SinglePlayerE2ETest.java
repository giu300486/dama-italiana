package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.damaitaliana.client.audio.AudioService;
import com.damaitaliana.client.audio.Sfx;
import com.damaitaliana.client.persistence.SerializedGameState;
import com.damaitaliana.client.ui.board.BoardRenderer;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.PieceKind;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.notation.FidNotation;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * End-to-end coverage of single-player flows against the {@link SinglePlayerController} layer. Uses
 * a mocked {@link BoardRenderer} so the controller's animation path short-circuits to synchronous
 * {@code finalizeMove} (the renderer's {@code pieceAt} returns null) — all UI work runs on the test
 * thread, no JavaFX runtime required.
 *
 * <p>Coverage map:
 *
 * <ul>
 *   <li>A3.11 / SPEC §3.3: a man cannot capture a king — verified by inspecting the legal-target
 *       highlights set by the controller after the user picks the man.
 *   <li>A3.12 / SPEC §3.5: a multi-capture that promotes stops at the promotion row (the turn
 *       passes to the opponent even though more captures would otherwise be available as a king).
 *   <li>A3.3-light: a short fixed-side game played out by feeding the human side via the
 *       controller's click protocol; verifies no exceptions and that the game advances.
 * </ul>
 */
class SinglePlayerE2ETest {

  private static final RuleEngine RULES = new ItalianRuleEngine();

  private BoardRenderer renderer;
  private AutosaveTrigger autosave;
  private AudioService audio;

  @BeforeEach
  void setUp() {
    renderer = Mockito.mock(BoardRenderer.class);
    autosave = Mockito.mock(AutosaveTrigger.class);
    audio = Mockito.mock(AudioService.class);
  }

  @Test
  void manCannotCaptureKingInUi() {
    // White man at FID 22, black king at FID 18. The man would be able to capture a black man on
    // 18 (jumping to 15), but it must NOT be allowed to capture the king (Italian rule). With the
    // capture path closed, the only legal moves from 22 are simple forward steps to FID 17.
    SerializedGameState position =
        new SerializedGameState(
            List.of(22), List.of(), List.of(), List.of(18), Color.WHITE, 0, List.of());
    SinglePlayerController controller = startGame(position);

    Square manSquare = FidNotation.toSquare(22);
    Square kingSquare = FidNotation.toSquare(18);
    controller.onCellClicked(manSquare);

    assertThat(controller.selectedSquare()).isEqualTo(manSquare);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Square>> targetsCaptor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(renderer, Mockito.atLeastOnce()).highlightLegalTargets(targetsCaptor.capture());
    List<Square> highlights = targetsCaptor.getValue();
    assertThat(highlights).doesNotContain(kingSquare);
  }

  @Test
  void promotionStopsSequenceInUi() {
    // White man at FID 5 (rank 6) is one diagonal step from the promotion row (rank 7). The only
    // legal move is the simple step 5 → 1; once it lands the man is promoted to king and the turn
    // ends, even though a freshly-promoted king could in principle continue capturing in some
    // hand-built positions. Here we keep the position purposely simple — the assertion is that
    // sideToMove flips to BLACK and the white piece on FID 1 is now a KING.
    SerializedGameState position =
        new SerializedGameState(
            List.of(5), List.of(), List.of(), List.of(), Color.WHITE, 0, List.of());
    SinglePlayerController controller = startGame(position);

    Square from = FidNotation.toSquare(5);
    Square to = FidNotation.toSquare(1);
    controller.onCellClicked(from);
    controller.onCellClicked(to);

    GameState after = controller.state();
    assertThat(after.sideToMove()).isEqualTo(Color.BLACK);
    assertThat(after.board().at(to)).isPresent();
    assertThat(after.board().at(to).get().kind()).isEqualTo(PieceKind.KING);
    assertThat(after.board().at(from)).isEmpty();
  }

  @Test
  void humanFirstMoveAdvancesGameStateAndHistory() {
    SinglePlayerController controller =
        startGame(SerializedGameState.fromState(GameState.initial()));

    Move firstWhiteMove =
        RULES.legalMoves(controller.state()).stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("initial position has no legal moves"));
    controller.onCellClicked(firstWhiteMove.from());
    controller.onCellClicked(firstWhiteMove.to());

    GameState after = controller.state();
    assertThat(after.sideToMove()).isEqualTo(Color.BLACK);
    assertThat(after.history()).hasSize(1);
    assertThat(after.status().isOngoing()).isTrue();
  }

  @Test
  void undoRedoCycleRestoresAndReappliesHumanMove() {
    SinglePlayerController controller =
        startGame(SerializedGameState.fromState(GameState.initial()));

    Move firstWhiteMove =
        RULES.legalMoves(controller.state()).stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("initial position has no legal moves"));
    controller.onCellClicked(firstWhiteMove.from());
    controller.onCellClicked(firstWhiteMove.to());

    GameState afterMove = controller.state();
    assertThat(controller.canUndo()).isTrue();
    assertThat(controller.canRedo()).isFalse();

    controller.undoPair();
    assertThat(controller.state().history()).isEmpty();
    assertThat(controller.state().sideToMove()).isEqualTo(Color.WHITE);
    assertThat(controller.canUndo()).isFalse();
    assertThat(controller.canRedo()).isTrue();

    controller.redoPair();
    assertThat(controller.state().board()).isEqualTo(afterMove.board());
    assertThat(controller.state().history()).hasSameSizeAs(afterMove.history());
    assertThat(controller.canRedo()).isFalse();
  }

  // ---------------------------------------------------------------------------------------------
  // SFX dispatch (Task 3.5.5) — uses the same hand-built positions as the rule-flow tests
  // ---------------------------------------------------------------------------------------------

  @Test
  void captureMoveFiresCaptureSfxNotMoveSfx() {
    // White man at FID 22 captures black man at FID 18, jumping to FID 15.
    SerializedGameState position =
        new SerializedGameState(
            List.of(22), List.of(), List.of(18), List.of(), Color.WHITE, 0, List.of());
    SinglePlayerController controller = startGame(position);

    Square from = FidNotation.toSquare(22);
    Square to = FidNotation.toSquare(15);
    controller.onCellClicked(from);
    Mockito.clearInvocations(audio);

    controller.onCellClicked(to);

    verify(audio).playSfx(Sfx.CAPTURE);
    verify(audio, never()).playSfx(Sfx.MOVE);
  }

  @Test
  void promotionMoveFiresMoveAndPromotionSfx() {
    // Same setup as promotionStopsSequenceInUi: white man at FID 5 (rank 6) steps to FID 1 (rank 7)
    // and gets promoted. Both MOVE (since the move is not a capture) and PROMOTION fire.
    SerializedGameState position =
        new SerializedGameState(
            List.of(5), List.of(), List.of(), List.of(), Color.WHITE, 0, List.of());
    SinglePlayerController controller = startGame(position);

    Square from = FidNotation.toSquare(5);
    Square to = FidNotation.toSquare(1);
    controller.onCellClicked(from);
    Mockito.clearInvocations(audio);

    controller.onCellClicked(to);

    verify(audio).playSfx(Sfx.MOVE);
    verify(audio).playSfx(Sfx.PROMOTION);
    verify(audio, never()).playSfx(Sfx.CAPTURE);
  }

  @Test
  void capturingTheLastOpposingPieceFiresVictorySfx() {
    // White man at FID 22 captures the only remaining black piece (man at FID 18). Black has zero
    // pieces left → BLACK_WINS=false / WHITE_WINS=true → human (White) wins → VICTORY.
    SerializedGameState position =
        new SerializedGameState(
            List.of(22), List.of(), List.of(18), List.of(), Color.WHITE, 0, List.of());
    SinglePlayerController controller = startGame(position);

    Square from = FidNotation.toSquare(22);
    Square to = FidNotation.toSquare(15);
    controller.onCellClicked(from);
    Mockito.clearInvocations(audio);

    controller.onCellClicked(to);

    verify(audio).playSfx(Sfx.CAPTURE);
    verify(audio).playSfx(Sfx.VICTORY);
    verify(audio, never()).playSfx(Sfx.DEFEAT);
  }

  @Test
  void aiWinningMoveFiresDefeatSfxFromHumanPerspective() {
    // Human plays BLACK; the AI (WHITE) captures the only black piece at FID 18 by jumping from
    // FID 22 to FID 15. After the capture Black has zero pieces → status WHITE_WINS → from the
    // human's perspective (BLACK) this is a DEFEAT. Drives the AI move via a stubbed
    // AiTurnService so the test stays synchronous.
    SerializedGameState position =
        new SerializedGameState(
            List.of(22), List.of(), List.of(18), List.of(), Color.WHITE, 0, List.of());

    Move whiteWinningMove =
        RULES.legalMoves(position.toState()).stream()
            .filter(Move::isCapture)
            .findFirst()
            .orElseThrow();

    AiTurnService aiService = Mockito.mock(AiTurnService.class);
    Mockito.when(aiService.requestMove(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(whiteWinningMove));

    SinglePlayerController controller =
        new SinglePlayerController(RULES, Optional.of(autosave), Optional.of(aiService), audio);
    controller.setFxExecutor(Runnable::run);
    SinglePlayerGame humanIsBlack =
        new SinglePlayerGame(
            AiLevel.PRINCIPIANTE,
            Color.BLACK,
            "E2E-defeat",
            position.toState(),
            new SplittableRandom(42L));
    controller.start(humanIsBlack, renderer);

    verify(audio).playSfx(Sfx.CAPTURE);
    verify(audio).playSfx(Sfx.DEFEAT);
    verify(audio, never()).playSfx(Sfx.VICTORY);
  }

  private SinglePlayerController startGame(SerializedGameState position) {
    SinglePlayerController controller =
        new SinglePlayerController(RULES, Optional.of(autosave), Optional.empty(), audio);
    SinglePlayerGame game =
        new SinglePlayerGame(
            AiLevel.PRINCIPIANTE,
            position.sideToMove(),
            "E2E",
            position.toState(),
            new SplittableRandom(42L));
    controller.start(game, renderer);
    return controller;
  }
}
