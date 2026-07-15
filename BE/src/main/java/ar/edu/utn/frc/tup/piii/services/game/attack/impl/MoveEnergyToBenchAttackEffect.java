package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.effect.MoveEnergyEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Models attacks like Yveltal-EX's Y Cyclone: move one Energy attached to the
 * attacker onto one of the attacker's own Benched Pokemon. The FE's generic
 * single-target picker for OWN_BENCH attacks sends the bench choice as
 * "selfTargetPokemonInPlayId" (same key SELF_SWITCH_WITH_BENCH uses); since
 * there is no second slot for picking which Energy to move, the first Energy
 * found on the attacker is moved automatically.
 */
@Service
@RequiredArgsConstructor
public class MoveEnergyToBenchAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "MOVE_ENERGY_TO_BENCH";
    private static final String BENCH_TARGET_POKEMON_IN_PLAY_ID_KEY = "selfTargetPokemonInPlayId";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final GameActionPayloadReader payloadReader;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameEventFactory gameEventFactory;
    private final MoveEnergyEffectService moveEnergyEffectService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        PokemonAttachedCard energyCard = firstAttachedEnergy(attackerPokemon);
        if (energyCard == null) {
            return AttackEffectResult.empty();
        }

        PokemonInPlay benchTarget = benchTarget(context);
        moveEnergyEffectService.moveEnergy(energyCard, attackerPokemon, benchTarget);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "energyCardInstanceId", energyCard.getGameCardInstance().getId().toString(),
                        "fromPokemonInPlayId", attackerPokemon.getId().toString(),
                        "toPokemonInPlayId", benchTarget.getId().toString(),
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private PokemonAttachedCard firstAttachedEnergy(PokemonInPlay attackerPokemon) {
        return pokemonAttachedCardStateService.findByPokemonInPlayId(attackerPokemon.getId()).stream()
                .filter(this::isEnergy)
                .findFirst()
                .orElse(null);
    }

    private boolean isEnergy(PokemonAttachedCard attachedCard) {
        return attachedCard.getAttachedCardType() == ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType.BASIC_ENERGY
                || attachedCard.getAttachedCardType() == ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType.SPECIAL_ENERGY;
    }

    private PokemonInPlay benchTarget(AttackEffectContext context) {
        UUID benchTargetPokemonInPlayId = payloadReader.requiredUuid(context.payload(), BENCH_TARGET_POKEMON_IN_PLAY_ID_KEY);
        PokemonInPlay benchTarget = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                        benchTargetPokemonInPlayId,
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId())
                .orElseThrow(() -> new InvalidGameActionException("Energy move target was not found among your Pokemon"));
        if (benchTarget.getSlotPosition() == null || benchTarget.getSlotPosition() == ACTIVE_SLOT_POSITION) {
            throw new InvalidGameActionException("Energy move target must be one of your Benched Pokemon");
        }
        return benchTarget;
    }
}
