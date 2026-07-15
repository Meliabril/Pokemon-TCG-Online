package ar.edu.utn.frc.tup.piii.services.mail;

import ar.edu.utn.frc.tup.piii.entities.User;

public interface MailService {

    void sendVerificationCode(User user, String code);

    void sendPasswordResetCode(User user, String code);

    void sendPasswordChangeCode(User user, String code);
}
