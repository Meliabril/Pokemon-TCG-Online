package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromoteBenchPokemonActionHandler implements GameActionHandler {

    private final PromotionService promotionService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.PROMOTE_BENCH_POKEMON;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return promotionService.promoteBenchPokemon(context);
    }
}
