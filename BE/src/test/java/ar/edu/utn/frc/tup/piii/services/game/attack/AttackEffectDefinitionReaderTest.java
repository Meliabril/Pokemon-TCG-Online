package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.impl.AttackEffectDefinitionReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttackEffectDefinitionReaderTest {

    private AttackEffectDefinitionReader reader;

    @BeforeEach
    void setUp() {
        reader = new AttackEffectDefinitionReader(new ObjectMapper());
    }

    @Test
    void shouldMatchDefinitionByCardExternalIdAndAttackName() {
        Card card = cardOf("xy1-23");
        AttackEffectDefinition definition = reader.read(card, attack(card, "Flamethrower", 0));

        assertThat(definition.isEmpty()).isFalse();
        assertThat(definition.cardExternalId()).isEqualTo("xy1-23");
        assertThat(definition.attackOrder()).isEqualTo(1);
        assertThat(definition.attackName()).isEqualTo("Flamethrower");
        assertThat(definition.operations())
                .singleElement()
                .satisfies(operation -> {
                    assertThat(operation.type()).isEqualTo("DISCARD_ENERGY");
                    assertThat(operation.energyType()).isEqualTo("Fire");
                });
    }

    @Test
    void shouldPreferAttackNameOverAttackOrder() {
        Card card = cardOf("xy1-23");
        Attack attack = attack(card, "Flamethrower", 0);
        attack.setEffectText("Discard a Fire Energy attached to this Pokemon.");

        AttackEffectDefinition definition = reader.read(card, attack);

        assertThat(definition.isEmpty()).isFalse();
        assertThat(definition.attackOrder()).isEqualTo(1);
        assertThat(definition.attackName()).isEqualTo("Flamethrower");
        assertThat(definition.operations())
                .singleElement()
                .satisfies(operation -> assertThat(operation.type()).isEqualTo("DISCARD_ENERGY"));
    }

    @Test
    void shouldKeepRepeatedAttackNamesSeparatedByCardAndName() {
        Card inkay = cardOf("xy1-75");
        Card malamar = cardOf("xy1-77");
        AttackEffectDefinition inkayPuncture = reader.read(inkay, attack(inkay, "Puncture", 1));
        AttackEffectDefinition malamarPuncture = reader.read(malamar, attack(malamar, "Puncture", 1));

        assertThat(inkayPuncture.isEmpty()).isFalse();
        assertThat(malamarPuncture.isEmpty()).isFalse();
        assertThat(inkayPuncture.cardExternalId()).isEqualTo("xy1-75");
        assertThat(malamarPuncture.cardExternalId()).isEqualTo("xy1-77");
        assertThat(inkayPuncture.operations())
                .singleElement()
                .satisfies(operation -> assertThat(operation.type()).isEqualTo("IGNORE_RESISTANCE"));
        assertThat(malamarPuncture.operations())
                .singleElement()
                .satisfies(operation -> assertThat(operation.type()).isEqualTo("IGNORE_RESISTANCE"));
    }

    @Test
    void shouldReturnEmptyDefinitionForUnlistedAttackOrderWithBlankEffectText() {
        Attack attack = attack("Tackle", 0);
        attack.setEffectText(" ");

        AttackEffectDefinition definition = reader.read(cardOf("xy1-7"), attack);

        assertThat(definition.isEmpty()).isTrue();
    }

    @Test
    void shouldModelMachPunchAsDeclareAttackBenchDamageInsteadOfPendingChoice() {
        AttackEffectDefinition definition = reader.read(cardOf("xy1-7"), attack("Mach Punch", 1));

        assertThat(definition.isEmpty()).isFalse();
        assertThat(definition.cardExternalId()).isEqualTo("xy1-7");
        assertThat(definition.attackOrder()).isEqualTo(1);
        assertThat(definition.operations())
                .singleElement()
                .satisfies(operation -> {
                    assertThat(operation.type()).isEqualTo("BENCH_DAMAGE");
                    assertThat(operation.target()).isEqualTo("OPPONENT_BENCH");
                    assertThat(operation.amount()).isEqualTo(10);
                });
    }

    @Test
    void shouldThrowForUnlistedAttackOrderWithNonBlankEffectText() {
        Attack attack = attack("Head Turn", 0);
        attack.setEffectText("Flip a coin. If heads, the Defending Pokemon is now Confused.");

        assertThatThrownBy(() -> reader.read(cardOf("xy1-74"), attack))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Attack effect is not supported by the current XY1 effect engine");
    }

    private Card cardOf(String externalId) {
        Card card = new Card();
        card.setExternalId(externalId);
        card.setName("Card " + externalId);
        return card;
    }

    private Attack attack(String name, int attackOrder) {
        Attack attack = new Attack();
        attack.setName(name);
        attack.setAttackOrder(attackOrder);
        return attack;
    }

    private Attack attack(Card card, String name, int attackOrder) {
        Attack attack = attack(name, attackOrder);
        attack.setCard(card);
        return attack;
    }
}
