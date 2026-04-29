package com.damaitaliana.client.ui.save;

import static org.assertj.core.api.Assertions.assertThat;

import com.damaitaliana.client.ClientApplication;
import com.damaitaliana.client.controller.AutosaveTrigger;
import com.damaitaliana.client.controller.SinglePlayerAutosaveTrigger;
import com.damaitaliana.client.persistence.AutosaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves"})
class SaveDialogSpringContextTest {

  @Autowired ApplicationContext context;

  @Test
  void saveDialogControllerPrototypeCanBeResolved() {
    SaveDialogController controller = context.getBean(SaveDialogController.class);
    assertThat(controller).isNotNull();
  }

  @Test
  void loadScreenControllerPrototypeCanBeResolved() {
    LoadScreenController controller = context.getBean(LoadScreenController.class);
    assertThat(controller).isNotNull();
  }

  @Test
  void miniatureRendererBeanIsResolvable() {
    MiniatureRenderer renderer = context.getBean(MiniatureRenderer.class);
    assertThat(renderer).isNotNull().isInstanceOf(CanvasMiniatureRenderer.class);
  }

  @Test
  void autosaveServiceBeanIsResolvable() {
    AutosaveService service = context.getBean(AutosaveService.class);
    assertThat(service).isNotNull();
  }

  @Test
  void autosaveTriggerBeanIsTheSinglePlayerImpl() {
    AutosaveTrigger trigger = context.getBean(AutosaveTrigger.class);
    assertThat(trigger).isNotNull().isInstanceOf(SinglePlayerAutosaveTrigger.class);
  }
}
