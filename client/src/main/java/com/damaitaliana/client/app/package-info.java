/**
 * Application bootstrap and scene orchestration.
 *
 * <p>Bridges Spring Boot DI (non-web) and the JavaFX {@link javafx.application.Application}
 * lifecycle. Provides {@link com.damaitaliana.client.app.SceneRouter} as the single bean
 * responsible for swapping the primary {@link javafx.stage.Stage}'s scene as the user navigates.
 *
 * <p>Constraints (CLAUDE.md §8.6, ADR-005, ADR-029): no embedded servlet container is started in
 * single-player mode. JavaFX must always run on its own application thread; Spring beans are
 * obtained via {@link com.damaitaliana.client.app.JavaFxAppContextHolder} from inside the JavaFX
 * lifecycle callbacks.
 */
package com.damaitaliana.client.app;
