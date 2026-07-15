package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class RefreshTokenRevokedException extends BusinessException {

    public RefreshTokenRevokedException(String message) {
        super("REFRESH_TOKEN_REVOKED", message, HttpStatus.UNAUTHORIZED);
    }
}
