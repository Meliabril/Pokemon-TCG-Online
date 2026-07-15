package ar.edu.utn.frc.tup.piii.services.game.query;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;

import java.util.List;
import java.util.UUID;

public interface GameHistoryQueryService {

    List<GameActionLogEntryDto> getHistory(UUID gameId);
}
