package ar.edu.utn.frc.tup.piii.services.mail.impl;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.exceptions.BusinessException;
import ar.edu.utn.frc.tup.piii.services.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final String VERIFICATION_SUBJECT = "Codigo de verificacion - Pokemon TCG Online";
    private static final String PASSWORD_RESET_SUBJECT = "Codigo para recuperar tu contrasena - Pokemon TCG";
    private static final String PASSWORD_CHANGE_SUBJECT = "Codigo para cambiar tu contrasena - Pokemon TCG";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Override
    public void sendVerificationCode(User user, String code) {
        sendCodeEmail(user.getEmail(), VERIFICATION_SUBJECT, buildVerificationBody(code));
    }

    @Override
    public void sendPasswordResetCode(User user, String code) {
        sendCodeEmail(user.getEmail(), PASSWORD_RESET_SUBJECT, buildPasswordResetBody(code));
    }

    @Override
    public void sendPasswordChangeCode(User user, String code) {
        sendCodeEmail(user.getEmail(), PASSWORD_CHANGE_SUBJECT, buildPasswordChangeBody(code));
    }

    private void sendCodeEmail(String to, String subject, String body) {
        ensureMailIsConfigured();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(
                    "MAIL_SEND_FAILED",
                    "Could not send verification email",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private void ensureMailIsConfigured() {
        if (!StringUtils.hasText(fromAddress)) {
            throw new BusinessException(
                    "MAIL_NOT_CONFIGURED",
                    "Mail username is not configured. Set MAIL_USERNAME and MAIL_PASSWORD.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private String buildVerificationBody(String code) {
        return """
                Tu codigo de verificacion es: %s

                Este codigo vence en 5 minutos.
                Si no solicitaste esta cuenta, podes ignorar este mensaje.
                """.formatted(code);
    }

    private String buildPasswordResetBody(String code) {
        return """
                Tu codigo para recuperar la contrasena es: %s

                Este codigo vence en 5 minutos.
                Si no pediste cambiar tu contrasena, podes ignorar este mensaje.
                """.formatted(code);
    }

    private String buildPasswordChangeBody(String code) {
        return """
                Tu codigo para cambiar la contrasena desde tu perfil es: %s

                Este codigo vence en 5 minutos.
                Si no pediste cambiar tu contrasena, podes ignorar este mensaje.
                """.formatted(code);
    }
}
