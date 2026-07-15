package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameEvent;

import java.util.List;
import java.util.UUID;

public interface GameEventAccessRepository {

    <S extends GameEvent> S save(S event);

    List<GameEvent> findVisibleEvents(UUID gameId, UUID viewerUserId);
}
