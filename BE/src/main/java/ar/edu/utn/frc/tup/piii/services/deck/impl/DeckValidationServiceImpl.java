package ar.edu.utn.frc.tup.piii.services.deck.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationSummary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeckValidationServiceImpl implements DeckValidationService {

    private static final int REQUIRED_DECK_SIZE = 60;
    private static final int MAX_COPIES_BY_NAME = 4;
    private static final String OUTSIDE_PLAYABLE_SETS_ERROR = "Deck can only contain cards from playable sets";

    @Override
    public DeckValidationResult validate(Deck deck) {
        int totalCards = deck.getCards().stream()
                .mapToInt(DeckCard::getQuantity)
                .sum();
        boolean containsCardsOutsideSet = deck.getCards().stream()
                .anyMatch(deckCard -> !Card.isPlayableSetCode(deckCard.getCard().getSetCode()));
        boolean containsBasicPokemon = deck.getCards().stream()
                .anyMatch(deckCard -> deckCard.getCard().isBasicStage());

        return validate(new DeckValidationSummary(
                totalCards,
                containsCardsOutsideSet,
                containsBasicPokemon,
                duplicatedNames(deck)));
    }

    @Override
    public DeckValidationResult validate(DeckValidationSummary summary) {
        List<String> errors = new ArrayList<>();
        if (summary.totalCards() != REQUIRED_DECK_SIZE) {
            errors.add("Deck must contain exactly 60 cards");
        }
        if (summary.containsCardsOutsideSet()) {
            errors.add(OUTSIDE_PLAYABLE_SETS_ERROR);
        }
        if (!summary.containsBasicPokemon()) {
            errors.add("Deck must contain at least 1 Basic Pokemon");
        }
        summary.duplicatedNames().forEach(name -> errors.add("Deck can contain at most 4 copies of " + name));
        return new DeckValidationResult(errors.isEmpty(), errors);
    }

    private List<String> duplicatedNames(Deck deck) {
        Map<String, Integer> copiesByName = deck.getCards().stream()
                .filter(deckCard -> deckCard.getCard().getCategory() != CardCategory.BASIC_ENERGY)
                .collect(Collectors.groupingBy(
                        deckCard -> deckCard.getCard().getName(),
                        Collectors.summingInt(DeckCard::getQuantity)));
        return copiesByName.entrySet().stream()
                .filter(entry -> entry.getValue() > MAX_COPIES_BY_NAME)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
