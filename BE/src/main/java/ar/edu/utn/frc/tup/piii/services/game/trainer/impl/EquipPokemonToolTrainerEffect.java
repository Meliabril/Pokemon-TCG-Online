package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EquipPokemonToolTrainerEffect implements TrainerEffect {

    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "targetPokemonInPlayId";

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(Card card) {
        return card != null && CardCategory.POKEMON_TOOL_TRAINER.equals(card.getCategory());
    }

    @Override
    public boolean keepsCardInPlay() {
        return true;
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        UUID targetPokemonInPlayId = requiredUuid(context.request().payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);
        Optional<PokemonInPlay> targetPokemonResult = pokemonInPlayStateService
                .findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId);
        if (targetPokemonResult.isEmpty()) {
            throw new InvalidGameActionException("Target Pokemon was not found for tool attachment");
        }

        PokemonInPlay targetPokemon = targetPokemonResult.get();

        boolean alreadyHasTool = pokemonAttachedCardStateService
                .findByPokemonInPlayId(targetPokemonInPlayId)
                .stream()
                .anyMatch(attached -> AttachedCardType.POKEMON_TOOL.equals(attached.getAttachedCardType()));
        if (alreadyHasTool) {
            throw new InvalidGameActionException("This Pokemon already has a Tool card attached");
        }

        context.trainerCardInstance().setZone(CardZone.ATTACHED);
        context.trainerCardInstance().setZonePosition(
                gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.ATTACHED));
        context.trainerCardInstance().setFaceDown(false);
        gameCardInstanceStateService.save(context.trainerCardInstance());
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(targetPokemon);
        attachedCard.setGameCardInstance(context.trainerCardInstance());
        attachedCard.setAttachedCardType(AttachedCardType.POKEMON_TOOL);
        pokemonAttachedCardStateService.save(attachedCard);

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", "EQUIP_POKEMON_TOOL");
        effectData.put("targetPokemonInPlayId", targetPokemonInPlayId.toString());
        effectData.put("cardExternalId", context.trainerCard().getExternalId());

        return new TrainerEffectResult(
                Map.copyOf(effectData),
                List.of(gameEventFactory.publicEvent(
                        gameId,
                        GameEventType.ENERGY_ATTACHED,
                        context.stateVersion(),
                        Map.of(
                                "playerId", actorUserId.toString(),
                                "cardId", context.trainerCard().getId().toString(),
                                "pokemonInPlayId", targetPokemonInPlayId.toString(),
                                "attachedCardType", AttachedCardType.POKEMON_TOOL.name()))));
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
}
