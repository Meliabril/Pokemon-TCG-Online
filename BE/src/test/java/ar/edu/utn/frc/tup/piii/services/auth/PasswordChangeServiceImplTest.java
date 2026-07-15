package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.user.ChangePasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.VerifyPasswordChangeCodeRequestDto;
import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.auth.impl.PasswordChangeServiceImpl;
import ar.edu.utn.frc.tup.piii.services.user.UserAccountService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceImplTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private PasswordChangeServiceImpl passwordChangeService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        passwordChangeService = new PasswordChangeServiceImpl(
                userAccountService,
                verificationCodeService,
                passwordService,
                refreshTokenService,
                validator);
    }

    @Test
    void requestPasswordChangeCodeShouldGeneratePasswordChangeCode() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);

        passwordChangeService.requestPasswordChangeCode(userId);

        verify(verificationCodeService).generateAndSendCode(user, VerificationCodePurpose.PASSWORD_CHANGE);
    }

    @Test
    void requestPasswordChangeCodeShouldRejectInactiveUser() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        user.setStatus(UserStatus.BLOCKED);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);

        assertThatThrownBy(() -> passwordChangeService.requestPasswordChangeCode(userId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("PASSWORD_CHANGE_USER_NOT_ACTIVE");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verify(verificationCodeService, never()).generateAndSendCode(any(), any());
    }

    @Test
    void changeCurrentUserPasswordShouldUpdatePasswordMarkCodeUsedAndRevokeRefreshTokens() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        PasswordResetCode code = activeCode(user);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);
        when(verificationCodeService.getVerifiedCode(user, code.getId().toString(), VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(code);
        when(passwordService.matches("NuevaPass123!", user.getPasswordHash())).thenReturn(false);
        when(passwordService.hash("NuevaPass123!")).thenReturn("hashed-new-password");

        passwordChangeService.changeCurrentUserPassword(
                userId,
                new ChangePasswordRequestDto(code.getId().toString(), "NuevaPass123!", "NuevaPass123!"));

        verify(userAccountService).updatePasswordHash(user, "hashed-new-password");
        verify(verificationCodeService).markCodeAsUsed(code);
        verify(refreshTokenService).revokeAllUserRefreshTokens(userId);
    }

    @Test
    void changeCurrentUserPasswordShouldRejectMismatchedConfirmation() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);

        assertThatThrownBy(() -> passwordChangeService.changeCurrentUserPassword(
                userId,
                new ChangePasswordRequestDto(UUID.randomUUID().toString(), "NuevaPass123!", "OtraPass123!")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("PASSWORD_CONFIRMATION_MISMATCH"));

        verify(verificationCodeService, never()).getVerifiedCode(any(User.class), any(), any());
    }

    @Test
    void changeCurrentUserPasswordShouldRejectWeakPassword() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        PasswordResetCode code = activeCode(user);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);
        when(verificationCodeService.getVerifiedCode(user, code.getId().toString(), VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(code);

        assertThatThrownBy(() -> passwordChangeService.changeCurrentUserPassword(
                userId,
                new ChangePasswordRequestDto(code.getId().toString(), "weak", "weak")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("WEAK_PASSWORD"));

        verify(passwordService, never()).hash(any());
    }

    @Test
    void changeCurrentUserPasswordShouldRejectSamePassword() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        PasswordResetCode code = activeCode(user);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);
        when(verificationCodeService.getVerifiedCode(user, code.getId().toString(), VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(code);
        when(passwordService.matches("NuevaPass123!", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> passwordChangeService.changeCurrentUserPassword(
                userId,
                new ChangePasswordRequestDto(code.getId().toString(), "NuevaPass123!", "NuevaPass123!")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("PASSWORD_REUSE_NOT_ALLOWED"));

        verify(passwordService, never()).hash(any());
        verify(refreshTokenService, never()).revokeAllUserRefreshTokens(any());
    }

    @Test
    void verifyPasswordChangeCodeShouldReturnVerificationToken() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        when(userAccountService.getUserEntityById(userId)).thenReturn(user);
        when(verificationCodeService.verifyCode(user, "839214", VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn("verification-token");

        assertThat(passwordChangeService.verifyPasswordChangeCode(
                userId,
                new VerifyPasswordChangeCodeRequestDto("839214")).verificationToken())
                .isEqualTo("verification-token");
    }

    private PasswordResetCode activeCode(User user) {
        PasswordResetCode code = new PasswordResetCode();
        code.setId(UUID.randomUUID());
        code.setUser(user);
        code.setPurpose(VerificationCodePurpose.PASSWORD_CHANGE);
        code.setCodeHash("hashed-code");
        code.setExpiresAt(Instant.now().plusSeconds(300));
        code.setCreatedAt(Instant.now().minusSeconds(120));
        return code;
    }

    private User activeUser(UUID id) {
        User user = new User();
        user.setId(id);
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
