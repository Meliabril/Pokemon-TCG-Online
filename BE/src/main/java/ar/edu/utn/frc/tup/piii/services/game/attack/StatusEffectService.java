package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StatusEffectService {

    AppliedConditionsResult applyAttackConditions(
            UUID gameId,
            UUID sourcePlayerId,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int currentTurnNumber,
            int stateVersion);

    ConfusionResolution resolveConfusionBeforeAttack(
            UUID gameId,
            UUID attackingPlayerId,
            PokemonInPlay attackerPokemon,
            int currentTurnNumber,
            int stateVersion);

    BetweenTurnsResolution processBetweenTurns(
            UUID gameId,
            UUID endingPlayerId,
            int currentTurnNumber,
            int stateVersion);

    Map<UUID, List<SpecialConditionType>> snapshotActiveConditions(UUID gameId, List<UUID> playerIds);

    Map<UUID, Integer> snapshotBenchCounts(UUID gameId, List<UUID> playerIds);

    record AppliedConditionsResult(List<GameEventDto> events, Map<UUID, List<SpecialConditionType>> updatedConditionsByPlayer) {
    }

    record ConfusionResolution(boolean attackCanProceed, UUID winnerUserId, List<GameEventDto> events) {

        public static ConfusionResolution canProceed() {
            return new ConfusionResolution(true, null, List.of());
        }
    }

    record BetweenTurnsResolution(
            List<GameEventDto> events,
            Map<UUID, List<SpecialConditionType>> activeConditionsByPlayer,
            Map<UUID, Integer> benchCountByPlayer,
            UUID winnerUserId) {

        public boolean gameFinished() {
            return winnerUserId != null;
        }
    }
}
