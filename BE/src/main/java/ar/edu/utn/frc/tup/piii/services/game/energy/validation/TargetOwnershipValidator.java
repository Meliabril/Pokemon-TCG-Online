package ar.edu.utn.frc.tup.piii.services.game.energy.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Order(16)
public class TargetOwnershipValidator implements ActionValidator {

    private static final String TARGET_POKEMON_ID_KEY = "targetPokemonId";
    private static final String POKEMON_IN_PLAY_ID_KEY = "pokemonInPlayId";

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!GameActionType.ATTACH_ENERGY.equals(context.request().actionType())) {
            return;
        }

        UUID targetPokemonId = extractTargetId(context.request().payload());
        UUID actorUserId = context.actorUserId();
        Map<UUID, UUID> cardOwners = context.currentState().board().ownerByCardReferenceId();

        if (!isOwnedByActor(cardOwners, targetPokemonId, actorUserId)) {
            throw new InvalidGameActionException("Cannot target an opponent's card for this action");
        }
    }

    private boolean isOwnedByActor(Map<UUID, UUID> cardOwners, UUID targetPokemonId, UUID actorUserId) {
        if (cardOwners == null || targetPokemonId == null || actorUserId == null) {
            return false;
        }

        UUID ownerUserId = cardOwners.get(targetPokemonId);
        return actorUserId.equals(ownerUserId);
    }

    private UUID extractTargetId(Map<String, Object> payload) {
        UUID targetPokemonId = extractUuid(payload, TARGET_POKEMON_ID_KEY);
        if (targetPokemonId != null) {
            return targetPokemonId;
        }

        return extractUuid(payload, POKEMON_IN_PLAY_ID_KEY);
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
