/**
 * Italian Draughts FID notation utilities (SPEC §3.8).
 *
 * <p>{@link com.damaitaliana.shared.notation.FidNotation} is a stateless utility that converts
 * between board coordinates ({@link com.damaitaliana.shared.domain.Square}) and the FID 1-32
 * numbering (ADR-020) and parses/formats move strings such as {@code "12-16"}, {@code "12x19"} and
 * {@code "12x19x26"}.
 */
package com.damaitaliana.shared.notation;
