/**
 * Single-player game orchestration.
 *
 * <p>Holds the data classes that describe a configured single-player session ({@link
 * com.damaitaliana.client.controller.SinglePlayerGame}, {@link
 * com.damaitaliana.client.controller.ColorChoice}) and the {@link
 * com.damaitaliana.client.controller.GameSession} bean that lets the setup screen hand the
 * configured game to the board view across an FXML scene change.
 *
 * <p>The actual game-loop controller (move selection, AI scheduling, autosave hooks) lands in Task
 * 3.9 / 3.13 / 3.16 and lives in this same package.
 */
package com.damaitaliana.client.controller;
