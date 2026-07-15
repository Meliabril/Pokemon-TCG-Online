package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AvailableActionsFactoryImplTest {

    private final AvailableActionsFactoryImpl factory = new AvailableActionsFactoryImpl();

    @Test
    void shouldExposeEvolutionAndRetreatFromMainPhaseAvailableActions() {
        assertThat(factory.mainPhaseActions(false))
                .contains(GameActionType.EVOLVE_POKEMON, GameActionType.RETREAT, GameActionType.DECLARE_ATTACK);
    }

    @Test
    void shouldStopExposingRetreatAfterPlayerRetreatedThisTurn() {
        assertThat(factory.mainPhaseActionsAfterRetreat(false))
                .contains(GameActionType.EVOLVE_POKEMON, GameActionType.DECLARE_ATTACK)
                .doesNotContain(GameActionType.RETREAT);
    }

    @Test
    void shouldExposeUseAbilityBeforeAttack() {
        assertThat(factory.mainPhaseActions(false)).contains(GameActionType.USE_ABILITY);
        assertThat(factory.mainPhaseActionsAfterRetreat(false)).contains(GameActionType.USE_ABILITY);
        assertThat(factory.attackPhaseActions()).contains(GameActionType.USE_ABILITY);
        assertThat(factory.drawPhaseActions()).doesNotContain(GameActionType.USE_ABILITY);
        assertThat(factory.promotionActions()).doesNotContain(GameActionType.USE_ABILITY);
    }
}
