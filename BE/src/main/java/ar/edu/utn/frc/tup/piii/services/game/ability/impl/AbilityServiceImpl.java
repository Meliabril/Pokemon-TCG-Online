package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AbilityServiceImpl implements AbilityService {

    private final GameActionPayloadReader payloadReader;
    private final GameLookupService gameLookupService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final AbilityCatalogService abilityCatalogService;
    private final AbilityUsageTracker abilityUsageTracker;
    private final GameStateQueryService gameStateQueryService;
    private final Map<AbilityCode, AbilityEffectHandler> handlersByCode;

    public AbilityServiceImpl(
            GameActionPayloadReader payloadReader,
            GameLookupService gameLookupService,
            PokemonInPlayStateService pokemonInPlayStateService,
            CardService cardService,
            AbilityCatalogService abilityCatalogService,
            AbilityUsageTracker abilityUsageTracker,
            GameStateQueryService gameStateQueryService,
            List<AbilityEffectHandler> handlers) {
        this.payloadReader = payloadReader;
        this.gameLookupService = gameLookupService;
        this.pokemonInPlayStateService = pokemonInPlayStateService;
        this.cardService = cardService;
        this.abilityCatalogService = abilityCatalogService;
        this.abilityUsageTracker = abilityUsageTracker;
        this.gameStateQueryService = gameStateQueryService;
        this.handlersByCode = handlersByCode(handlers);
    }

    @Override
    public GameActionExecutionResult useAbility(GameActionContext context) {
        Game game = gameLookupService.getRequiredGame(context.gameId());
        validateGameState(game, context.actorUserId());

        UseAbilityRequest request = readRequest(context);
        PokemonInPlay sourcePokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                        request.sourcePokemonId(),
                        context.gameId(),
                        context.actorUserId())
                .orElseThrow(() -> new InvalidGameActionException("Source Pokemon was not found for the acting player"));

        Card sourceCard = cardService.getCardEntityById(sourcePokemon.getActiveCardInstance().getCardId());
        AbilityDefinition abilityDefinition = abilityCatalogService.find(sourceCard.getExternalId(), request.abilityCode())
                .orElseThrow(() -> new InvalidGameActionException("The selected Pokemon does not have that ability"));
        if (!AbilityActivationType.ACTIVATED.equals(abilityDefinition.activation())) {
            throw new InvalidGameActionException("Passive abilities cannot be used manually");
        }
        if (abilityDefinition.oncePerTurn() && abilityUsageTracker.wasUsedThisTurn(game, sourcePokemon.getId(), request.abilityCode())) {
            throw new InvalidGameActionException("That ability was already used this turn");
        }

        AbilityEffectHandler handler = Optional.ofNullable(handlersByCode.get(request.abilityCode()))
                .orElseThrow(() -> new InvalidGameActionException("No handler is registered for ability " + request.abilityCode()));

        int stateVersion = context.currentState().stateVersion() + 1;
        if (abilityDefinition.oncePerTurn()) {
            abilityUsageTracker.markUsed(game, sourcePokemon.getId(), request.abilityCode());
        }
        AbilityResolution resolution = handler.resolve(context, game, context.actorUserId(), sourcePokemon, request, stateVersion);

        return new GameActionExecutionResult(
                gameStateQueryService.buildVisibleState(game).toBuilder()
                        .stateVersion(stateVersion)
                        .updatedAt(Instant.now())
                        .build(),
                resolution.events());
    }

    private void validateGameState(Game game, UUID actorUserId) {
        if (!GameStatus.ACTIVE.equals(game.getStatus())) {
            throw new InvalidGameActionException("Abilities can only be used during an active game");
        }
        if (!actorUserId.equals(game.getActivePlayerId())) {
            throw new InvalidGameActionException("It is not the acting player's turn");
        }
        if (!TurnPhase.MAIN.equals(game.getCurrentPhase()) && !TurnPhase.ATTACK.equals(game.getCurrentPhase())) {
            throw new InvalidGameActionException("Abilities can only be used before attacking");
        }
    }

    private UseAbilityRequest readRequest(GameActionContext context) {
        Map<String, Object> payload = context.request().payload();
        String rawAbilityCode = String.valueOf(payload.get("abilityCode"));
        if (rawAbilityCode == null || rawAbilityCode.isBlank() || "null".equalsIgnoreCase(rawAbilityCode)) {
            throw new InvalidGameActionException("Payload field 'abilityCode' is required");
        }

        return new UseAbilityRequest(
                payloadReader.requiredUuid(payload, "sourcePokemonId"),
                AbilityCode.valueOf(rawAbilityCode),
                payloadReader.optionalUuid(payload, "selectedHandCardId").orElse(null),
                payloadReader.optionalUuid(payload, "targetPokemonId").orElse(null),
                payloadReader.optionalUuid(payload, "sourceEnergyCardId").orElse(null),
                payloadReader.optionalUuid(payload, "fromPokemonId").orElse(null),
                payloadReader.optionalUuid(payload, "toPokemonId").orElse(null),
                payloadReader.optionalUuid(payload, "selectedDeckCardId").orElse(null));
    }

    private Map<AbilityCode, AbilityEffectHandler> handlersByCode(List<AbilityEffectHandler> handlers) {
        Map<AbilityCode, AbilityEffectHandler> resolvedHandlers = new EnumMap<>(AbilityCode.class);
        for (AbilityEffectHandler handler : handlers) {
            AbilityCode supportedAbility = handler.supports();
            AbilityEffectHandler previousHandler = resolvedHandlers.putIfAbsent(supportedAbility, handler);
            if (previousHandler != null) {
                throw new IllegalStateException("Duplicate handler registered for ability " + supportedAbility);
            }
        }
        return Map.copyOf(resolvedHandlers);
    }
}
