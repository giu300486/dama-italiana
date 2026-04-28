/**
 * Italian Draughts rule engine (SPEC §3, §8.2).
 *
 * <p>{@link com.damaitaliana.shared.rules.RuleEngine} is the contract; {@link
 * com.damaitaliana.shared.rules.ItalianRuleEngine} the reference implementation enforcing
 * exclusively the Italian variant — {@code DEVE NOT} mix in International or English-American rules
 * (CLAUDE.md §1, SPEC §3 preamble).
 *
 * <p>The engine implements:
 *
 * <ul>
 *   <li>Movement and capture generation for men and kings (SPEC §3.2, §3.3).
 *   <li>The man-cannot-capture-king rule and the mandatory-capture rule (§3.3).
 *   <li>Multi-jump capture sequences with the same piece never re-jumped (§3.4).
 *   <li>The four laws of precedence: quantity, quality, king precedence, first king (§3.4).
 *   <li>Promotion and the stop-at-promotion-row mid-sequence rule (§3.5).
 *   <li>Game status: ongoing, win, stalemate-as-loss, 40-move draw, threefold repetition (§3.6);
 *       {@link com.damaitaliana.shared.domain.GameStatus#DRAW_AGREEMENT} is reserved for the
 *       UI/network layer.
 * </ul>
 */
package com.damaitaliana.shared.rules;
