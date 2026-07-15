package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<EmailVerificationCode> findTopByUserEmailIgnoreCaseAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    List<EmailVerificationCode> findByUserIdAndUsedAtIsNull(UUID userId);
}
