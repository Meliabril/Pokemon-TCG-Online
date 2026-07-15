package ar.edu.utn.frc.tup.piii.services.game.presence;

import java.util.UUID;

public record GameParticipantConnectedEvent(
        UUID gameId,
        UUID userId) {
}
