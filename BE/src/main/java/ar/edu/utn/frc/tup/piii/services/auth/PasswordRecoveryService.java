package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.ForgotPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.PasswordCodeVerificationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResetPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifiedPasswordResetRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifyPasswordResetCodeRequestDto;

public interface PasswordRecoveryService {

    void requestPasswordReset(ForgotPasswordRequestDto request);

    PasswordCodeVerificationResponseDto verifyPasswordResetCode(VerifyPasswordResetCodeRequestDto request);

    void resetVerifiedPassword(VerifiedPasswordResetRequestDto request);

    void resetPassword(ResetPasswordRequestDto request);
}
