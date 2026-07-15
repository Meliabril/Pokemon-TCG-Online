package ar.edu.utn.frc.tup.piii.services.game.energy.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.energy.AttachEnergyService;
import ar.edu.utn.frc.tup.piii.services.game.engine.AvailableActionsFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachEnergyServiceImpl implements AttachEnergyService {

    private static final String CARD_ID_KEY = "cardId";
    private static final String CARD_INSTANCE_ID_KEY = "cardInstanceId";
    private static final String POKEMON_IN_PLAY_ID_KEY = "pokemonInPlayId";
    private static final String RAINBOW_ENERGY_EXTERNAL_ID = "xy1-131";
    private static final int RAINBOW_ENERGY_SELF_DAMAGE = 10;

    private final GameActionPayloadReader payloadReader;
    private final AvailableActionsFactory availableActionsFactory;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;
    private final DamageApplicationService damageApplicationService;
    private final CombatResolutionService combatResolutionService;
    private final GameParticipantStateService gameParticipantStateService;
    private final GameLookupService gameLookupService;
    private final GameStateQueryService gameStateQueryService;
    private final PassiveAbilityService passiveAbilityService;

    @Override
    public GameActionExecutionResult attachEnergy(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID gameId = context.gameId();
        UUID cardId = payloadReader.requiredUuid(context.request().payload(), CARD_ID_KEY);
        UUID cardInstanceId = optionalUuid(context.request().payload(), CARD_INSTANCE_ID_KEY);
        UUID pokemonInPlayId = payloadReader.requiredUuid(context.request().payload(), POKEMON_IN_PLAY_ID_KEY);

        GameCardInstance handCard = findHandCard(
                gameId,
                actorUserId,
                cardInstanceId,
                cardId,
                "Energy card is not available in the player's hand");
        PokemonInPlay targetPokemon = findOwnPokemonInPlay(pokemonInPlayId, gameId, actorUserId);
        Card card = cardService.getCardEntityById(cardId);
        AttachedCardType attachedCardType = toAttachedCardType(card);

        handCard.setZone(CardZone.ATTACHED);
        handCard.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.ATTACHED));
        handCard.setFaceDown(false);
        gameCardInstanceStateService.save(handCard);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(targetPokemon);
        attachedCard.setGameCardInstance(handCard);
        attachedCard.setAttachedCardType(attachedCardType);
        pokemonAttachedCardStateService.save(attachedCard);

        int newStateVersion = context.currentState().stateVersion() + 1;
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.ENERGY_ATTACHED,
                newStateVersion,
                Map.of(
                        "playerId", actorUserId.toString(),
                        "cardId", cardId.toString(),
                        "pokemonInPlayId", pokemonInPlayId.toString(),
                        "attachedCardType", attachedCardType.name())));
        events.addAll(passiveAbilityService.recalculateSweetVeil(
                gameLookupService.getRequiredGame(gameId),
                actorUserId,
                newStateVersion));

        if (RAINBOW_ENERGY_EXTERNAL_ID.equals(card.getExternalId())) {
            CombatResolutionService.CombatResolutionResult combatResult = applyRainbowEnergySelfDamage(
                    context,
                    targetPokemon,
                    actorUserId,
                    newStateVersion);
            events.addAll(combatResult.events());
            if (combatResult.gameFinished() || combatResult.promotionPending()) {
                return resultFromCurrentGame(gameId, newStateVersion, events);
            }
        }

        TurnContextDto newTurn = context.currentState().turn().toBuilder()
                .energyAttachedThisTurn(true)
                .build();
        ActionStateDto newActions = context.currentState().actions().toBuilder()
                .availableActions(availableActionsFactory.mainPhaseActions(true))
                .build();
        GameStateDto newState = context.currentState().toBuilder()
                .stateVersion(newStateVersion)
                .turn(newTurn)
                .actions(newActions)
                .updatedAt(Instant.now())
                .build();

        return new GameActionExecutionResult(newState, events);
    }

    private CombatResolutionService.CombatResolutionResult applyRainbowEnergySelfDamage(
            GameActionContext context,
            PokemonInPlay targetPokemon,
            UUID actorUserId,
            int stateVersion) {
        UUID gameId = context.gameId();
        int damageCounters = damageApplicationService.applyDamage(targetPokemon, RAINBOW_ENERGY_SELF_DAMAGE);
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", targetPokemon.getId().toString(),
                        "damage", RAINBOW_ENERGY_SELF_DAMAGE,
                        "damageCounters", damageCounters,
                        "reason", "RAINBOW_ENERGY_ATTACHED")));

        UUID opponentUserId = gameParticipantStateService.findOpponentUserId(gameId, actorUserId);
        int nextTurnNumber = context.currentState().turn().turnNumber() + 1;
        CombatResolutionService.CombatResolutionResult combatResult = combatResolutionService.resolveKnockoutIfNeeded(
                gameId,
                actorUserId,
                opponentUserId,
                targetPokemon,
                opponentUserId,
                nextTurnNumber,
                stateVersion,
                "RAINBOW_ENERGY_SELF_DAMAGE");

        events.addAll(combatResult.events());
        return new CombatResolutionService.CombatResolutionResult(
                combatResult.knockedOut(),
                combatResult.gameFinished(),
                combatResult.promotionPending(),
                combatResult.winnerUserId(),
                events);
    }

    private GameActionExecutionResult resultFromCurrentGame(UUID gameId, int stateVersion, List<GameEventDto> events) {
        Game game = gameLookupService.getRequiredGame(gameId);
        GameStateDto state = gameStateQueryService.buildVisibleState(game).toBuilder()
                .stateVersion(stateVersion)
                .updatedAt(Instant.now())
                .build();
        return new GameActionExecutionResult(state, List.copyOf(events));
    }

    private AttachedCardType toAttachedCardType(Card card) {
        if (CardCategory.BASIC_ENERGY.equals(card.getCategory())) {
            return AttachedCardType.BASIC_ENERGY;
        }
        if (CardCategory.SPECIAL_ENERGY.equals(card.getCategory())) {
            return AttachedCardType.SPECIAL_ENERGY;
        }

        throw new InvalidGameActionException("Only Energy cards can be attached with ATTACH_ENERGY");
    }

    private GameCardInstance findHandCard(
            UUID gameId,
            UUID actorUserId,
            UUID cardInstanceId,
            UUID cardId,
            String errorMessage) {
        Optional<GameCardInstance> handCard = cardInstanceId != null
                ? gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(cardInstanceId, gameId, actorUserId)
                : gameCardInstanceStateService.findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                        gameId,
                        actorUserId,
                        cardId,
                        CardZone.HAND);
        if (handCard.isEmpty()) {
            throw new InvalidGameActionException(errorMessage);
        }

        GameCardInstance selectedCard = handCard.get();
        if (!CardZone.HAND.equals(selectedCard.getZone()) || !cardId.equals(selectedCard.getCardId())) {
            throw new InvalidGameActionException(errorMessage);
        }

        return selectedCard;
    }

    private UUID optionalUuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    private PokemonInPlay findOwnPokemonInPlay(UUID pokemonInPlayId, UUID gameId, UUID actorUserId) {
        Optional<PokemonInPlay> targetPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                pokemonInPlayId,
                gameId,
                actorUserId);
        if (targetPokemon.isEmpty()) {
            throw new InvalidGameActionException("Target Pokemon was not found for the acting player");
        }

        return targetPokemon.get();
    }
}
