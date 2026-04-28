/**
 * Pure domain model and rule engine of the Italian Draughts game.
 *
 * <p>Sub-packages introduced in Fase 1:
 *
 * <ul>
 *   <li>{@code domain} — Square, Color, PieceKind, Piece, Board, Move (sealed) + SimpleMove +
 *       CaptureSequence, GameStatus, GameState.
 *   <li>{@code notation} — FidNotation: 1-32 numeric notation per ADR-020 and FID-format move
 *       parsing/rendering.
 *   <li>{@code rules} — RuleEngine interface, ItalianRuleEngine reference implementation, four laws
 *       of precedence (SPEC §3.4), threefold repetition (ADR-021).
 * </ul>
 *
 * <p>{@code ai} (Minimax + alpha-beta) and {@code dto} arrive in Fase 2+.
 *
 * <p>Constraint (CLAUDE.md §8.7): this module MUST NOT depend on Spring, JavaFX, JPA, or any
 * WebSocket library.
 */
package com.damaitaliana.shared;
