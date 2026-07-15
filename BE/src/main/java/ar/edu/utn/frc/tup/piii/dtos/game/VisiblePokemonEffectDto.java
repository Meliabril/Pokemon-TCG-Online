package ar.edu.utn.frc.tup.piii.dtos.game;

/**
 * Minimal, public, purely-cosmetic description of a temporary effect currently affecting a
 * {@link VisiblePokemonDto}. This is intentionally decoupled from any rules engine state: it
 * exists only so the frontend can render a visual indicator (e.g. an aura/shield) without
 * having to know or recompute any game rule. Backend is the single source of truth for whether
 * the effect is active; frontend must only render based on presence/absence of this entry.
 */
public record VisiblePokemonEffectDto(String type, String source, String expiresAt) {

    public static final String TYPE_DAMAGE_PREVENTION_SHIELD = "DAMAGE_PREVENTION_SHIELD";
    public static final String SOURCE_HARDEN = "HARDEN";
    public static final String EXPIRES_AT_OPPONENT_TURN_END = "OPPONENT_TURN_END";

    public static VisiblePokemonEffectDto hardenShield() {
        return new VisiblePokemonEffectDto(
                TYPE_DAMAGE_PREVENTION_SHIELD,
                SOURCE_HARDEN,
                EXPIRES_AT_OPPONENT_TURN_END);
    }
}
