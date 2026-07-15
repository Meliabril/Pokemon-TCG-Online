package ar.edu.utn.frc.tup.piii.services.matchhistory;

import ar.edu.utn.frc.tup.piii.dtos.auth.GenericMessageResponseDto;

public interface MatchHistorySeederService {

    GenericMessageResponseDto seedFinishedMatches();
}
