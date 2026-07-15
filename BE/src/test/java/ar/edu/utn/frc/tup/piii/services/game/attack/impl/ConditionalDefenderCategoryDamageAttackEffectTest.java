package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalDefenderCategoryDamageAttackEffectTest {

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAddDamageWhenDefenderIsPokemonEx() {
        ConditionalDefenderCategoryDamageAttackEffect effect = new ConditionalDefenderCategoryDamageAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(CardCategory.POKEMON_EX));

        assertThat(result.damageModifier()).isEqualTo(60);
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldTreatMegaPokemonAsPokemonExForDamageBonus() {
        ConditionalDefenderCategoryDamageAttackEffect effect = new ConditionalDefenderCategoryDamageAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(CardCategory.MEGA_POKEMON));

        assertThat(result.damageModifier()).isEqualTo(60);
    }

    @Test
    void shouldNotAddDamageWhenDefenderIsNotPokemonEx() {
        ConditionalDefenderCategoryDamageAttackEffect effect = new ConditionalDefenderCategoryDamageAttackEffect(gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(CardCategory.STAGE_2_POKEMON));

        assertThat(result.damageModifier()).isZero();
    }

    private AttackEffectContext context(CardCategory defenderCategory) {
        Card defenderCard = new Card();
        defenderCard.setCategory(defenderCategory);
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());

        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                attacker,
                null,
                null,
                defenderCard,
                null);
        return new AttackEffectContext(resolutionContext, null, operation(), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "CONDITIONAL_DEFENDER_CATEGORY_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                60,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                null,
                null,
                null,
                "POKEMON_EX");
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
