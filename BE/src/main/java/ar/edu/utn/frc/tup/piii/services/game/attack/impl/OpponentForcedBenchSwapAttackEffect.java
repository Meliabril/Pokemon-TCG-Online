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
import ar.edu.utn.frc.tup.piii.services.game.effect.SwitchActivePokemonEffectService;
import ar.edu.utn.frc.tup.piii.services.game.effect.impl.SwitchActivePokemonEffectServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OpponentForcedBenchSwapAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "OPPONENT_FORCED_BENCH_SWAP";
    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "switchTargetPokemonInPlayId";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final SwitchActivePokemonEffectService switchActivePokemonEffectService;
    private final GameEventFactory gameEventFactory;
    private final GameActionPayloadReader payloadReader;

    public OpponentForcedBenchSwapAttackEffect(
            PokemonInPlayStateService pokemonInPlayStateService,
            GameCardInstanceStateService gameCardInstanceStateService,
            GameEventFactory gameEventFactory,
            GameActionPayloadReader payloadReader) {
        this(pokemonInPlayStateService, new SwitchActivePokemonEffectServiceImpl(gameCardInstanceStateService, pokemonInPlayStateService), gameEventFactory, payloadReader);
    }

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay activePokemon = context.resolutionContext().defenderPokemon();
        Optional<PokemonInPlay> benchPokemon = selectedOpponentBenchPokemon(context);

        boolean switched = benchPokemon.isPresent();
        if (switched) {
            switchActivePokemonEffectService.switchWithBench(activePokemon, benchPokemon.get());
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "switched", switched,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "targetPlayerId", context.resolutionContext().defenderUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().defenderPokemon().getId().toString(),
                        "newActivePokemonInPlayId", switched ? benchPokemon.get().getId().toString() : "",
                        "benchedPokemonInPlayId", switched ? activePokemon.getId().toString() : ""));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private Optional<PokemonInPlay> selectedOpponentBenchPokemon(AttackEffectContext context) {
        List<PokemonInPlay> opponentBench = pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                        context.resolutionContext().gameId(),
                        context.resolutionContext().defenderUserId())
                .stream()
                .filter(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > ACTIVE_SLOT_POSITION)
                .sorted(Comparator.comparing(PokemonInPlay::getSlotPosition))
                .toList();
        if (opponentBench.isEmpty()) {
            return Optional.empty();
        }

        /*
         * When this is the only target an attack needs, the attacker chooses it
         * explicitly. When it is a secondary target on a multi-target attack
         * (e.g. Rapid Spin already consumes the single own-target slot for its
         * self-switch), the FE has no slot left to send this id, so fall back
         * to the first Benched Pokemon instead of failing the action.
         */
        Optional<UUID> selectedPokemonInPlayId = payloadReader.optionalUuid(context.payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);
        if (selectedPokemonInPlayId.isEmpty()) {
            return Optional.of(opponentBench.get(0));
        }

        PokemonInPlay selectedPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                        selectedPokemonInPlayId.get(),
                        context.resolutionContext().gameId(),
                        context.resolutionContext().defenderUserId())
                .orElseThrow(() -> new InvalidGameActionException("Switch target was not found among opponent Pokemon"));

        if (selectedPokemon.getSlotPosition() == null || selectedPokemon.getSlotPosition() == ACTIVE_SLOT_POSITION) {
            throw new InvalidGameActionException("Switch target must be one of opponent Benched Pokemon");
        }

        return Optional.of(selectedPokemon);
    }

}
