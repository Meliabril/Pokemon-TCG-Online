package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.entities.EmailVerificationCode;
import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.repositories.EmailVerificationCodeRepository;
import ar.edu.utn.frc.tup.piii.services.auth.AuthSessionResult;
import ar.edu.utn.frc.tup.piii.services.auth.EmailVerificationService;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.auth.RefreshTokenService;
import ar.edu.utn.frc.tup.piii.services.mail.MailService;
import ar.edu.utn.frc.tup.piii.services.user.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int CODE_BOUND = 1_000_000;
    private static final int MAX_ATTEMPTS = 5;
    private static final long EXPIRATION_SECONDS = 300;
    private static final String TOKEN_TYPE = "Bearer";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserAccountService userAccountService;
    private final MailService mailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSendVerificationCode(User user) {
        invalidatePreviousCodes(user);

        String code = generateCode();
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setCodeHash(hash(code));
        verificationCode.setExpiresAt(Instant.now().plusSeconds(EXPIRATION_SECONDS));
        verificationCode.setAttempts(0);

        verificationCodeRepository.save(verificationCode);
        mailService.sendVerificationCode(user, code);
    }

    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        User user = userAccountService.getUserEntityByEmail(email);

        ensureCanVerify(user);
        generateAndSendVerificationCode(user);
    }

    @Override
    @Transactional
    public AuthSessionResult verifyAccount(String email, String code) {
        User user = userAccountService.getUserEntityByEmail(email);

        ensureCanVerify(user);

        EmailVerificationCode verificationCode = verificationCodeRepository
                .findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid verification code"));

        ensureCodeCanBeUsed(verificationCode);

        if (!constantTimeEquals(verificationCode.getCodeHash(), hash(code))) {
            verificationCode.setAttempts(verificationCode.getAttempts() + 1);
            verificationCodeRepository.save(verificationCode);
            throw new InvalidCredentialsException("Invalid verification code");
        }

        verificationCode.setUsedAt(Instant.now());
        User savedUser = userAccountService.markEmailAsVerified(user);

        RefreshTokenService.RefreshTokenResult refreshToken = refreshTokenService.createRefreshToken(savedUser);
        return new AuthSessionResult(
                jwtService.generateAccessToken(savedUser),
                refreshToken.rawToken(),
                TOKEN_TYPE,
                jwtService.getAccessTokenExpirationSeconds(),
                userMapper.toUserResponseDto(savedUser));
    }

    private void ensureCanVerify(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(
                    "ACCOUNT_ALREADY_VERIFIED",
                    "Account is already verified",
                    HttpStatus.CONFLICT
            );
        }

        if (!isPendingVerificationUser(user)) {
            throw new BusinessException(
                    "ACCOUNT_NOT_VERIFIABLE",
                    "Only pending accounts can be verified",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private boolean isPendingVerificationUser(User user) {
        return user.getStatus() == UserStatus.PENDING_VERIFICATION
                || (user.getStatus() == UserStatus.ACTIVE && Boolean.FALSE.equals(user.getEmailVerified()));
    }

    private void ensureCodeCanBeUsed(EmailVerificationCode verificationCode) {
        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            verificationCode.setUsedAt(Instant.now());
            verificationCodeRepository.save(verificationCode);
            throw new BusinessException(
                    "VERIFICATION_CODE_EXPIRED",
                    "Verification code expired",
                    HttpStatus.GONE
            );
        }

        if (verificationCode.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException(
                    "TOO_MANY_VERIFICATION_ATTEMPTS",
                    "Too many verification attempts",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    private void invalidatePreviousCodes(User user) {
        Instant now = Instant.now();
        verificationCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())
                .forEach(previousCode -> {
                    previousCode.setUsedAt(now);
                    verificationCodeRepository.save(previousCode);
                });
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return BASE64_URL_ENCODER.encodeToString(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(
                    "VERIFICATION_CODE_HASH_FAILED",
                    "Could not hash verification code",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
