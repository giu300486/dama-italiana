package com.damaitaliana.core;

import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Java config root for the core-server module.
 *
 * <p>Imported by other modules ({@code client} for LAN host, {@code server} for Internet) via
 * {@code @Import(CoreServerConfiguration.class)} or via a parent {@code @ComponentScan} that
 * already covers the {@code com.damaitaliana.core} base package.
 *
 * <p>Constraint (CLAUDE.md §8.8): this configuration MUST NOT pull a transport. It only enables
 * component scanning over the core-server sub-packages (match, tournament, repository, eventbus,
 * stomp) and registers domain-pure beans from {@code shared} that {@link
 * com.damaitaliana.core.match.MatchManager} requires. Transport adapters (Tomcat, Jetty, JPA) live
 * in the importing modules.
 */
@Configuration
@ComponentScan(basePackages = "com.damaitaliana.core")
public class CoreServerConfiguration {

  /**
   * Provides the production {@link RuleEngine} bean (Italian rules, SPEC §3) to {@link
   * com.damaitaliana.core.match.MatchManager}. Registered via {@code @Bean} rather than
   * {@code @Component} because the {@code shared} module is dependency-pure and intentionally does
   * not import Spring annotations (CLAUDE.md §8.7).
   */
  @Bean
  public RuleEngine ruleEngine() {
    return new ItalianRuleEngine();
  }
}
