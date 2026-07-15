package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.effect.impl.DrawCardsEffectServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DrawCardsAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DRAW_CARDS";
    private static final String EVENT_SOURCE = "ATTACK";

    private final DrawCardsEffectService drawCardsEffectService;
    private final GameEventFactory gameEventFactory;

    public DrawCardsAttackEffect(GameCardInstanceStateService gameCardInstanceStateService, GameEventFactory gameEventFactory) {
        this(new DrawCardsEffectServiceImpl(gameCardInstanceStateService), gameEventFactory);
    }

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int cardsToDraw = context.operation().amount();
        if (cardsToDraw <= 0) {
            throw new InvalidGameActionException("Attack draw amount must be positive");
        }

        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        List<GameCardInstance> drawnCards = drawCardsEffectService.drawCards(gameId, attackerUserId, cardsToDraw);
        List<String> drawnCardIds = new ArrayList<>();
        for (GameCardInstance drawnCard : drawnCards) {
            drawnCardIds.add(drawnCard.getCardId().toString());
        }

        List<GameEventDto> events = new ArrayList<>();
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", attackerUserId.toString());
        publicPayload.put("cardsDrawn", cardsToDraw);
        publicPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.publicEvent(gameId, GameEventType.CARD_DRAWN, context.stateVersion(), Map.copyOf(publicPayload)));

        Map<String, Object> privatePayload = new LinkedHashMap<>();
        privatePayload.put("playerId", attackerUserId.toString());
        privatePayload.put("cardIds", List.copyOf(drawnCardIds));
        privatePayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.CARD_DRAWN,
                context.stateVersion(),
                Map.copyOf(privatePayload),
                attackerUserId));

        Map<String, Object> effectPayload = new LinkedHashMap<>();
        effectPayload.put("effectType", EFFECT_TYPE);
        effectPayload.put("cardsDrawn", cardsToDraw);
        effectPayload.put("actorPlayerId", attackerUserId.toString());
        effectPayload.put("pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString());
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.copyOf(effectPayload)));
        return new AttackEffectResult(0, false, List.copyOf(events));
    }
}
