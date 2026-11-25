/**
 * Spring Data JPA repositories, one per entity.
 *
 * <p>{@link com.damaitaliana.server.repository.RefreshTokenRepository} offers a {@code
 * PESSIMISTIC_WRITE} lookup used during token rotation so that two concurrent refreshes of the same
 * token cannot both succeed.
 */
package com.damaitaliana.server.repository;
