package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CardMapperSpringInjectionTest {

    @Autowired
    private CardMapper cardMapper;

    @Test
    void shouldUseRealTranslationServiceWhenInjectedBySpring() {
        CardResponseDto response = cardMapper.toDto(venusaurEx());

        assertThat(response.externalId()).isEqualTo("xy1-1");
        assertThat(response.pokemonType()).isEqualTo("Grass");
        assertThat(response.displayPokemonType()).isEqualTo("Planta");
        assertThat(response.displaySubtypes()).contains("Básico", "EX");
        assertThat(response.attacks()).hasSize(2);
        assertThat(response.attacks().get(0).name()).isEqualTo("Poison Powder");
        assertThat(response.attacks().get(0).displayName()).isEqualTo("Polvo Veneno");
        assertThat(response.attacks().get(1).name()).isEqualTo("Jungle Hammer");
        assertThat(response.attacks().get(1).displayName()).isEqualTo("Martillo Selvático");
    }

    private Card venusaurEx() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId("xy1-1");
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setNumber("1");
        card.setName("Venusaur-EX");
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setSubtype("Basic, EX");
        card.setHp(180);
        card.setPokemonType("Grass");
        card.setRawJson("{}");

        card.addAttack(attack("Poison Powder", 0, "Grass", 1, "Colorless", 2));
        card.addAttack(attack("Jungle Hammer", 1, "Grass", 2, "Colorless", 2));
        return card;
    }

    private Attack attack(
            String name,
            int order,
            String firstEnergyType,
            int firstQuantity,
            String secondEnergyType,
            int secondQuantity) {
        Attack attack = new Attack();
        attack.setName(name);
        attack.setEffectText(name + " effect");
        attack.setAttackOrder(order);
        attack.addCost(cost(firstEnergyType, firstQuantity));
        attack.addCost(cost(secondEnergyType, secondQuantity));
        return attack;
    }

    private AttackCost cost(String energyType, int quantity) {
        AttackCost cost = new AttackCost();
        cost.setEnergyType(energyType);
        cost.setQuantity(quantity);
        return cost;
    }
}
