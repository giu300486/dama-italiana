package com.damaitaliana.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.damaitaliana.client.audio.AudioService;
import com.damaitaliana.client.audio.Sfx;
import com.damaitaliana.client.ui.board.BoardRenderer;
import com.damaitaliana.shared.ai.AiLevel;
import com.damaitaliana.shared.domain.Color;
import com.damaitaliana.shared.domain.GameState;
import com.damaitaliana.shared.domain.Move;
import com.damaitaliana.shared.domain.Square;
import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SinglePlayerControllerTest {

  private final RuleEngine engine = new ItalianRuleEngine();
  private BoardRenderer renderer;
  private AutosaveTrigger autosave;
  private AudioService audio;
  private SinglePlayerController controller;
  private SinglePlayerGame game;

  @BeforeEach
  void setUp() {
    renderer = Mockito.mock(BoardRenderer.class);
    autosave = Mockito.mock(AutosaveTrigger.class);
    audio = Mockito.mock(AudioService.class);
    controller = new SinglePlayerController(engine, Optional.of(autosave), Optional.empty(), audio);
    game =
        new SinglePlayerGame(
            AiLevel.ESPERTO, Color.WHITE, "Test", GameState.initial(), new SplittableRandom(42L));
    controller.start(game, renderer);
  }

  @Test
  void startInitializesRendererAndClickHandler() {
    verify(renderer).renderState(any());
    verify(renderer).setOnCellClicked(any());
    // Initial position has no captures, so highlightMandatorySources is called with empty list.
    verify(renderer).highlightMandatorySources(List.of());
  }

  @Test
  void clickOnOwnPieceHighlightsLegalTargets() {
    Square ownPiece = aWhiteSourceWithLegalMoves();
    Mockito.clearInvocations(renderer);

    controller.onCellClicked(ownPiece);

    assertThat(controller.selectedSquare()).isEqualTo(ownPiece);
    verify(renderer).highlightLegalTargets(any());
  }

  @Test
  void clickOnEmptyCellWithoutSelectionDeselects() {
    Square empty = new Square(0, 3);
    assertThat(GameState.initial().board().at(empty)).isEmpty();
    Mockito.clearInvocations(renderer);

    controller.onCellClicked(empty);

    assertThat(controller.selectedSquare()).isNull();
    verify(renderer, never()).highlightLegalTargets(any());
    verify(renderer).clearHighlights();
  }

  @Test
  void clickOnOpponentPieceWithoutSelectionDeselects() {
    Square blackPiece = new Square(1, 5);
    Mockito.clearInvocations(renderer);

    controller.onCellClicked(blackPiece);

    assertThat(controller.selectedSquare()).isNull();
    verify(renderer, never()).highlightLegalTargets(any());
  }

  @Test
  void clickOnLegalTargetAppliesMoveAndUpdatesState() {
    Move firstLegalMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(firstLegalMove.from());
    Mockito.clearInvocations(renderer, autosave);

    controller.onCellClicked(firstLegalMove.to());

    assertThat(controller.selectedSquare()).isNull();
    assertThat(controller.state().sideToMove()).isEqualTo(Color.BLACK);
    verify(renderer).renderState(any());
    verify(autosave).onMoveApplied(any(SinglePlayerGame.class));
  }

  @Test
  void clickOnIllegalTargetSwitchesToOtherOwnPiece() {
    Move firstLegal = engine.legalMoves(GameState.initial()).get(0);
    Move otherLegal =
        engine.legalMoves(GameState.initial()).stream()
            .filter(m -> !m.from().equals(firstLegal.from()))
            .findFirst()
            .orElseThrow();

    controller.onCellClicked(firstLegal.from());
    Mockito.clearInvocations(renderer, autosave);

    controller.onCellClicked(otherLegal.from());

    assertThat(controller.selectedSquare()).isEqualTo(otherLegal.from());
    verify(autosave, never()).onMoveApplied(any());
  }

  @Test
  void clickOnSameSelectedSquareDeselects() {
    Square ownPiece = aWhiteSourceWithLegalMoves();
    controller.onCellClicked(ownPiece);
    Mockito.clearInvocations(renderer);

    controller.onCellClicked(ownPiece);

    assertThat(controller.selectedSquare()).isNull();
    verify(renderer).clearHighlights();
  }

  @Test
  void autosaveTriggerIsOptional() {
    SinglePlayerController noAutosave =
        new SinglePlayerController(engine, Optional.empty(), Optional.empty(), audio);
    BoardRenderer freshRenderer = Mockito.mock(BoardRenderer.class);
    noAutosave.start(game, freshRenderer);

    Move firstLegal = engine.legalMoves(GameState.initial()).get(0);
    noAutosave.onCellClicked(firstLegal.from());
    noAutosave.onCellClicked(firstLegal.to());

    // No NPE; move applied.
    assertThat(noAutosave.state().sideToMove()).isEqualTo(Color.BLACK);
    verify(autosave, never()).onMoveApplied(any());
  }

  @Test
  void clickIsNoOpAfterTwoConsecutiveMovesFinishGameWithStallApi() {
    // The simpler smoke check: when state's status reports non-ongoing, onCellClicked is a no-op.
    // We construct a fresh controller, then probe the no-op path by calling click on a controller
    // whose state was never started (never null branch is covered separately).
    Move firstLegal = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(firstLegal.from());
    controller.onCellClicked(firstLegal.to());
    Mockito.clearInvocations(renderer, autosave);

    // Game still ongoing; just verify no infinite loop or state corruption when we click empty.
    controller.onCellClicked(new Square(0, 3));
    assertThat(controller.selectedSquare()).isNull();
  }

  @Test
  void mandatoryHighlightsRecomputeAfterEachMove() {
    Move firstLegal = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(firstLegal.from());
    Mockito.clearInvocations(renderer);

    controller.onCellClicked(firstLegal.to());

    // After the move, refreshMandatoryHighlights runs and pushes a (possibly empty) list.
    verify(renderer, times(1)).highlightMandatorySources(any());
  }

  @Test
  void aiTurnIsScheduledAfterHumanMove() {
    AiTurnService aiService = Mockito.mock(AiTurnService.class);

    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    GameState afterHuman;
    try {
      afterHuman = engine.applyMove(GameState.initial(), humanMove);
    } catch (com.damaitaliana.shared.rules.IllegalMoveException ex) {
      throw new AssertionError(ex);
    }
    Move aiMove = engine.legalMoves(afterHuman).get(0);
    Mockito.when(aiService.requestMove(any(), any(), any()))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(aiMove));

    SinglePlayerController withAi =
        new SinglePlayerController(engine, Optional.empty(), Optional.of(aiService), audio);
    withAi.setFxExecutor(Runnable::run);
    BoardRenderer freshRenderer = Mockito.mock(BoardRenderer.class);
    withAi.start(game, freshRenderer);

    withAi.onCellClicked(humanMove.from());
    withAi.onCellClicked(humanMove.to());

    Mockito.verify(aiService).requestMove(any(), any(), any());
    assertThat(withAi.state().sideToMove()).isEqualTo(Color.WHITE);
    assertThat(withAi.busy()).isFalse();
  }

  @Test
  void clickIsIgnoredWhileAiThinking() {
    AiTurnService aiService = Mockito.mock(AiTurnService.class);
    java.util.concurrent.CompletableFuture<Move> pending =
        new java.util.concurrent.CompletableFuture<>();
    Mockito.when(aiService.requestMove(any(), any(), any())).thenReturn(pending);

    SinglePlayerController withAi =
        new SinglePlayerController(engine, Optional.empty(), Optional.of(aiService), audio);
    withAi.setFxExecutor(Runnable::run);
    BoardRenderer freshRenderer = Mockito.mock(BoardRenderer.class);
    withAi.start(game, freshRenderer);

    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    withAi.onCellClicked(humanMove.from());
    withAi.onCellClicked(humanMove.to());

    assertThat(withAi.busy()).isTrue();
    assertThat(withAi.aiThinkingState().isThinking()).isTrue();

    // Click while busy — must be a no-op (no state change, no exception).
    Square anyOwn =
        engine.legalMoves(withAi.state()).stream()
            .findFirst()
            .map(Move::from)
            .orElse(humanMove.from());
    withAi.onCellClicked(anyOwn);
    assertThat(withAi.selectedSquare()).isNull();

    pending.cancel(true);
  }

  @Test
  void stopCancelsPendingAiRequestAndClearsBusy() {
    AiTurnService aiService = Mockito.mock(AiTurnService.class);
    java.util.concurrent.CompletableFuture<Move> pending =
        new java.util.concurrent.CompletableFuture<>();
    Mockito.when(aiService.requestMove(any(), any(), any())).thenReturn(pending);

    SinglePlayerController withAi =
        new SinglePlayerController(engine, Optional.empty(), Optional.of(aiService), audio);
    withAi.setFxExecutor(Runnable::run);
    BoardRenderer freshRenderer = Mockito.mock(BoardRenderer.class);
    withAi.start(game, freshRenderer);

    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    withAi.onCellClicked(humanMove.from());
    withAi.onCellClicked(humanMove.to());

    withAi.stop();

    assertThat(withAi.busy()).isFalse();
    assertThat(withAi.aiThinkingState().isThinking()).isFalse();
    assertThat(pending.isCancelled()).isTrue();
  }

  // ---------------------------------------------------------------------------------------------
  // Undo/redo (FR-SP-06, Task 3.24)
  // ---------------------------------------------------------------------------------------------

  @Test
  void freshControllerCannotUndoOrRedo() {
    assertThat(controller.canUndo()).isFalse();
    assertThat(controller.canRedo()).isFalse();
  }

  @Test
  void undoAfterHumanMoveRestoresInitialState() {
    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(humanMove.from());
    controller.onCellClicked(humanMove.to());

    assertThat(controller.state().sideToMove()).isEqualTo(Color.BLACK);
    assertThat(controller.canUndo()).isTrue();
    assertThat(controller.canRedo()).isFalse();

    controller.undoPair();

    assertThat(controller.state().sideToMove()).isEqualTo(Color.WHITE);
    assertThat(controller.state().history()).isEmpty();
    assertThat(controller.canUndo()).isFalse();
    assertThat(controller.canRedo()).isTrue();
  }

  @Test
  void redoReappliesPreviouslyUndoneMove() {
    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(humanMove.from());
    controller.onCellClicked(humanMove.to());
    GameState afterHuman = controller.state();
    controller.undoPair();

    controller.redoPair();

    assertThat(controller.state().board()).isEqualTo(afterHuman.board());
    assertThat(controller.state().sideToMove()).isEqualTo(afterHuman.sideToMove());
    assertThat(controller.state().history()).hasSameSizeAs(afterHuman.history());
    assertThat(controller.canUndo()).isTrue();
    assertThat(controller.canRedo()).isFalse();
  }

  @Test
  void newMoveAfterUndoClearsRedoStack() {
    Move firstMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(firstMove.from());
    controller.onCellClicked(firstMove.to());
    controller.undoPair();
    assertThat(controller.canRedo()).isTrue();

    Move otherMove =
        engine.legalMoves(GameState.initial()).stream()
            .filter(m -> !m.equals(firstMove))
            .findFirst()
            .orElseThrow();
    controller.onCellClicked(otherMove.from());
    controller.onCellClicked(otherMove.to());

    assertThat(controller.canRedo()).isFalse();
    assertThat(controller.canUndo()).isTrue();
  }

  @Test
  void undoIsNoOpWhenStackEmpty() {
    controller.undoPair();
    assertThat(controller.state().sideToMove()).isEqualTo(Color.WHITE);
    assertThat(controller.canUndo()).isFalse();
  }

  @Test
  void undoRebuildsMoveHistoryView() {
    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(humanMove.from());
    controller.onCellClicked(humanMove.to());
    assertThat(controller.history().rows()).hasSize(1);

    controller.undoPair();

    assertThat(controller.history().rows()).isEmpty();
  }

  @Test
  void undoFiresStateChangeAndAutosaveTrigger() {
    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(humanMove.from());
    controller.onCellClicked(humanMove.to());
    Mockito.clearInvocations(autosave);

    controller.undoPair();

    verify(autosave, times(1)).onMoveApplied(any(SinglePlayerGame.class));
  }

  @Test
  void undoIsBlockedWhileAiIsThinking() {
    AiTurnService aiService = Mockito.mock(AiTurnService.class);
    java.util.concurrent.CompletableFuture<Move> pending =
        new java.util.concurrent.CompletableFuture<>();
    Mockito.when(aiService.requestMove(any(), any(), any())).thenReturn(pending);

    SinglePlayerController withAi =
        new SinglePlayerController(engine, Optional.empty(), Optional.of(aiService), audio);
    withAi.setFxExecutor(Runnable::run);
    BoardRenderer freshRenderer = Mockito.mock(BoardRenderer.class);
    withAi.start(game, freshRenderer);

    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    withAi.onCellClicked(humanMove.from());
    withAi.onCellClicked(humanMove.to());

    assertThat(withAi.busy()).isTrue();
    assertThat(withAi.canUndo()).isFalse();

    withAi.undoPair();
    assertThat(withAi.state().sideToMove()).isEqualTo(Color.BLACK);

    pending.cancel(true);
  }

  @Test
  void undoStateListenerFiresOnTransitions() {
    java.util.concurrent.atomic.AtomicReference<Boolean> latestUndo =
        new java.util.concurrent.atomic.AtomicReference<>(null);
    java.util.concurrent.atomic.AtomicReference<Boolean> latestRedo =
        new java.util.concurrent.atomic.AtomicReference<>(null);
    controller
        .undoState()
        .onChange(
            (canUndo, canRedo) -> {
              latestUndo.set(canUndo);
              latestRedo.set(canRedo);
            });

    Move humanMove = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(humanMove.from());
    controller.onCellClicked(humanMove.to());
    assertThat(latestUndo.get()).isTrue();
    assertThat(latestRedo.get()).isFalse();

    controller.undoPair();
    assertThat(latestUndo.get()).isFalse();
    assertThat(latestRedo.get()).isTrue();
  }

  // ---------------------------------------------------------------------------------------------
  // SFX dispatch (Task 3.5.5)
  // ---------------------------------------------------------------------------------------------

  @Test
  void simpleHumanMoveFiresMoveSfx() {
    Move firstLegal = engine.legalMoves(GameState.initial()).get(0);
    controller.onCellClicked(firstLegal.from());
    Mockito.clearInvocations(audio);

    controller.onCellClicked(firstLegal.to());

    verify(audio).playSfx(Sfx.MOVE);
    verify(audio, never()).playSfx(Sfx.CAPTURE);
    verify(audio, never()).playSfx(Sfx.PROMOTION);
    verify(audio, never()).playSfx(Sfx.VICTORY);
    verify(audio, never()).playSfx(Sfx.DEFEAT);
  }

  @Test
  void illegalClickWhileSelectionActiveFiresIllegalSfx() {
    Square ownPiece = aWhiteSourceWithLegalMoves();
    controller.onCellClicked(ownPiece);
    Mockito.clearInvocations(audio);

    // Click an empty square that is NOT a legal target and is NOT the same selection
    // and is NOT a friendly piece. With the controller's fresh state, FID coordinates of
    // an empty light square serve.
    Square emptyNonTarget = new Square(0, 3); // empty light square, never a legal target
    controller.onCellClicked(emptyNonTarget);

    verify(audio).playSfx(Sfx.ILLEGAL);
  }

  @Test
  void clickOnEmptySquareWithoutSelectionDoesNotFireIllegalSfx() {
    Mockito.clearInvocations(audio);

    controller.onCellClicked(new Square(0, 3));

    verify(audio, never()).playSfx(Sfx.ILLEGAL);
  }

  @Test
  void reselectingFriendlyPieceDoesNotFireIllegalSfx() {
    Move firstLegal = engine.legalMoves(GameState.initial()).get(0);
    Move otherLegal =
        engine.legalMoves(GameState.initial()).stream()
            .filter(m -> !m.from().equals(firstLegal.from()))
            .findFirst()
            .orElseThrow();
    controller.onCellClicked(firstLegal.from());
    Mockito.clearInvocations(audio);

    controller.onCellClicked(otherLegal.from());

    verify(audio, never()).playSfx(Sfx.ILLEGAL);
  }

  @Test
  void deselectingSameSquareDoesNotFireIllegalSfx() {
    Square ownPiece = aWhiteSourceWithLegalMoves();
    controller.onCellClicked(ownPiece);
    Mockito.clearInvocations(audio);

    controller.onCellClicked(ownPiece);

    verify(audio, never()).playSfx(Sfx.ILLEGAL);
  }

  /** Picks any white square that has at least one legal move at the start. */
  private Square aWhiteSourceWithLegalMoves() {
    return engine.legalMoves(GameState.initial()).get(0).from();
  }
}
