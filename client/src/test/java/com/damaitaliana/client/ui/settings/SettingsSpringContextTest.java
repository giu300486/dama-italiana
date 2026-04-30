package com.damaitaliana.client.ui.settings;

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
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves-settings"})
class SettingsSpringContextTest {

  @Autowired ApplicationContext context;

  @Test
  void settingsControllerPrototypeCanBeResolved() {
    SettingsController controller = context.getBean(SettingsController.class);
    assertThat(controller).isNotNull();
  }

  @Test
  void settingsControllerIsPrototypeScoped() {
    SettingsController first = context.getBean(SettingsController.class);
    SettingsController second = context.getBean(SettingsController.class);
    assertThat(first).isNotSameAs(second);
  }
}
