package ar.edu.utn.frc.tup.piii.services.matchhistory;

import ar.edu.utn.frc.tup.piii.dtos.common.PageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchHistoryFilter;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.PlayerStatsDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MatchHistoryService {

    PlayerStatsDto getPlayerStats(UUID userId);

    PageResponseDto<MatchHistoryDto> getMatchHistory(
            UUID userId,
            MatchHistoryFilter filter,
            Pageable pageable);
}
