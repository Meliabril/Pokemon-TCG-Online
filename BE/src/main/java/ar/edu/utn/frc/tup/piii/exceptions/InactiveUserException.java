package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class InactiveUserException extends BusinessException {

    public InactiveUserException(String message) {
        super("INACTIVE_USER", message, HttpStatus.FORBIDDEN);
    }
}
