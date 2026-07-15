package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;
import java.util.UUID;

public record VisibleBoardActionHintsDto(
        List<UUID> attachEnergyTargetPokemonInPlayIds,
        List<UUID> retreatTargetPokemonInPlayIds,
        List<UUID> promoteTargetPokemonInPlayIds,
        List<UUID> trainerTargetPokemonInPlayIds,
        List<UUID> trainerToolTargetPokemonInPlayIds) {

    public VisibleBoardActionHintsDto {
        attachEnergyTargetPokemonInPlayIds = attachEnergyTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(attachEnergyTargetPokemonInPlayIds);
        retreatTargetPokemonInPlayIds = retreatTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(retreatTargetPokemonInPlayIds);
        promoteTargetPokemonInPlayIds = promoteTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(promoteTargetPokemonInPlayIds);
        trainerTargetPokemonInPlayIds = trainerTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(trainerTargetPokemonInPlayIds);
        trainerToolTargetPokemonInPlayIds = trainerToolTargetPokemonInPlayIds == null
                ? List.of()
                : List.copyOf(trainerToolTargetPokemonInPlayIds);
    }

    public static VisibleBoardActionHintsDto empty() {
        return new VisibleBoardActionHintsDto(List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
