package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "HEAL_DAMAGE";
    private static final String SELF_TARGET_POKEMON_IN_PLAY_ID_KEY = "selfTargetPokemonInPlayId";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final DamageApplicationService damageApplicationService;
    private final GameEventFactory gameEventFactory;
    private final GameActionPayloadReader payloadReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        if ("ALL_OWN".equals(context.operation().target())) {
            return healAllOwnPokemon(context);
        }

        PokemonInPlay targetPokemon = targetPokemon(context);
        return new AttackEffectResult(0, false, List.of(healEvent(context, targetPokemon)));
    }

    private AttackEffectResult healAllOwnPokemon(AttackEffectContext context) {
        List<PokemonInPlay> ownPokemon = pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId());

        List<GameEventDto> events = new ArrayList<>();
        for (PokemonInPlay pokemonInPlay : ownPokemon) {
            events.add(healEvent(context, pokemonInPlay));
        }
        return new AttackEffectResult(0, false, List.copyOf(events));
    }

    private GameEventDto healEvent(AttackEffectContext context, PokemonInPlay targetPokemon) {
        int amount = context.operation().amount();
        int remainingDamageCounters = damageApplicationService.healDamage(targetPokemon, amount);
        return gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", targetPokemon.getId().toString(),
                        "healedDamage", amount,
                        "remainingDamageCounters", remainingDamageCounters));
    }

    private PokemonInPlay targetPokemon(AttackEffectContext context) {
        String target = context.operation().target();
        if ("DEFENDER".equals(target)) {
            return context.targetPokemon();
        }
        if ("OWN_BENCH".equals(target)) {
            return ownSelectedPokemon(context, true);
        }
        if ("OWN_ANY".equals(target)) {
            return ownSelectedPokemon(context, false);
        }

        return context.resolutionContext().attackerPokemon();
    }

    private PokemonInPlay ownSelectedPokemon(AttackEffectContext context, boolean benchOnly) {
        UUID selectedPokemonInPlayId = payloadReader.requiredUuid(context.payload(), SELF_TARGET_POKEMON_IN_PLAY_ID_KEY);
        PokemonInPlay selectedPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                        selectedPokemonInPlayId,
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId())
                .orElseThrow(() -> new InvalidGameActionException("Heal target was not found among your Pokemon"));

        if (benchOnly && ACTIVE_SLOT_POSITION == selectedPokemon.getSlotPosition()) {
            throw new InvalidGameActionException("Heal target must be one of your Benched Pokemon");
        }

        return selectedPokemon;
    }
}
