package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VictoryConditionServiceImplTest {

    private final VictoryConditionServiceImpl service = new VictoryConditionServiceImpl();

    @Test
    void attackerWins_whenNoPrizeCardsRemaining() {
        assertThat(service.attackerWinsAfterKnockout(0, true)).isTrue();
    }

    @Test
    void attackerWins_whenDefenderHasNoBenchedPokemon() {
        assertThat(service.attackerWinsAfterKnockout(3, false)).isTrue();
    }

    @Test
    void attackerWins_whenBothConditions() {
        assertThat(service.attackerWinsAfterKnockout(0, false)).isTrue();
    }

    @Test
    void attackerDoesNotWin_whenPrizeCardsRemainingAndDefenderHasBench() {
        assertThat(service.attackerWinsAfterKnockout(1, true)).isFalse();
        assertThat(service.attackerWinsAfterKnockout(6, true)).isFalse();
    }
}
