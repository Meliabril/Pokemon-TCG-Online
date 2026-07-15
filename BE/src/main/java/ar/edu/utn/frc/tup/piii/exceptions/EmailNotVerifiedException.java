package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends BusinessException {

    public EmailNotVerifiedException(String message) {
        super("EMAIL_NOT_VERIFIED", message, HttpStatus.FORBIDDEN);
    }
}
