package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.AvailableActionsFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AvailableActionsFactoryImpl implements AvailableActionsFactory {

    @Override
    public List<GameActionType> mainPhaseActions(boolean energyAttachedThisTurn) {
        List<GameActionType> actions = new ArrayList<>();
        actions.add(GameActionType.PLAY_BASIC_POKEMON);
        if (!energyAttachedThisTurn) {
            actions.add(GameActionType.ATTACH_ENERGY);
        }
        actions.add(GameActionType.PLAY_TRAINER);
        actions.add(GameActionType.EVOLVE_POKEMON);
        actions.add(GameActionType.RETREAT);
        actions.add(GameActionType.USE_ABILITY);
        actions.add(GameActionType.DECLARE_ATTACK);
        actions.add(GameActionType.END_TURN);
        return List.copyOf(actions);
    }

    @Override
    public List<GameActionType> mainPhaseActionsAfterRetreat(boolean energyAttachedThisTurn) {
        List<GameActionType> actions = new ArrayList<>();
        actions.add(GameActionType.PLAY_BASIC_POKEMON);
        if (!energyAttachedThisTurn) {
            actions.add(GameActionType.ATTACH_ENERGY);
        }
        actions.add(GameActionType.PLAY_TRAINER);
        actions.add(GameActionType.EVOLVE_POKEMON);
        actions.add(GameActionType.USE_ABILITY);
        actions.add(GameActionType.DECLARE_ATTACK);
        actions.add(GameActionType.END_TURN);
        return List.copyOf(actions);
    }

    @Override
    public List<GameActionType> attackPhaseActions() {
        return List.of(GameActionType.USE_ABILITY, GameActionType.DECLARE_ATTACK, GameActionType.END_TURN);
    }

    @Override
    public List<GameActionType> drawPhaseActions() {
        return List.of(GameActionType.DRAW_CARD);
    }

    @Override
    public List<GameActionType> promotionActions() {
        return List.of(GameActionType.PROMOTE_BENCH_POKEMON);
    }

    @Override
    public List<GameActionType> noActions() {
        return List.of();
    }
}
