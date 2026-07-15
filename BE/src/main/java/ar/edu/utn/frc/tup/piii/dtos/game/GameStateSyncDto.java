package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;

import java.util.Objects;
import java.util.UUID;

public record GameStateSyncDto(
        UUID gameId,
        GameEventType eventType,
        int stateVersion,
        GameStateDto state) {

    public GameStateSyncDto {
        Objects.requireNonNull(gameId, "gameId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(state, "state is required");

        if (eventType != GameEventType.STATE_SYNC) {
            throw new IllegalArgumentException("GameStateSyncDto only supports STATE_SYNC events");
        }

        if (stateVersion != state.stateVersion()) {
            throw new IllegalArgumentException("stateVersion must match state.stateVersion()");
        }
    }

    public static GameStateSyncDto from(GameStateDto state) {
        return new GameStateSyncDto(state.gameId(), GameEventType.STATE_SYNC, state.stateVersion(), state);
    }
}
