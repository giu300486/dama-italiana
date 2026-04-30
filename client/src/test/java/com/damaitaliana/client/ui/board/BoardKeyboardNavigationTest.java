package com.damaitaliana.client.ui.board;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.shared.domain.Square;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoardKeyboardNavigationTest {

  private static boolean fxToolkitReady;

  @BeforeAll
  static void initToolkit() {
    try {
      Platform.startup(() -> {});
      fxToolkitReady = true;
    } catch (IllegalStateException alreadyStarted) {
      fxToolkitReady = true;
    } catch (UnsupportedOperationException headless) {
      fxToolkitReady = false;
    }
  }

  @Test
  void boardIsFocusTraversable() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    BoardRenderer board = runOnFxThread(BoardRenderer::new);
    assertThat(board.isFocusTraversable()).isTrue();
  }

  @Test
  void initialKeyboardFocusIsNullBeforeAnyEvent() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    BoardRenderer board = runOnFxThread(BoardRenderer::new);
    assertThat(board.keyboardFocus()).isNull();
  }

  @Test
  void rightArrowMovesFocusTwoFilesAwayWithinDarkCells() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    BoardRenderer board =
        runOnFxThread(
            () -> {
              BoardRenderer b = new BoardRenderer();
              b.setKeyboardFocus(new Square(0, 0));
              return b;
            });
    runOnFxThread(
        () -> {
          Event.fireEvent(board, keyEvent(KeyCode.RIGHT));
          return null;
        });
    assertThat(board.keyboardFocus()).isEqualTo(new Square(2, 0));
  }

  @Test
  void upArrowMovesFocusTwoRanksUpWithinDarkCells() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    BoardRenderer board =
        runOnFxThread(
            () -> {
              BoardRenderer b = new BoardRenderer();
              b.setKeyboardFocus(new Square(0, 0));
              return b;
            });
    runOnFxThread(
        () -> {
          Event.fireEvent(board, keyEvent(KeyCode.UP));
          return null;
        });
    assertThat(board.keyboardFocus()).isEqualTo(new Square(0, 2));
  }

  @Test
  void edgeOfBoardClampsArrowMovement() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    BoardRenderer board =
        runOnFxThread(
            () -> {
              BoardRenderer b = new BoardRenderer();
              b.setKeyboardFocus(new Square(0, 0));
              return b;
            });
    runOnFxThread(
        () -> {
          Event.fireEvent(board, keyEvent(KeyCode.LEFT));
          return null;
        });
    assertThat(board.keyboardFocus()).isEqualTo(new Square(0, 0));
  }

  @Test
  void enterDispatchesClickOnFocusedCell() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    AtomicReference<Square> clicked = new AtomicReference<>();
    BoardRenderer board =
        runOnFxThread(
            () -> {
              BoardRenderer b = new BoardRenderer();
              b.setOnCellClicked(clicked::set);
              b.setKeyboardFocus(new Square(2, 2));
              return b;
            });
    runOnFxThread(
        () -> {
          Event.fireEvent(board, keyEvent(KeyCode.ENTER));
          return null;
        });
    assertThat(clicked.get()).isEqualTo(new Square(2, 2));
  }

  @Test
  void escapeInvokesEscapeHandler() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");
    AtomicBoolean called = new AtomicBoolean(false);
    BoardRenderer board =
        runOnFxThread(
            () -> {
              BoardRenderer b = new BoardRenderer();
              b.setOnEscape(() -> called.set(true));
              b.setKeyboardFocus(new Square(2, 2));
              return b;
            });
    runOnFxThread(
        () -> {
          Event.fireEvent(board, keyEvent(KeyCode.ESCAPE));
          return null;
        });
    assertThat(called.get()).isTrue();
  }

  private static KeyEvent keyEvent(KeyCode code) {
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
  }

  private static <T> T runOnFxThread(java.util.concurrent.Callable<T> task) throws Exception {
    AtomicReference<T> holder = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(
        () -> {
          try {
            holder.set(task.call());
          } catch (Throwable t) {
            failure.set(t);
          } finally {
            latch.countDown();
          }
        });
    if (!latch.await(10, TimeUnit.SECONDS)) {
      throw new IllegalStateException("FX task did not complete within 10s");
    }
    if (failure.get() != null) {
      throw new RuntimeException(failure.get());
    }
    return holder.get();
  }
}
