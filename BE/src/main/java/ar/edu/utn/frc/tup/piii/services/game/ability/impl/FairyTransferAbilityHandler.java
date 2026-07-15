package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.effect.MoveEnergyEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutDetectionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FairyTransferAbilityHandler implements AbilityEffectHandler {

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final MoveEnergyEffectService moveEnergyEffectService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;
    private final KnockoutDetectionService knockoutDetectionService;

    @Override
    public AbilityCode supports() {
        return AbilityCode.FAIRY_TRANSFER;
    }

    @Override
    public AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion) {
        if (request.fromPokemonId() == null || request.toPokemonId() == null) {
            throw new InvalidGameActionException("Fairy Transfer requires source Pokemon and target Pokemon");
        }

        PokemonInPlay fromPokemon = ownPokemon(context.gameId(), playerId, request.fromPokemonId());
        PokemonInPlay toPokemon = ownPokemon(context.gameId(), playerId, request.toPokemonId());
        if (fromPokemon.getId().equals(toPokemon.getId())) {
            throw new InvalidGameActionException("Fairy Transfer requires different source and target Pokemon");
        }
        if (knockoutDetectionService.isKnockedOut(toPokemon)) {
            throw new InvalidGameActionException("Fairy Transfer target Pokemon is Knocked Out");
        }

        PokemonAttachedCard attachedCard = findFirstFairyEnergy(fromPokemon);

        moveEnergyEffectService.moveEnergy(attachedCard, fromPokemon, toPokemon);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectType", AbilityCode.FAIRY_TRANSFER.name());
        payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        payload.put("actorPlayerId", playerId.toString());
        payload.put("fromPokemonInPlayId", fromPokemon.getId().toString());
        payload.put("toPokemonInPlayId", toPokemon.getId().toString());
        payload.put("energyCardInstanceId", attachedCard.getGameCardInstance().getId().toString());
        return new AbilityResolution(
                false,
                false,
                null,
                List.of(gameEventFactory.publicEvent(context.gameId(), GameEventType.ATTACK_EFFECT_RESOLVED, stateVersion, Map.copyOf(payload))));
    }

    private PokemonInPlay ownPokemon(UUID gameId, UUID playerId, UUID pokemonInPlayId) {
        return pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, playerId)
                .orElseThrow(() -> new InvalidGameActionException("Selected Pokemon was not found among your Pokemon"));
    }

    private PokemonAttachedCard findFirstFairyEnergy(PokemonInPlay fromPokemon) {
        return pokemonAttachedCardStateService.findByPokemonInPlayId(fromPokemon.getId()).stream()
                .filter(this::isFairyEnergy)
                .findFirst()
                .orElseThrow(() -> new InvalidGameActionException("Fairy Transfer source Pokemon has no Fairy Energy"));
    }

    private boolean isFairyEnergy(PokemonAttachedCard attachedCard) {
        Card card = cardService.getCardEntityById(attachedCard.getGameCardInstance().getCardId());
        if (card.getPokemonType() != null && "Fairy".equalsIgnoreCase(card.getPokemonType())) {
            return true;
        }
        return card.getName() != null && "Fairy Energy".equalsIgnoreCase(card.getName());
    }
}
