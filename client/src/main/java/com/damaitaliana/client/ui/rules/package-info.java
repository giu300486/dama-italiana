/**
 * In-app rules reference (FR-RUL-01..05, AC §17.1.12, A3.10).
 *
 * <p>{@link com.damaitaliana.client.ui.rules.RulesController} renders the seven SPEC §3 sections
 * (setup, movement, capture, precedence laws, promotion, endgame, notation) as a left-rail {@code
 * ListView} + right-pane content. Each {@link com.damaitaliana.client.ui.rules.RuleSection}
 * resolves its title and body via {@link com.damaitaliana.client.i18n.I18n} (keys {@code
 * rules.section.<id>.title} / {@code rules.section.<id>.body}, paragraphs split on {@code \n\n}).
 *
 * <p>{@link com.damaitaliana.client.ui.rules.RuleDiagramLoader} reads {@code /rules/<section>.json}
 * (six total diagrams across four sections — exceeds FR-RUL-03's "≥5 diagrams across ≥4 sections"),
 * each entry materialised as a {@link com.damaitaliana.client.ui.rules.RuleDiagram} with a {@link
 * com.damaitaliana.client.persistence.SerializedGameState} position rendered via {@link
 * com.damaitaliana.client.ui.save.MiniatureRenderer}. {@link
 * com.damaitaliana.client.ui.rules.RulesAnimations} provides three demonstrative animations (simple
 * capture, multi-capture, promotion) plugged into the {@code CAPTURE} and {@code PROMOTION}
 * sections (FR-RUL-04, opt-in §7.6).
 *
 * <p>The screen is reachable from the main menu and from the in-game menu ("Regole"), satisfying
 * FR-RUL-01 from both surfaces.
 */
package com.damaitaliana.client.ui.rules;
