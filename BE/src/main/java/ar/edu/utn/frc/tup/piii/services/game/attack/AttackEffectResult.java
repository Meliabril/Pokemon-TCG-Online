package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AttackEffectResult(
        int damageModifier,
        boolean attackCancelled,
        List<GameEventDto> events,
        boolean choiceRequired,
        String choiceType,
        Map<String, Object> choicePayload,
        boolean ignoreWeakness,
        boolean ignoreResistance,
        boolean ignoreDefenderEffects,
        boolean gameFinished,
        boolean promotionPending,
        UUID winnerUserId,
        UUID choicePlayerId) {

    public AttackEffectResult {
        if (events == null) {
            events = List.of();
        } else {
            events = List.copyOf(events);
        }
        if (choicePayload == null) {
            choicePayload = Map.of();
        } else {
            choicePayload = Map.copyOf(choicePayload);
        }
    }

    public AttackEffectResult(
            int damageModifier,
            boolean attackCancelled,
            List<GameEventDto> events,
            boolean choiceRequired,
            String choiceType,
            Map<String, Object> choicePayload,
            boolean ignoreResistance) {
        this(damageModifier, attackCancelled, events, choiceRequired, choiceType, choicePayload, false, ignoreResistance,
                false, false, false, null, null);
    }

    public AttackEffectResult(int damageModifier, boolean attackCancelled, List<GameEventDto> events) {
        this(damageModifier, attackCancelled, events, false, null, Map.of(), false);
    }

    public AttackEffectResult(
            int damageModifier,
            boolean attackCancelled,
            List<GameEventDto> events,
            boolean ignoreResistance) {
        this(damageModifier, attackCancelled, events, false, null, Map.of(), ignoreResistance);
    }

    public AttackEffectResult(
            int damageModifier,
            boolean attackCancelled,
            List<GameEventDto> events,
            boolean choiceRequired,
            String choiceType) {
        this(damageModifier, attackCancelled, events, choiceRequired, choiceType, Map.of(), false);
    }

    public static AttackEffectResult empty() {
        return new AttackEffectResult(0, false, List.of());
    }

    public static AttackEffectResult requiringChoice(String choiceType, List<GameEventDto> events) {
        return new AttackEffectResult(0, false, events, true, choiceType, Map.of(), false);
    }

    public static AttackEffectResult requiringChoice(
            String choiceType,
            Map<String, Object> choicePayload,
            List<GameEventDto> events) {
        return new AttackEffectResult(0, false, events, true, choiceType, choicePayload, false);
    }

    /**
     * Used when the player who must resolve the pending choice is not the attacker (e.g. Mental
     * Trash, where the defender - not the attacker - picks which cards of their own hand to
     * discard). choicePlayerId is resolved against the attacker by the caller when left null.
     */
    public static AttackEffectResult requiringChoice(
            String choiceType,
            Map<String, Object> choicePayload,
            List<GameEventDto> events,
            UUID choicePlayerId) {
        return new AttackEffectResult(0, false, events, true, choiceType, choicePayload, false, false,
                false, false, false, null, choicePlayerId);
    }

    /**
     * Used when an effect itself resolves a knockout (e.g. a Pokemon other than the attack's
     * primary target is knocked out by a damage-counter effect) and the game has finished or is
     * now waiting on a promotion as a result. The caller must stop processing further effects and
     * surface the current game state immediately, the same way it already does for the primary
     * target's post-damage knockout check.
     */
    public static AttackEffectResult knockoutOutcome(
            List<GameEventDto> events,
            boolean gameFinished,
            boolean promotionPending,
            UUID winnerUserId) {
        return new AttackEffectResult(0, false, events, false, null, Map.of(), false, false,
                false, gameFinished, promotionPending, winnerUserId, null);
    }

    public static AttackEffectResult ignoringResistance(List<GameEventDto> events) {
        return new AttackEffectResult(0, false, events, false, null, Map.of(), true);
    }

    public static AttackEffectResult ignoringWeaknessAndResistance(List<GameEventDto> events) {
        return new AttackEffectResult(0, false, events, false, null, Map.of(), true, true,
                false, false, false, null, null);
    }

    public static AttackEffectResult ignoringDefenderEffects(List<GameEventDto> events) {
        return new AttackEffectResult(0, false, events, false, null, Map.of(), true, true,
                true, false, false, null, null);
    }
}
