package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateRestorer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GameStateRestorerImpl implements GameStateRestorer {

    @Override
    public void restoreFromSnapshot(Game game, GameStateDto snapshot) {
        game.setStatus(snapshot.status());
        game.setCurrentPhase(snapshot.turn().currentPhase());
        game.setTurnNumber(snapshot.turn().turnNumber());
        game.setActivePlayerId(snapshot.turn().activePlayerId());
        game.setPlayerWhoWentFirstId(snapshot.turn().playerWhoWentFirstId());
        game.setTurnStartedAt(snapshot.turn().turnStartedAt());
        game.setResolutionState(resolutionStateMap(snapshot.resolution(), game.getResolutionState()));
    }

    private Map<String, Object> resolutionStateMap(ResolutionStateDto resolutionState, Map<String, Object> previousState) {
        Map<String, Object> preservedValues = preservedAbilityUsage(previousState);
        if (resolutionState == null || resolutionState.resolutionType() == null) {
            return preservedValues;
        }

        Map<String, Object> values = new LinkedHashMap<>(preservedValues);
        values.put(ResolutionStateDto.RESOLUTION_TYPE_KEY, resolutionState.resolutionType());
        if (resolutionState.playerToPromoteId() != null) {
            values.put(ResolutionStateDto.PLAYER_TO_PROMOTE_ID_KEY, resolutionState.playerToPromoteId().toString());
        }
        if (resolutionState.nextActivePlayerId() != null) {
            values.put(ResolutionStateDto.NEXT_ACTIVE_PLAYER_ID_KEY, resolutionState.nextActivePlayerId().toString());
        }
        values.put(ResolutionStateDto.NEXT_TURN_NUMBER_KEY, resolutionState.nextTurnNumber());
        if (resolutionState.pendingChoicePlayerId() != null) {
            values.put(ResolutionStateDto.PENDING_CHOICE_PLAYER_ID_KEY, resolutionState.pendingChoicePlayerId().toString());
        }
        if (resolutionState.pendingChoiceType() != null) {
            values.put(ResolutionStateDto.PENDING_CHOICE_TYPE_KEY, resolutionState.pendingChoiceType());
        }
        if (resolutionState.pendingChoicePayload() != null && !resolutionState.pendingChoicePayload().isEmpty()) {
            values.put(ResolutionStateDto.PENDING_CHOICE_PAYLOAD_KEY, resolutionState.pendingChoicePayload());
        }
        if (resolutionState.turnEndingPlayerId() != null) {
            values.put(ResolutionStateDto.TURN_ENDING_PLAYER_ID_KEY, resolutionState.turnEndingPlayerId().toString());
        }
        return Map.copyOf(values);
    }

    private Map<String, Object> preservedAbilityUsage(Map<String, Object> previousState) {
        if (previousState == null || !previousState.containsKey(AbilityUsageTracker.ABILITY_USAGE_KEY)) {
            return Map.of();
        }
        return Map.of(AbilityUsageTracker.ABILITY_USAGE_KEY, previousState.get(AbilityUsageTracker.ABILITY_USAGE_KEY));
    }
}
