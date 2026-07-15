package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenActionException extends BusinessException {

    public ForbiddenActionException(String message) {
        super("FORBIDDEN_ACTION", message, HttpStatus.FORBIDDEN);
    }
}

