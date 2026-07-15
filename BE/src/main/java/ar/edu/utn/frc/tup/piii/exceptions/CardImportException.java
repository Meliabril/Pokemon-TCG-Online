package ar.edu.utn.frc.tup.piii.exceptions;

import org.springframework.http.HttpStatus;

public class CardImportException extends BusinessException {

    public CardImportException(String message) {
        super("CARD_IMPORT_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
