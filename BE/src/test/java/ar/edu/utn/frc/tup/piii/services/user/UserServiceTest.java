package ar.edu.utn.frc.tup.piii.services.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.DeleteUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserProfileRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserProfileResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.exceptions.EmailAlreadyExistsException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.UsernameAlreadyExistsException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.EmailVerificationService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import ar.edu.utn.frc.tup.piii.services.auth.impl.PasswordServiceImpl;
import ar.edu.utn.frc.tup.piii.services.user.impl.UserServiceImpl;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    private UserServiceImpl userService;
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
        userService = new UserServiceImpl(userRepository, passwordService, emailVerificationService, new UserMapper(), validator);
    }

    @Test
    void shouldRegisterValidUser() {
        RegisterRequestDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> savedUser(invocation.getArgument(0)));

        RegisterResponseDto response = userService.register(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.username()).isEqualTo(request.username());
        assertThat(response.avatar()).isEqualTo(request.avatar());
        assertThat(response.role()).isEqualTo(UserRole.USER);
        assertThat(response.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(response.emailVerified()).isFalse();
        assertThat(response.message()).isEqualTo("Cuenta creada. Revisa tu email para verificarla.");
    }

    @Test
    void shouldGenerateVerificationCodeWhenRegisteringValidUser() {
        RegisterRequestDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> savedUser(invocation.getArgument(0)));

        userService.register(request);

        verify(emailVerificationService).generateAndSendVerificationCode(any(User.class));
    }

    @Test
    void shouldReusePendingRegistrationWhenEmailAlreadyExistsButIsNotVerified() {
        RegisterRequestDto request = validRequest();
        User pendingUser = existingUser(UUID.randomUUID(), UserStatus.PENDING_VERIFICATION);
        pendingUser.setEmailVerified(false);
        pendingUser.setUsername("old-user");
        pendingUser.setPasswordHash(passwordService.hash("OldPass123!"));

        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(pendingUser));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot(request.username(), pendingUser.getId())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponseDto response = userService.register(request);

        assertThat(response.id()).isEqualTo(pendingUser.getId());
        assertThat(response.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(response.emailVerified()).isFalse();
        assertThat(response.message()).isEqualTo(
                "Ya existia un registro pendiente. Actualizamos tus datos y reenviamos el codigo de verificacion.");
        assertThat(pendingUser.getUsername()).isEqualTo(request.username());
        assertThat(passwordService.matches(request.password(), pendingUser.getPasswordHash())).isTrue();
        verify(emailVerificationService).generateAndSendVerificationCode(pendingUser);
    }

    @Test
    void shouldRejectRegisterWhenEmailAlreadyBelongsToVerifiedAccount() {
        RegisterRequestDto request = validRequest();
        User verifiedUser = existingUser(UUID.randomUUID(), UserStatus.ACTIVE);
        verifiedUser.setEmailVerified(true);
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.of(verifiedUser));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).generateAndSendVerificationCode(any(User.class));
    }

    @Test
    void shouldRejectRegisterWhenUsernameAlreadyExists() {
        RegisterRequestDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRequireStrongPassword() {
        RegisterRequestDto request = new RegisterRequestDto(
                "melina@gmail.com", "meli123", "password");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Password must have at least 8 characters");
    }

    @Test
    void shouldHashPasswordBeforeSaving() {
        RegisterRequestDto request = validRequest();
        when(userRepository.findByEmailIgnoreCase(request.email())).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> savedUser(invocation.getArgument(0)));

        userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(request.password());
        assertThat(passwordService.matches(request.password(), savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getEmailVerified()).isFalse();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void shouldUpdateCurrentUserProfileUsername() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("newuser", userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponseDto response = userService.updateCurrentUserProfile(
                userId,
                new UpdateUserProfileRequestDto("newuser", null));

        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.email()).isEqualTo("melina@gmail.com");
    }

    @Test
    void shouldUpdateCurrentUserProfileAvatar() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponseDto response = userService.updateCurrentUserProfile(
                userId,
                new UpdateUserProfileRequestDto(null, "avatar-snorlax-01"));

        assertThat(response.avatar()).isEqualTo("avatar-snorlax-01");
    }

    @Test
    void shouldNotUpdateProfileForBlockedUser() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.BLOCKED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(
                userId,
                new UpdateUserProfileRequestDto(null, "avatar-charizard-02")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Blocked users cannot update");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotUpdateProfileForDeletedUser() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.DELETED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(
                userId,
                new UpdateUserProfileRequestDto("newuser", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Deleted users cannot update");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldNotRepeatUsernameWhenUpdatingProfile() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser(userId, UserStatus.ACTIVE)));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot("useduser", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(
                userId,
                new UpdateUserProfileRequestDto("useduser", null)))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void shouldReturnCurrentProfileWhenNoProfileChangesAreSent() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponseDto response = userService.updateCurrentUserProfile(
                userId,
                new UpdateUserProfileRequestDto("  ", "  "));

        assertThat(response.username()).isEqualTo("meli123");
        assertThat(response.avatar()).isEqualTo("avatar-pikachu-01");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldSoftDeleteUser() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = userService.softDeleteUser(userId, validDeleteRequest());

        assertThat(response.status()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistentUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.softDeleteUser(userId, validDeleteRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDeleteWhenPasswordConfirmationIsInvalid() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.softDeleteUser(
                userId,
                new DeleteUserRequestDto("WrongPassword123!")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Current password confirmation is invalid");

        verify(userRepository, never()).save(any(User.class));
    }

    private RegisterRequestDto validRequest() {
        return new RegisterRequestDto(
                "melina@gmail.com",
                "meli123",
                "Contrasena123!",
                "avatar-pikachu-01");
    }

    private User savedUser(User user) {
        user.setId(UUID.randomUUID());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private User existingUser(UUID id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail("melina@gmail.com");
        user.setUsername("meli123");
        user.setAvatar("avatar-pikachu-01");
        user.setPasswordHash(passwordService.hash("Contrasena123!"));
        user.setRole(UserRole.USER);
        user.setStatus(status);
        user.setEmailVerified(status == UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        user.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return user;
    }

    private DeleteUserRequestDto validDeleteRequest() {
        return new DeleteUserRequestDto("Contrasena123!");
    }
}
