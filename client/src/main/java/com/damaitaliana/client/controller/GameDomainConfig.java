package com.damaitaliana.client.controller;

import com.damaitaliana.shared.rules.ItalianRuleEngine;
import com.damaitaliana.shared.rules.RuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for shared-domain singletons. The shared module has no Spring dependency, so the
 * client exposes its services as beans here.
 */
@Configuration
public class GameDomainConfig {

  @Bean
  public RuleEngine ruleEngine() {
    return new ItalianRuleEngine();
  }
}
