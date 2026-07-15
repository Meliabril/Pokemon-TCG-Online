package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameActionLogRepository extends JpaRepository<GameActionLog, UUID>, GameActionLogWriteRepository, GameActionLogReadRepository {

    @Override
    <S extends GameActionLog> S save(S log);

    List<GameActionLog> findByGame_IdOrderByVersionAsc(UUID gameId);

    @Override
    default List<GameActionLog> findHistory(UUID gameId) {
        return findByGame_IdOrderByVersionAsc(gameId);
    }
}
