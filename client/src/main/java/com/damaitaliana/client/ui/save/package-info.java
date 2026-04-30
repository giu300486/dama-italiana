/**
 * Save dialog and load screen (FR-SP-07, AC §17.1.9, A3.4).
 *
 * <p>{@link com.damaitaliana.client.ui.save.SaveDialogController} is a modal dialog opened from the
 * in-game "Salva con nome…" menu entry; it validates the name (non-blank, sluggable), confirms
 * overwrite if a slot already exists, then delegates to {@link
 * com.damaitaliana.client.persistence.SaveService}. {@link
 * com.damaitaliana.client.ui.save.LoadScreenController} renders a sortable {@code TableView} of
 * {@link com.damaitaliana.client.persistence.SaveSlotMetadata}, with miniature board snapshots
 * rendered on-demand by {@link com.damaitaliana.client.ui.save.MiniatureRenderer} (default impl
 * {@link com.damaitaliana.client.ui.save.CanvasMiniatureRenderer}, also reused by the rules screen
 * for board diagrams — ADR-022 schema).
 *
 * <p>Both controllers expose typed enum results (e.g. {@code ConfirmResult}, {@code LoadResult},
 * {@code DeleteResult}) so unit tests can assert on the outcome without touching the JavaFX
 * runtime. Schema mismatches and IO errors propagate through {@link
 * com.damaitaliana.client.persistence.SaveService.UnknownSchemaVersionException} + {@link
 * java.io.UncheckedIOException} into localized toasts (keys {@code load.toast.error.schema.*} /
 * {@code load.toast.error.io.*}).
 */
package com.damaitaliana.client.ui.save;
