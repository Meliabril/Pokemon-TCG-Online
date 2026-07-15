package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidGameActionException extends BusinessException {

    public InvalidGameActionException(String message) {
        super("INVALID_GAME_ACTION", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

