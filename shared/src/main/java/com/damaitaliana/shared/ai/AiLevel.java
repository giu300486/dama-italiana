package com.damaitaliana.shared.ai;

/** Difficulty levels of the AI engine (SPEC §12.2, ADR-015). */
public enum AiLevel {
  /** 2 ply, 500 ms, 25% probability of picking a sub-optimal move (noise). */
  PRINCIPIANTE,
  /** 5 ply, 2000 ms, alpha-beta + move ordering, always optimal within the depth budget. */
  ESPERTO,
  /** 8 ply, 5000 ms, iterative deepening + transposition table. */
  CAMPIONE
}
