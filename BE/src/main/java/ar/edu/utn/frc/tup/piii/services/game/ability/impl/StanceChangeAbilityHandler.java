package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StanceChangeAbilityHandler implements AbilityEffectHandler {

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;

    @Override
    public AbilityCode supports() {
        return AbilityCode.STANCE_CHANGE;
    }

    @Override
    public AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion) {
        if (request.selectedHandCardId() == null) {
            throw new InvalidGameActionException("Stance Change requires selecting an Aegislash from hand");
        }

        GameCardInstance replacementInstance = gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                        request.selectedHandCardId(),
                        context.gameId(),
                        playerId)
                .orElseThrow(() -> new InvalidGameActionException("Selected hand card was not found"));
        if (!CardZone.HAND.equals(replacementInstance.getZone())) {
            throw new InvalidGameActionException("Selected replacement card must be in hand");
        }

        Card replacementCard = cardService.getCardEntityById(replacementInstance.getCardId());
        if (!CardSupertype.POKEMON.equals(replacementCard.getSupertype())) {
            throw new InvalidGameActionException("Stance Change requires a Pokemon card from hand");
        }
        if (!"Aegislash".equalsIgnoreCase(replacementCard.getName())) {
            throw new InvalidGameActionException("Stance Change requires another Aegislash from hand");
        }

        PokemonEvolutionStack topStack = pokemonEvolutionStackStateService.findTopByPokemonInPlayId(sourcePokemon.getId())
                .orElseThrow(() -> new InvalidGameActionException("Evolution stack is missing for the source Pokemon"));
        GameCardInstance previousTop = sourcePokemon.getActiveCardInstance();
        Card currentCard = cardService.getCardEntityById(previousTop.getCardId());
        if (!"Aegislash".equalsIgnoreCase(currentCard.getName())) {
            throw new InvalidGameActionException("Stance Change can only be used by Aegislash");
        }
        if (samePrintedCard(currentCard, replacementCard)) {
            throw new InvalidGameActionException("You cannot switch Aegislash with an identical copy");
        }

        CardZone targetZone = Integer.valueOf(0).equals(sourcePokemon.getSlotPosition()) ? CardZone.ACTIVE : CardZone.BENCH;

        previousTop.setZone(CardZone.HAND);
        previousTop.setZonePosition(gameCardInstanceStateService.nextZonePosition(context.gameId(), playerId, CardZone.HAND));
        previousTop.setFaceDown(false);
        gameCardInstanceStateService.save(previousTop);

        replacementInstance.setZone(targetZone);
        replacementInstance.setZonePosition(sourcePokemon.getSlotPosition());
        replacementInstance.setFaceDown(false);
        gameCardInstanceStateService.save(replacementInstance);
        gameCardInstanceStateService.resequenceZone(context.gameId(), playerId, CardZone.HAND);

        sourcePokemon.setActiveCardInstance(replacementInstance);
        pokemonInPlayStateService.save(sourcePokemon);
        topStack.setGameCardInstance(replacementInstance);
        pokemonEvolutionStackStateService.save(topStack);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectType", AbilityCode.STANCE_CHANGE.name());
        payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        payload.put("actorPlayerId", playerId.toString());
        payload.put("previousCardId", previousTop.getCardId().toString());
        payload.put("newCardId", replacementInstance.getCardId().toString());
        return new AbilityResolution(
                false,
                false,
                null,
                List.of(gameEventFactory.publicEvent(context.gameId(), GameEventType.ATTACK_EFFECT_RESOLVED, stateVersion, Map.copyOf(payload))));
    }

    private boolean samePrintedCard(Card currentCard, Card replacementCard) {
        if (currentCard == null || replacementCard == null) {
            return false;
        }
        if (currentCard.getExternalId() != null || replacementCard.getExternalId() != null) {
            return Objects.equals(currentCard.getExternalId(), replacementCard.getExternalId());
        }
        if (currentCard.getNumber() != null || replacementCard.getNumber() != null) {
            return Objects.equals(currentCard.getNumber(), replacementCard.getNumber());
        }
        return Objects.equals(currentCard.getId(), replacementCard.getId());
    }
}
