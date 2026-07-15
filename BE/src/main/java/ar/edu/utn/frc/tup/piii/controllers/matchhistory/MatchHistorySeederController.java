package ar.edu.utn.frc.tup.piii.controllers.matchhistory;

import ar.edu.utn.frc.tup.piii.dtos.auth.GenericMessageResponseDto;
import ar.edu.utn.frc.tup.piii.services.matchhistory.MatchHistorySeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@Profile("!prod")
@RequiredArgsConstructor
public class MatchHistorySeederController {

    private final MatchHistorySeederService matchHistorySeederService;

    @GetMapping("/seed")
    public GenericMessageResponseDto seedMatchHistory() {
        return matchHistorySeederService.seedFinishedMatches();
    }
}
