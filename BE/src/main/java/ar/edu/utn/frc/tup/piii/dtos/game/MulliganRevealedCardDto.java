package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;

import java.util.UUID;

public record MulliganRevealedCardDto(
        UUID cardId,
        String name,
        String externalId,
        String setCode,
        String number,
        CardSupertype supertype,
        CardCategory category,
        String imageSmallUrl,
        String imageLargeUrl,
        Integer hp) {
}
