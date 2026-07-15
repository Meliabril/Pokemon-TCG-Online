package ar.edu.utn.frc.tup.piii.controllers.matchhistory;

import ar.edu.utn.frc.tup.piii.dtos.common.PageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchHistoryFilter;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.PlayerStatsDto;
import ar.edu.utn.frc.tup.piii.services.matchhistory.MatchHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Match history", description = "Endpoints for authenticated player match history")
public class MatchHistoryController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final MatchHistoryService matchHistoryService;

    @GetMapping("/stats")
    public PlayerStatsDto getPlayerStats(Authentication authentication) {
        return matchHistoryService.getPlayerStats(userId(authentication));
    }

    @GetMapping("/history")
    public PageResponseDto<MatchHistoryDto> getMatchHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") MatchHistoryFilter filter,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return matchHistoryService.getMatchHistory(userId(authentication), filter, pageable);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
