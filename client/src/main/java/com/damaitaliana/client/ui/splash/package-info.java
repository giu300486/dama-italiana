/**
 * Splash screen and bootstrap pipeline.
 *
 * <p>{@link com.damaitaliana.client.ui.splash.SplashController} is the first JavaFX controller
 * shown after {@link com.damaitaliana.client.app.JavaFxApp} starts: it triggers the (already mostly
 * idempotent) post-DI bootstrap (autosave detection, minimum splash duration per SPEC §9.1) and
 * then asks {@link com.damaitaliana.client.app.SceneRouter} to navigate to the main menu.
 */
package com.damaitaliana.client.ui.splash;
