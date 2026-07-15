package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MatchmakingGameAccessRepository {

    Optional<Game> findLatestByParticipantUserIdAndStatusIn(
            UUID userId,
            Collection<GameStatus> statuses);
}
