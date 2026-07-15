package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;

import java.util.List;
import java.util.UUID;

public record VisibleCardDto(
        UUID cardInstanceId,
        UUID cardId,
        String name,
        String externalId,
        String setCode,
        String number,
        CardSupertype supertype,
        CardCategory category,
        String subtype,
        String imageSmallUrl,
        String imageLargeUrl,
        Integer hp,
        boolean faceDown,
        boolean playable,
        GameActionType suggestedAction,
        String disabledReason,
        List<UUID> validTargetPokemonInPlayIds) {

    public VisibleCardDto {
        validTargetPokemonInPlayIds = validTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(validTargetPokemonInPlayIds);
    }
}
