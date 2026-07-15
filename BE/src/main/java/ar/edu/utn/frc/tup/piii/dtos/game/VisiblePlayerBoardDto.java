package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;
import java.util.UUID;

public record VisiblePlayerBoardDto(
        UUID playerId,
        int playerOrder,
        boolean connected,
        boolean local,
        int mulliganCount,
        boolean mulliganNoticePending,
        boolean mulliganFlowActive,
        boolean mulliganReadyForInitialSelection,
        int mulliganRoundNumber,
        boolean setupSelectionSubmitted,
        boolean setupActiveOccupied,
        int setupBenchOccupiedCount,
        VisibleZoneDto hand,
        VisibleZoneDto deck,
        VisibleZoneDto prize,
        VisibleZoneDto discard,
        VisiblePokemonDto activePokemon,
        List<VisiblePokemonDto> benchPokemon) {

    public VisiblePlayerBoardDto {
        benchPokemon = benchPokemon == null ? List.of() : List.copyOf(benchPokemon);
    }
}
