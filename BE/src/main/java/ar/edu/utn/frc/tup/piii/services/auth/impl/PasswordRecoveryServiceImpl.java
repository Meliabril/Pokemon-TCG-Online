package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.ForgotPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.PasswordCodeVerificationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResetPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifiedPasswordResetRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifyPasswordResetCodeRequestDto;
import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordRecoveryService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import ar.edu.utn.frc.tup.piii.services.auth.RefreshTokenService;
import ar.edu.utn.frc.tup.piii.services.auth.VerificationCodeService;
import ar.edu.utn.frc.tup.piii.services.user.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final String PASSWORD_RESET_CODE_VERIFIED_MESSAGE =
            "Codigo verificado. Ya podes ingresar una nueva contrasena.";

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^\\p{L}\\d]).{8,}$");

    private final UserAccountService userAccountService;
    private final PasswordService passwordService;
    private final RefreshTokenService refreshTokenService;
    private final VerificationCodeService verificationCodeService;

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequestDto request) {
        userAccountService.findUserEntityByEmail(request.email())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> verificationCodeService.generateAndSendCode(
                        user,
                        VerificationCodePurpose.PASSWORD_RESET));
    }

    @Override
    @Transactional
    public PasswordCodeVerificationResponseDto verifyPasswordResetCode(VerifyPasswordResetCodeRequestDto request) {
        User user = userAccountService.findUserEntityByEmail(request.email())
                .orElseThrow(this::invalidPasswordResetCode);
        ensureActiveUser(user);

        String verificationToken = verificationCodeService.verifyCode(
                user,
                request.code(),
                VerificationCodePurpose.PASSWORD_RESET);

        return new PasswordCodeVerificationResponseDto(
                verificationToken,
                PASSWORD_RESET_CODE_VERIFIED_MESSAGE);
    }

    @Override
    @Transactional
    public void resetVerifiedPassword(VerifiedPasswordResetRequestDto request) {
        PasswordResetCode passwordResetCode = verificationCodeService.getVerifiedCode(
                request.verificationToken(),
                VerificationCodePurpose.PASSWORD_RESET);
        User user = passwordResetCode.getUser();
        ensureActiveUser(user);

        ensurePasswordsMatch(request.newPassword(), request.confirmPassword());
        updatePassword(user, request.newPassword(), passwordResetCode);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        User user = userAccountService.findUserEntityByEmail(request.email())
                .orElseThrow(this::invalidPasswordResetCode);

        ensureActiveUser(user);

        PasswordResetCode passwordResetCode = verificationCodeService.validateCode(
                user,
                request.code(),
                VerificationCodePurpose.PASSWORD_RESET);

        updatePassword(user, request.newPassword(), passwordResetCode);
    }

    private void ensureActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    "PASSWORD_RESET_USER_NOT_ACTIVE",
                    "Only active users can reset password",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void ensurePasswordsMatch(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "New password and confirmation must match",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void updatePassword(User user, String newPassword, PasswordResetCode passwordResetCode) {
        ensureStrongPassword(newPassword);
        ensurePasswordIsDifferent(newPassword, user.getPasswordHash());

        userAccountService.updatePasswordHash(user, passwordService.hash(newPassword));
        verificationCodeService.markCodeAsUsed(passwordResetCode);
        refreshTokenService.revokeAllUserRefreshTokens(user.getId());
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

    private BusinessException invalidPasswordResetCode() {
        return new BusinessException(
                "INVALID_PASSWORD_RESET_CODE",
                "Invalid password reset code",
                HttpStatus.BAD_REQUEST
        );
    }
}
