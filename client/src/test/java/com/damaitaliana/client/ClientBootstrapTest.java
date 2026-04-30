package com.damaitaliana.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damaitaliana.client.app.ClientProperties;
import com.damaitaliana.client.app.SceneId;
import com.damaitaliana.client.app.SceneRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = ClientApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves"})
class ClientBootstrapTest {

  @Autowired ApplicationContext context;
  @Autowired SceneRouter sceneRouter;
  @Autowired ClientProperties clientProperties;

  @Test
  void springContextStartsWithoutWeb(CapturedOutput output) {
    assertThat(context).isNotNull();
    assertThat(context.getEnvironment().getProperty("spring.main.web-application-type"))
        .isEqualToIgnoringCase("none");
    assertThat(output.getOut()).doesNotContain("Tomcat started");
  }

  @Test
  void sceneRouterBeanIsResolvable() {
    assertThat(sceneRouter).isNotNull();
    assertThat(sceneRouter.isInitialized()).isFalse();
  }

  @Test
  void sceneRouterShowFailsBeforeInitialize() {
    assertThatThrownBy(() -> sceneRouter.show(SceneId.SPLASH))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("initialize");
  }

  @Test
  void clientPropertiesBindFromTestPropertySource() {
    assertThat(clientProperties).isNotNull();
    assertThat(clientProperties.savesDir()).isEqualTo("/tmp/test-saves");
    assertThat(clientProperties.configFile()).endsWith(".dama-italiana/config.json");
  }
}
