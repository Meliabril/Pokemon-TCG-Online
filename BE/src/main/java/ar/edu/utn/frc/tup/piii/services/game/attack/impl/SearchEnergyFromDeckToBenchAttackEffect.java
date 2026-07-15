package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchEnergyFromDeckToBenchAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SEARCH_ENERGY_FROM_DECK_TO_BENCH";
    private static final String ENERGY_SUFFIX = " Energy";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        List<PokemonInPlay> benchPokemon = benchPokemon(gameId, attackerUserId);
        if (benchPokemon.isEmpty()) {
            return resultEvent(context, List.of(), List.of());
        }

        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                attackerUserId,
                CardZone.DECK);

        List<String> attachedPokemonInPlayIds = new ArrayList<>();
        List<String> attachedCardInstanceIds = new ArrayList<>();
        attachEnergyCards(context, benchPokemon, deckCards, attachedPokemonInPlayIds, attachedCardInstanceIds);
        shuffleDeck(gameId, attackerUserId, deckCards, attachedCardInstanceIds);

        return resultEvent(context, attachedPokemonInPlayIds, attachedCardInstanceIds);
    }

    private AttackEffectResult resultEvent(
            AttackEffectContext context,
            List<String> attachedPokemonInPlayIds,
            List<String> attachedCardInstanceIds) {
        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "attachedCount", attachedCardInstanceIds.size(),
                        "attachedPokemonInPlayIds", List.copyOf(attachedPokemonInPlayIds),
                        "attachedCardInstanceIds", List.copyOf(attachedCardInstanceIds),
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private void attachEnergyCards(
            AttackEffectContext context,
            List<PokemonInPlay> benchPokemon,
            List<GameCardInstance> deckCards,
            List<String> attachedPokemonInPlayIds,
            List<String> attachedCardInstanceIds) {
        int attachLimit = Math.min(context.operation().amount(), benchPokemon.size());
        if (attachLimit <= 0) {
            return;
        }

        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        List<GameCardInstance> matchingEnergyCards = matchingEnergyCards(deckCards, context.operation().energyType(), attachLimit);
        int nextAttachedPosition = gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED);

        for (int index = 0; index < matchingEnergyCards.size(); index++) {
            GameCardInstance energyCard = matchingEnergyCards.get(index);
            PokemonInPlay targetPokemon = benchPokemon.get(index);
            energyCard.setZone(CardZone.ATTACHED);
            energyCard.setZonePosition(nextAttachedPosition + index);
            energyCard.setFaceDown(false);
            gameCardInstanceStateService.save(energyCard);

            PokemonAttachedCard attachedCard = new PokemonAttachedCard();
            attachedCard.setPokemonInPlay(targetPokemon);
            attachedCard.setGameCardInstance(energyCard);
            attachedCard.setAttachedCardType(attachedCardType(energyCard));
            pokemonAttachedCardStateService.save(attachedCard);

            attachedPokemonInPlayIds.add(targetPokemon.getId().toString());
            attachedCardInstanceIds.add(energyCard.getId().toString());
        }
    }

    private List<GameCardInstance> matchingEnergyCards(List<GameCardInstance> deckCards, String energyType, int attachLimit) {
        List<GameCardInstance> matchingEnergyCards = new ArrayList<>();
        for (GameCardInstance deckCard : sortedByZonePosition(deckCards)) {
            if (matchingEnergyCards.size() >= attachLimit) {
                break;
            }
            Card card = cardService.getCardEntityById(deckCard.getCardId());
            if (isMatchingEnergy(card, energyType)) {
                matchingEnergyCards.add(deckCard);
            }
        }
        return matchingEnergyCards;
    }

    private boolean isMatchingEnergy(Card card, String energyType) {
        if (card == null || energyType == null || energyType.isBlank()) {
            return false;
        }

        boolean energyCard = CardCategory.BASIC_ENERGY.equals(card.getCategory())
                || CardCategory.SPECIAL_ENERGY.equals(card.getCategory());
        if (!energyCard) {
            return false;
        }
        if (card.getPokemonType() != null && card.getPokemonType().equalsIgnoreCase(energyType)) {
            return true;
        }
        return card.getName() != null && card.getName().equalsIgnoreCase(energyType + ENERGY_SUFFIX);
    }

    private AttachedCardType attachedCardType(GameCardInstance energyCard) {
        Card card = cardService.getCardEntityById(energyCard.getCardId());
        if (card != null && CardCategory.SPECIAL_ENERGY.equals(card.getCategory())) {
            return AttachedCardType.SPECIAL_ENERGY;
        }
        return AttachedCardType.BASIC_ENERGY;
    }

    private List<PokemonInPlay> benchPokemon(UUID gameId, UUID attackerUserId) {
        return pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId).stream()
                .filter(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > ACTIVE_SLOT_POSITION)
                .sorted(Comparator.comparing(PokemonInPlay::getSlotPosition))
                .toList();
    }

    private List<GameCardInstance> sortedByZonePosition(List<GameCardInstance> deckCards) {
        return deckCards.stream()
                .sorted(Comparator.comparing(GameCardInstance::getZonePosition, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private void shuffleDeck(
            UUID gameId,
            UUID attackerUserId,
            List<GameCardInstance> previousDeckCards,
            List<String> attachedCardInstanceIds) {
        List<GameCardInstance> remainingDeckCards = previousDeckCards.stream()
                .filter(deckCard -> !attachedCardInstanceIds.contains(deckCard.getId().toString()))
                .toList();
        if (remainingDeckCards.isEmpty()) {
            return;
        }

        List<GameCardInstance> shuffledDeckCards = gameRandomService.shuffledCopy(remainingDeckCards);
        List<GameCardInstance> cardsToSave = new ArrayList<>();
        int temporaryPosition = -1;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(temporaryPosition--);
            cardsToSave.add(deckCard);
        }
        gameCardInstanceStateService.saveAll(cardsToSave);
        gameCardInstanceStateService.flush();

        int position = 1;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(shuffledDeckCards);
    }
}
