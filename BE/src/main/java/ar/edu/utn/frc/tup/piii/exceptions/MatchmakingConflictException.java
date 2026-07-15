package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class MatchmakingConflictException extends BusinessException {

    public MatchmakingConflictException(String message) {
        super("MATCHMAKING_CONFLICT", message, HttpStatus.CONFLICT);
    }
}
