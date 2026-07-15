package ar.edu.utn.frc.tup.piii.services.game.evolution.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Order(15)
public class CardZoneValidator implements ActionValidator {

    private static final String TARGET_CARD_ID_KEY = "targetCardId";
    private static final String POKEMON_IN_PLAY_ID_KEY = "pokemonInPlayId";

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!GameActionType.EVOLVE_POKEMON.equals(context.request().actionType())) {
            return;
        }

        UUID targetCardId = extractTargetId(context.request().payload());
        Map<UUID, CardZone> cardZones = context.currentState().board().zoneByCardReferenceId();

        if (!isTargetInPlayableZone(cardZones, targetCardId)) {
            throw new InvalidGameActionException("Target card must be in ACTIVE or BENCH zone");
        }
    }

    private boolean isTargetInPlayableZone(Map<UUID, CardZone> cardZones, UUID targetCardId) {
        if (cardZones == null || targetCardId == null) {
            return false;
        }

        CardZone zone = cardZones.get(targetCardId);
        return CardZone.ACTIVE.equals(zone) || CardZone.BENCH.equals(zone);
    }

    private UUID extractTargetId(Map<String, Object> payload) {
        UUID targetCardId = extractUuid(payload, TARGET_CARD_ID_KEY);
        if (targetCardId != null) {
            return targetCardId;
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
