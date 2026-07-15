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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BenchDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "BENCH_DAMAGE";
    // Matches the key the FE's generic single-target picker sends for any
    // attack whose operation targets OPPONENT_BENCH (see AttackTargetMode).
    private static final String BENCH_TARGET_POKEMON_IN_PLAY_ID_KEY = "switchTargetPokemonInPlayId";

    private final GameActionPayloadReader payloadReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final DamageApplicationService damageApplicationService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        Optional<UUID> selectedTargetPokemonInPlayId = payloadReader.optionalUuid(
                context.payload(),
                BENCH_TARGET_POKEMON_IN_PLAY_ID_KEY);
        if (selectedTargetPokemonInPlayId.isEmpty()) {
            if (opponentBenchIsEmpty(context)) {
                return AttackEffectResult.empty();
            }
            throw new InvalidGameActionException("Bench damage target is required");
        }

        UUID targetPokemonInPlayId = selectedTargetPokemonInPlayId.get();
        Optional<PokemonInPlay> targetPokemonResult = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonInPlayId,
                context.resolutionContext().gameId(),
                context.resolutionContext().defenderUserId());
        if (targetPokemonResult.isEmpty()) {
            throw new InvalidGameActionException("Bench damage target was not found for the defending player");
        }

        PokemonInPlay targetPokemon = targetPokemonResult.get();
        if (targetPokemon.getSlotPosition() == null || targetPokemon.getSlotPosition() <= 0) {
            throw new InvalidGameActionException("Bench damage target must be on the Bench");
        }

        int damageCounters = damageApplicationService.applyDamage(targetPokemon, context.operation().amount());
        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.DAMAGE_APPLIED,
                context.stateVersion(),
                Map.of(
                        "defenderPokemonInPlayId", targetPokemon.getId().toString(),
                        "damage", context.operation().amount(),
                        "damageCounters", damageCounters,
                        "reason", EFFECT_TYPE));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private boolean opponentBenchIsEmpty(AttackEffectContext context) {
        return pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                context.resolutionContext().defenderUserId())
                .stream()
                .noneMatch(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > 0);
    }
}
