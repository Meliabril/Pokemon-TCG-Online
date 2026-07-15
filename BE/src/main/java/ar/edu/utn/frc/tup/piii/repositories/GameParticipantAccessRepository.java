package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;

import java.util.List;
import java.util.UUID;

public interface GameParticipantAccessRepository {

    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    List<GameParticipant> findOrderedByGameId(UUID gameId);
}
