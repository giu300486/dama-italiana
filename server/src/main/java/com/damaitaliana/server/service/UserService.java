package com.damaitaliana.server.service;

import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.exception.UserAlreadyExistsException;
import com.damaitaliana.server.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for {@link UserEntity}: registration, lookup, last-login tracking.
 *
 * <p>Owns the only legitimate code path that writes a new user to the database: {@link
 * #register(RegisterRequest)} enforces uniqueness on both {@code username} and {@code email}
 * (defensive check before the DB-level UNIQUE constraint would catch it — gives a typed exception
 * with a useful message and avoids the wasted BCrypt cost on a duplicate registration).
 *
 * <p>Read methods are marked {@code @Transactional(readOnly = true)} so Hibernate skips dirty
 * checking; write methods are explicit {@code @Transactional}.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserEntity register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new UserAlreadyExistsException("username already taken: " + request.username());
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new UserAlreadyExistsException("email already registered: " + request.email());
    }
    String hash = passwordEncoder.encode(request.password());
    UserEntity user = new UserEntity(request.username(), request.email(), hash, Role.USER);
    return userRepository.save(user);
  }

  public Optional<UserEntity> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public Optional<UserEntity> findById(Long id) {
    return userRepository.findById(id);
  }

  @Transactional
  public void updateLastLogin(Long userId) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("user not found: " + userId));
    user.setLastLoginAt(Instant.now());
    // Inside an @Transactional method Hibernate's dirty checking will flush the change at commit;
    // the explicit save is redundant but documents intent for readers unfamiliar with the
    // managed-entity lifecycle.
    userRepository.save(user);
  }
}
