package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;

import java.util.List;

public record VisibleZoneDto(
        CardZone zone,
        int count,
        List<VisibleCardDto> cards) {

    public VisibleZoneDto {
        cards = cards == null ? List.of() : List.copyOf(cards);
    }
}
