package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BetweenTurnsResolutionService {

    BetweenTurnsResolutionResult resolveBetweenTurns(
            UUID gameId,
            UUID endingPlayerId,
            UUID nextActivePlayerId,
            int currentTurnNumber,
            int nextTurnNumber,
            int stateVersion);

    Map<UUID, List<SpecialConditionType>> snapshotActiveConditions(UUID gameId, List<UUID> playerIds);

    Map<UUID, Integer> snapshotBenchCounts(UUID gameId, List<UUID> playerIds);

    record BetweenTurnsResolutionResult(
            List<GameEventDto> events,
            Map<UUID, List<SpecialConditionType>> activeConditionsByPlayer,
            Map<UUID, Integer> benchCountByPlayer,
            boolean gameFinished,
            boolean promotionPending,
            UUID winnerUserId) {

        public BetweenTurnsResolutionResult {
            if (events == null) {
                events = List.of();
            } else {
                events = List.copyOf(events);
            }
            if (activeConditionsByPlayer == null) {
                activeConditionsByPlayer = Map.of();
            } else {
                activeConditionsByPlayer = Map.copyOf(activeConditionsByPlayer);
            }
            if (benchCountByPlayer == null) {
                benchCountByPlayer = Map.of();
            } else {
                benchCountByPlayer = Map.copyOf(benchCountByPlayer);
            }
        }
    }
}
