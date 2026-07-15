package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordResetCodeService;
import ar.edu.utn.frc.tup.piii.services.auth.VerificationCodeService;
import ar.edu.utn.frc.tup.piii.services.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final int CODE_BOUND = 1_000_000;
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);
    private static final Duration MIN_RESEND_INTERVAL = Duration.ofSeconds(60);
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PasswordResetCodeService passwordResetCodeService;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSendCode(User user, VerificationCodePurpose purpose) {
        ensureSupportedPurpose(purpose);

        Instant now = Instant.now();
        if (recentPendingCodeExists(user, purpose, now)) {
            return;
        }

        invalidatePreviousCodes(user, purpose, now);

        String code = generateCode();
        PasswordResetCode passwordResetCode = new PasswordResetCode();
        passwordResetCode.setUser(user);
        passwordResetCode.setPurpose(purpose);
        passwordResetCode.setCodeHash(hash(code, purpose));
        passwordResetCode.setExpiresAt(now.plus(CODE_EXPIRATION));

        passwordResetCodeService.save(passwordResetCode);
        sendCode(user, code, purpose);
    }

    @Override
    @Transactional
    public PasswordResetCode validateCode(User user, String code, VerificationCodePurpose purpose) {
        ensureSupportedPurpose(purpose);

        PasswordResetCode passwordResetCode = passwordResetCodeService
                .findLatestPendingCode(user, purpose)
                .orElseThrow(() -> invalidCode(purpose));

        Instant now = Instant.now();
        if (passwordResetCode.isUsed()) {
            throw alreadyUsedCode(purpose);
        }

        if (passwordResetCode.isExpired(now)) {
            passwordResetCode.markAsUsed(now);
            passwordResetCodeService.save(passwordResetCode);
            throw expiredCode(purpose);
        }

        if (!constantTimeEquals(passwordResetCode.getCodeHash(), hash(code, purpose))) {
            throw invalidCode(purpose);
        }

        return passwordResetCode;
    }

    @Override
    @Transactional
    public String verifyCode(User user, String code, VerificationCodePurpose purpose) {
        PasswordResetCode passwordResetCode = validateCode(user, code, purpose);
        passwordResetCode.markAsVerified(Instant.now());
        passwordResetCodeService.save(passwordResetCode);
        return passwordResetCode.getId().toString();
    }

    @Override
    @Transactional
    public PasswordResetCode getVerifiedCode(String verificationToken, VerificationCodePurpose purpose) {
        ensureSupportedPurpose(purpose);

        PasswordResetCode passwordResetCode = passwordResetCodeService
                .findPendingCodeByToken(parseVerificationToken(verificationToken, purpose), purpose)
                .orElseThrow(() -> invalidCode(purpose));

        ensureCodeWasVerified(passwordResetCode, purpose);
        ensureCodeIsNotExpired(passwordResetCode, purpose);
        return passwordResetCode;
    }

    @Override
    @Transactional
    public PasswordResetCode getVerifiedCode(
            User user,
            String verificationToken,
            VerificationCodePurpose purpose) {
        PasswordResetCode passwordResetCode = getVerifiedCode(verificationToken, purpose);
        if (!user.getId().equals(passwordResetCode.getUser().getId())) {
            throw invalidCode(purpose);
        }

        return passwordResetCode;
    }

    @Override
    @Transactional
    public void markCodeAsUsed(PasswordResetCode code) {
        code.markAsUsed(Instant.now());
        passwordResetCodeService.save(code);
    }

    private boolean recentPendingCodeExists(User user, VerificationCodePurpose purpose, Instant now) {
        return passwordResetCodeService
                .findLatestPendingCode(user, purpose)
                .filter(code -> code.getCreatedAt() != null)
                .filter(code -> !code.isExpired(now))
                .map(code -> code.getCreatedAt().isAfter(now.minus(MIN_RESEND_INTERVAL)))
                .orElse(false);
    }

    private void invalidatePreviousCodes(User user, VerificationCodePurpose purpose, Instant now) {
        passwordResetCodeService.findPendingCodes(user, purpose)
                .forEach(previousCode -> {
                    previousCode.markAsUsed(now);
                    passwordResetCodeService.save(previousCode);
                });
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
    }

    private String hash(String code, VerificationCodePurpose purpose) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return BASE64_URL_ENCODER.encodeToString(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(
                    hashFailureCode(purpose),
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

    private void sendCode(User user, String code, VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            mailService.sendPasswordResetCode(user, code);
            return;
        }

        mailService.sendPasswordChangeCode(user, code);
    }

    private UUID parseVerificationToken(String verificationToken, VerificationCodePurpose purpose) {
        try {
            return UUID.fromString(verificationToken);
        } catch (RuntimeException exception) {
            throw invalidCode(purpose);
        }
    }

    private void ensureCodeWasVerified(PasswordResetCode passwordResetCode, VerificationCodePurpose purpose) {
        if (!passwordResetCode.isVerified()) {
            throw invalidCode(purpose);
        }
    }

    private void ensureCodeIsNotExpired(PasswordResetCode passwordResetCode, VerificationCodePurpose purpose) {
        if (passwordResetCode.isExpired(Instant.now())) {
            passwordResetCode.markAsUsed(Instant.now());
            passwordResetCodeService.save(passwordResetCode);
            throw expiredCode(purpose);
        }
    }

    private void ensureSupportedPurpose(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.EMAIL_VERIFICATION) {
            throw new BusinessException(
                    "UNSUPPORTED_VERIFICATION_CODE_PURPOSE",
                    "Email verification codes are not handled by this service",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private BusinessException invalidCode(VerificationCodePurpose purpose) {
        return new BusinessException(
                invalidCodeName(purpose),
                invalidMessage(purpose),
                HttpStatus.BAD_REQUEST
        );
    }

    private BusinessException expiredCode(VerificationCodePurpose purpose) {
        return new BusinessException(
                prefix(purpose) + "_CODE_EXPIRED",
                expiredMessage(purpose),
                HttpStatus.BAD_REQUEST
        );
    }

    private BusinessException alreadyUsedCode(VerificationCodePurpose purpose) {
        return new BusinessException(
                prefix(purpose) + "_CODE_ALREADY_USED",
                alreadyUsedMessage(purpose),
                HttpStatus.BAD_REQUEST
        );
    }

    private String hashFailureCode(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            return "PASSWORD_RESET_CODE_HASH_FAILED";
        }

        return "PASSWORD_CHANGE_CODE_HASH_FAILED";
    }

    private String invalidCodeName(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            return "INVALID_PASSWORD_RESET_CODE";
        }

        return "INVALID_PASSWORD_CHANGE_CODE";
    }

    private String prefix(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            return "PASSWORD_RESET";
        }

        return "PASSWORD_CHANGE";
    }

    private String invalidMessage(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            return "Invalid password reset code";
        }

        return "Invalid password change code";
    }

    private String expiredMessage(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            return "Password reset code expired";
        }

        return "Password change code expired";
    }

    private String alreadyUsedMessage(VerificationCodePurpose purpose) {
        if (purpose == VerificationCodePurpose.PASSWORD_RESET) {
            return "Password reset code was already used";
        }

        return "Password change code was already used";
    }
}
