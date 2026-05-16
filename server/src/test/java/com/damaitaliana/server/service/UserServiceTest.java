package com.damaitaliana.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.damaitaliana.server.dto.RegisterRequest;
import com.damaitaliana.server.entity.Role;
import com.damaitaliana.server.entity.UserEntity;
import com.damaitaliana.server.exception.UserAlreadyExistsException;
import com.damaitaliana.server.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link UserService}. No Spring context, no DB — pure Mockito. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @InjectMocks private UserService userService;

  private RegisterRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new RegisterRequest("alice", "alice@example.com", "Secret123");
  }

  @Test
  void register_persistsHashedUserWithDefaultRoleUser_whenInputIsValid() {
    when(userRepository.existsByUsername("alice")).thenReturn(false);
    when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
    when(passwordEncoder.encode("Secret123")).thenReturn("$2a$12$hashedValue");
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserEntity saved = userService.register(validRequest);

    ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository, times(1)).save(captor.capture());
    UserEntity persisted = captor.getValue();
    assertThat(persisted.getUsername()).isEqualTo("alice");
    assertThat(persisted.getEmail()).isEqualTo("alice@example.com");
    assertThat(persisted.getPasswordHash()).isEqualTo("$2a$12$hashedValue");
    assertThat(persisted.getRole()).isEqualTo(Role.USER);
    assertThat(saved).isSameAs(persisted);
  }

  @Test
  void register_throwsUserAlreadyExists_whenUsernameTaken() {
    when(userRepository.existsByUsername("alice")).thenReturn(true);

    assertThatThrownBy(() -> userService.register(validRequest))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining("alice");

    verify(passwordEncoder, never()).encode(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void register_throwsUserAlreadyExists_whenEmailTaken() {
    when(userRepository.existsByUsername("alice")).thenReturn(false);
    when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

    assertThatThrownBy(() -> userService.register(validRequest))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining("alice@example.com");

    verify(passwordEncoder, never()).encode(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void findByUsername_delegatesToRepository() {
    UserEntity stub = new UserEntity("bob", "bob@example.com", "hash", Role.USER);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(stub));

    Optional<UserEntity> result = userService.findByUsername("bob");

    assertThat(result).containsSame(stub);
  }

  @Test
  void findById_delegatesToRepository() {
    UserEntity stub = new UserEntity("carol", "carol@example.com", "hash", Role.SYSTEM_ADMIN);
    when(userRepository.findById(42L)).thenReturn(Optional.of(stub));

    Optional<UserEntity> result = userService.findById(42L);

    assertThat(result).containsSame(stub);
  }

  @Test
  void updateLastLogin_setsTimestampAndSaves_whenUserExists() {
    UserEntity dave = new UserEntity("dave", "dave@example.com", "hash", Role.USER);
    when(userRepository.findById(7L)).thenReturn(Optional.of(dave));

    userService.updateLastLogin(7L);

    assertThat(dave.getLastLoginAt()).isNotNull();
    verify(userRepository, times(1)).save(dave);
  }

  @Test
  void updateLastLogin_throwsEntityNotFound_whenUserMissing() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateLastLogin(999L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("999");

    verify(userRepository, never()).save(any());
  }

  @Test
  void register_doesNotInvokeEncoder_whenUsernameOrEmailExistsBeforeHashing() {
    when(userRepository.existsByUsername("alice")).thenReturn(true);

    assertThatThrownBy(() -> userService.register(validRequest))
        .isInstanceOf(UserAlreadyExistsException.class);

    // BCrypt at strength 12 costs ~250ms — verifying we short-circuit before the hash is a
    // (minor) performance guarantee against trivial DoS by attacker spamming /register with
    // already-taken usernames.
    verify(passwordEncoder, never()).encode(eq("Secret123"));
  }
}
