package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for the Gourgeist (xy1-57) bug report: the catalog defines "Eerie Voice"
 * (ALL_OPPONENT) and "Spirit Scream" (BOTH_ACTIVE) as two distinct attacks on the same card, keyed
 * by attackName. Matching used to fall back to the numeric attackOrder field alone, which is set
 * from external card-import data - if that import ever assigned an order different from what this
 * catalog assumed, the wrong attack's rules (including who it targets) would silently apply.
 * AttackEffectDefinitionReader must prefer the attack's actual name whenever the catalog provides one.
 */
class AttackEffectDefinitionReaderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldMatchByAttackNameRegardlessOfAttackOrder() {
        AttackEffectDefinitionReader reader = new AttackEffectDefinitionReader(OBJECT_MAPPER);
        Card card = card("xy1-57");

        // Simulate an import where "Eerie Voice" ended up at attackOrder 1 instead of the catalog's
        // assumed 0 - the old order-only matching would have applied Spirit Scream's BOTH_ACTIVE
        // rule to a player who selected Eerie Voice.
        Attack eerieVoiceWithUnexpectedOrder = attack(card, "Eerie Voice", 1);

        AttackEffectDefinition definition = reader.read(card, eerieVoiceWithUnexpectedOrder);

        assertThat(definition.attackName()).isEqualTo("Eerie Voice");
        assertThat(definition.operations()).hasSize(1);
        assertThat(definition.operations().get(0).target()).isEqualTo("ALL_OPPONENT");
    }

    @Test
    void shouldNotConflateEerieVoiceAndSpiritScreamDefinitions() {
        AttackEffectDefinitionReader reader = new AttackEffectDefinitionReader(OBJECT_MAPPER);
        Card card = card("xy1-57");

        AttackEffectDefinition eerieVoice = reader.read(card, attack(card, "Eerie Voice", 0));
        AttackEffectDefinition spiritScream = reader.read(card, attack(card, "Spirit Scream", 1));

        assertThat(eerieVoice.operations().get(0).target()).isEqualTo("ALL_OPPONENT");
        assertThat(eerieVoice.operations().get(0).type()).isEqualTo("DAMAGE_COUNTERS");

        assertThat(spiritScream.operations().get(0).target()).isEqualTo("BOTH_ACTIVE");
        assertThat(spiritScream.operations().get(0).type()).isEqualTo("DAMAGE_COUNTERS_UNTIL_REMAINING_HP");
    }

    private Card card(String externalId) {
        Card card = new Card();
        card.setExternalId(externalId);
        return card;
    }

    private Attack attack(Card card, String name, int attackOrder) {
        Attack attack = new Attack();
        attack.setCard(card);
        attack.setName(name);
        attack.setAttackOrder(attackOrder);
        return attack;
    }
}
