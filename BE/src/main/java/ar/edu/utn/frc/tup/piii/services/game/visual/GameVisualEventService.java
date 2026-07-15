package ar.edu.utn.frc.tup.piii.services.game.visual;

import ar.edu.utn.frc.tup.piii.dtos.websocket.CardHoverChangedVisualEventDto;

import java.util.UUID;

public interface GameVisualEventService {

    void publishVisualEvent(UUID gameId, UUID playerId, CardHoverChangedVisualEventDto event);
}
