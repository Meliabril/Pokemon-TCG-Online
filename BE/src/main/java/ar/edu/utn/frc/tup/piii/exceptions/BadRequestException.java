package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Signals malformed client requests that should return HTTP 400.
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
    }
}
