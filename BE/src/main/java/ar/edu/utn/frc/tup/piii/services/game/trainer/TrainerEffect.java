package ar.edu.utn.frc.tup.piii.services.game.trainer;

import ar.edu.utn.frc.tup.piii.entities.Card;

import java.util.Map;

/**
 * Strategy contract for one executable Trainer card effect.
 */
public interface TrainerEffect {

    boolean supports(Card card);

    TrainerEffectResult apply(TrainerEffectContext context);

    /**
     * Returns true when the played card should remain attached to a Pokemon instead of
     * being sent to the discard pile after the effect resolves (e.g. Pokémon Tools).
     */
    default boolean keepsCardInPlay() {
        return false;
    }

    /**
     * Returns a read-only description of the choices available to the player before the
     * effect is actually applied (e.g. which evolutions were found in the deck, which
     * Pokemon are within the top cards looked at, which basic energies are available).
     * Implementations MUST NOT mutate game state or emit events here - this is only used
     * to let the frontend show a selection UI before the player confirms the action via
     * the regular PLAY_TRAINER action.
     */
    default Map<String, Object> preview(TrainerEffectContext context) {
        return Map.of();
    }
}
