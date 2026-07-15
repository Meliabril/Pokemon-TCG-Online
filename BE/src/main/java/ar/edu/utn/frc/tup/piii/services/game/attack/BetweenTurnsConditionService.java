package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.UUID;

public interface BetweenTurnsConditionService {

    BetweenTurnsConditionResult resolveConditions(
            UUID gameId,
            PokemonInPlay activePokemon,
            UUID activePokemonOwnerId,
            UUID endingPlayerId,
            SpecialConditionType conditionType,
            int currentTurnNumber,
            int stateVersion);

    record BetweenTurnsConditionResult(
            List<GameEventDto> events,
            boolean damageApplied,
            String knockoutReason) {

        public BetweenTurnsConditionResult {
            if (events == null) {
                events = List.of();
            } else {
                events = List.copyOf(events);
            }
        }
    }
}
