package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;

import java.util.List;

public record GameActionExecutionResult(
        GameStateDto gameState,
        List<GameEventDto> emittedEvents) {

    public GameActionExecutionResult {
        if (emittedEvents == null) {
            emittedEvents = List.of();
        } else {
            emittedEvents = List.copyOf(emittedEvents);
        }
    }
}

