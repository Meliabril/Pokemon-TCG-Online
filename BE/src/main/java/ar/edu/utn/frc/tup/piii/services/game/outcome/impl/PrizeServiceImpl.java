package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrizeServiceImpl implements PrizeService {

    private final GameCardInstanceStateService gameCardInstanceStateService;

    @Override
    public PrizeCardsResult takePrizes(UUID gameId, UUID playerUserId, int prizeCount) {
        if (prizeCount <= 0) {
            return new PrizeCardsResult(List.of(), remainingPrizeCards(gameId, playerUserId));
        }

        List<GameCardInstance> prizeCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, playerUserId, CardZone.PRIZE);
        if (prizeCards.isEmpty()) {
            throw new InvalidGameActionException("No prize cards are available to take");
        }

        int cardsToTake = prizeCount;
        if (cardsToTake > prizeCards.size()) {
            cardsToTake = prizeCards.size();
        }

        List<UUID> takenCardIds = new ArrayList<>();
        for (int index = 0; index < cardsToTake; index++) {
            GameCardInstance prizeCard = prizeCards.get(index);
            int nextHandPosition = gameCardInstanceStateService.nextZonePosition(gameId, playerUserId, CardZone.HAND);
            prizeCard.setZone(CardZone.HAND);
            prizeCard.setZonePosition(nextHandPosition);
            prizeCard.setFaceDown(false);
            gameCardInstanceStateService.save(prizeCard);
            takenCardIds.add(prizeCard.getCardId());
        }

        gameCardInstanceStateService.resequenceZone(gameId, playerUserId, CardZone.PRIZE);
        return new PrizeCardsResult(List.copyOf(takenCardIds), prizeCards.size() - cardsToTake);
    }

    @Override
    public PrizeResult takeSinglePrize(UUID gameId, UUID playerUserId) {
        PrizeCardsResult prizeCardsResult = takePrizes(gameId, playerUserId, 1);
        if (prizeCardsResult.cardIds().isEmpty()) {
            throw new InvalidGameActionException("No prize cards are available to take");
        }

        return new PrizeResult(prizeCardsResult.cardIds().get(0), prizeCardsResult.remainingPrizeCards());
    }

    private int remainingPrizeCards(UUID gameId, UUID playerUserId) {
        return gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, playerUserId, CardZone.PRIZE)
                .size();
    }
}
