package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;
import java.util.UUID;

public record BoardAttackDto(
        UUID attackId,
        String name,
        String damageText,
        Integer baseDamage,
        String effectText,
        int attackOrder,
        boolean available,
        String disabledReason,
        boolean requiresTarget,
        List<UUID> validTargetPokemonInPlayIds,
        boolean targetsOwnPokemon) {

    public BoardAttackDto {
        validTargetPokemonInPlayIds = validTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(validTargetPokemonInPlayIds);
    }
}
