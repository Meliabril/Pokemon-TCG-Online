package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends BusinessException {

    public UsernameAlreadyExistsException(String username) {
        super("USERNAME_ALREADY_EXISTS", "Username is already in use: " + username, HttpStatus.CONFLICT);
    }
}
