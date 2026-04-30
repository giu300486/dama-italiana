package com.damaitaliana.client.ui.rules;

import java.util.List;

/**
 * One of the seven sections shown in the rules screen (FR-RUL-02).
 *
 * <p>The fixed list {@link #ALL} mirrors the order from SPEC §3 (setup → movement → capture →
 * precedence laws → promotion → endgame → notation). Title and body i18n keys follow the convention
 * {@code rules.section.<id>.title} / {@code rules.section.<id>.body}; bodies use {@code \n\n} to
 * delimit paragraphs.
 *
 * <p>{@link #diagramResource()} points at a JSON file under {@code /rules/} listing one or more
 * {@link RuleDiagram diagrams}; sections without a diagram resource simply have no images. SPEC
 * §4.6 / FR-RUL-03 require at least 5 diagrams across 4 sections — see PLAN-fase-3 §Task 3.18.
 */
public record RuleSection(String id, String diagramResource) {

  public static final RuleSection SETUP = new RuleSection("setup", "/rules/setup.json");
  public static final RuleSection MOVEMENT = new RuleSection("movement", "/rules/movement.json");
  public static final RuleSection CAPTURE = new RuleSection("capture", "/rules/capture.json");
  public static final RuleSection PRECEDENCE = new RuleSection("precedence", null);
  public static final RuleSection PROMOTION = new RuleSection("promotion", "/rules/promotion.json");
  public static final RuleSection ENDGAME = new RuleSection("endgame", null);
  public static final RuleSection NOTATION = new RuleSection("notation", null);

  /** Display order — matches SPEC §3 walkthrough. */
  public static final List<RuleSection> ALL =
      List.of(SETUP, MOVEMENT, CAPTURE, PRECEDENCE, PROMOTION, ENDGAME, NOTATION);

  public String titleKey() {
    return "rules.section." + id + ".title";
  }

  public String bodyKey() {
    return "rules.section." + id + ".body";
  }

  public boolean hasDiagrams() {
    return diagramResource != null;
  }
}
