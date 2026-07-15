package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.CardImportException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Xy1CardImportValidator {

    /**
     * XY1 / XY Unlimited is complete only when the local cache contains all 146 cards.
     */
    public static final int EXPECTED_XY1_CARDS = 146;

    public void validateFetchedCards(List<PokemonTcgCardPayload> cards) {
        if (cards.size() != EXPECTED_XY1_CARDS) {
            throw new CardImportException("XY1 import requires exactly 146 cards but received " + cards.size());
        }
        boolean invalidSet = cards.stream()
                .anyMatch(card -> !Card.XY1_SET_CODE.equalsIgnoreCase(card.setCode()));
        if (invalidSet) {
            throw new CardImportException("Only cards from set xy1 can be imported");
        }
    }

    public boolean isComplete(long importedCards) {
        return importedCards == EXPECTED_XY1_CARDS;
    }
}
