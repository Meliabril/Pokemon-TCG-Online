package ar.edu.utn.frc.tup.piii.services.game.query;

import ar.edu.utn.frc.tup.piii.dtos.game.GameDetailDto;

import java.util.UUID;

public interface GameQueryService {

    GameDetailDto getById(UUID gameId);
}
