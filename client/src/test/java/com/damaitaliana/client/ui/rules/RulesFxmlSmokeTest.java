package com.damaitaliana.client.ui.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.ClientApplication;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves-rules-fxml"})
class RulesFxmlSmokeTest {

  private static boolean fxToolkitReady;

  @Autowired ApplicationContext springContext;

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
  void rulesFxmlLoadsAndDefaultsToSetupSection() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    RulesController loaded =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rules.fxml"));
              loader.setControllerFactory(springContext::getBean);
              Parent root = loader.load();
              assertThat(root).isNotNull();
              return loader.getController();
            });

    assertThat(loaded.currentSection()).isEqualTo(RuleSection.SETUP);
    List<Node> children = loaded.contentChildren();
    assertThat(children).isNotEmpty();
    long imageCount = children.stream().flatMap(n -> n.lookupAll(".rule-diagram").stream()).count();
    assertThat(imageCount).isGreaterThanOrEqualTo(1);
  }

  @Test
  void clickingSectionUpdatesContent() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    RulesController loaded =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rules.fxml"));
              loader.setControllerFactory(springContext::getBean);
              loader.load();
              return loader.getController();
            });

    runOnFxThread(
        () -> {
          loaded.selectSection(RuleSection.NOTATION);
          return null;
        });

    assertThat(loaded.currentSection()).isEqualTo(RuleSection.NOTATION);
  }

  @Test
  void captureSectionRendersTwoDiagramImageViews() throws Exception {
    Assumptions.assumeTrue(fxToolkitReady, "JavaFX toolkit unavailable in this environment");

    RulesController loaded =
        runOnFxThread(
            () -> {
              FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/rules.fxml"));
              loader.setControllerFactory(springContext::getBean);
              loader.load();
              return loader.getController();
            });

    long imageCount =
        runOnFxThread(
            () -> {
              loaded.selectSection(RuleSection.CAPTURE);
              return loaded.contentChildren().stream()
                  .flatMap(n -> n.lookupAll(".rule-diagram").stream())
                  .filter(n -> n instanceof ImageView)
                  .count();
            });

    assertThat(imageCount).isEqualTo(2);
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
