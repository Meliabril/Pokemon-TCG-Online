package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;

import java.util.UUID;

/**
 * Application service for game action orchestration.
 */
public interface GameService {

    /**
     * Coordinates validation and execution of a game action.
     *
     * @param gameId game identifier from path
     * @param actorUserId actor user identifier
     * @param request action request payload
     * @return standardized action response
     */
    GameActionResponseDto executeAction(UUID gameId, UUID actorUserId, GameActionRequestDto request);
}
