package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;
import java.util.UUID;

public record VisibleBoardDto(
        UUID localPlayerId,
        List<VisiblePlayerBoardDto> players,
        VisibleStadiumDto stadium,
        VisibleBoardActionHintsDto actionHints) {

    public VisibleBoardDto {
        players = players == null ? List.of() : List.copyOf(players);
        if (actionHints == null) {
            actionHints = VisibleBoardActionHintsDto.empty();
        }
    }

    public static VisibleBoardDto empty() {
        return new VisibleBoardDto(null, List.of(), null, VisibleBoardActionHintsDto.empty());
    }
}
