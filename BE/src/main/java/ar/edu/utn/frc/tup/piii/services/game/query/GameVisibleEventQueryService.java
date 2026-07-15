package ar.edu.utn.frc.tup.piii.services.game.query;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.UUID;

public interface GameVisibleEventQueryService {

    List<GameEventDto> getVisibleEvents(UUID gameId, UUID viewerUserId);
}
