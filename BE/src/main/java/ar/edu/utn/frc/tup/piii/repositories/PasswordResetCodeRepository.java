package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    Optional<PasswordResetCode> findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            User user,
            VerificationCodePurpose purpose);

    List<PasswordResetCode> findByUserAndPurposeAndUsedAtIsNull(
            User user,
            VerificationCodePurpose purpose);

    Optional<PasswordResetCode> findByIdAndPurposeAndUsedAtIsNull(
            UUID id,
            VerificationCodePurpose purpose);
}
