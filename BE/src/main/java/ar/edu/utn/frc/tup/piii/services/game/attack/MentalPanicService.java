package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.UUID;

public interface MentalPanicService {

    MentalPanicResult resolveBeforeAttack(
            PokemonInPlay pokemonInPlay,
            int currentTurnNumber,
            UUID gameId,
            int stateVersion);

    void expireLock(PokemonInPlay pokemonInPlay, int currentTurnNumber);

    record MentalPanicResult(boolean attackCanProceed, List<GameEventDto> events) {

        public MentalPanicResult {
            if (events == null) {
                events = List.of();
            } else {
                events = List.copyOf(events);
            }
        }
    }
}
