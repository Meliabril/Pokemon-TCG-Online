package ar.edu.utn.frc.tup.piii.services.game.board.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.board.MainPhaseActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayBasicPokemonActionHandler implements GameActionHandler {

    private final MainPhaseActionService mainPhaseActionService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.PLAY_BASIC_POKEMON;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return mainPhaseActionService.playBasicPokemon(context);
    }
}
