package ar.edu.utn.frc.tup.piii.services.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.EmailAlreadyExistsException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.UsernameAlreadyExistsException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.EmailVerificationService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import ar.edu.utn.frc.tup.piii.services.auth.impl.PasswordServiceImpl;
import ar.edu.utn.frc.tup.piii.services.user.impl.AdminUserServiceImpl;
import ar.edu.utn.frc.tup.piii.services.user.impl.UserServiceImpl;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AdminUserServiceImpl adminUserService;
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
        UserService userService = new UserServiceImpl(userRepository, passwordService, emailVerificationService, new UserMapper(), validator);
        adminUserService = new AdminUserServiceImpl(userService);
    }

    @Test
    void shouldCreateAdmin() {
        RegisterRequestDto request = validRequest();
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> savedUser(invocation.getArgument(0)));

        RegisterResponseDto response = adminUserService.createAdmin(request);

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.avatar()).isEqualTo(request.avatar());
        assertThat(response.emailVerified()).isTrue();
    }

    @Test
    void shouldHashPasswordWhenCreatingAdmin() {
        RegisterRequestDto request = validRequest();
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> savedUser(invocation.getArgument(0)));

        adminUserService.createAdmin(request);

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                !request.password().equals(user.getPasswordHash())
                        && passwordService.matches(request.password(), user.getPasswordHash())));
        verify(emailVerificationService, never()).generateAndSendVerificationCode(any(User.class));
    }

    @Test
    void shouldRejectAdminCreationWhenEmailAlreadyExists() {
        RegisterRequestDto request = validRequest();
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createAdmin(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectAdminCreationWhenUsernameAlreadyExists() {
        RegisterRequestDto request = validRequest();
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createAdmin(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldChangeStatusToActive() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.BLOCKED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.ACTIVE));

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldReactivateDeletedUserFromAdminStatusEndpoint() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId, UserStatus.DELETED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.ACTIVE));

        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldChangeStatusToBlocked() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.BLOCKED));

        assertThat(response.status()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void shouldChangeStatusToDeleted() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.DELETED));

        assertThat(response.status()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    void shouldOnlyUpdateStatus() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        String originalEmail = user.getEmail();
        String originalUsername = user.getUsername();
        String originalPasswordHash = user.getPasswordHash();
        UserRole originalRole = user.getRole();
        Instant originalCreatedAt = user.getCreatedAt();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.BLOCKED));

        assertThat(response.status()).isEqualTo(UserStatus.BLOCKED);
        assertThat(response.email()).isEqualTo(originalEmail);
        assertThat(response.username()).isEqualTo(originalUsername);
        assertThat(user.getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(response.role()).isEqualTo(originalRole);
        assertThat(response.createdAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void shouldUpdateUpdatedAtWhenChangingStatus() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        Instant previousUpdatedAt = user.getUpdatedAt();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.BLOCKED));

        assertThat(response.updatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void shouldThrowNotFoundWhenChangingStatusOfNonExistentUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.BLOCKED)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowBadRequestWhenStatusIsNull() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(null)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Status is required");

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnUserWithUpdatedStatus() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.BLOCKED));

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.status()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void shouldGetUserById() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponseDto response = adminUserService.getUserById(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("melina@gmail.com");
        assertThat(response.username()).isEqualTo("meli123");
    }

    @Test
    void shouldThrowNotFoundWhenGettingNonExistentUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldNotExposePasswordHashAfterStatusChange() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto response = adminUserService.changeUserStatus(
                userId,
                new UpdateUserStatusRequestDto(UserStatus.BLOCKED));

        assertThat(response).hasNoNullFieldsOrProperties();
        assertThat(UserResponseDto.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("password", "passwordHash");
    }

    private RegisterRequestDto validRequest() {
        return new RegisterRequestDto(
                "admin@gmail.com",
                "admin123",
                "Contrasena123!",
                "avatar-admin-01");
    }

    private User savedUser(User user) {
        user.setId(UUID.randomUUID());
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private User existingUser(UUID id) {
        return existingUser(id, UserStatus.ACTIVE);
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
        user.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        user.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return user;
    }
}
