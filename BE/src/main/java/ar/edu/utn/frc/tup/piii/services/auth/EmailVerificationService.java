package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.User;

public interface EmailVerificationService {

    void generateAndSendVerificationCode(User user);

    void resendVerificationCode(String email);

    AuthSessionResult verifyAccount(String email, String code);
}
