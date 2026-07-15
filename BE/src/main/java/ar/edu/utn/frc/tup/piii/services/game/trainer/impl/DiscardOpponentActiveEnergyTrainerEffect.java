package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DiscardOpponentActiveEnergyTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "DISCARD_OPPONENT_ACTIVE_ENERGY";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(Card card) {
        if (!isItemOrSupporter(card)) {
            return false;
        }
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(card);
        return EFFECT_TYPE.equals(definition.type());
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();
        UUID opponentUserId = opponentOf(context, actorUserId);

        Optional<PokemonInPlay> activePokemonResult = pokemonInPlayStateService
                .findActivePokemon(gameId, opponentUserId);
        if (activePokemonResult.isEmpty()) {
            throw new InvalidGameActionException("Opponent has no Active Pokemon");
        }

        PokemonInPlay activePokemon = activePokemonResult.get();
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService
                .findByPokemonInPlayId(activePokemon.getId());

        PokemonAttachedCard energyToDiscard = attachedCards.stream()
                .filter(a -> AttachedCardType.BASIC_ENERGY.equals(a.getAttachedCardType())
                          || AttachedCardType.SPECIAL_ENERGY.equals(a.getAttachedCardType()))
                .findFirst()
                .orElseThrow(() -> new InvalidGameActionException("Opponent's Active Pokemon has no attached Energy"));

        GameCardInstance energyInstance = energyToDiscard.getGameCardInstance();
        pokemonAttachedCardStateService.delete(energyToDiscard);

        energyInstance.setZone(CardZone.DISCARD);
        energyInstance.setFaceDown(false);
        energyInstance.setZonePosition(gameCardInstanceStateService
                .nextZonePosition(gameId, opponentUserId, CardZone.DISCARD));
        gameCardInstanceStateService.save(energyInstance);
        gameCardInstanceStateService.resequenceZone(gameId, opponentUserId, CardZone.ATTACHED);

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.TRAINER_PLAYED, context.stateVersion(),
                Map.of(
                        "source", "TRAINER",
                        "effectType", EFFECT_TYPE,
                        "targetPlayerId", opponentUserId.toString(),
                        "pokemonInPlayId", activePokemon.getId().toString())));

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("targetPlayerId", opponentUserId.toString());
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    private UUID opponentOf(TrainerEffectContext context, UUID actorUserId) {
        return context.currentState().playerIds().stream()
                .filter(id -> !id.equals(actorUserId))
                .findFirst()
                .orElseThrow(() -> new InvalidGameActionException("Could not determine opponent"));
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }
}
