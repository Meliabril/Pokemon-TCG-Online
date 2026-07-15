package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.MentalPanicService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MentalPanicServiceImpl implements MentalPanicService {

    private static final String EFFECT_TYPE = "MENTAL_PANIC";

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public MentalPanicResult resolveBeforeAttack(
            PokemonInPlay pokemonInPlay,
            int currentTurnNumber,
            UUID gameId,
            int stateVersion) {
        Integer lockedTurn = pokemonInPlay.getMentalPanicTurn();
        if (lockedTurn == null || lockedTurn != currentTurnNumber) {
            return new MentalPanicResult(true, List.of());
        }

        boolean heads = gameRandomService.flipCoin();
        pokemonInPlay.setMentalPanicTurn(null);
        pokemonInPlayStateService.save(pokemonInPlay);

        GameEventDto event = gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", pokemonInPlay.getId().toString(),
                        "actorPlayerId", pokemonInPlay.getOwnerUserId().toString(),
                        "coinResults", List.of(heads ? "HEADS" : "TAILS"),
                        "attackCancelled", !heads));
        return new MentalPanicResult(heads, List.of(event));
    }

    @Override
    public void expireLock(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer lockedTurn = pokemonInPlay.getMentalPanicTurn();
        if (lockedTurn == null || lockedTurn > currentTurnNumber) {
            return;
        }

        pokemonInPlay.setMentalPanicTurn(null);
        pokemonInPlayStateService.save(pokemonInPlay);
    }
}
