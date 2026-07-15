package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;

import java.util.List;
import java.util.UUID;

public interface GameCardInstanceAccessRepository {

    List<GameCardInstance> findByGameId(UUID gameId);
}
