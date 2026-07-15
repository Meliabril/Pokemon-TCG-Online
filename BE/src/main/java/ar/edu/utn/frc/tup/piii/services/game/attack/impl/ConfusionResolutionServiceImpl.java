package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.ConfusionResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfusionResolutionServiceImpl implements ConfusionResolutionService {

    private static final int CONFUSION_SELF_DAMAGE = 30;

    private final SpecialConditionStateService specialConditionStateService;
    private final GameRandomService gameRandomService;
    private final DamageApplicationService damageApplicationService;
    private final CombatResolutionService combatResolutionService;
    private final GameEventFactory gameEventFactory;

    @Override
    public ConfusionResolutionResult resolveBeforeAttack(
            AttackResolutionContext context,
            int currentTurnNumber,
            int nextTurnNumber,
            int stateVersion) {
        boolean confused = specialConditionStateService
                .findByPokemonInPlayIdAndConditionType(context.attackerPokemon().getId(), SpecialConditionType.CONFUSED)
                .isPresent();
        if (!confused) {
            return ConfusionResolutionResult.canProceed();
        }

        boolean heads = gameRandomService.flipCoin();
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", "CONFUSION_CHECK",
                        "coinResult", coinResult(heads),
                        "pokemonInPlayId", context.attackerPokemon().getId().toString())));
        if (heads) {
            return new ConfusionResolutionResult(true, false, false, null, List.copyOf(events));
        }

        PokemonInPlay attackerPokemon = context.attackerPokemon();
        int damageCounters = damageApplicationService.applyDamage(attackerPokemon, CONFUSION_SELF_DAMAGE);
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", attackerPokemon.getId().toString(),
                        "damage", CONFUSION_SELF_DAMAGE,
                        "damageCounters", damageCounters,
                        "reason", "CONFUSION_SELF_DAMAGE")));

        CombatResolutionService.CombatResolutionResult combatResult = combatResolutionService.resolveKnockoutIfNeeded(
                context.gameId(),
                context.attackerUserId(),
                context.defenderUserId(),
                attackerPokemon,
                context.defenderUserId(),
                nextTurnNumber,
                stateVersion,
                "CONFUSION_SELF_KNOCKOUT");
        events.addAll(combatResult.events());

        return new ConfusionResolutionResult(
                false,
                combatResult.gameFinished(),
                combatResult.promotionPending(),
                combatResult.winnerUserId(),
                List.copyOf(events));
    }

    private String coinResult(boolean heads) {
        if (heads) {
            return "HEADS";
        }

        return "TAILS";
    }
}
