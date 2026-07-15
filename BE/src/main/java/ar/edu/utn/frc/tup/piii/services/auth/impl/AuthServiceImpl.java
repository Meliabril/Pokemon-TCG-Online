package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.LoginRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.EmailNotVerifiedException;
import ar.edu.utn.frc.tup.piii.exceptions.InactiveUserException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.services.auth.AuthService;
import ar.edu.utn.frc.tup.piii.services.auth.AuthSessionResult;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import ar.edu.utn.frc.tup.piii.services.auth.RefreshTokenService;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserService userService;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final Validator validator;

    @Override
    @Transactional
    public AuthSessionResult login(LoginRequestDto request) {
        validate(request);

        User user = findUserByIdentifier(request.identifier());
        ensureActiveUser(user);

        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenService.RefreshTokenResult refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthSessionResult(
                accessToken,
                refreshToken.rawToken(),
                TOKEN_TYPE,
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toUserResponseDto(user));
    }

    @Override
    @Transactional
    public AuthSessionResult refresh(String rawRefreshToken) {
        RefreshTokenService.RefreshTokenResult refreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        RefreshToken savedRefreshToken = refreshToken.refreshToken();
        User user = savedRefreshToken.getUser();
        ensureActiveUser(user);

        return new AuthSessionResult(
                jwtService.generateAccessToken(user),
                refreshToken.rawToken(),
                TOKEN_TYPE,
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toUserResponseDto(user));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(UUID userId) {
        User user = userService.getUserEntityById(userId);
        ensureActiveUser(user);
        return userMapper.toUserResponseDto(user);
    }

    private User findUserByIdentifier(String identifier) {
        try {
            return userService.getUserEntityByIdentifier(identifier);
        } catch (ResourceNotFoundException exception) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    private void ensureActiveUser(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new InactiveUserException("Blocked users cannot login");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new InactiveUserException("Deleted users cannot login");
        }

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION || Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException("La cuenta todavia no fue verificada. Revisa tu email.");
        }
    }

    private void validate(LoginRequestDto request) {
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
