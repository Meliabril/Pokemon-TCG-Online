package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.EmailVerificationCode;
import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.repositories.EmailVerificationCodeRepository;
import ar.edu.utn.frc.tup.piii.services.auth.impl.EmailVerificationServiceImpl;
import ar.edu.utn.frc.tup.piii.services.mail.MailService;
import ar.edu.utn.frc.tup.piii.services.user.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private EmailVerificationCodeRepository verificationCodeRepository;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private MailService mailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private EmailVerificationServiceImpl emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationServiceImpl(
                verificationCodeRepository,
                userAccountService,
                mailService,
                jwtService,
                refreshTokenService,
                new UserMapper());

        lenient().when(verificationCodeRepository.save(any(EmailVerificationCode.class)))
                .thenAnswer(invocation -> {
                    EmailVerificationCode code = invocation.getArgument(0);
                    if (code.getId() == null) {
                        code.setId(UUID.randomUUID());
                    }
                    if (code.getCreatedAt() == null) {
                        code.setCreatedAt(Instant.now());
                    }
                    return code;
                });
    }

    @Test
    void generateAndSendVerificationCodeShouldSaveHashedCodeAndSendEmail() {
        User user = unverifiedUser();
        when(verificationCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());

        emailVerificationService.generateAndSendVerificationCode(user);

        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        ArgumentCaptor<String> plainCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationCodeRepository).save(codeCaptor.capture());
        verify(mailService).sendVerificationCode(any(User.class), plainCodeCaptor.capture());

        EmailVerificationCode savedCode = codeCaptor.getValue();
        String plainCode = plainCodeCaptor.getValue();
        assertThat(plainCode).matches("[0-9]{6}");
        assertThat(savedCode.getUser()).isEqualTo(user);
        assertThat(savedCode.getAttempts()).isZero();
        assertThat(savedCode.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedCode.getCodeHash()).isNotEqualTo(plainCode);
    }

    @Test
    void verifyAccountShouldVerifyUserAndReturnAuthResponse() {
        User user = unverifiedUser();
        GeneratedCode generatedCode = generateCodeFor(user);
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(verificationCodeRepository.findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(user.getEmail()))
                .thenReturn(Optional.of(generatedCode.entity()));
        when(userAccountService.markEmailAsVerified(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setStatus(UserStatus.ACTIVE);
            savedUser.setEmailVerified(true);
            return savedUser;
        });
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("refresh-token", refreshToken(user)));

        AuthSessionResult response = emailVerificationService.verifyAccount(user.getEmail(), generatedCode.plainCode());

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(generatedCode.entity().getUsedAt()).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().emailVerified()).isTrue();
    }

    @Test
    void verifyAccountShouldRejectWrongCodeAndIncreaseAttempts() {
        User user = unverifiedUser();
        GeneratedCode generatedCode = generateCodeFor(user);
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(verificationCodeRepository.findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(user.getEmail()))
                .thenReturn(Optional.of(generatedCode.entity()));

        assertThatThrownBy(() -> emailVerificationService.verifyAccount(user.getEmail(), "000000"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(generatedCode.entity().getAttempts()).isEqualTo(1);
        assertThat(user.getEmailVerified()).isFalse();
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void verifyAccountShouldRejectExpiredCode() {
        User user = unverifiedUser();
        GeneratedCode generatedCode = generateCodeFor(user);
        generatedCode.entity().setExpiresAt(Instant.now().minusSeconds(1));
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(verificationCodeRepository.findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(user.getEmail()))
                .thenReturn(Optional.of(generatedCode.entity()));

        assertThatThrownBy(() -> emailVerificationService.verifyAccount(user.getEmail(), generatedCode.plainCode()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("VERIFICATION_CODE_EXPIRED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.GONE);
                });

        assertThat(generatedCode.entity().getUsedAt()).isNotNull();
        assertThat(user.getEmailVerified()).isFalse();
    }

    @Test
    void verifyAccountShouldRejectUsedCodeWhenNoActiveCodeExists() {
        User user = unverifiedUser();
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(verificationCodeRepository.findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(user.getEmail()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyAccount(user.getEmail(), "123456"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void verifyAccountShouldRejectUnknownAccount() {
        when(userAccountService.getUserEntityByEmail("missing@gmail.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: missing@gmail.com"));

        assertThatThrownBy(() -> emailVerificationService.verifyAccount("missing@gmail.com", "123456"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyAccountShouldRejectAlreadyVerifiedAccount() {
        User user = verifiedUser();
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);

        assertThatThrownBy(() -> emailVerificationService.verifyAccount(user.getEmail(), "123456"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("ACCOUNT_ALREADY_VERIFIED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void verifyAccountShouldRejectTooManyAttempts() {
        User user = unverifiedUser();
        GeneratedCode generatedCode = generateCodeFor(user);
        generatedCode.entity().setAttempts(5);
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(verificationCodeRepository.findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(user.getEmail()))
                .thenReturn(Optional.of(generatedCode.entity()));

        assertThatThrownBy(() -> emailVerificationService.verifyAccount(user.getEmail(), generatedCode.plainCode()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("TOO_MANY_VERIFICATION_ATTEMPTS");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
    }

    @Test
    void resendVerificationCodeShouldInvalidatePreviousCodeGenerateNewCodeAndSendEmail() {
        User user = unverifiedUser();
        EmailVerificationCode previousCode = activeCode(user);
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);
        when(verificationCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of(previousCode));

        emailVerificationService.resendVerificationCode(user.getEmail());

        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(verificationCodeRepository, org.mockito.Mockito.times(2)).save(codeCaptor.capture());
        assertThat(previousCode.getUsedAt()).isNotNull();
        assertThat(codeCaptor.getAllValues().get(1).getUsedAt()).isNull();
        verify(mailService).sendVerificationCode(any(User.class), org.mockito.ArgumentMatchers.matches("[0-9]{6}"));
    }

    @Test
    void resendVerificationCodeShouldRejectUnknownAccount() {
        when(userAccountService.getUserEntityByEmail("missing@gmail.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: missing@gmail.com"));

        assertThatThrownBy(() -> emailVerificationService.resendVerificationCode("missing@gmail.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mailService, never()).sendVerificationCode(any(), any());
    }

    @Test
    void resendVerificationCodeShouldRejectAlreadyVerifiedAccount() {
        User user = verifiedUser();
        when(userAccountService.getUserEntityByEmail(user.getEmail())).thenReturn(user);

        assertThatThrownBy(() -> emailVerificationService.resendVerificationCode(user.getEmail()))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("ACCOUNT_ALREADY_VERIFIED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(mailService, never()).sendVerificationCode(any(), any());
    }

    private GeneratedCode generateCodeFor(User user) {
        when(verificationCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())).thenReturn(List.of());

        emailVerificationService.generateAndSendVerificationCode(user);

        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        ArgumentCaptor<String> plainCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationCodeRepository).save(codeCaptor.capture());
        verify(mailService).sendVerificationCode(any(User.class), plainCodeCaptor.capture());
        return new GeneratedCode(codeCaptor.getValue(), plainCodeCaptor.getValue());
    }

    private EmailVerificationCode activeCode(User user) {
        EmailVerificationCode code = new EmailVerificationCode();
        code.setId(UUID.randomUUID());
        code.setUser(user);
        code.setCodeHash("hashed-code");
        code.setExpiresAt(Instant.now().plusSeconds(300));
        code.setCreatedAt(Instant.now());
        code.setAttempts(0);
        return code;
    }

    private RefreshToken refreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash("hashed-refresh-token");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setCreatedAt(Instant.now());
        return refreshToken;
    }

    private User unverifiedUser() {
        User user = baseUser();
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        return user;
    }

    private User verifiedUser() {
        User user = baseUser();
        user.setEmailVerified(true);
        return user;
    }

    private User baseUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("melina@gmail.com");
        user.setUsername("meli123");
        user.setAvatar("avatar-pikachu-01");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        return user;
    }

    private record GeneratedCode(EmailVerificationCode entity, String plainCode) {
    }
}
