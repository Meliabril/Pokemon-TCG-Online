package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlayStadiumTrainerEffect implements TrainerEffect {

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;

    @Override
    public boolean supports(Card card) {
        return card != null && CardCategory.STADIUM_TRAINER.equals(card.getCategory());
    }

    @Override
    public boolean keepsCardInPlay() {
        return true;
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        discardExistingStadium(gameId, context.trainerCard());

        context.trainerCardInstance().setZone(CardZone.STADIUM);
        context.trainerCardInstance().setZonePosition(1);
        context.trainerCardInstance().setFaceDown(false);
        gameCardInstanceStateService.save(context.trainerCardInstance());
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", "PLAY_STADIUM");
        effectData.put("cardExternalId", context.trainerCard().getExternalId());

        return new TrainerEffectResult(Map.copyOf(effectData), List.of());
    }

    private void discardExistingStadium(UUID gameId, Card newStadiumCard) {
        Optional<GameCardInstance> existing = gameCardInstanceStateService.findByGameId(gameId)
                .stream()
                .filter(c -> CardZone.STADIUM.equals(c.getZone()))
                .findFirst();

        if (existing.isEmpty()) {
            return;
        }

        GameCardInstance old = existing.get();
        Card oldCard = cardService.getCardEntityById(old.getCardId());
        if (oldCard != null
                && oldCard.getName() != null
                && newStadiumCard.getName() != null
                && oldCard.getName().equalsIgnoreCase(newStadiumCard.getName())) {
            throw new InvalidGameActionException("A Stadium card with the same name is already in play");
        }

        UUID oldOwnerUserId = old.getOwnerUserId();
        old.setZone(CardZone.DISCARD);
        old.setZonePosition(
                gameCardInstanceStateService.nextZonePosition(gameId, oldOwnerUserId, CardZone.DISCARD));
        old.setFaceDown(false);
        gameCardInstanceStateService.save(old);
    }
}
