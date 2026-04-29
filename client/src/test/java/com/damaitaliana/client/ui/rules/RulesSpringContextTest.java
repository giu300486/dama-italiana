package com.damaitaliana.client.ui.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.ClientApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves-rules"})
class RulesSpringContextTest {

  @Autowired ApplicationContext context;

  @Test
  void rulesControllerPrototypeCanBeResolved() {
    RulesController controller = context.getBean(RulesController.class);
    assertThat(controller).isNotNull();
  }

  @Test
  void rulesControllerIsPrototypeScoped() {
    RulesController first = context.getBean(RulesController.class);
    RulesController second = context.getBean(RulesController.class);
    assertThat(first).isNotSameAs(second);
  }

  @Test
  void ruleDiagramLoaderBeanIsResolvable() {
    RuleDiagramLoader loader = context.getBean(RuleDiagramLoader.class);
    assertThat(loader).isNotNull();
  }
}
