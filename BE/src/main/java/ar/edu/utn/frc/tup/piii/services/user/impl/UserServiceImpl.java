package ar.edu.utn.frc.tup.piii.services.user.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.DeleteUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserProfileRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
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
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String REGISTER_PENDING_MESSAGE = "Cuenta creada. Revisa tu email para verificarla.";
    private static final String REGISTER_PENDING_RESUMED_MESSAGE =
            "Ya existia un registro pendiente. Actualizamos tus datos y reenviamos el codigo de verificacion.";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;
    private final UserMapper userMapper;
    private final Validator validator;

    @Override
    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        validate(request);

        User existingUser = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (existingUser != null) {
            return registerPendingUser(existingUser, request);
        }

        ensureUsernameIsAvailable(request.username());

        User user = buildPendingUser(request);
        User savedUser = userRepository.save(user);
        emailVerificationService.generateAndSendVerificationCode(savedUser);
        return userMapper.toRegisterResponseDto(savedUser, REGISTER_PENDING_MESSAGE);
    }

    @Override
    @Transactional
    public RegisterResponseDto createAdmin(RegisterRequestDto request) {
        validate(request);
        ensureEmailIsAvailable(request.email());
        ensureUsernameIsAvailable(request.username());

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setAvatar(request.avatar());
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        User savedUser = userRepository.save(user);
        return userMapper.toRegisterResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getCurrentUserProfile(UUID userId) {
        return userMapper.toUserProfileResponseDto(getUserEntityById(userId));
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID id, UpdateUserRequestDto request) {
        validate(request);

        UpdateUserRequestDto normalizedRequest = new UpdateUserRequestDto(
                normalizeText(request.email()),
                normalizeText(request.username()),
                request.password(),
                normalizeText(request.avatar())
        );
        ensureAtLeastOneFieldToUpdate(normalizedRequest);

        User user = getUserEntityById(id);
        ensureUserCanBeUpdated(user);

        boolean hasChanges = false;

        if (normalizedRequest.email() != null) {
            ensureEmailChangeIsAllowed(user, normalizedRequest.email());
            ensureEmailIsAvailableForUser(normalizedRequest.email(), id);
            user.setEmail(normalizedRequest.email());
            hasChanges = true;
        }

        if (normalizedRequest.username() != null
                && !normalizedRequest.username().equalsIgnoreCase(user.getUsername())) {
            ensureUsernameIsAvailableForUser(normalizedRequest.username(), id);
            user.setUsername(normalizedRequest.username());
            hasChanges = true;
        }

        if (normalizedRequest.password() != null) {
            user.setPasswordHash(passwordService.hash(normalizedRequest.password()));
            hasChanges = true;
        }

        if (normalizedRequest.avatar() != null
                && !normalizedRequest.avatar().equals(user.getAvatar())) {
            user.setAvatar(normalizedRequest.avatar());
            hasChanges = true;
        }

        if (!hasChanges) {
            return userMapper.toUserResponseDto(user);
        }

        user.setUpdatedAt(Instant.now());
        return userMapper.toUserResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateCurrentUserProfile(UUID userId, UpdateUserProfileRequestDto request) {
        UpdateUserProfileRequestDto profileRequest = new UpdateUserProfileRequestDto(
                normalizeText(request.username()),
                normalizeText(request.avatar())
        );
        validate(profileRequest);

        User user = getUserEntityById(userId);
        ensureUserCanBeUpdated(user);

        boolean hasChanges = false;

        if (profileRequest.username() != null
                && !profileRequest.username().equalsIgnoreCase(user.getUsername())) {
            ensureUsernameIsAvailableForUser(profileRequest.username(), userId);
            user.setUsername(profileRequest.username());
            hasChanges = true;
        }

        if (profileRequest.avatar() != null
                && !profileRequest.avatar().equals(user.getAvatar())) {
            user.setAvatar(profileRequest.avatar());
            hasChanges = true;
        }

        if (!hasChanges) {
            return userMapper.toUserProfileResponseDto(user);
        }

        user.setUpdatedAt(Instant.now());
        return userMapper.toUserProfileResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDto softDeleteUser(UUID id, DeleteUserRequestDto request) {
        validate(request);

        User user = getUserEntityById(id);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(
                    "USER_ALREADY_DELETED",
                    "User is already deleted",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "INVALID_PASSWORD_CONFIRMATION",
                    "Current password confirmation is invalid",
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setStatus(UserStatus.DELETED);
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponseDto(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto changeUserStatus(UUID id, UpdateUserStatusRequestDto request) {
        validate(request);

        User user = getUserEntityById(id);
        user.setStatus(request.status());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID id) {
        return userMapper.toUserResponseDto(getUserEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + identifier));
        }

        return userRepository.findByUsernameIgnoreCase(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + identifier));
    }

    private void ensureUserCanBeUpdated(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessException(
                    "USER_BLOCKED",
                    "Blocked users cannot update their information",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessException(
                    "USER_DELETED",
                    "Deleted users cannot update their information",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void ensureAtLeastOneFieldToUpdate(UpdateUserRequestDto request) {
        if (request.email() == null
                && request.username() == null
                && request.password() == null
                && request.avatar() == null) {
            throw new BusinessException(
                    "NO_FIELDS_TO_UPDATE",
                    "At least one field must be provided",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void ensureEmailChangeIsAllowed(User user, String newEmail) {
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(
                    "EMAIL_UNCHANGED",
                    "New email must be different from the current email",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validate(RegisterRequestDto request) {
        Set<ConstraintViolation<RegisterRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(UpdateUserStatusRequestDto request) {
        Set<ConstraintViolation<UpdateUserStatusRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(UpdateUserRequestDto request) {
        Set<ConstraintViolation<UpdateUserRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(DeleteUserRequestDto request) {
        Set<ConstraintViolation<DeleteUserRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(UpdateUserProfileRequestDto request) {
        Set<ConstraintViolation<UpdateUserProfileRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void ensureEmailIsAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private void ensureUsernameIsAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    private void ensureEmailIsAvailableForUser(String email, UUID userId) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private void ensureUsernameIsAvailableForUser(String username, UUID userId) {
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, userId)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }

    private RegisterResponseDto registerPendingUser(User existingUser, RegisterRequestDto request) {
        if (!isPendingRegistration(existingUser)) {
            throw new EmailAlreadyExistsException(request.email());
        }

        ensureUsernameIsAvailableForUser(request.username(), existingUser.getId());

        existingUser.setUsername(request.username());
        existingUser.setAvatar(request.avatar());
        existingUser.setPasswordHash(passwordService.hash(request.password()));
        existingUser.setRole(UserRole.USER);
        existingUser.setStatus(UserStatus.PENDING_VERIFICATION);
        existingUser.setEmailVerified(false);
        existingUser.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(existingUser);
        emailVerificationService.generateAndSendVerificationCode(savedUser);
        return userMapper.toRegisterResponseDto(savedUser, REGISTER_PENDING_RESUMED_MESSAGE);
    }

    private User buildPendingUser(RegisterRequestDto request) {
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setAvatar(request.avatar());
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        return user;
    }

    private boolean isPendingRegistration(User user) {
        return Boolean.FALSE.equals(user.getEmailVerified())
                && user.getStatus() != UserStatus.BLOCKED
                && user.getStatus() != UserStatus.DELETED;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
