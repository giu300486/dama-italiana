package com.damaitaliana.client.ui.settings;

import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import com.damaitaliana.client.app.UiScalingService;
import com.damaitaliana.client.app.UserPromptService;
import com.damaitaliana.client.i18n.I18n;
import com.damaitaliana.client.persistence.PreferencesService;
import com.damaitaliana.client.persistence.UserPreferences;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Controller backing {@code settings.fxml}. Exposes three preference sections — language, UI
 * scaling, and (stub) theme — and persists the user's choice through {@link PreferencesService}.
 *
 * <p>Language follows the restart-required pattern (PLAN-fase-3 §7.10 option A): on save, if the
 * locale changed, an informational toast asks the user to restart. Runtime locale rebinding is
 * deferred to Fase 11.
 *
 * <p>UI scaling is applied immediately to the live scene via {@link UiScalingService} for the
 * preview while the user picks a step; the same service is invoked from {@link SceneRouter#show} on
 * every navigation, so the persisted choice carries across screens (Task 3.20).
 *
 * <p>The theme section is intentionally disabled in Fase 3 (PLAN-fase-3 §7.3): only "Light"
 * appears, a localized note announces dark mode for Fase 11.
 */
@Component
@Scope("prototype")
public class SettingsController {

  /** UI scaling steps allowed by SPEC §13.5. */
  static final int SCALE_100 = 100;

  static final int SCALE_125 = 125;
  static final int SCALE_150 = 150;
  private static final List<Integer> ALLOWED_SCALES = List.of(SCALE_100, SCALE_125, SCALE_150);

  /** Outcome of {@link #saveSelections(Locale, int)}; surfaced for unit tests. */
  enum SaveOutcome {
    SAVED_NO_RESTART_NEEDED,
    SAVED_RESTART_REQUIRED
  }

  private final SceneRouter sceneRouter;
  private final I18n i18n;
  private final PreferencesService preferencesService;
  private final UserPromptService prompt;
  private final UiScalingService uiScalingService;

  @FXML private Label titleLabel;

  @FXML private Label languageLabel;
  @FXML private ChoiceBox<Locale> languageChoice;

  @FXML private Label scalingLabel;
  @FXML private ToggleGroup scalingGroup;
  @FXML private RadioButton scaling100;
  @FXML private RadioButton scaling125;
  @FXML private RadioButton scaling150;

  @FXML private Label themeLabel;
  @FXML private ChoiceBox<String> themeChoice;
  @FXML private Label themeNoteLabel;

  @FXML private Button backButton;
  @FXML private Button saveButton;

  public SettingsController(
      SceneRouter sceneRouter,
      I18n i18n,
      PreferencesService preferencesService,
      UserPromptService prompt,
      UiScalingService uiScalingService) {
    this.sceneRouter = Objects.requireNonNull(sceneRouter, "sceneRouter");
    this.i18n = Objects.requireNonNull(i18n, "i18n");
    this.preferencesService = Objects.requireNonNull(preferencesService, "preferencesService");
    this.prompt = Objects.requireNonNull(prompt, "prompt");
    this.uiScalingService = Objects.requireNonNull(uiScalingService, "uiScalingService");
  }

  @FXML
  void initialize() {
    UserPreferences prefs = preferencesService.load();
    bindLabels();
    populateLanguageChoice(prefs.locale());
    populateThemeChoice();
    selectScaling(prefs.uiScalePercent());
  }

  private void bindLabels() {
    titleLabel.setText(i18n.t("settings.title"));
    languageLabel.setText(i18n.t("settings.section.language"));
    scalingLabel.setText(i18n.t("settings.section.scaling"));
    themeLabel.setText(i18n.t("settings.section.theme"));
    themeNoteLabel.setText(i18n.t("settings.theme.disabled.note"));
    scaling100.setText(i18n.t("settings.scaling.100"));
    scaling125.setText(i18n.t("settings.scaling.125"));
    scaling150.setText(i18n.t("settings.scaling.150"));
    backButton.setText(i18n.t("common.button.back"));
    saveButton.setText(i18n.t("settings.button.save"));
  }

  private void populateLanguageChoice(Locale current) {
    languageChoice.getItems().setAll(java.util.Arrays.asList(Locale.ITALIAN, Locale.ENGLISH));
    languageChoice.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Locale locale) {
            if (locale == null) {
              return "";
            }
            String key = "en".equals(locale.getLanguage()) ? "locale.english" : "locale.italian";
            return i18n.t(key);
          }

          @Override
          public Locale fromString(String s) {
            return null;
          }
        });
    Locale match = "en".equals(current.getLanguage()) ? Locale.ENGLISH : Locale.ITALIAN;
    languageChoice.getSelectionModel().select(match);
  }

  private void populateThemeChoice() {
    themeChoice.getItems().setAll(java.util.Arrays.asList(i18n.t("settings.theme.light")));
    themeChoice.getSelectionModel().selectFirst();
  }

  private void selectScaling(int percent) {
    int normalised = ALLOWED_SCALES.contains(percent) ? percent : SCALE_100;
    switch (normalised) {
      case SCALE_125 -> scaling125.setSelected(true);
      case SCALE_150 -> scaling150.setSelected(true);
      default -> scaling100.setSelected(true);
    }
  }

  /**
   * Reads the radio selection back out as a percentage. Defaults to {@link #SCALE_100} if no toggle
   * is selected (initialisation race) so callers always see a valid step.
   */
  int selectedScalingPercent() {
    if (scaling125 != null && scaling125.isSelected()) {
      return SCALE_125;
    }
    if (scaling150 != null && scaling150.isSelected()) {
      return SCALE_150;
    }
    return SCALE_100;
  }

  @FXML
  void onScalingChanged() {
    int percent = selectedScalingPercent();
    Scene scene = currentScene();
    uiScalingService.applyTo(scene, percent);
  }

  @FXML
  void onSave() {
    Locale chosenLocale = languageChoice.getValue();
    int chosenScale = selectedScalingPercent();
    saveSelections(chosenLocale != null ? chosenLocale : Locale.ITALIAN, chosenScale);
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  @FXML
  void onBack() {
    sceneRouter.show(SceneId.MAIN_MENU);
  }

  /**
   * Persists the requested locale + scaling and returns whether the user needs to restart for the
   * locale change to take effect. Visible for tests so the FXML state can be bypassed.
   */
  SaveOutcome saveSelections(Locale chosenLocale, int chosenScalingPercent) {
    Objects.requireNonNull(chosenLocale, "chosenLocale");
    int scaling = ALLOWED_SCALES.contains(chosenScalingPercent) ? chosenScalingPercent : SCALE_100;
    UserPreferences current = preferencesService.load();
    UserPreferences updated = current.withLocale(chosenLocale).withUiScalePercent(scaling);
    preferencesService.save(updated);
    boolean localeChanged = !current.locale().getLanguage().equals(chosenLocale.getLanguage());
    if (localeChanged) {
      prompt.info("settings.toast.locale.restart.title", "settings.toast.locale.restart.content");
      return SaveOutcome.SAVED_RESTART_REQUIRED;
    }
    return SaveOutcome.SAVED_NO_RESTART_NEEDED;
  }

  private Scene currentScene() {
    Node anchor = saveButton != null ? saveButton : backButton;
    return anchor != null ? anchor.getScene() : null;
  }
}
