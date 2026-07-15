package ar.edu.utn.frc.tup.piii.services.game.trainer;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;

import java.util.UUID;

/**
 * Immutable execution context passed to a Trainer effect strategy.
 */
public record TrainerEffectContext(
        UUID gameId,
        UUID actorUserId,
        GameActionRequestDto request,
        GameStateDto currentState,
        GameCardInstance trainerCardInstance,
        Card trainerCard,
        int stateVersion) {
}
