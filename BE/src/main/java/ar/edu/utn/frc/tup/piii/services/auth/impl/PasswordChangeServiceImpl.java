package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.PasswordCodeVerificationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.ChangePasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.VerifyPasswordChangeCodeRequestDto;
import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordChangeService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import ar.edu.utn.frc.tup.piii.services.auth.RefreshTokenService;
import ar.edu.utn.frc.tup.piii.services.auth.VerificationCodeService;
import ar.edu.utn.frc.tup.piii.services.user.UserAccountService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordChangeServiceImpl implements PasswordChangeService {

    private static final String PASSWORD_CHANGE_CODE_VERIFIED_MESSAGE =
            "Codigo verificado. Ya podes ingresar una nueva contrasena.";

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^\\p{L}\\d]).{8,}$");

    private final UserAccountService userAccountService;
    private final VerificationCodeService verificationCodeService;
    private final PasswordService passwordService;
    private final RefreshTokenService refreshTokenService;
    private final Validator validator;

    @Override
    @Transactional
    public void requestPasswordChangeCode(UUID userId) {
        User user = userAccountService.getUserEntityById(userId);
        ensureActiveUser(user);
        verificationCodeService.generateAndSendCode(user, VerificationCodePurpose.PASSWORD_CHANGE);
    }

    @Override
    @Transactional
    public PasswordCodeVerificationResponseDto verifyPasswordChangeCode(
            UUID userId,
            VerifyPasswordChangeCodeRequestDto request) {
        validate(request);

        User user = userAccountService.getUserEntityById(userId);
        ensureActiveUser(user);

        String verificationToken = verificationCodeService.verifyCode(
                user,
                request.code(),
                VerificationCodePurpose.PASSWORD_CHANGE);

        return new PasswordCodeVerificationResponseDto(
                verificationToken,
                PASSWORD_CHANGE_CODE_VERIFIED_MESSAGE);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(UUID userId, ChangePasswordRequestDto request) {
        validate(request);

        User user = userAccountService.getUserEntityById(userId);
        ensureActiveUser(user);
        ensurePasswordsMatch(request);

        PasswordResetCode passwordChangeCode = verificationCodeService.getVerifiedCode(
                user,
                request.verificationToken(),
                VerificationCodePurpose.PASSWORD_CHANGE);

        ensureStrongPassword(request.newPassword());
        ensurePasswordIsDifferent(request.newPassword(), user.getPasswordHash());

        userAccountService.updatePasswordHash(user, passwordService.hash(request.newPassword()));
        verificationCodeService.markCodeAsUsed(passwordChangeCode);
        refreshTokenService.revokeAllUserRefreshTokens(userId);
    }

    private void ensureActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    "PASSWORD_CHANGE_USER_NOT_ACTIVE",
                    "Only active users can change password",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void ensurePasswordsMatch(ChangePasswordRequestDto request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "New password and confirmation must match",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void ensureStrongPassword(String newPassword) {
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(
                    "WEAK_PASSWORD",
                    "Password must have at least 8 characters, one uppercase letter, one number and one special character",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void ensurePasswordIsDifferent(String newPassword, String currentPasswordHash) {
        if (passwordService.matches(newPassword, currentPasswordHash)) {
            throw new BusinessException(
                    "PASSWORD_REUSE_NOT_ALLOWED",
                    "New password must be different from the current password",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validate(ChangePasswordRequestDto request) {
        Set<ConstraintViolation<ChangePasswordRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validate(VerifyPasswordChangeCodeRequestDto request) {
        Set<ConstraintViolation<VerifyPasswordChangeCodeRequestDto>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
