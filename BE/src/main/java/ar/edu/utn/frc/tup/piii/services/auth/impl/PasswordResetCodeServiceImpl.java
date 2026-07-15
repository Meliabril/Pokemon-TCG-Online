package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.entities.PasswordResetCode;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.VerificationCodePurpose;
import ar.edu.utn.frc.tup.piii.repositories.PasswordResetCodeRepository;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordResetCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetCodeServiceImpl implements PasswordResetCodeService {

    private final PasswordResetCodeRepository passwordResetCodeRepository;

    @Override
    public PasswordResetCode save(PasswordResetCode passwordResetCode) {
        return passwordResetCodeRepository.save(passwordResetCode);
    }

    @Override
    public Optional<PasswordResetCode> findLatestPendingCode(
            User user,
            VerificationCodePurpose purpose) {
        return passwordResetCodeRepository
                .findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, purpose);
    }

    @Override
    public List<PasswordResetCode> findPendingCodes(
            User user,
            VerificationCodePurpose purpose) {
        return passwordResetCodeRepository.findByUserAndPurposeAndUsedAtIsNull(user, purpose);
    }

    @Override
    public Optional<PasswordResetCode> findPendingCodeByToken(
            UUID id,
            VerificationCodePurpose purpose) {
        return passwordResetCodeRepository.findByIdAndPurposeAndUsedAtIsNull(id, purpose);
    }
}
