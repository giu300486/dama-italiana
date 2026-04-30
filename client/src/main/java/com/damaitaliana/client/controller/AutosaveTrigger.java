package com.damaitaliana.client.controller;

/**
 * Hook the {@link SinglePlayerController} fires after every applied move. Task 3.16 will provide
 * the disk-backed implementation; Task 3.9 declares only the contract so the controller is
 * decoupled from the persistence layer that hasn't landed yet.
 */
public interface AutosaveTrigger {

  /** Persist a snapshot of the current game state for crash recovery (FR-SP-08, ADR-016). */
  void onMoveApplied(SinglePlayerGame game);
}
