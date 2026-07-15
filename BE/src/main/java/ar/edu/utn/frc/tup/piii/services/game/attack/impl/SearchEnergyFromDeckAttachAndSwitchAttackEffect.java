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
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
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
import java.util.Optional;
import java.util.UUID;

/*
 * Models attacks like Emolga-EX's Energy Glide: search the deck for a card of
 * the given energyType, attach it to the attacker and shuffle the deck; only
 * if a card was actually attached, switch the attacker with a Benched Pokemon.
 * The two steps are coupled by the search outcome, so they live in one effect
 * instead of two independent catalog operations.
 */
@Service
@RequiredArgsConstructor
public class SearchEnergyFromDeckAttachAndSwitchAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SEARCH_ENERGY_FROM_DECK_ATTACH_AND_SWITCH";
    private static final String ENERGY_SUFFIX = " Energy";
    private static final String SELF_TARGET_POKEMON_IN_PLAY_ID_KEY = "selfTargetPokemonInPlayId";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final GameActionPayloadReader payloadReader;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        boolean attached = searchAndAttachEnergy(context);

        boolean switched = false;
        if (attached) {
            switched = switchWithBench(context);
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "attachedEnergy", attached,
                        "switched", switched,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private boolean searchAndAttachEnergy(AttackEffectContext context) {
        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        String energyType = context.operation().energyType();

        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                attackerUserId,
                CardZone.DECK);
        GameCardInstance energyCard = firstMatchingEnergy(deckCards, energyType);
        if (energyCard == null) {
            return false;
        }

        attachToAttacker(context, gameId, attackerUserId, energyCard);
        shuffleDeck(gameId, attackerUserId, deckCards, energyCard);
        return true;
    }

    private GameCardInstance firstMatchingEnergy(List<GameCardInstance> deckCards, String energyType) {
        for (GameCardInstance deckCard : deckCards) {
            Card card = cardService.getCardEntityById(deckCard.getCardId());
            if (isMatchingEnergy(card, energyType)) {
                return deckCard;
            }
        }
        return null;
    }

    private boolean isMatchingEnergy(Card card, String energyType) {
        if (card == null || energyType == null) {
            return false;
        }
        boolean isEnergyCard = CardCategory.BASIC_ENERGY.equals(card.getCategory())
                || CardCategory.SPECIAL_ENERGY.equals(card.getCategory());
        if (!isEnergyCard) {
            return false;
        }
        if (card.getPokemonType() != null && card.getPokemonType().equalsIgnoreCase(energyType)) {
            return true;
        }
        return card.getName() != null && card.getName().equalsIgnoreCase(energyType + ENERGY_SUFFIX);
    }

    private void attachToAttacker(
            AttackEffectContext context,
            UUID gameId,
            UUID attackerUserId,
            GameCardInstance energyCard) {
        int nextAttachedPosition = gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED);
        energyCard.setZone(CardZone.ATTACHED);
        energyCard.setZonePosition(nextAttachedPosition);
        energyCard.setFaceDown(false);
        gameCardInstanceStateService.save(energyCard);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(context.resolutionContext().attackerPokemon());
        attachedCard.setGameCardInstance(energyCard);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        pokemonAttachedCardStateService.save(attachedCard);
    }

    private void shuffleDeck(
            UUID gameId,
            UUID attackerUserId,
            List<GameCardInstance> previousDeckCards,
            GameCardInstance removedCard) {
        List<GameCardInstance> remainingDeckCards = previousDeckCards.stream()
                .filter(deckCard -> !deckCard.getId().equals(removedCard.getId()))
                .sorted(Comparator.comparing(GameCardInstance::getZonePosition, Comparator.nullsLast(Integer::compareTo)))
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

    private boolean switchWithBench(AttackEffectContext context) {
        PokemonInPlay activePokemon = context.resolutionContext().attackerPokemon();
        Optional<PokemonInPlay> benchPokemon = selectedBenchPokemon(context);
        if (benchPokemon.isEmpty()) {
            return false;
        }

        int formerBenchSlot = benchPokemon.get().getSlotPosition();
        GameCardInstance activeCardInstance = activePokemon.getActiveCardInstance();
        GameCardInstance benchCardInstance = benchPokemon.get().getActiveCardInstance();
        gameCardInstanceStateService.swapActiveWithBench(
                activeCardInstance.getId(),
                benchCardInstance.getId(),
                formerBenchSlot);
        pokemonInPlayStateService.swapActiveWithBench(
                activePokemon.getId(),
                benchPokemon.get().getId(),
                formerBenchSlot);
        return true;
    }

    private Optional<PokemonInPlay> selectedBenchPokemon(AttackEffectContext context) {
        boolean hasOwnBench = pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId())
                .stream()
                .anyMatch(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > ACTIVE_SLOT_POSITION);
        if (!hasOwnBench) {
            return Optional.empty();
        }

        UUID selectedPokemonInPlayId = payloadReader.requiredUuid(context.payload(), SELF_TARGET_POKEMON_IN_PLAY_ID_KEY);
        return pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                selectedPokemonInPlayId,
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId());
    }
}
