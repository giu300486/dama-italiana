/**
 * Main menu screen.
 *
 * <p>Six-card landing surface (SPEC §13.1.2). Single Player, Rules and Settings are wired in Fase
 * 3; LAN and Online cards are visible but disabled with a "Available in Fase X" tooltip
 * (PLAN-fase-3 §7.11). The Profile card is hidden in Fase 3 because authentication does not exist
 * yet.
 *
 * <p>{@link com.damaitaliana.client.ui.menu.MainMenuController} also drives the "resume interrupted
 * game?" prompt when the splash bootstrap detected an autosave file (Task 3.5/3.6); the actual
 * resume action is wired by Task 3.16 once the autosave service exists.
 */
package com.damaitaliana.client.ui.menu;
