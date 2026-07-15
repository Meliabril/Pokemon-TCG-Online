package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameActionLog;

import java.util.List;
import java.util.UUID;

public interface GameActionLogReadRepository {

    List<GameActionLog> findHistory(UUID gameId);
}
