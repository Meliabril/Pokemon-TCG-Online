package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LookOpponentDeckTopCardAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "LOOK_OPPONENT_DECK_TOP_CARD_OPTIONAL_SHUFFLE";
    public static final String CHOICE_TYPE = "OPPONENT_DECK_TOP_CARD_SHUFFLE_OPTIONAL";

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        UUID defenderUserId = context.resolutionContext().defenderUserId();

        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                defenderUserId,
                CardZone.DECK);
        if (deckCards.isEmpty()) {
            return AttackEffectResult.empty();
        }

        GameCardInstance topCard = deckCards.get(0);
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "revealedCardInstanceId", topCard.getId().toString(),
                        "revealedCardId", topCard.getCardId().toString(),
                        "opponentPlayerId", defenderUserId.toString()),
                attackerUserId));
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "actorPlayerId", attackerUserId.toString(),
                        "opponentPlayerId", defenderUserId.toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString())));

        return AttackEffectResult.requiringChoice(CHOICE_TYPE, events);
    }
}
