package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DrawCardsEffectServiceImpl implements DrawCardsEffectService {

    private final GameCardInstanceStateService gameCardInstanceStateService;

    @Override
    public List<GameCardInstance> drawCards(UUID gameId, UUID playerId, int amount) {
        if (amount <= 0) {
            return List.of();
        }

        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerId,
                CardZone.DECK);
        if (deckCards.size() < amount) {
            throw new InvalidGameActionException("Deck does not have enough cards for this effect");
        }

        List<GameCardInstance> drawnCards = new ArrayList<>();
        for (int index = 0; index < amount; index++) {
            GameCardInstance drawnCard = deckCards.get(index);
            drawnCard.setZone(CardZone.HAND);
            drawnCard.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, playerId, CardZone.HAND));
            drawnCard.setFaceDown(false);
            drawnCards.add(gameCardInstanceStateService.save(drawnCard));
        }
        gameCardInstanceStateService.resequenceZone(gameId, playerId, CardZone.DECK);
        return List.copyOf(drawnCards);
    }

    @Override
    public List<GameCardInstance> drawUntilHandSize(UUID gameId, UUID playerId, int targetHandSize) {
        if (targetHandSize <= 0) {
            return List.of();
        }

        int currentHandSize = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.HAND).size();
        if (currentHandSize >= targetHandSize) {
            return List.of();
        }

        int neededCards = targetHandSize - currentHandSize;
        int availableDeckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.DECK).size();
        return drawCards(gameId, playerId, Math.min(neededCards, availableDeckCards));
    }
}
