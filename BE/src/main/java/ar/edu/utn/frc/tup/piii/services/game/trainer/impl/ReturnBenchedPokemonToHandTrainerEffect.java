package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReturnBenchedPokemonToHandTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "RETURN_BENCHED_POKEMON_TO_HAND";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final SpecialConditionStateService specialConditionStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;

    @Override
    public boolean supports(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        if (!CardCategory.SUPPORTER_TRAINER.equals(card.getCategory())) {
            return false;
        }
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(card);
        return EFFECT_TYPE.equals(definition.type());
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        UUID targetPokemonInPlayId = requiredUuid(context.request().payload(), "targetPokemonInPlayId");
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        PokemonInPlay target = pokemonInPlayStateService
                .findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId)
                .orElseThrow(() -> new InvalidGameActionException("Target Pokemon not found or does not belong to you"));

        if (target.getSlotPosition() == null || target.getSlotPosition() == 0) {
            throw new InvalidGameActionException("Cassius can only be used on a Benched Pokémon");
        }

        Set<UUID> shuffledInstanceIds = new HashSet<>();

        // Move evolution stack cards to deck
        List<PokemonEvolutionStack> evolutionStack =
                pokemonEvolutionStackStateService.findByPokemonInPlayId(target.getId());
        for (PokemonEvolutionStack stackEntry : evolutionStack) {
            moveToDeck(gameId, actorUserId, stackEntry.getGameCardInstance(), shuffledInstanceIds);
            pokemonEvolutionStackStateService.delete(stackEntry);
        }

        // Move attached cards (energies, tools) to deck
        List<PokemonAttachedCard> attachedCards =
                pokemonAttachedCardStateService.findByPokemonInPlayId(target.getId());
        for (PokemonAttachedCard attachedCard : attachedCards) {
            moveToDeck(gameId, actorUserId, attachedCard.getGameCardInstance(), shuffledInstanceIds);
            pokemonAttachedCardStateService.delete(attachedCard);
        }

        // Clear any special conditions on the returning Pokémon
        specialConditionStateService.deleteByPokemonInPlayId(target.getId());

        // Move the Pokémon itself to deck
        moveToDeck(gameId, actorUserId, target.getActiveCardInstance(), shuffledInstanceIds);

        // Remove from play
        pokemonInPlayStateService.delete(target);

        // Shuffle the deck
        shuffleDeck(gameId, actorUserId);

        // Resequence attached zone
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.ATTACHED);

        return new TrainerEffectResult(
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "targetPokemonInPlayId", targetPokemonInPlayId.toString(),
                        "shuffledIntoDeck", shuffledInstanceIds.size()),
                List.of());
    }

    private UUID requiredUuid(Map<String, Object> payload, String key) {
        Object value = payload != null ? payload.get(key) : null;
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new InvalidGameActionException("Payload field '" + key + "' must be a valid UUID");
    }

    private void moveToDeck(UUID gameId, UUID ownerUserId, GameCardInstance cardInstance, Set<UUID> processedIds) {
        if (cardInstance == null || processedIds.contains(cardInstance.getId())) {
            return;
        }
        cardInstance.setZone(CardZone.DECK);
        cardInstance.setFaceDown(true);
        cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, ownerUserId, CardZone.DECK));
        gameCardInstanceStateService.save(cardInstance);
        processedIds.add(cardInstance.getId());
    }

    private void shuffleDeck(UUID gameId, UUID ownerUserId) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, ownerUserId, CardZone.DECK);
        Collections.shuffle(deckCards);
        int position = 1;
        for (GameCardInstance card : deckCards) {
            card.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(deckCards);
    }
}
