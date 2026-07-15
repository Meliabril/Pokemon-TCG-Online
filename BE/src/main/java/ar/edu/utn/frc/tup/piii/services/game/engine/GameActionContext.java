package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;

import java.util.UUID;

/**
 * Immutable context shared across the game action orchestration pipeline.
 *
 * @param gameId game identifier
 * @param actorUserId actor user identifier
 * @param request validated client action request
 * @param currentState optional current game state snapshot
 */
public record GameActionContext(
        UUID gameId,
        UUID actorUserId,
        GameActionRequestDto request,
        GameStateDto currentState) {
}

