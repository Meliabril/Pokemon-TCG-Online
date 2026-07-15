package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MysticalFireAbilityHandler implements AbilityEffectHandler {

    private final DrawCardsEffectService drawCardsEffectService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public AbilityCode supports() {
        return AbilityCode.MYSTICAL_FIRE;
    }

    @Override
    public AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion) {
        int currentHandSize = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                context.gameId(),
                playerId,
                CardZone.HAND).size();
        if (currentHandSize >= 6) {
            throw new InvalidGameActionException("Ya tenes 6 o mas cartas en mano.");
        }

        List<GameCardInstance> drawnCards = drawCardsEffectService.drawUntilHandSize(context.gameId(), playerId, 6);
        List<String> drawnCardIds = new ArrayList<>();
        for (GameCardInstance drawnCard : drawnCards) {
            drawnCardIds.add(drawnCard.getCardId().toString());
        }

        List<GameEventDto> events = new ArrayList<>();
        if (!drawnCards.isEmpty()) {
            events.add(gameEventFactory.publicEvent(
                    context.gameId(),
                    GameEventType.CARD_DRAWN,
                    stateVersion,
                    Map.of(
                            "playerId", playerId.toString(),
                            "cardsDrawn", drawnCards.size(),
                            "source", "ABILITY")));
            events.add(gameEventFactory.privateEvent(
                    context.gameId(),
                    GameEventType.CARD_DRAWN,
                    stateVersion,
                    Map.of(
                            "playerId", playerId.toString(),
                            "cardIds", List.copyOf(drawnCardIds),
                            "source", "ABILITY"),
                    playerId));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectType", AbilityCode.MYSTICAL_FIRE.name());
        payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        payload.put("sourcePokemonId", sourcePokemon.getId().toString());
        payload.put("cardsDrawn", drawnCards.size());
        payload.put("actorPlayerId", playerId.toString());
        events.add(gameEventFactory.publicEvent(context.gameId(), GameEventType.ATTACK_EFFECT_RESOLVED, stateVersion, Map.copyOf(payload)));
        return new AbilityResolution(false, false, null, List.copyOf(events));
    }
}
