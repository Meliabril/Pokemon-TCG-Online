package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Game;

import java.util.Optional;
import java.util.UUID;

public interface GameAccessRepository {

    Optional<Game> findById(UUID gameId);

    Optional<Game> findDetailById(UUID gameId);

    boolean existsById(UUID gameId);

    <S extends Game> S save(S game);
}
