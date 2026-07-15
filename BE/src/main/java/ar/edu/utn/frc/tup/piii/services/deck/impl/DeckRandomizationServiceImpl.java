package ar.edu.utn.frc.tup.piii.services.deck.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckRandomizationService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import ar.edu.utn.frc.tup.piii.services.deck.RandomDeckComposition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckRandomizationServiceImpl implements DeckRandomizationService {

    private static final int TARGET_POKEMON_COUNT = 18;
    private static final int TARGET_TRAINER_COUNT = 24;
    private static final int TARGET_DECK_SIZE = 60;
    private static final int MAX_GENERATION_ATTEMPTS = 12;

    private final CardService cardService;
    private final DeckValidationService deckValidationService;

    @Override
    public RandomDeckComposition generateRandomDeck() {
        List<Card> xy1Cards = cardService.getCardEntities(Card.XY1_SET_CODE);
        if (xy1Cards.isEmpty()) {
            throw new InvalidDeckException("Cannot randomize deck because XY1 cards are not available");
        }

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            CandidateDeck candidate = buildCandidate(xy1Cards);
            if (isValid(candidate.cards())) {
                return new RandomDeckComposition(candidate.name(), candidate.cards());
            }
        }

        throw new InvalidDeckException("Could not generate a valid random deck");
    }

    private CandidateDeck buildCandidate(List<Card> xy1Cards) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Card> basics = filterByCategory(xy1Cards, CardCategory.BASIC_POKEMON).stream()
                .filter(card -> card.getPokemonType() != null)
                .toList();
        Map<String, List<Card>> basicsByType = basics.stream()
                .collect(Collectors.groupingBy(Card::getPokemonType));

        if (basicsByType.isEmpty()) {
            throw new InvalidDeckException("Cannot randomize deck because XY1 basic Pokemon are not available");
        }

        String primaryType = choosePrimaryType(basicsByType, random);
        List<String> chosenTypes = new ArrayList<>();
        chosenTypes.add(primaryType);
        chooseSecondaryType(basicsByType, primaryType, random).ifPresent(chosenTypes::add);

        List<Card> typedBasics = basics.stream()
                .filter(card -> chosenTypes.contains(card.getPokemonType()))
                .sorted(Comparator.comparing(Card::getName))
                .toList();
        List<Card> typedStage1 = filterByCategory(xy1Cards, CardCategory.STAGE_1_POKEMON).stream()
                .filter(card -> chosenTypes.contains(card.getPokemonType()))
                .toList();
        List<Card> typedStage2 = filterByCategory(xy1Cards, CardCategory.STAGE_2_POKEMON).stream()
                .filter(card -> chosenTypes.contains(card.getPokemonType()))
                .toList();
        List<Card> trainers = xy1Cards.stream()
                .filter(card -> switch (card.getCategory()) {
                    case ITEM_TRAINER, SUPPORTER_TRAINER, STADIUM_TRAINER, POKEMON_TOOL_TRAINER -> true;
                    default -> false;
                })
                .toList();
        List<Card> basicEnergies = filterByCategory(xy1Cards, CardCategory.BASIC_ENERGY);

        Map<Card, Integer> composition = new LinkedHashMap<>();
        List<Card> selectedBasics = addBasicPokemon(composition, typedBasics, random);
        List<Card> selectedStage1 = addStage1Pokemon(composition, typedStage1, selectedBasics, random);
        addStage2Pokemon(composition, typedStage2, selectedStage1, random);
        fillPokemonCount(composition, typedBasics, random);
        addTrainers(composition, trainers, random);
        addEnergies(composition, basicEnergies, chosenTypes);

        String generatedName = chosenTypes.stream()
                .map(this::capitalize)
                .collect(Collectors.joining("-"));
        return new CandidateDeck("Random " + generatedName + " Deck", toEntries(composition));
    }

    private String choosePrimaryType(Map<String, List<Card>> basicsByType, ThreadLocalRandom random) {
        List<String> strongTypes = basicsByType.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 3)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        List<String> source = strongTypes.isEmpty()
                ? new ArrayList<>(basicsByType.keySet())
                : strongTypes;
        return source.get(random.nextInt(source.size()));
    }

    private java.util.Optional<String> chooseSecondaryType(
            Map<String, List<Card>> basicsByType,
            String primaryType,
            ThreadLocalRandom random) {
        List<String> candidates = basicsByType.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(primaryType))
                .filter(entry -> entry.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (candidates.isEmpty() || random.nextBoolean()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    private List<Card> addBasicPokemon(Map<Card, Integer> composition, List<Card> basics, ThreadLocalRandom random) {
        if (basics.isEmpty()) {
            throw new InvalidDeckException("Cannot randomize deck because XY1 basic Pokemon are not available");
        }
        List<Card> shuffled = new ArrayList<>(basics);
        Collections.shuffle(shuffled, random);
        int targetDistinctBasics = Math.min(shuffled.size(), 4 + random.nextInt(2));
        List<Card> selected = new ArrayList<>();
        int added = 0;
        for (Card card : shuffled) {
            if (selected.size() >= targetDistinctBasics) {
                break;
            }
            int quantity = 2 + random.nextInt(2);
            addCopies(composition, card, quantity);
            selected.add(card);
            added += quantity;
        }

        int selectedIndex = 0;
        while (added < 10 && !selected.isEmpty()) {
            Card card = selected.get(selectedIndex % selected.size());
            if (currentCopies(composition, card) < 4) {
                addCopies(composition, card, 1);
                added++;
            }
            selectedIndex++;
        }
        return selected;
    }

    private List<Card> addStage1Pokemon(
            Map<Card, Integer> composition,
            List<Card> stage1Pool,
            List<Card> selectedBasics,
            ThreadLocalRandom random) {
        List<String> basicNames = selectedBasics.stream()
                .map(Card::getName)
                .collect(Collectors.toSet())
                .stream()
                .toList();
        List<Card> candidates = stage1Pool.stream()
                .filter(card -> basicNames.contains(card.getEvolvesFrom()))
                .sorted(Comparator.comparing(Card::getName))
                .toList();
        List<Card> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        List<Card> selected = new ArrayList<>();
        for (Card card : shuffled.stream().limit(3).toList()) {
            int quantity = 1 + random.nextInt(2);
            addCopies(composition, card, quantity);
            selected.add(card);
        }
        return selected;
    }

    private void addStage2Pokemon(
            Map<Card, Integer> composition,
            List<Card> stage2Pool,
            List<Card> selectedStage1,
            ThreadLocalRandom random) {
        List<String> stage1Names = selectedStage1.stream()
                .map(Card::getName)
                .toList();
        List<Card> candidates = stage2Pool.stream()
                .filter(card -> stage1Names.contains(card.getEvolvesFrom()))
                .sorted(Comparator.comparing(Card::getName))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        Card selected = candidates.get(random.nextInt(candidates.size()));
        addCopies(composition, selected, 1);
    }

    private void fillPokemonCount(Map<Card, Integer> composition, List<Card> basics, ThreadLocalRandom random) {
        List<Card> shuffled = new ArrayList<>(basics);
        Collections.shuffle(shuffled, random);
        int index = 0;
        int attempts = 0;
        while (pokemonCount(composition) < TARGET_POKEMON_COUNT && !shuffled.isEmpty() && attempts < shuffled.size() * 8) {
            Card card = shuffled.get(index % shuffled.size());
            if (currentCopies(composition, card) < 4) {
                addCopies(composition, card, 1);
            }
            index++;
            attempts++;
        }
    }

    private void addTrainers(Map<Card, Integer> composition, List<Card> trainers, ThreadLocalRandom random) {
        addTrainerCategory(composition, trainers, CardCategory.SUPPORTER_TRAINER, 6, random);
        addTrainerCategory(composition, trainers, CardCategory.ITEM_TRAINER, 12, random);
        addTrainerCategory(composition, trainers, CardCategory.POKEMON_TOOL_TRAINER, 3, random);
        addTrainerCategory(composition, trainers, CardCategory.STADIUM_TRAINER, 3, random);

        List<Card> fallback = new ArrayList<>(trainers);
        Collections.shuffle(fallback, random);
        int index = 0;
        int attempts = 0;
        while (trainerCount(composition) < TARGET_TRAINER_COUNT && !fallback.isEmpty() && attempts < fallback.size() * 8) {
            Card card = fallback.get(index % fallback.size());
            if (currentCopies(composition, card) < 4) {
                addCopies(composition, card, 1);
            }
            index++;
            attempts++;
        }
    }

    private void addTrainerCategory(
            Map<Card, Integer> composition,
            List<Card> trainers,
            CardCategory category,
            int targetCount,
            ThreadLocalRandom random) {
        List<Card> candidates = trainers.stream()
                .filter(card -> card.getCategory() == category)
                .sorted(Comparator.comparing(Card::getName))
                .toList();
        List<Card> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        int added = 0;
        for (Card card : shuffled) {
            if (added >= targetCount) {
                break;
            }
            int quantity = Math.min(2, targetCount - added);
            addCopies(composition, card, quantity);
            added += quantity;
        }
    }

    private void addEnergies(Map<Card, Integer> composition, List<Card> basicEnergies, List<String> chosenTypes) {
        int remaining = TARGET_DECK_SIZE - deckSize(composition);
        if (remaining <= 0) {
            return;
        }
        List<Card> selectedEnergies = chosenTypes.stream()
                .map(type -> findEnergyCard(basicEnergies, type))
                .filter(Objects::nonNull)
                .toList();
        if (selectedEnergies.isEmpty()) {
            throw new InvalidDeckException("Cannot randomize deck because XY1 energies are not available");
        }
        if (selectedEnergies.size() == 1) {
            addCopies(composition, selectedEnergies.getFirst(), remaining);
            return;
        }
        int firstShare = remaining / 2;
        addCopies(composition, selectedEnergies.getFirst(), firstShare);
        addCopies(composition, selectedEnergies.get(1), remaining - firstShare);
    }

    private Card findEnergyCard(List<Card> basicEnergies, String type) {
        String normalizedType = type.toLowerCase(Locale.ROOT);
        return basicEnergies.stream()
                .filter(card -> card.getName().toLowerCase(Locale.ROOT).contains(normalizedType))
                .findFirst()
                .orElse(null);
    }

    private boolean isValid(List<RandomDeckComposition.CardEntry> cards) {
        Deck deck = new Deck();
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        List<DeckCard> deckCards = cards.stream().map(entry -> {
            DeckCard deckCard = new DeckCard();
            deckCard.setCard(entry.card());
            deckCard.setQuantity(entry.quantity());
            return deckCard;
        }).toList();
        deck.setCards(new ArrayList<>(deckCards));
        return deckValidationService.validate(deck).valid();
    }

    private List<RandomDeckComposition.CardEntry> toEntries(Map<Card, Integer> composition) {
        return composition.entrySet().stream()
                .map(entry -> new RandomDeckComposition.CardEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<Card> filterByCategory(List<Card> cards, CardCategory category) {
        return cards.stream()
                .filter(card -> card.getCategory() == category)
                .sorted(Comparator.comparing(Card::getName))
                .toList();
    }

    private int pokemonCount(Map<Card, Integer> composition) {
        return sumByCategory(composition, List.of(
                CardCategory.BASIC_POKEMON,
                CardCategory.STAGE_1_POKEMON,
                CardCategory.STAGE_2_POKEMON));
    }

    private int trainerCount(Map<Card, Integer> composition) {
        return sumByCategory(composition, List.of(
                CardCategory.ITEM_TRAINER,
                CardCategory.SUPPORTER_TRAINER,
                CardCategory.STADIUM_TRAINER,
                CardCategory.POKEMON_TOOL_TRAINER));
    }

    private int sumByCategory(Map<Card, Integer> composition, Collection<CardCategory> categories) {
        return composition.entrySet().stream()
                .filter(entry -> categories.contains(entry.getKey().getCategory()))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    private int deckSize(Map<Card, Integer> composition) {
        return composition.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int currentCopies(Map<Card, Integer> composition, Card card) {
        return composition.getOrDefault(card, 0);
    }

    private void addCopies(Map<Card, Integer> composition, Card card, int quantity) {
        composition.merge(card, quantity, Integer::sum);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private record CandidateDeck(String name, List<RandomDeckComposition.CardEntry> cards) {
    }
}
