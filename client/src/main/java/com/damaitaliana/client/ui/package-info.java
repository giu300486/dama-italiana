/**
 * Top-level container for all JavaFX user-interface code in the client module.
 *
 * <p>The UI is split by screen: {@link com.damaitaliana.client.ui.splash splash}, {@link
 * com.damaitaliana.client.ui.menu menu}, {@link com.damaitaliana.client.ui.setup setup}, {@link
 * com.damaitaliana.client.ui.board board}+{@link com.damaitaliana.client.ui.board.animation
 * animation}, {@link com.damaitaliana.client.ui.save save}, {@link
 * com.damaitaliana.client.ui.settings settings}, {@link com.damaitaliana.client.ui.rules rules}.
 * Each sub-package owns its FXML controller (loaded via {@link
 * com.damaitaliana.client.app.SceneRouter} with {@code
 * FXMLLoader.setControllerFactory(applicationContext::getBean)} — ADR-030) and any
 * presentation-only helpers (cell nodes, view-models, accessibility text builders).
 *
 * <p>UI strings live in {@code i18n/messages_*.properties}; controllers obtain them via the {@link
 * com.damaitaliana.client.i18n.I18n} bean (ADR-033). No string literals in {@code setText} calls.
 * Controllers use the {@link com.damaitaliana.client.controller.SinglePlayerController} domain
 * layer for game state and never touch {@link com.damaitaliana.shared.rules.RuleEngine} directly.
 */
package com.damaitaliana.client.ui;
