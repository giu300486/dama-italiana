package com.damaitaliana.client.ui.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.ui.save.MiniatureRenderer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesControllerTest {

  private SceneRouter sceneRouter;
  private I18n i18n;
  private RuleDiagramLoader diagramLoader;
  private MiniatureRenderer renderer;
  private RulesController controller;

  @BeforeEach
  void setUp() {
    sceneRouter = Mockito.mock(SceneRouter.class);
    i18n = Mockito.mock(I18n.class);
    diagramLoader = new RuleDiagramLoader();
    renderer = Mockito.mock(MiniatureRenderer.class);
    when(i18n.t(anyString())).thenAnswer(inv -> inv.getArgument(0));
    when(i18n.t(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
    controller = new RulesController(sceneRouter, i18n, diagramLoader, renderer);
  }

  @Test
  void allSevenSectionsAreExposedInDisplayOrder() {
    assertThat(RuleSection.ALL)
        .extracting(RuleSection::id)
        .containsExactly(
            "setup", "movement", "capture", "precedence", "promotion", "endgame", "notation");
  }

  @Test
  void allSevenSectionsHaveLocalizedTitleAndBody() throws IOException {
    Properties it = readBundle("/i18n/messages_it.properties");
    Properties en = readBundle("/i18n/messages_en.properties");
    for (RuleSection section : RuleSection.ALL) {
      assertThat(it.getProperty(section.titleKey()))
          .as("IT title for %s", section.id())
          .isNotBlank();
      assertThat(it.getProperty(section.bodyKey())).as("IT body for %s", section.id()).isNotBlank();
      assertThat(en.getProperty(section.titleKey()))
          .as("EN title for %s", section.id())
          .isNotBlank();
      assertThat(en.getProperty(section.bodyKey())).as("EN body for %s", section.id()).isNotBlank();
    }
  }

  @Test
  void diagramResourcesCoverTheRequiredSections() {
    assertThat(RuleSection.SETUP.hasDiagrams()).isTrue();
    assertThat(RuleSection.MOVEMENT.hasDiagrams()).isTrue();
    assertThat(RuleSection.CAPTURE.hasDiagrams()).isTrue();
    assertThat(RuleSection.PROMOTION.hasDiagrams()).isTrue();

    assertThat(RuleSection.PRECEDENCE.hasDiagrams()).isFalse();
    assertThat(RuleSection.ENDGAME.hasDiagrams()).isFalse();
    assertThat(RuleSection.NOTATION.hasDiagrams()).isFalse();
  }

  @Test
  void selectingSectionTracksCurrentSection() {
    controller.selectSection(RuleSection.CAPTURE);
    assertThat(controller.currentSection()).isEqualTo(RuleSection.CAPTURE);

    controller.selectSection(RuleSection.NOTATION);
    assertThat(controller.currentSection()).isEqualTo(RuleSection.NOTATION);
  }

  @Test
  void onBackNavigatesToMainMenu() {
    controller.onBack();
    verify(sceneRouter).show(SceneId.MAIN_MENU);
  }

  private Properties readBundle(String classpath) throws IOException {
    Properties p = new Properties();
    try (var in = getClass().getResourceAsStream(classpath)) {
      assertThat(in).as("resource %s", classpath).isNotNull();
      p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return p;
  }
}
