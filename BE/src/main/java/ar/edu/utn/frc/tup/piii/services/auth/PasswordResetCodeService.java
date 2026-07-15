package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeService {

    PasswordResetCode save(PasswordResetCode passwordResetCode);

    Optional<PasswordResetCode> findLatestPendingCode(
            User user,
            VerificationCodePurpose purpose);

    List<PasswordResetCode> findPendingCodes(
            User user,
            VerificationCodePurpose purpose);

    Optional<PasswordResetCode> findPendingCodeByToken(
            UUID id,
            VerificationCodePurpose purpose);
}
