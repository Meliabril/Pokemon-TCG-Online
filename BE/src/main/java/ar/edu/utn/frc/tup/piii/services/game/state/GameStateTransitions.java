package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class GameStateTransitions {

    private GameStateTransitions() {
    }

    public static GameStateDto fromCurrentGame(GameStateDto sourceState, Game game, int stateVersion) {
        return sourceState.toBuilder()
                .status(game.getStatus())
                .stateVersion(stateVersion)
                .turn(sourceState.turn().toBuilder()
                        .currentPhase(game.getCurrentPhase())
                        .turnNumber(game.getTurnNumber())
                        .activePlayerId(game.getActivePlayerId())
                        .playerWhoWentFirstId(game.getPlayerWhoWentFirstId())
                        .energyAttachedThisTurn(false)
                        .supporterPlayedThisTurn(false)
                        .retreatedThisTurn(false)
                        .build())
                .resolution(resolutionState(game.getResolutionState()))
                .updatedAt(Instant.now())
                .build();
    }

    private static ResolutionStateDto resolutionState(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return ResolutionStateDto.builder().build();
        }

        return ResolutionStateDto.builder()
                .resolutionType(stringValue(source.get(ResolutionStateDto.RESOLUTION_TYPE_KEY)))
                .playerToPromoteId(uuidValue(source.get(ResolutionStateDto.PLAYER_TO_PROMOTE_ID_KEY)))
                .nextActivePlayerId(uuidValue(source.get(ResolutionStateDto.NEXT_ACTIVE_PLAYER_ID_KEY)))
                .nextTurnNumber(integerValue(source.get(ResolutionStateDto.NEXT_TURN_NUMBER_KEY)))
                .build();
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static UUID uuidValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private static int integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
