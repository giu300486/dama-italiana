/**
 * Transactional domain services of the central server.
 *
 * <p>{@link com.damaitaliana.server.service.UserService} registers users (BCrypt hash, duplicate
 * username and email check), looks them up and records the last login. Token handling lives in
 * {@code auth}.
 */
package com.damaitaliana.server.service;
