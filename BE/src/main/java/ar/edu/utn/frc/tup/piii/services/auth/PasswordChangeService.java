package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.PasswordCodeVerificationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.ChangePasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.VerifyPasswordChangeCodeRequestDto;

import java.util.UUID;

public interface PasswordChangeService {

    void requestPasswordChangeCode(UUID userId);

    PasswordCodeVerificationResponseDto verifyPasswordChangeCode(UUID userId, VerifyPasswordChangeCodeRequestDto request);

    void changeCurrentUserPassword(UUID userId, ChangePasswordRequestDto request);
}
