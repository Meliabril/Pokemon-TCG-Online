package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.auth.impl.VerificationCodeServiceImpl;
import ar.edu.utn.frc.tup.piii.services.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceImplTest {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Mock
    private PasswordResetCodeService passwordResetCodeService;

    @Mock
    private MailService mailService;

    private VerificationCodeServiceImpl verificationCodeService;

    @BeforeEach
    void setUp() {
        verificationCodeService = new VerificationCodeServiceImpl(passwordResetCodeService, mailService);
    }

    @Test
    void generateAndSendCodeShouldSavePasswordResetPurposeAndSendResetEmail() {
        User user = activeUser();
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(passwordResetCodeService.findPendingCodes(
                user,
                VerificationCodePurpose.PASSWORD_RESET))
                .thenReturn(List.of());

        verificationCodeService.generateAndSendCode(user, VerificationCodePurpose.PASSWORD_RESET);

        ArgumentCaptor<PasswordResetCode> codeCaptor = ArgumentCaptor.forClass(PasswordResetCode.class);
        ArgumentCaptor<String> plainCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetCodeService).save(codeCaptor.capture());
        verify(mailService).sendPasswordResetCode(eq(user), plainCodeCaptor.capture());
        verify(mailService, never()).sendPasswordChangeCode(any(), any());

        PasswordResetCode savedCode = codeCaptor.getValue();
        String plainCode = plainCodeCaptor.getValue();
        assertThat(plainCode).matches("[0-9]{6}");
        assertThat(savedCode.getPurpose()).isEqualTo(VerificationCodePurpose.PASSWORD_RESET);
        assertThat(savedCode.getCodeHash()).isNotEqualTo(plainCode);
        assertThat(savedCode.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void generateAndSendCodeShouldSavePasswordChangePurposeAndSendChangeEmail() {
        User user = activeUser();
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(Optional.empty());
        when(passwordResetCodeService.findPendingCodes(
                user,
                VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(List.of());

        verificationCodeService.generateAndSendCode(user, VerificationCodePurpose.PASSWORD_CHANGE);

        ArgumentCaptor<PasswordResetCode> codeCaptor = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(passwordResetCodeService).save(codeCaptor.capture());
        verify(mailService).sendPasswordChangeCode(any(User.class), any(String.class));
        verify(mailService, never()).sendPasswordResetCode(any(), any());
        assertThat(codeCaptor.getValue().getPurpose()).isEqualTo(VerificationCodePurpose.PASSWORD_CHANGE);
    }

    @Test
    void generateAndSendCodeShouldRespectCooldown() {
        User user = activeUser();
        PasswordResetCode recentCode = activeCode(user, "111111", VerificationCodePurpose.PASSWORD_CHANGE);
        recentCode.setCreatedAt(Instant.now().minusSeconds(20));
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(Optional.of(recentCode));

        verificationCodeService.generateAndSendCode(user, VerificationCodePurpose.PASSWORD_CHANGE);

        verify(passwordResetCodeService, never()).save(any());
        verify(mailService, never()).sendPasswordChangeCode(any(), any());
    }

    @Test
    void generateAndSendCodeShouldInvalidatePreviousCodesForSamePurpose() {
        User user = activeUser();
        PasswordResetCode previousCode = activeCode(user, "111111", VerificationCodePurpose.PASSWORD_CHANGE);
        previousCode.setCreatedAt(Instant.now().minusSeconds(120));
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(Optional.of(previousCode));
        when(passwordResetCodeService.findPendingCodes(
                user,
                VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(List.of(previousCode));

        verificationCodeService.generateAndSendCode(user, VerificationCodePurpose.PASSWORD_CHANGE);

        assertThat(previousCode.getUsedAt()).isNotNull();
        verify(mailService).sendPasswordChangeCode(any(), any());
    }

    @Test
    void validateCodeShouldRejectCodeFromAnotherPurpose() {
        User user = activeUser();
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationCodeService.validateCode(
                user,
                "839214",
                VerificationCodePurpose.PASSWORD_CHANGE))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("INVALID_PASSWORD_CHANGE_CODE");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void validateCodeShouldMarkExpiredCodeAsUsedBeforeThrowing() {
        User user = activeUser();
        PasswordResetCode code = activeCode(user, "839214", VerificationCodePurpose.PASSWORD_RESET);
        code.setExpiresAt(Instant.now().minusSeconds(1));
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(code));

        assertThatThrownBy(() -> verificationCodeService.validateCode(
                user,
                "839214",
                VerificationCodePurpose.PASSWORD_RESET))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("PASSWORD_RESET_CODE_EXPIRED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        assertThat(code.getUsedAt()).isNotNull();
        verify(passwordResetCodeService).save(code);
    }

    @Test
    void validateCodeShouldReturnMatchingCode() {
        User user = activeUser();
        PasswordResetCode code = activeCode(user, "839214", VerificationCodePurpose.PASSWORD_CHANGE);
        when(passwordResetCodeService
                .findLatestPendingCode(
                        user,
                        VerificationCodePurpose.PASSWORD_CHANGE))
                .thenReturn(Optional.of(code));

        PasswordResetCode response = verificationCodeService.validateCode(
                user,
                "839214",
                VerificationCodePurpose.PASSWORD_CHANGE);

        assertThat(response).isEqualTo(code);
    }

    @Test
    void emailVerificationPurposeShouldRemainUnsupported() {
        assertThatThrownBy(() -> verificationCodeService.generateAndSendCode(
                activeUser(),
                VerificationCodePurpose.EMAIL_VERIFICATION))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo("UNSUPPORTED_VERIFICATION_CODE_PURPOSE"));
    }

    private PasswordResetCode activeCode(User user, String plainCode, VerificationCodePurpose purpose) {
        PasswordResetCode code = new PasswordResetCode();
        code.setId(UUID.randomUUID());
        code.setUser(user);
        code.setPurpose(purpose);
        code.setCodeHash(hash(plainCode));
        code.setExpiresAt(Instant.now().plusSeconds(300));
        code.setCreatedAt(Instant.now().minusSeconds(120));
        return code;
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return BASE64_URL_ENCODER.encodeToString(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
