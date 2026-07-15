package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityEffectHandler;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.effect.SwitchActivePokemonEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriveOffAbilityHandler implements AbilityEffectHandler {

    private static final int ACTIVE_SLOT_POSITION = 0;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final SwitchActivePokemonEffectService switchActivePokemonEffectService;
    private final GameEventFactory gameEventFactory;

    @Override
    public AbilityCode supports() {
        return AbilityCode.DRIVE_OFF;
    }

    @Override
    public AbilityResolution resolve(
            GameActionContext context,
            Game game,
            UUID playerId,
            PokemonInPlay sourcePokemon,
            UseAbilityRequest request,
            int stateVersion) {
        PokemonInPlay opponentActive = pokemonInPlayStateService.findByGameIdOrdered(context.gameId()).stream()
                .filter(pokemon -> !playerId.equals(pokemon.getOwnerUserId()) && Integer.valueOf(0).equals(pokemon.getSlotPosition()))
                .findFirst()
                .orElseThrow(() -> new InvalidGameActionException("Opponent active Pokemon was not found"));

        List<PokemonInPlay> opponentBench = pokemonInPlayStateService.findByGameIdOrdered(context.gameId()).stream()
                .filter(pokemon -> !playerId.equals(pokemon.getOwnerUserId()) && pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > ACTIVE_SLOT_POSITION)
                .sorted(Comparator.comparing(PokemonInPlay::getSlotPosition))
                .toList();
        if (opponentBench.isEmpty()) {
            throw new InvalidGameActionException("Drive Off cannot be used if the opponent has no Benched Pokemon");
        }

        PokemonInPlay selectedPokemon = request.targetPokemonId() == null
                ? opponentBench.get(0)
                : opponentBench.stream()
                        .filter(pokemon -> pokemon.getId().equals(request.targetPokemonId()))
                        .findFirst()
                        .orElseThrow(() -> new InvalidGameActionException("Selected switch target was not found among opponent Benched Pokemon"));
        switchActivePokemonEffectService.switchWithBench(opponentActive, selectedPokemon);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectType", AbilityCode.DRIVE_OFF.name());
        payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        payload.put("actorPlayerId", playerId.toString());
        payload.put("targetPlayerId", opponentActive.getOwnerUserId().toString());
        payload.put("newActivePokemonInPlayId", selectedPokemon.getId().toString());
        payload.put("benchedPokemonInPlayId", opponentActive.getId().toString());
        return new AbilityResolution(
                false,
                false,
                null,
                List.of(gameEventFactory.publicEvent(context.gameId(), GameEventType.ATTACK_EFFECT_RESOLVED, stateVersion, Map.copyOf(payload))));
    }
}
