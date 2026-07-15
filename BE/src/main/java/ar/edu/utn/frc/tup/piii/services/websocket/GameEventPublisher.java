package ar.edu.utn.frc.tup.piii.services.websocket;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateSyncDto;

import java.util.UUID;

public interface GameEventPublisher {

    void publishPublic(GameEventDto event);

    void publishPrivate(GameEventDto event, UUID recipientUserId);

    void publishPrivateStateSync(GameStateSyncDto event, UUID recipientUserId);
}

