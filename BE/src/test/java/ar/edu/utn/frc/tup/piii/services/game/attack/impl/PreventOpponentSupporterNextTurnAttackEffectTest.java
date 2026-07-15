package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.trainer.SupporterLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreventOpponentSupporterNextTurnAttackEffectTest {

    @Mock
    private SupporterLockService supporterLockService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldLockDefenderSupportersForNextTurn() {
        PreventOpponentSupporterNextTurnAttackEffect effect =
                new PreventOpponentSupporterNextTurnAttackEffect(supporterLockService, gameEventFactory);
        UUID gameId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(3), any()))
                .thenReturn(event(gameId));

        AttackEffectResult result = effect.apply(context(gameId, defenderUserId));

        verify(supporterLockService).lockSupporters(gameId, defenderUserId, 6);
        assertThat(result.damageModifier()).isZero();
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    private AttackEffectContext context(UUID gameId, UUID defenderUserId) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId, UUID.randomUUID(), defenderUserId, null, null, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "PREVENT_OPPONENT_SUPPORTER_NEXT_TURN",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.HEADS,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 3, 5, 0);
    }

    private GameEventDto event(UUID gameId) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 3, false, Instant.now(), Map.of());
    }
}
