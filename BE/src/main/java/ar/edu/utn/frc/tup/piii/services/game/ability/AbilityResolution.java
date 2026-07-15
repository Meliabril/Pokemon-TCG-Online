package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.UUID;

public record AbilityResolution(
        boolean gameFinished,
        boolean promotionPending,
        UUID winnerUserId,
        List<GameEventDto> events) {

    public AbilityResolution {
        events = events == null ? List.of() : List.copyOf(events);
    }

    public static AbilityResolution empty() {
        return new AbilityResolution(false, false, null, List.of());
    }
}
