/**
 * Settings screen: language, UI scaling, theme placeholder (NFR-U-01, A3.9, PLAN-fase-3 §7.10).
 *
 * <p>{@link com.damaitaliana.client.ui.settings.SettingsController} reads {@link
 * com.damaitaliana.client.persistence.UserPreferences} on init, lets the user switch locale
 * (Italiano / Inglese), pick a UI scale ({100, 125, 150}%) and shows a disabled "Light" theme
 * picker (dark mode toggle is deferred to Fase 11 — NFR-U-02 partial). On save the controller
 * persists the merged preferences via {@link
 * com.damaitaliana.client.persistence.PreferencesService} and, when the locale changed, prompts the
 * user to restart the client (Fase 3 does not implement runtime locale swap; ADR-033).
 *
 * <p>Scaling is applied through {@link com.damaitaliana.client.app.UiScalingService}, which is also
 * invoked by {@link com.damaitaliana.client.app.SceneRouter#show} on every scene change — the
 * preference therefore reflects on every screen, not just the live preview inside the settings
 * stage (Task 3.20).
 */
package com.damaitaliana.client.ui.settings;
