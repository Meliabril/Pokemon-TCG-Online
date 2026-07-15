package ar.edu.utn.frc.tup.piii.dtos.card;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;

import java.util.List;
import java.util.UUID;

public record CardResponseDto(
        UUID id,
        String externalId,
        String setCode,
        String setName,
        String number,
        String name,
        CardSupertype supertype,
        CardCategory category,
        String subtype,
        String evolvesFrom,
        Integer hp,
        String pokemonType,
        Integer retreatCost,
        String imageSmallUrl,
        String imageLargeUrl,
        List<AttackDto> attacks,
        List<CardRelationDto> weaknesses,
        List<CardRelationDto> resistances,
        String displayName,
        String displayPokemonType,
        String displaySupertype,
        List<String> displaySubtypes,
        List<CardAbilityDto> abilities,
        List<AbilityTranslationDto> displayAbilities,
        List<String> rules,
        List<String> displayRules) {
}
