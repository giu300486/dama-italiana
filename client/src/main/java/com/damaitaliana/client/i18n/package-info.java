/**
 * Localization (i18n) for the desktop client.
 *
 * <p>{@link com.damaitaliana.client.i18n.MessageSourceConfig} wires Spring's {@code
 * ReloadableResourceBundleMessageSource} against the {@code i18n/messages_*.properties} bundles
 * (UTF-8, Italian as the fallback locale per ADR-033). {@link
 * com.damaitaliana.client.i18n.LocaleService} keeps track of the user's active locale; {@link
 * com.damaitaliana.client.i18n.I18n} is the static-style helper FXML controllers use to look up
 * strings without injecting both beans every time.
 *
 * <p>Constraint (PLAN-fase-3 §7.10): in Fase 3, switching language requires an application restart.
 * Runtime dynamic re-binding of UI strings lands in Fase 11 along with the dark-mode runtime
 * toggle.
 */
package com.damaitaliana.client.i18n;
