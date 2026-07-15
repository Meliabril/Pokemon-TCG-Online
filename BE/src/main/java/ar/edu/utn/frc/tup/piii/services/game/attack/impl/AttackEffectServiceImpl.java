package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AttackEffectServiceImpl implements AttackEffectService {

    private final AttackEffectDefinitionReader attackEffectDefinitionReader;
    private final List<AttackEffect> attackEffects;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    public AttackEffectServiceImpl(
            AttackEffectDefinitionReader attackEffectDefinitionReader,
            List<AttackEffect> attackEffects,
            GameRandomService gameRandomService,
            GameEventFactory gameEventFactory) {
        this.attackEffectDefinitionReader = attackEffectDefinitionReader;
        if (attackEffects == null) {
            this.attackEffects = List.of();
        } else {
            this.attackEffects = List.copyOf(attackEffects);
        }
        this.gameRandomService = gameRandomService;
        this.gameEventFactory = gameEventFactory;
    }

    @Override
    public AttackEffectDefinition definitionFor(AttackResolutionContext context) {
        return attackEffectDefinitionReader.read(context.attackerCard(), context.selectedAttack());
    }

    @Override
    public AttackEffectResult applyBeforeDamage(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int turnNumber,
            int stateVersion) {
        return applyEffects(context, definition, targetPokemon, payload, stateVersion, turnNumber, 0, AttackEffectPhase.BEFORE_DAMAGE);
    }

    @Override
    public AttackEffectResult applyAfterDamage(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int turnNumber,
            int stateVersion,
            int currentDamage) {
        return applyEffects(context, definition, targetPokemon, payload, stateVersion, turnNumber, currentDamage, AttackEffectPhase.AFTER_DAMAGE);
    }

    private AttackEffectResult applyEffects(
            AttackResolutionContext context,
            AttackEffectDefinition definition,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int stateVersion,
            int turnNumber,
            int currentDamage,
            AttackEffectPhase phase) {
        if (definition == null || definition.isEmpty()) {
            return AttackEffectResult.empty();
        }

        int damageModifier = 0;
        boolean attackCancelled = false;
        boolean choiceRequired = false;
        boolean ignoreWeakness = false;
        boolean ignoreResistance = false;
        boolean ignoreDefenderEffects = false;
        String choiceType = null;
        Map<String, Object> choicePayload = Map.of();
        UUID choicePlayerId = null;
        List<GameEventDto> events = new ArrayList<>();
        Map<String, Boolean> groupCoinResults = new HashMap<>();
        boolean gameFinished = false;
        boolean promotionPending = false;
        UUID winnerUserId = null;

        for (AttackEffectOperation operation : definition.operations()) {
            if (operation == null || !phase.equals(operation.phase())) {
                continue;
            }

            CoinRequirement coinRequirement = coinRequirement(operation);
            if (coinRequirement != CoinRequirement.NONE) {
                String groupKey = operation.coinGroupKey();
                Boolean groupResult = groupKey == null ? null : groupCoinResults.get(groupKey);
                boolean heads;
                if (groupResult != null) {
                    heads = groupResult;
                } else {
                    heads = gameRandomService.flipCoin();
                    events.add(coinFlipEvent(context, operation, stateVersion, heads));
                    if (groupKey != null) {
                        groupCoinResults.put(groupKey, heads);
                    }
                }
                if ((coinRequirement == CoinRequirement.HEADS) != heads) {
                    if (coinRequirement == CoinRequirement.HEADS && operation.cancelOnTails()) {
                        attackCancelled = true;
                    }
                    continue;
                }
            }

            AttackEffect attackEffect = effectFor(operation);
            AttackEffectContext effectContext = new AttackEffectContext(
                    context,
                    targetPokemon,
                    operation,
                    payload,
                    stateVersion,
                    turnNumber,
                    currentDamage);
            AttackEffectResult result = attackEffect.apply(effectContext);
            if (result == null) {
                continue;
            }
            damageModifier = damageModifier + result.damageModifier();
            if (result.attackCancelled()) {
                attackCancelled = true;
            }
            if (result.choiceRequired()) {
                choiceRequired = true;
                choiceType = result.choiceType();
                choicePayload = result.choicePayload();
                choicePlayerId = result.choicePlayerId();
            }
            if (result.ignoreWeakness()) {
                ignoreWeakness = true;
            }
            if (result.ignoreResistance()) {
                ignoreResistance = true;
            }
            if (result.ignoreDefenderEffects()) {
                ignoreDefenderEffects = true;
            }
            events.addAll(result.events());

            if (result.gameFinished() || result.promotionPending()) {
                // An effect resolved a knockout on a Pokemon other than the attack's primary
                // target (e.g. a bench Pokemon hit by an "all opponent Pokemon" damage-counter
                // effect) that finished the game or now requires a promotion. The caller cannot
                // safely keep applying further operations from this same definition on top of
                // that - surface the outcome immediately, the same way the primary target's own
                // post-damage knockout is surfaced by AttackServiceImpl.
                gameFinished = result.gameFinished();
                promotionPending = result.promotionPending();
                winnerUserId = result.winnerUserId();
                break;
            }
        }

        return new AttackEffectResult(
                damageModifier,
                attackCancelled,
                List.copyOf(events),
                choiceRequired,
                choiceType,
                choicePayload,
                ignoreWeakness,
                ignoreResistance,
                ignoreDefenderEffects,
                gameFinished,
                promotionPending,
                winnerUserId,
                choicePlayerId);
    }

    private CoinRequirement coinRequirement(AttackEffectOperation operation) {
        return operation.coinRequirement() == null ? CoinRequirement.NONE : operation.coinRequirement();
    }

    private GameEventDto coinFlipEvent(
            AttackResolutionContext context,
            AttackEffectOperation operation,
            int stateVersion,
            boolean heads) {
        return gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", "COIN_FLIP",
                        "operationType", operation.type(),
                        "coinResults", List.of(heads ? "HEADS" : "TAILS"),
                        "actorPlayerId", context.attackerUserId().toString(),
                        "pokemonInPlayId", context.attackerPokemon().getId().toString()));
    }

    private AttackEffect effectFor(AttackEffectOperation operation) {
        for (AttackEffect attackEffect : attackEffects) {
            if (attackEffect.supports(operation)) {
                return attackEffect;
            }
        }

        throw new InvalidGameActionException("Unsupported attack effect operation: " + operation.type());
    }
}
