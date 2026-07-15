package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalOpponentActiveTypeDamageAttackEffectTest {

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAddDamageWhenOpponentActiveMatchesExpectedType() {
        ConditionalOpponentActiveTypeDamageAttackEffect effect = new ConditionalOpponentActiveTypeDamageAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context("Grass"));

        assertThat(result.damageModifier()).isEqualTo(20);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldNotAddDamageWhenOpponentActiveDoesNotMatchExpectedType() {
        ConditionalOpponentActiveTypeDamageAttackEffect effect = new ConditionalOpponentActiveTypeDamageAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context("Fire"));

        assertThat(result.damageModifier()).isZero();
    }

    private AttackEffectContext context(String defenderType) {
        Card defenderCard = new Card();
        defenderCard.setPokemonType(defenderType);

        AttackEffectOperation operation = new AttackEffectOperation(
                "CONDITIONAL_OPPONENT_ACTIVE_TYPE_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                20,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                null,
                "Grass",
                null);
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                attacker(),
                new PokemonInPlay(),
                null,
                defenderCard,
                null);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 1, 1, 0);
    }

    private PokemonInPlay attacker() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        return attacker;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
