package ar.edu.utn.frc.tup.piii.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardTranslationDto(
        String displayName,
        String displayPokemonType,
        String displaySupertype,
        List<String> displaySubtypes,
        List<AbilityTranslationDto> displayAbilities,
        List<AttackTranslationDto> displayAttacks,
        List<TypeValueTranslationDto> displayWeaknesses,
        List<TypeValueTranslationDto> displayResistances,
        List<String> displayRules) {
}
