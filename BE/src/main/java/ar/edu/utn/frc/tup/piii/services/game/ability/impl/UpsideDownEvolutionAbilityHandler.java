package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpsideDownEvolutionAbilityHandler implements AbilityEffectHandler {

    private final SpecialConditionStateService specialConditionStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public AbilityCode supports() {
        return AbilityCode.UPSIDE_DOWN_EVOLUTION;
    }

    @Override
    public AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion) {
        if (!specialConditionStateService.activeConditionTypes(sourcePokemon.getId()).contains(SpecialConditionType.CONFUSED)) {
            throw new InvalidGameActionException("Upside-Down Evolution can only be used while Inkay is Confused");
        }
        if (request.selectedDeckCardId() == null) {
            throw new InvalidGameActionException("Upside-Down Evolution requires selecting an evolution card from deck");
        }

        GameCardInstance deckCardInstance = gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                        request.selectedDeckCardId(),
                        context.gameId(),
                        playerId)
                .orElseThrow(() -> new InvalidGameActionException("Selected evolution card was not found"));
        if (!CardZone.DECK.equals(deckCardInstance.getZone())) {
            throw new InvalidGameActionException("Selected evolution card must come from deck");
        }

        PokemonEvolutionStack currentTopStack = pokemonEvolutionStackStateService.findTopByPokemonInPlayId(sourcePokemon.getId())
                .orElseThrow(() -> new InvalidGameActionException("Evolution stack is missing for the target Pokemon"));
        Card evolutionCard = cardService.getCardEntityById(deckCardInstance.getCardId());
        Card currentTopCard = cardService.getCardEntityById(currentTopStack.getGameCardInstance().getCardId());
        validateEvolvesFromCurrentPokemon(evolutionCard, currentTopCard);

        GameCardInstance previousTop = currentTopStack.getGameCardInstance();
        previousTop.setZone(CardZone.EVOLUTION_STACK);
        previousTop.setZonePosition(gameCardInstanceStateService.nextZonePosition(context.gameId(), playerId, CardZone.EVOLUTION_STACK));
        gameCardInstanceStateService.save(previousTop);
        gameCardInstanceStateService.flush();

        deckCardInstance.setZone(targetZoneFor(sourcePokemon));
        deckCardInstance.setZonePosition(sourcePokemon.getSlotPosition());
        deckCardInstance.setFaceDown(false);
        gameCardInstanceStateService.save(deckCardInstance);

        sourcePokemon.setActiveCardInstance(deckCardInstance);
        sourcePokemon.setEnteredPlayTurn(game.getTurnNumber());
        pokemonInPlayStateService.save(sourcePokemon);

        PokemonEvolutionStack evolvedStack = new PokemonEvolutionStack();
        evolvedStack.setPokemonInPlay(sourcePokemon);
        evolvedStack.setGameCardInstance(deckCardInstance);
        evolvedStack.setStackOrder(pokemonEvolutionStackStateService.nextStackOrder(sourcePokemon.getId()));
        evolvedStack.setCreatedAtTurn(game.getTurnNumber());
        pokemonEvolutionStackStateService.save(evolvedStack);

        specialConditionStateService.deleteByPokemonInPlayId(sourcePokemon.getId());
        shuffleDeck(context.gameId(), playerId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectType", AbilityCode.UPSIDE_DOWN_EVOLUTION.name());
        payload.put("playerId", playerId.toString());
        payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        payload.put("cardId", deckCardInstance.getCardId().toString());
        payload.put("cardInstanceId", deckCardInstance.getId().toString());
        payload.put("previousCardId", previousTop.getCardId().toString());
        return new AbilityResolution(false, false, null, List.of(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.POKEMON_EVOLVED,
                stateVersion,
                Map.copyOf(payload))));
    }

    private CardZone targetZoneFor(PokemonInPlay sourcePokemon) {
        return Integer.valueOf(0).equals(sourcePokemon.getSlotPosition()) ? CardZone.ACTIVE : CardZone.BENCH;
    }

    private void validateEvolvesFromCurrentPokemon(Card evolutionCard, Card currentTopCard) {
        if (evolutionCard == null || currentTopCard == null || evolutionCard.getEvolvesFrom() == null) {
            throw new InvalidGameActionException("Selected card does not evolve from this Pokemon");
        }
        if (!CardSupertype.POKEMON.equals(evolutionCard.getSupertype())) {
            throw new InvalidGameActionException("Selected evolution card must be a Pokemon");
        }
        if (!evolutionCard.getEvolvesFrom().equalsIgnoreCase(currentTopCard.getName())) {
            throw new InvalidGameActionException("Selected card does not evolve from this Pokemon");
        }
    }

    private void shuffleDeck(UUID gameId, UUID ownerUserId) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                ownerUserId,
                CardZone.DECK);
        List<GameCardInstance> shuffledDeckCards = gameRandomService.shuffledCopy(new ArrayList<>(deckCards));
        int position = 0;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(shuffledDeckCards);
    }
}
