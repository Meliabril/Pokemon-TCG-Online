package ar.edu.utn.frc.tup.piii.services.game.outcome;

import java.util.UUID;
import java.util.List;

public interface PrizeService {

    PrizeCardsResult takePrizes(UUID gameId, UUID playerUserId, int prizeCount);

    PrizeResult takeSinglePrize(UUID gameId, UUID playerUserId);

    record PrizeResult(UUID cardId, int remainingPrizeCards) {
    }

    record PrizeCardsResult(List<UUID> cardIds, int remainingPrizeCards) {

        public PrizeCardsResult {
            if (cardIds == null) {
                cardIds = List.of();
            } else {
                cardIds = List.copyOf(cardIds);
            }
        }
    }
}
