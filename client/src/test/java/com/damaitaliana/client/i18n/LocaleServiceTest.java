package com.damaitaliana.client.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocaleServiceTest {

  private final LocaleService service = new LocaleService();

  @Test
  void defaultLocaleIsItalian() {
    assertThat(service.current()).isEqualTo(Locale.ITALIAN);
  }

  @Test
  void switchToEnglishUpdatesCurrent() {
    service.switchTo(Locale.ENGLISH);
    assertThat(service.current()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  void switchToNullThrows() {
    assertThatNullPointerException().isThrownBy(() -> service.switchTo(null));
  }

  @Test
  void switchToIsIdempotent() {
    service.switchTo(Locale.ENGLISH);
    service.switchTo(Locale.ENGLISH);
    assertThat(service.current()).isEqualTo(Locale.ENGLISH);
  }
}
