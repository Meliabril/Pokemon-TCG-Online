package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.effect.DamageCounterEffectService;
import ar.edu.utn.frc.tup.piii.services.game.effect.DiscardCardEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaterShurikenAbilityHandler implements AbilityEffectHandler {

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final DiscardCardEffectService discardCardEffectService;
    private final DamageCounterEffectService damageCounterEffectService;
    private final GameEventFactory gameEventFactory;

    @Override
    public AbilityCode supports() {
        return AbilityCode.WATER_SHURIKEN;
    }

    @Override
    public AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion) {
        if (request.selectedHandCardId() == null || request.targetPokemonId() == null) {
            throw new InvalidGameActionException("Water Shuriken requires a Water Energy from hand and a target Pokemon");
        }

        GameCardInstance selectedCard = gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                        request.selectedHandCardId(),
                        context.gameId(),
                        playerId)
                .orElseThrow(() -> new InvalidGameActionException("Selected hand card was not found"));
        if (!CardZone.HAND.equals(selectedCard.getZone())) {
            throw new InvalidGameActionException("Selected card is not in hand");
        }
        if (!isWaterEnergy(selectedCard)) {
            throw new InvalidGameActionException("Water Shuriken requires a Water Energy card from hand");
        }

        PokemonInPlay targetPokemon = opponentPokemon(context.gameId(), playerId, request.targetPokemonId());
        GameCardInstance discardedCard = discardCardEffectService.discardFromHand(context.gameId(), playerId, selectedCard.getId());

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.copyOf(abilityPayload(sourcePokemon, playerId, Map.of(
                        "discardedCardId", discardedCard.getCardId().toString(),
                        "discardedCardInstanceId", discardedCard.getId().toString(),
                        "targetPokemonInPlayId", targetPokemon.getId().toString())))));

        DamageCounterEffectService.DamageCounterResult damageResult = damageCounterEffectService.placeDamageCounters(
                context.gameId(),
                targetPokemon,
                3,
                playerId,
                playerId,
                game.getTurnNumber(),
                stateVersion,
                AbilityCode.WATER_SHURIKEN.name());
        events.addAll(damageResult.events());
        return new AbilityResolution(damageResult.gameFinished(), damageResult.promotionPending(), damageResult.winnerUserId(), List.copyOf(events));
    }

    private PokemonInPlay opponentPokemon(UUID gameId, UUID playerId, UUID pokemonInPlayId) {
        return pokemonInPlayStateService.findByGameIdOrdered(gameId).stream()
                .filter(pokemon -> pokemon.getId().equals(pokemonInPlayId) && !playerId.equals(pokemon.getOwnerUserId()))
                .findFirst()
                .orElseThrow(() -> new InvalidGameActionException("Target Pokemon was not found among opponent Pokemon"));
    }

    private boolean isWaterEnergy(GameCardInstance cardInstance) {
        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (card.getPokemonType() != null && "Water".equalsIgnoreCase(card.getPokemonType())) {
            return true;
        }
        return card.getName() != null && "Water Energy".equalsIgnoreCase(card.getName());
    }

    private Map<String, Object> abilityPayload(PokemonInPlay sourcePokemon, UUID playerId, Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectType", AbilityCode.WATER_SHURIKEN.name());
        payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        payload.put("actorPlayerId", playerId.toString());
        payload.putAll(details);
        return payload;
    }
}
