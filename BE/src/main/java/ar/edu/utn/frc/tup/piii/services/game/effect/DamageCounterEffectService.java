package ar.edu.utn.frc.tup.piii.services.game.effect;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.UUID;

public interface DamageCounterEffectService {

    DamageCounterResult placeDamageCounters(
            UUID gameId,
            PokemonInPlay targetPokemon,
            int counters,
            UUID rewardPlayerId,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            String reason);

    default DamageCounterResult placeDamageCounters(
            UUID gameId,
            PokemonInPlay targetPokemon,
            int counters,
            UUID rewardPlayerId,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            String reason,
            boolean resolveKnockout) {
        return placeDamageCounters(
                gameId,
                targetPokemon,
                counters,
                rewardPlayerId,
                nextActivePlayerId,
                nextTurnNumber,
                stateVersion,
                reason);
    }

    record DamageCounterResult(
            int damage,
            int damageCounters,
            boolean gameFinished,
            boolean promotionPending,
            UUID winnerUserId,
            List<GameEventDto> events) {

        public DamageCounterResult {
            events = events == null ? List.of() : List.copyOf(events);
        }
    }
}
