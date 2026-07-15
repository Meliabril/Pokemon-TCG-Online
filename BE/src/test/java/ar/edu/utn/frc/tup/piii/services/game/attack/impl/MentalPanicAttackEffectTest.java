package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
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
class MentalPanicAttackEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldSetMentalPanicTurnOnTargetPokemon() {
        MentalPanicAttackEffect effect = new MentalPanicAttackEffect(pokemonInPlayStateService, gameEventFactory);
        PokemonInPlay defender = new PokemonInPlay();
        defender.setId(UUID.randomUUID());
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(defender, 7));

        assertThat(defender.getMentalPanicTurn()).isEqualTo(8);
        assertThat(result.damageModifier()).isZero();
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
        verify(pokemonInPlayStateService).save(defender);
    }

    private AttackEffectContext context(PokemonInPlay defender, int turnNumber) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                defender,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, defender, operation(), Map.of(), 1, turnNumber, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "MENTAL_PANIC",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
