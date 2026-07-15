package ar.edu.utn.frc.tup.piii.services.game.trainer;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

import java.util.Map;
import java.util.UUID;

/**
 * Executes a validated Trainer card action.
 */
public interface TrainerEffectService {

    GameActionExecutionResult executeTrainer(GameActionContext context);

    /**
     * Builds a read-only preview of the choices available for a Trainer card still in the
     * player's hand (e.g. valid Evosoda evolutions found in the deck, Great Ball top-7
     * Pokemon, Professor's Letter available basic energies). Does not mutate game state or
     * emit any event - it only resolves which Trainer card/effect applies and delegates to
     * that effect's {@link TrainerEffect#preview(TrainerEffectContext)}.
     *
     * @param gameId game identifier
     * @param actorUserId player requesting the preview
     * @param payload same shape as a PLAY_TRAINER action payload (cardInstanceId or cardId,
     *                plus any effect-specific filter such as targetPokemonInPlayId)
     */
    Map<String, Object> previewTrainer(UUID gameId, UUID actorUserId, Map<String, Object> payload);
}
