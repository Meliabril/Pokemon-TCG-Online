package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;

import java.util.UUID;

public interface GameResumeUseCase {

    GameStateDto resume(UUID gameId);
}
