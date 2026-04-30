/**
 * Local persistence: user preferences ({@code config.json}) and single-player saves under {@code
 * ~/.dama-italiana/} (SPEC §14.1).
 *
 * <p>Fase 3 introduces {@link com.damaitaliana.client.persistence.PreferencesService} (Task 3.4)
 * and the multi-slot {@code SaveService} / {@code AutosaveService} (Task 3.14/3.16). All writes are
 * atomic via temp file + {@code Files.move(ATOMIC_MOVE, REPLACE_EXISTING)} so a crash mid-write
 * cannot leave a half-written target file (ADR-032).
 */
package com.damaitaliana.client.persistence;
