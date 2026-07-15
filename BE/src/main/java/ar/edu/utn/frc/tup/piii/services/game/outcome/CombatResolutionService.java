package ar.edu.utn.frc.tup.piii.services.game.outcome;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.UUID;

public interface CombatResolutionService {

    CombatResolutionResult resolveKnockoutIfNeeded(
            UUID gameId,
            UUID knockedOutOwnerUserId,
            UUID rewardPlayerId,
            PokemonInPlay damagedPokemon,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            String reason);

    record CombatResolutionResult(
            boolean knockedOut,
            boolean gameFinished,
            boolean promotionPending,
            UUID winnerUserId,
            List<GameEventDto> events) {

        public CombatResolutionResult {
            if (events == null) {
                events = List.of();
            } else {
                events = List.copyOf(events);
            }
        }

        public static CombatResolutionResult noKnockout() {
            return new CombatResolutionResult(false, false, false, null, List.of());
        }
    }
}
