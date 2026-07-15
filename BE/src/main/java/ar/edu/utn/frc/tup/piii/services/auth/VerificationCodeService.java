package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;

public interface VerificationCodeService {

    void generateAndSendCode(User user, VerificationCodePurpose purpose);

    PasswordResetCode validateCode(User user, String code, VerificationCodePurpose purpose);

    String verifyCode(User user, String code, VerificationCodePurpose purpose);

    PasswordResetCode getVerifiedCode(String verificationToken, VerificationCodePurpose purpose);

    PasswordResetCode getVerifiedCode(User user, String verificationToken, VerificationCodePurpose purpose);

    void markCodeAsUsed(PasswordResetCode code);
}
