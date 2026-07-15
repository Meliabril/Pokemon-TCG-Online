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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscardTopDeckAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DISCARD_TOP_DECK";
    private static final String DEFENDER_TARGET = "DEFENDER";

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        UUID ownerUserId = ownerUserId(context);
        Optional<GameCardInstance> topDeckCard = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(
                        context.resolutionContext().gameId(),
                        ownerUserId,
                        CardZone.DECK)
                .stream()
                .min(Comparator.comparing(GameCardInstance::getZonePosition));
        if (topDeckCard.isEmpty()) {
            return AttackEffectResult.empty();
        }

        GameCardInstance card = topDeckCard.get();
        card.setZone(CardZone.DISCARD);
        card.setZonePosition(gameCardInstanceStateService.nextZonePosition(
                context.resolutionContext().gameId(),
                ownerUserId,
                CardZone.DISCARD));
        card.setFaceDown(false);
        gameCardInstanceStateService.save(card);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "playerId", ownerUserId.toString(),
                        "discardedCardId", card.getCardId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private UUID ownerUserId(AttackEffectContext context) {
        if (DEFENDER_TARGET.equals(context.operation().target())) {
            return context.resolutionContext().defenderUserId();
        }
        return context.resolutionContext().attackerUserId();
    }
}
