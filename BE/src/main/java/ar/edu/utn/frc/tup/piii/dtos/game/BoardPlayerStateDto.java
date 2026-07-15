package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;
import java.util.UUID;

public record BoardPlayerStateDto(
        UUID playerId,
        BoardZoneSummaryDto hand,
        BoardZoneSummaryDto deck,
        BoardZoneSummaryDto prizes,
        BoardZoneSummaryDto discard,
        BoardZoneSummaryDto stadium,
        BoardPokemonDto activePokemon,
        List<BoardPokemonDto> benchPokemon) {

    public BoardPlayerStateDto {
        if (hand == null) {
            hand = BoardZoneSummaryDto.empty();
        }
        if (deck == null) {
            deck = BoardZoneSummaryDto.empty();
        }
        if (prizes == null) {
            prizes = BoardZoneSummaryDto.empty();
        }
        if (discard == null) {
            discard = BoardZoneSummaryDto.empty();
        }
        if (stadium == null) {
            stadium = BoardZoneSummaryDto.empty();
        }
        if (benchPokemon == null) {
            benchPokemon = List.of();
        } else {
            benchPokemon = List.copyOf(benchPokemon);
        }
    }

    public static BoardPlayerStateDto empty(UUID playerId) {
        return new BoardPlayerStateDto(
                playerId,
                BoardZoneSummaryDto.empty(),
                BoardZoneSummaryDto.empty(),
                BoardZoneSummaryDto.empty(),
                BoardZoneSummaryDto.empty(),
                BoardZoneSummaryDto.empty(),
                null,
                List.of());
    }
}
