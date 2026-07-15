package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChooseSpecialConditionAttackEffectTest {

    @Test
    void shouldRequireChoiceWithAllowedSpecialConditionsForConversionPowder() {
        ChooseSpecialConditionAttackEffect effect = new ChooseSpecialConditionAttackEffect();
        PokemonInPlay defender = pokemon();

        AttackEffectResult result = effect.apply(context(defender, operation(List.of("asleep", "POISONED"))));

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choiceType()).isEqualTo(ChooseSpecialConditionAttackEffect.CHOICE_TYPE);
        assertThat(result.choicePayload())
                .containsEntry("defenderPokemonInPlayId", defender.getId().toString())
                .containsEntry(ChooseSpecialConditionAttackEffect.CONDITION_TYPES_KEY, List.of("ASLEEP", "POISONED"));
        assertThat(result.damageModifier()).isZero();
        assertThat(result.events()).isEmpty();
    }

    @Test
    void shouldRejectMissingConditionChoices() {
        ChooseSpecialConditionAttackEffect effect = new ChooseSpecialConditionAttackEffect();

        assertThatThrownBy(() -> effect.apply(context(pokemon(), operation(List.of()))))
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void shouldRejectUnsupportedConditionChoices() {
        ChooseSpecialConditionAttackEffect effect = new ChooseSpecialConditionAttackEffect();

        assertThatThrownBy(() -> effect.apply(context(pokemon(), operation(List.of("FROZEN")))))
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void shouldOnlySupportChooseSpecialConditionOperations() {
        ChooseSpecialConditionAttackEffect effect = new ChooseSpecialConditionAttackEffect();

        assertThat(effect.supports(operation(List.of("ASLEEP")))).isTrue();
        assertThat(effect.supports(null)).isFalse();
        assertThat(effect.supports(new AttackEffectOperation(
                "APPLY_SPECIAL_CONDITION",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                "ASLEEP",
                CoinRequirement.NONE,
                false,
                0,
                null))).isFalse();
    }

    private AttackEffectContext context(PokemonInPlay defender, AttackEffectOperation operation) {
        return new AttackEffectContext(
                new AttackResolutionContext(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        pokemon(),
                        defender,
                        card(),
                        card(),
                        attack()),
                defender,
                operation,
                Map.of(),
                5,
                3,
                0);
    }

    private AttackEffectOperation operation(List<String> conditionTypes) {
        return new AttackEffectOperation(
                ChooseSpecialConditionAttackEffect.EFFECT_TYPE,
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                conditionTypes);
    }

    private PokemonInPlay pokemon() {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(UUID.randomUUID());
        return pokemon;
    }

    private Card card() {
        Card card = new Card();
        card.setExternalId("xy1-17");
        card.setName("Vivillon");
        return card;
    }

    private Attack attack() {
        Attack attack = new Attack();
        attack.setName("Conversion Powder");
        return attack;
    }
}
