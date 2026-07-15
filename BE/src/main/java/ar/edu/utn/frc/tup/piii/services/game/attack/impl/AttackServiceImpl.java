package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEnergyRequirementService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackLockService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContextFactory;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackTargetResolverService;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.ConfusionResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageProtectionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationRequest;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculatorService;
import ar.edu.utn.frc.tup.piii.services.game.attack.MentalPanicService;
import ar.edu.utn.frc.tup.piii.services.game.attack.OutgoingDamageReductionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.PokemonToolModifierService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttackServiceImpl implements AttackService {

    private final AttackResolutionContextFactory attackResolutionContextFactory;
    private final AttackTargetResolverService attackTargetResolverService;
    private final AttackEnergyRequirementService attackEnergyRequirementService;
    private final ConfusionResolutionService confusionResolutionService;
    private final AttackEffectService attackEffectService;
    private final DamageCalculatorService damageCalculatorService;
    private final DamageApplicationService damageApplicationService;
    private final DamageProtectionService damageProtectionService;
    private final AttackLockService attackLockService;
    private final MentalPanicService mentalPanicService;
    private final OutgoingDamageReductionService outgoingDamageReductionService;
    private final PokemonToolModifierService pokemonToolModifierService;
    private final CombatResolutionService combatResolutionService;
    private final BetweenTurnsResolutionService betweenTurnsResolutionService;
    private final PassiveAbilityService passiveAbilityService;
    private final CardService cardService;
    private final GameLookupService gameLookupService;
    private final GameStateQueryService gameStateQueryService;
    private final GameEventFactory gameEventFactory;
    private final AbilityUsageTracker abilityUsageTracker;

    @Override
    public GameActionExecutionResult declareAttack(GameActionContext context) {
        GameStateDto currentState = context.currentState();
        int newStateVersion = currentState.stateVersion() + 1;
        int currentTurnNumber = currentState.turn().turnNumber();
        int nextTurnNumber = currentTurnNumber + 1;

        AttackResolutionContext resolutionContext = attackResolutionContextFactory.create(context);
        if (attackLockService.consumeLock(resolutionContext.attackerPokemon(), currentTurnNumber)) {
            List<GameEventDto> lockEvents = new ArrayList<>();
            lockEvents.add(gameEventFactory.publicEvent(
                    resolutionContext.gameId(),
                    GameEventType.ATTACK_EFFECT_RESOLVED,
                    newStateVersion,
                    Map.of(
                            "effectType", "ATTACK_LOCKED",
                            "pokemonInPlayId", resolutionContext.attackerPokemon().getId().toString())));
            return resultFromCurrentGame(context, newStateVersion, lockEvents);
        }
        if (consumeBlockedAttack(resolutionContext.attackerPokemon(), resolutionContext.selectedAttack().getAttackOrder(), currentTurnNumber)) {
            List<GameEventDto> lockEvents = new ArrayList<>();
            lockEvents.add(gameEventFactory.publicEvent(
                    resolutionContext.gameId(),
                    GameEventType.ATTACK_EFFECT_RESOLVED,
                    newStateVersion,
                    Map.of(
                            "effectType", "BLOCK_ATTACK",
                            "pokemonInPlayId", resolutionContext.attackerPokemon().getId().toString(),
                            "attackOrder", resolutionContext.selectedAttack().getAttackOrder())));
            return finishAttackTurn(
                    context,
                    resolutionContext.attackerUserId(),
                    resolutionContext.defenderUserId(),
                    newStateVersion,
                    lockEvents);
        }

        if (!attackEnergyRequirementService.hasRequiredEnergy(
                resolutionContext.attackerPokemon(),
                resolutionContext.selectedAttack())) {
            throw new InvalidGameActionException("Active Pokemon does not have the required energy for the selected attack");
        }

        AttackEffectDefinition effectDefinition = attackEffectService.definitionFor(resolutionContext);
        PokemonInPlay targetPokemon = attackTargetResolverService.resolveTarget(
                resolutionContext,
                effectDefinition,
                context.request().payload());
        Card targetCard = cardService.getCardEntityById(targetPokemon.getActiveCardInstance().getCardId());

        List<GameEventDto> events = new ArrayList<>();
        events.add(attackDeclaredEvent(resolutionContext, targetPokemon, newStateVersion));

        MentalPanicService.MentalPanicResult mentalPanicResult = mentalPanicService.resolveBeforeAttack(
                resolutionContext.attackerPokemon(),
                currentTurnNumber,
                resolutionContext.gameId(),
                newStateVersion);
        events.addAll(mentalPanicResult.events());
        if (!mentalPanicResult.attackCanProceed()) {
            return finishAttackTurn(
                    context,
                    resolutionContext.attackerUserId(),
                    resolutionContext.defenderUserId(),
                    newStateVersion,
                    events);
        }

        ConfusionResolutionService.ConfusionResolutionResult confusionResult =
                confusionResolutionService.resolveBeforeAttack(
                        resolutionContext,
                        currentTurnNumber,
                        nextTurnNumber,
                        newStateVersion);
        events.addAll(confusionResult.events());
        if (!confusionResult.attackCanProceed()) {
            if (confusionResult.gameFinished() || confusionResult.promotionPending()) {
                return resultFromCurrentGame(context, newStateVersion, events);
            }
            return finishAttackTurn(
                    context,
                    resolutionContext.attackerUserId(),
                    resolutionContext.defenderUserId(),
                    newStateVersion,
                    events);
        }

        AttackEffectResult beforeDamageEffects = attackEffectService.applyBeforeDamage(
                resolutionContext,
                effectDefinition,
                targetPokemon,
                context.request().payload(),
                currentTurnNumber,
                newStateVersion);
        events.addAll(beforeDamageEffects.events());
        if (beforeDamageEffects.attackCancelled()) {
            return finishAttackTurn(
                    context,
                    resolutionContext.attackerUserId(),
                    resolutionContext.defenderUserId(),
                    newStateVersion,
                    events);
        }
        if (beforeDamageEffects.choiceRequired()) {
            return createAttackChoiceResolution(
                    context,
                    resolutionContext,
                    beforeDamageEffects.choiceType(),
                    beforeDamageEffects.choicePayload(),
                    beforeDamageEffects.choicePlayerId(),
                    nextTurnNumber,
                    newStateVersion,
                    events);
        }
        if (beforeDamageEffects.gameFinished() || beforeDamageEffects.promotionPending()) {
            return resultFromCurrentGame(context, newStateVersion, events);
        }

        AttackDamageResult damageResult = resolveAttackDamage(
                context,
                gameLookupService.getRequiredGame(context.gameId()),
                resolutionContext,
                targetPokemon,
                targetCard,
                beforeDamageEffects,
                currentTurnNumber,
                newStateVersion);
        events.addAll(damageResult.events());

        PassiveAbilityService.PassiveAbilityResolution passiveResolution = passiveAbilityService.applyAfterAttackDamage(
                gameLookupService.getRequiredGame(context.gameId()),
                resolutionContext.attackerUserId(),
                resolutionContext.defenderUserId(),
                resolutionContext.attackerPokemon(),
                targetPokemon,
                damageResult.finalDamage(),
                nextTurnNumber,
                newStateVersion);
        events.addAll(passiveResolution.events());
        if (passiveResolution.gameFinished() || passiveResolution.promotionPending()) {
            return resultFromCurrentGame(context, newStateVersion, events);
        }

        AttackEffectResult afterDamageEffects = attackEffectService.applyAfterDamage(
                resolutionContext,
                effectDefinition,
                targetPokemon,
                context.request().payload(),
                currentTurnNumber,
                newStateVersion,
                damageResult.finalDamage());
        events.addAll(afterDamageEffects.events());
        if (afterDamageEffects.gameFinished() || afterDamageEffects.promotionPending()) {
            // A damage-counter effect already knocked out (and fully resolved prizes for) a
            // Pokemon other than this attack's primary target - e.g. a benched Pokemon hit by an
            // "all opponent Pokemon" effect - and the game has finished or is now waiting on a
            // promotion because of it. The primary target's own knockout (handled below by
            // evaluateKnockoutAfterDamage) must not be evaluated against a game state that has
            // already moved on.
            return resultFromCurrentGame(context, newStateVersion, events);
        }

        CombatResolutionService.CombatResolutionResult targetResolution = evaluateKnockoutAfterDamage(
                context,
                resolutionContext,
                targetPokemon,
                nextTurnNumber,
                newStateVersion);
        events.addAll(targetResolution.events());
        if (targetResolution.gameFinished() || targetResolution.promotionPending()) {
            return resultFromCurrentGame(context, newStateVersion, events);
        }

        if (afterDamageEffects.choiceRequired()) {
            return createAttackChoiceResolution(
                    context,
                    resolutionContext,
                    afterDamageEffects.choiceType(),
                    afterDamageEffects.choicePayload(),
                    afterDamageEffects.choicePlayerId(),
                    nextTurnNumber,
                    newStateVersion,
                    events);
        }

        return finishAttackTurn(
                context,
                resolutionContext.attackerUserId(),
                resolutionContext.defenderUserId(),
                newStateVersion,
                events);
    }

    private GameActionExecutionResult createAttackChoiceResolution(
            GameActionContext context,
            AttackResolutionContext resolutionContext,
            String choiceType,
            Map<String, Object> choicePayload,
            UUID choicePlayerId,
            int nextTurnNumber,
            int stateVersion,
            List<GameEventDto> events) {
        UUID resolvingPlayerId = choicePlayerId != null ? choicePlayerId : resolutionContext.attackerUserId();
        Game game = gameLookupService.getRequiredGame(context.gameId());
        game.setCurrentPhase(TurnPhase.BETWEEN_TURNS);
        game.setResolutionState(attackChoiceResolutionMap(
                resolvingPlayerId,
                resolutionContext.attackerUserId(),
                resolutionContext.defenderUserId(),
                choiceType,
                choicePayload,
                nextTurnNumber));

        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.ATTACK_CHOICE_REQUIRED,
                stateVersion,
                Map.of(
                        "playerId", resolvingPlayerId.toString(),
                        "choiceType", choiceType,
                        "choicePayload", choicePayload == null ? Map.of() : choicePayload)));
        return resultFromCurrentGame(context, stateVersion, events);
    }

    private Map<String, Object> attackChoiceResolutionMap(
            UUID resolvingPlayerId,
            UUID turnEndingPlayerId,
            UUID nextActivePlayerId,
            String choiceType,
            Map<String, Object> choicePayload,
            int nextTurnNumber) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(ResolutionStateDto.RESOLUTION_TYPE_KEY, ResolutionStateDto.ATTACK_CHOICE_REQUIRED);
        values.put(ResolutionStateDto.PENDING_CHOICE_PLAYER_ID_KEY, resolvingPlayerId.toString());
        values.put(ResolutionStateDto.PENDING_CHOICE_TYPE_KEY, choiceType);
        values.put(ResolutionStateDto.PENDING_CHOICE_PAYLOAD_KEY, choicePayload == null ? Map.of() : choicePayload);
        values.put(ResolutionStateDto.NEXT_ACTIVE_PLAYER_ID_KEY, nextActivePlayerId.toString());
        values.put(ResolutionStateDto.NEXT_TURN_NUMBER_KEY, nextTurnNumber);
        values.put(ResolutionStateDto.TURN_ENDING_PLAYER_ID_KEY, turnEndingPlayerId.toString());
        return Map.copyOf(values);
    }

    private boolean consumeBlockedAttack(PokemonInPlay pokemonInPlay, int attackOrder, int currentTurnNumber) {
        Integer blockedTurn = pokemonInPlay.getBlockedAttackTurn();
        Integer blockedOrder = pokemonInPlay.getBlockedAttackOrder();
        if (blockedTurn == null || blockedOrder == null) {
            return false;
        }
        if (blockedTurn != currentTurnNumber || blockedOrder != attackOrder) {
            if (blockedTurn <= currentTurnNumber) {
                pokemonInPlay.setBlockedAttackTurn(null);
                pokemonInPlay.setBlockedAttackOrder(null);
            }
            return false;
        }
        pokemonInPlay.setBlockedAttackTurn(null);
        pokemonInPlay.setBlockedAttackOrder(null);
        return true;
    }

    private GameEventDto attackDeclaredEvent(
            AttackResolutionContext context,
            PokemonInPlay targetPokemon,
            int stateVersion) {
        return gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.ATTACK_DECLARED,
                stateVersion,
                Map.of(
                        "playerId", context.attackerUserId().toString(),
                        "attackId", context.selectedAttack().getId().toString(),
                        "attackerPokemonInPlayId", context.attackerPokemon().getId().toString(),
                        "defenderPokemonInPlayId", targetPokemon.getId().toString()));
    }

    private AttackDamageResult resolveAttackDamage(
            GameActionContext context,
            Game game,
            AttackResolutionContext resolutionContext,
            PokemonInPlay targetPokemon,
            Card targetCard,
            AttackEffectResult beforeDamageEffects,
            int currentTurnNumber,
            int stateVersion) {
        int toolAttackerBonus = pokemonToolModifierService.getOutgoingDamageBonus(resolutionContext.attackerPokemon());
        int toolDefenderModifier = pokemonToolModifierService.getIncomingDamageModifier(targetPokemon);
        DamageCalculationResult damageResult = damageCalculatorService.calculateDamage(new DamageCalculationRequest(
                game,
                resolutionContext.selectedAttack(),
                resolutionContext.attackerCard(),
                targetCard,
                targetPokemon,
                beforeDamageEffects.damageModifier()
                        + toolAttackerBonus
                        - outgoingDamageReductionService.consumeReduction(resolutionContext.attackerPokemon(), currentTurnNumber),
                toolDefenderModifier,
                beforeDamageEffects.ignoreWeakness(),
                beforeDamageEffects.ignoreResistance(),
                beforeDamageEffects.ignoreDefenderEffects()));

        List<GameEventDto> events = new ArrayList<>();
        int finalDamage = damageResult.finalDamage();
        if (!beforeDamageEffects.ignoreDefenderEffects()
                && damageProtectionService.consumeProtection(targetPokemon, currentTurnNumber, finalDamage)) {
            finalDamage = 0;
            events.add(gameEventFactory.publicEvent(
                    context.gameId(),
                    GameEventType.ATTACK_EFFECT_RESOLVED,
                    stateVersion,
                    Map.of(
                            "effectType", "DAMAGE_PREVENTED",
                            "pokemonInPlayId", targetPokemon.getId().toString())));
        }

        int damageCounters = damageApplicationService.applyDamage(targetPokemon, finalDamage);
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", targetPokemon.getId().toString(),
                        "damage", finalDamage,
                        "damageCounters", damageCounters)));
        return new AttackDamageResult(finalDamage, events);
    }

    private CombatResolutionService.CombatResolutionResult evaluateKnockoutAfterDamage(
            GameActionContext context,
            AttackResolutionContext resolutionContext,
            PokemonInPlay targetPokemon,
            int nextTurnNumber,
            int stateVersion) {
        return combatResolutionService.resolveKnockoutIfNeeded(
                context.gameId(),
                targetPokemon.getOwnerUserId(),
                resolutionContext.attackerUserId(),
                targetPokemon,
                resolutionContext.defenderUserId(),
                nextTurnNumber,
                stateVersion,
                "ATTACK_DAMAGE");
    }

    @Override
    public GameActionExecutionResult finishAttackTurn(
            GameActionContext context,
            UUID attackerUserId,
            UUID defenderUserId,
            int stateVersion,
            List<GameEventDto> events) {
        int currentTurnNumber = context.currentState().turn().turnNumber();
        int nextTurnNumber = currentTurnNumber + 1;
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.PHASE_CHANGED,
                stateVersion,
                Map.of(
                        "phase", TurnPhase.BETWEEN_TURNS.name(),
                        "activePlayerId", attackerUserId.toString())));

        BetweenTurnsResolutionService.BetweenTurnsResolutionResult betweenTurnsResult =
                betweenTurnsResolutionService.resolveBetweenTurns(
                        context.gameId(),
                        attackerUserId,
                        defenderUserId,
                        currentTurnNumber,
                        nextTurnNumber,
                        stateVersion);
        events.addAll(betweenTurnsResult.events());
        if (betweenTurnsResult.gameFinished() || betweenTurnsResult.promotionPending()) {
            return resultFromCurrentGame(context, stateVersion, events);
        }

        Game game = gameLookupService.getRequiredGame(context.gameId());
        game.setActivePlayerId(defenderUserId);
        game.setCurrentPhase(TurnPhase.DRAW);
        game.setTurnNumber(nextTurnNumber);
        abilityUsageTracker.clearTurnUsage(game);
        game.setResolutionState(Map.of());

        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.PHASE_CHANGED,
                stateVersion,
                Map.of(
                        "phase", TurnPhase.DRAW.name(),
                        "activePlayerId", defenderUserId.toString())));
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.TURN_STARTED,
                stateVersion,
                Map.of(
                        "playerId", defenderUserId.toString(),
                        "turnNumber", nextTurnNumber)));

        return resultFromCurrentGame(context, stateVersion, events);
    }

    private GameActionExecutionResult resultFromCurrentGame(
            GameActionContext context,
            int stateVersion,
            List<GameEventDto> events) {
        Game game = gameLookupService.getRequiredGame(context.gameId());
        GameStateDto state = gameStateQueryService.buildVisibleState(game).toBuilder()
                .stateVersion(stateVersion)
                .updatedAt(Instant.now())
                .build();
        return new GameActionExecutionResult(state, List.copyOf(events));
    }

    private record AttackDamageResult(int finalDamage, List<GameEventDto> events) {
    }
}
