package ar.edu.utn.frc.tup.piii.services.game.attack.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Order(14)
public class AttackEnergyCostValidator implements ActionValidator {

    private static final String ATTACK_ID_KEY = "attackId";

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!GameActionType.DECLARE_ATTACK.equals(context.request().actionType())) {
            return;
        }

        UUID attackId = extractUuid(context.request().payload(), ATTACK_ID_KEY);
        UUID actorUserId = context.actorUserId();
        PlayerStateDto playerState = context.currentState().players().get(actorUserId);

        if (!isAttackAffordable(playerState, attackId)) {
            throw new InvalidGameActionException("Not enough energy attached to perform this attack");
        }
    }

    private boolean isAttackAffordable(PlayerStateDto playerState, UUID attackId) {
        if (playerState == null || attackId == null) {
            return false;
        }

        Set<UUID> affordableAttacks = playerState.affordableAttackIds();
        if (affordableAttacks == null) {
            return false;
        }

        return affordableAttacks.contains(attackId);
    }

    private UUID extractUuid(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }

        Object value = payload.get(key);
        if (value instanceof UUID) {
            return (UUID) value;
        }

        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        return null;
    }
}
