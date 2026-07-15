package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SelfSwitchWithBenchAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "SELF_SWITCH_WITH_BENCH";
    private static final String SELF_TARGET_POKEMON_IN_PLAY_ID_KEY = "selfTargetPokemonInPlayId";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;
    private final GameActionPayloadReader payloadReader;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay activePokemon = context.resolutionContext().attackerPokemon();
        Optional<PokemonInPlay> benchPokemon = selectedBenchPokemon(context);

        boolean switched = benchPokemon.isPresent();
        if (switched) {
            switchWithBench(activePokemon, benchPokemon.get());
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "switched", switched,
                        "newActivePokemonInPlayId", switched ? benchPokemon.get().getId().toString() : "",
                        "benchedPokemonInPlayId", switched ? activePokemon.getId().toString() : "",
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private Optional<PokemonInPlay> selectedBenchPokemon(AttackEffectContext context) {
        boolean hasOwnBench = pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId())
                .stream()
                .anyMatch(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > ACTIVE_SLOT_POSITION);
        if (!hasOwnBench) {
            return Optional.empty();
        }

        UUID selectedPokemonInPlayId = payloadReader.requiredUuid(context.payload(), SELF_TARGET_POKEMON_IN_PLAY_ID_KEY);
        PokemonInPlay selectedPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                        selectedPokemonInPlayId,
                        context.resolutionContext().gameId(),
                        context.resolutionContext().attackerUserId())
                .orElseThrow(() -> new InvalidGameActionException("Switch target was not found among your Pokemon"));

        if (selectedPokemon.getSlotPosition() == null || selectedPokemon.getSlotPosition() == ACTIVE_SLOT_POSITION) {
            throw new InvalidGameActionException("Switch target must be one of your Benched Pokemon");
        }

        return Optional.of(selectedPokemon);
    }

    private void switchWithBench(PokemonInPlay activePokemon, PokemonInPlay benchPokemon) {
        int formerBenchSlot = benchPokemon.getSlotPosition();
        GameCardInstance activeCardInstance = activePokemon.getActiveCardInstance();
        GameCardInstance benchCardInstance = benchPokemon.getActiveCardInstance();
        gameCardInstanceStateService.swapActiveWithBench(
                activeCardInstance.getId(),
                benchCardInstance.getId(),
                formerBenchSlot);
        pokemonInPlayStateService.swapActiveWithBench(
                activePokemon.getId(),
                benchPokemon.getId(),
                formerBenchSlot);
    }
}
