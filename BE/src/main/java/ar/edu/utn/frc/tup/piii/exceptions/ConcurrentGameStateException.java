package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class ConcurrentGameStateException extends BusinessException {

    public ConcurrentGameStateException(String message) {
        super("CONCURRENT_GAME_STATE", message, HttpStatus.CONFLICT);
    }
}

