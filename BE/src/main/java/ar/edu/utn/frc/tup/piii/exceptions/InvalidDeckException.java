package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidDeckException extends BusinessException {

    public InvalidDeckException(String message) {
        super("INVALID_DECK", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

