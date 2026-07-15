package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.UUID;

public record VisibleStadiumDto(
        VisibleCardDto card,
        UUID playedByPlayerId) {
}
