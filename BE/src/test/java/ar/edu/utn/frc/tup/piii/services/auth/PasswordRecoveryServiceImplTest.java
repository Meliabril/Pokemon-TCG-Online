package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.ForgotPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResetPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.auth.impl.PasswordRecoveryServiceImpl;
import ar.edu.utn.frc.tup.piii.services.user.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceImplTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private VerificationCodeService verificationCodeService;

    private PasswordRecoveryServiceImpl passwordRecoveryService;

    @BeforeEach
    void setUp() {
        passwordRecoveryService = new PasswordRecoveryServiceImpl(
                userAccountService,
                passwordService,
                refreshTokenService,
                verificationCodeService);
    }

    @Test
    void requestPasswordResetShouldGeneratePasswordResetCodeForActiveUser() {
        User user = activeUser();
        when(userAccountService.findUserEntityByEmail(user.getEmail())).thenReturn(Optional.of(user));

        passwordRecoveryService.requestPasswordReset(new ForgotPasswordRequestDto(user.getEmail()));

        verify(verificationCodeService).generateAndSendCode(user, VerificationCodePurpose.PASSWORD_RESET);
    }

    @Test
    void requestPasswordResetShouldIgnoreMissingEmail() {
        when(userAccountService.findUserEntityByEmail("missing@gmail.com")).thenReturn(Optional.empty());

        passwordRecoveryService.requestPasswordReset(new ForgotPasswordRequestDto("missing@gmail.com"));

        verify(verificationCodeService, never()).generateAndSendCode(any(), any());
    }

    @Test
    void requestPasswordResetShouldIgnoreBlockedOrDeletedUsers() {
        User blocked = activeUser();
        blocked.setStatus(UserStatus.BLOCKED);
        when(userAccountService.findUserEntityByEmail(blocked.getEmail())).thenReturn(Optional.of(blocked));

        passwordRecoveryService.requestPasswordReset(new ForgotPasswordRequestDto(blocked.getEmail()));

        verify(verificationCodeService, never()).generateAndSendCode(any(), any());
    }

    @Test
    void resetPasswordShouldUpdatePasswordMarkCodeUsedAndRevokeRefreshTokens() {
        User user = activeUser();
        PasswordResetCode code = activeCode(user);
        when(userAccountService.findUserEntityByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeService.validateCode(user, "839214", VerificationCodePurpose.PASSWORD_RESET))
                .thenReturn(code);
        when(passwordService.matches("NuevaPass123!", user.getPasswordHash())).thenReturn(false);
        when(passwordService.hash("NuevaPass123!")).thenReturn("hashed-new-password");
        when(userAccountService.updatePasswordHash(user, "hashed-new-password")).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setPasswordHash(invocation.getArgument(1));
            return savedUser;
        });

        passwordRecoveryService.resetPassword(new ResetPasswordRequestDto(
                user.getEmail(),
                "839214",
                "NuevaPass123!"));

        assertThat(user.getPasswordHash()).isEqualTo("hashed-new-password");
        verify(userAccountService).updatePasswordHash(user, "hashed-new-password");
        verify(verificationCodeService).markCodeAsUsed(code);
        verify(refreshTokenService).revokeAllUserRefreshTokens(user.getId());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void resetPasswordShouldRejectSameCurrentPassword() {
        User user = activeUser();
        PasswordResetCode code = activeCode(user);
        when(userAccountService.findUserEntityByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeService.validateCode(user, "839214", VerificationCodePurpose.PASSWORD_RESET))
                .thenReturn(code);
        when(passwordService.matches("NuevaPass123!", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> passwordRecoveryService.resetPassword(new ResetPasswordRequestDto(
                user.getEmail(),
                "839214",
                "NuevaPass123!")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("PASSWORD_REUSE_NOT_ALLOWED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(passwordService, never()).hash(any());
        verify(userAccountService, never()).updatePasswordHash(any(), any());
        verify(refreshTokenService, never()).revokeAllUserRefreshTokens(any());
    }

    @Test
    void resetPasswordShouldRejectWeakPassword() {
        User user = activeUser();
        PasswordResetCode code = activeCode(user);
        when(userAccountService.findUserEntityByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(verificationCodeService.validateCode(user, "839214", VerificationCodePurpose.PASSWORD_RESET))
                .thenReturn(code);

        assertThatThrownBy(() -> passwordRecoveryService.resetPassword(new ResetPasswordRequestDto(
                user.getEmail(),
                "839214",
                "weak")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("WEAK_PASSWORD");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(passwordService, never()).hash(any());
        verify(refreshTokenService, never()).revokeAllUserRefreshTokens(any());
    }

    @Test
    void resetPasswordShouldRejectInactiveUser() {
        User user = activeUser();
        user.setStatus(UserStatus.DELETED);
        when(userAccountService.findUserEntityByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> passwordRecoveryService.resetPassword(new ResetPasswordRequestDto(
                user.getEmail(),
                "839214",
                "NuevaPass123!")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("PASSWORD_RESET_USER_NOT_ACTIVE");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    private PasswordResetCode activeCode(User user) {
        PasswordResetCode code = new PasswordResetCode();
        code.setId(UUID.randomUUID());
        code.setUser(user);
        code.setPurpose(VerificationCodePurpose.PASSWORD_RESET);
        code.setCodeHash("hashed-code");
        code.setExpiresAt(Instant.now().plusSeconds(300));
        code.setCreatedAt(Instant.now().minusSeconds(120));
        return code;
    }

    private User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("melina@gmail.com");
        user.setUsername("meli123");
        user.setAvatar("avatar-pikachu-01");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        return user;
    }
}
