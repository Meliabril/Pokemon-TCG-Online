package ar.edu.utn.frc.tup.piii.services.game.board.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Order(13)
public class CardOwnershipValidator implements ActionValidator {

    private static final String CARD_ID_KEY = "cardId";
    private static final String CARD_INSTANCE_ID_KEY = "cardInstanceId";

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!requiresHandOwnership(context.request().actionType())) {
            return;
        }

        UUID cardId = extractUuid(context.request().payload(), CARD_ID_KEY);
        UUID cardInstanceId = extractUuid(context.request().payload(), CARD_INSTANCE_ID_KEY);
        UUID actorUserId = context.actorUserId();
        PlayerStateDto playerState = context.currentState().players().get(actorUserId);

        if (!isCardInPlayerHand(playerState, cardId, cardInstanceId)) {
            throw new InvalidGameActionException("Card is not in player's hand");
        }
    }

    private boolean requiresHandOwnership(GameActionType actionType) {
        if (actionType == null) {
            return false;
        }

        return GameActionType.PLAY_BASIC_POKEMON.equals(actionType)
                || GameActionType.PLAY_TRAINER.equals(actionType)
                || GameActionType.ATTACH_ENERGY.equals(actionType)
                || GameActionType.EVOLVE_POKEMON.equals(actionType);
    }

    private boolean isCardInPlayerHand(PlayerStateDto playerState, UUID cardId, UUID cardInstanceId) {
        if (playerState == null) {
            return false;
        }

        List<UUID> cardInstanceIdsInHand = playerState.cardInstanceIdsInHand();
        if (cardInstanceId != null && cardInstanceIdsInHand != null && cardInstanceIdsInHand.contains(cardInstanceId)) {
            return true;
        }

        List<UUID> cardsInHand = playerState.cardIdsInHand();
        return cardId != null && cardsInHand != null && cardsInHand.contains(cardId);
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
