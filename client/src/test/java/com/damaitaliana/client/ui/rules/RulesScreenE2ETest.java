package com.damaitaliana.client.ui.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.ClientApplication;
import com.damaitaliana.client.i18n.I18n;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * End-to-end coverage of A3.10 — opening the rules screen and navigating every section. Spins up
 * the real Spring context, loads {@code rules.fxml} via the controller factory, then walks each
 * {@link RuleSection} in display order and verifies that {@link RulesController#currentSection()}
 * tracks the selection and that the rendered content box leads with a {@link Label} whose text
 * matches the localized section title. Skipped when the JavaFX toolkit is unavailable (headless CI
 * with no software pipeline).
 */
@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves-rules-e2e"})
class RulesScreenE2ETest {

  private static boolean fxToolkitReady;

  @Autowired ApplicationContext springContext;
  @Autowired I18n i18n;

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
  void openRulesAndNavigateSections() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    RulesController controller =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rules.fxml"));
              loader.setControllerFactory(springContext::getBean);
              loader.load();
              return loader.getController();
            });

    // The controller defaults to SETUP after FXML initialization.
    assertThat(controller.currentSection()).isEqualTo(RuleSection.SETUP);

    for (RuleSection section : RuleSection.ALL) {
      String headerText =
          runOnFxThread(
              () -> {
                controller.selectSection(section);
                Node first = controller.contentChildren().get(0);
                assertThat(first).isInstanceOf(Label.class);
                return ((Label) first).getText();
              });

      assertThat(controller.currentSection())
          .as("currentSection after selecting %s", section.id())
          .isEqualTo(section);
      assertThat(headerText)
          .as("header label for %s should match localized title", section.id())
          .isEqualTo(i18n.t(section.titleKey()));
      assertThat(controller.contentChildren())
          .as("content box for %s must include header + at least one body paragraph", section.id())
          .hasSizeGreaterThanOrEqualTo(2);
    }
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
    if (!latch.await(15, TimeUnit.SECONDS)) {
      throw new IllegalStateException("FX task did not complete within 15s");
    }
    if (failure.get() != null) {
      throw new RuntimeException(failure.get());
    }
    return holder.get();
  }
}
