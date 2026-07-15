package ar.edu.utn.frc.tup.piii.services.game.query.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameDetailDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.mappers.GameDetailMapper;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameQueryServiceImpl implements GameQueryService {

    private final GameDataService gameDataService;
    private final GameDetailMapper gameDetailMapper;

    @Override
    @Transactional(readOnly = true)
    public GameDetailDto getById(UUID gameId) {
        Game game = gameDataService.getRequiredGameDetail(gameId);
        return gameDetailMapper.toDto(game);
    }
}
