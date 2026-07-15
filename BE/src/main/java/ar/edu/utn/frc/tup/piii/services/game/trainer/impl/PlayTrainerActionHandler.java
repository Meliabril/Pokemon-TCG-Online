package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayTrainerActionHandler implements GameActionHandler {

    private final TrainerEffectService trainerEffectService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.PLAY_TRAINER;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return trainerEffectService.executeTrainer(context);
    }
}
