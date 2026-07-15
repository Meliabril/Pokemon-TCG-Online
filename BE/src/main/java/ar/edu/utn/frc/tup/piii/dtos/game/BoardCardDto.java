package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;

import java.util.List;
import java.util.UUID;

public record BoardCardDto(
        UUID cardInstanceId,
        UUID cardId,
        String externalId,
        String name,
        String setCode,
        String number,
        CardSupertype supertype,
        CardCategory category,
        String subtype,
        String imageSmallUrl,
        String imageLargeUrl,
        Integer hp,
        CardZone zone,
        Integer zonePosition,
        boolean faceDown,
        boolean playable,
        GameActionType suggestedAction,
        String disabledReason,
        List<UUID> validTargetPokemonInPlayIds) {

    public static BoardCardDto hidden(CardZone zone) {
        return new BoardCardDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                zone,
                null,
                true,
                false,
                null,
                null,
                List.of());
    }
}
