package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;
import java.util.UUID;

public record VisibleAttackDto(
        UUID attackId,
        String name,
        String damageText,
        Integer baseDamage,
        String effectText,
        List<VisibleAttackCostDto> costs,
        boolean enabled,
        String disabledReason,
        boolean requiresTarget,
        List<UUID> validTargetPokemonInPlayIds,
        boolean targetsOwnPokemon) {

    public VisibleAttackDto {
        costs = costs == null ? List.of() : List.copyOf(costs);
        validTargetPokemonInPlayIds = validTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(validTargetPokemonInPlayIds);
    }
}
