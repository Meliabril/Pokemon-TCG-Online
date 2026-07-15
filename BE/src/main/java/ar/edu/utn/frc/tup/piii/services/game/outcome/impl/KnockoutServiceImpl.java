package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnockoutServiceImpl implements KnockoutService {

    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final SpecialConditionStateService specialConditionStateService;

    @Override
    public KnockoutResult resolveKnockout(UUID gameId, UUID ownerUserId, PokemonInPlay knockedOutPokemon) {
        Set<UUID> discardedCardInstanceIds = new HashSet<>();
        discardCardInstance(gameId, ownerUserId, knockedOutPokemon.getActiveCardInstance(), discardedCardInstanceIds);

        List<PokemonEvolutionStack> evolutionStack = pokemonEvolutionStackStateService.findByPokemonInPlayId(knockedOutPokemon.getId());
        for (PokemonEvolutionStack stackEntry : evolutionStack) {
            discardCardInstance(gameId, ownerUserId, stackEntry.getGameCardInstance(), discardedCardInstanceIds);
            pokemonEvolutionStackStateService.delete(stackEntry);
        }

        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(knockedOutPokemon.getId());
        for (PokemonAttachedCard attachedCard : attachedCards) {
            discardCardInstance(gameId, ownerUserId, attachedCard.getGameCardInstance(), discardedCardInstanceIds);
            pokemonAttachedCardStateService.delete(attachedCard);
        }

        specialConditionStateService.deleteByPokemonInPlayId(knockedOutPokemon.getId());
        boolean wasActive = knockedOutPokemon.getSlotPosition() == 0;
        pokemonInPlayStateService.delete(knockedOutPokemon);

        boolean hasReplacementActivePokemon = false;
        if (wasActive) {
            List<PokemonInPlay> benchPokemon = sortedBenchPokemon(gameId, ownerUserId);
            hasReplacementActivePokemon = !benchPokemon.isEmpty();
        }

        gameCardInstanceStateService.resequenceZone(gameId, ownerUserId, CardZone.ATTACHED);
        gameCardInstanceStateService.resequenceZone(gameId, ownerUserId, CardZone.DISCARD);
        return new KnockoutResult(null, hasReplacementActivePokemon);
    }

    private void discardCardInstance(
            UUID gameId,
            UUID ownerUserId,
            GameCardInstance cardInstance,
            Set<UUID> discardedCardInstanceIds) {
        if (cardInstance == null || discardedCardInstanceIds.contains(cardInstance.getId())) {
            return;
        }
        cardInstance.setZone(CardZone.DISCARD);
        cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, ownerUserId, CardZone.DISCARD));
        cardInstance.setFaceDown(false);
        gameCardInstanceStateService.save(cardInstance);
        discardedCardInstanceIds.add(cardInstance.getId());
    }

    private List<PokemonInPlay> sortedBenchPokemon(UUID gameId, UUID ownerUserId) {
        List<PokemonInPlay> allPokemon = pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, ownerUserId);
        List<PokemonInPlay> benchPokemon = new ArrayList<>();
        for (PokemonInPlay pokemonInPlay : allPokemon) {
            Integer slotPosition = pokemonInPlay.getSlotPosition();
            if (slotPosition != null && slotPosition > 0) {
                benchPokemon.add(pokemonInPlay);
            }
        }
        sortBySlotPosition(benchPokemon);
        return benchPokemon;
    }

    private void sortBySlotPosition(List<PokemonInPlay> benchPokemon) {
        for (int leftIndex = 0; leftIndex < benchPokemon.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < benchPokemon.size(); rightIndex++) {
                PokemonInPlay leftPokemon = benchPokemon.get(leftIndex);
                PokemonInPlay rightPokemon = benchPokemon.get(rightIndex);
                if (leftPokemon.getSlotPosition() > rightPokemon.getSlotPosition()) {
                    benchPokemon.set(leftIndex, rightPokemon);
                    benchPokemon.set(rightIndex, leftPokemon);
                }
            }
        }
    }
}
