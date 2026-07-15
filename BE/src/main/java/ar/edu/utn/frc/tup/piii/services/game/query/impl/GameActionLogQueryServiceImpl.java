package ar.edu.utn.frc.tup.piii.services.game.query.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.mappers.GameActionLogMapper;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogReadRepository;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import ar.edu.utn.frc.tup.piii.services.game.query.GameHistoryQueryService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameActionLogQueryServiceImpl implements GameHistoryQueryService {

    private final GameActionLogReadRepository gameActionLogReadRepository;
    private final GameLookupService gameLookupService;
    private final GameActionLogMapper gameActionLogMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GameActionLogEntryDto> getHistory(UUID gameId) {
        gameLookupService.assertGameExists(gameId);

        List<GameActionLogEntryDto> historyEntries = new ArrayList<>();
        for (GameActionLog actionLog : gameActionLogReadRepository.findHistory(gameId)) {
            historyEntries.add(gameActionLogMapper.toDto(actionLog));
        }
        return List.copyOf(historyEntries);
    }
}
