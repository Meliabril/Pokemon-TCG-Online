package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;

import java.util.UUID;

public record UseAbilityRequest(
        UUID sourcePokemonId,
        AbilityCode abilityCode,
        UUID selectedHandCardId,
        UUID targetPokemonId,
        UUID sourceEnergyCardId,
        UUID fromPokemonId,
        UUID toPokemonId,
        UUID selectedDeckCardId) {
}
