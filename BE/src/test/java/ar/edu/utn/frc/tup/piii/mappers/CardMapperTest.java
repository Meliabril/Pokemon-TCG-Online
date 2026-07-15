package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.card.AttackTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.card.TypeValueTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardMapperTest {

    @Test
    void shouldAddDisplayFieldsWithoutChangingCanonicalFields() {
        Card card = card();
        CardTranslationDto translation = new CardTranslationDto(
                "Pikachu traducido",
                "Rayo",
                "Pokemon",
                List.of("Basico"),
                List.of(),
                List.of(new AttackTranslationDto(0, "Ataque rapido", List.of("Rayo"), "Texto traducido")),
                List.of(new TypeValueTranslationDto("Lucha", "x2")),
                List.of(),
                List.of("Regla traducida"));
        CardTranslationDto englishTranslation = new CardTranslationDto(
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(new AttackTranslationDto(0, null, List.of(), "English display text")),
                List.of(),
                List.of(),
                List.of());
        CardTranslationService translationService = mock(CardTranslationService.class);
        when(translationService.findByExternalId("xy1-42")).thenReturn(Optional.of(translation));
        when(translationService.findEnglishByExternalId("xy1-42")).thenReturn(Optional.of(englishTranslation));

        CardResponseDto response = new CardMapper(translationService, mock(AbilityCatalogService.class), new ObjectMapper()).toDto(card);

        assertThat(response.name()).isEqualTo("Pikachu");
        assertThat(response.pokemonType()).isEqualTo("Lightning");
        assertThat(response.displayName()).isEqualTo("Pikachu traducido");
        assertThat(response.displayPokemonType()).isEqualTo("Rayo");
        assertThat(response.rules()).containsExactly("Raw rule");
        assertThat(response.displayRules()).containsExactly("Regla traducida");
        assertThat(response.attacks()).singleElement().satisfies(attack -> {
            assertThat(attack.name()).isEqualTo("Quick Attack");
            assertThat(attack.costs()).singleElement().satisfies(cost -> assertThat(cost.energyType()).isEqualTo("Lightning"));
            assertThat(attack.displayName()).isEqualTo("Ataque rapido");
            assertThat(attack.displayCost()).containsExactly("Rayo");
            assertThat(attack.displayText()).isEqualTo("Texto traducido");
            assertThat(attack.displayTextEn()).isEqualTo("English display text");
        });
        assertThat(response.weaknesses()).singleElement().satisfies(weakness -> {
            assertThat(weakness.energyType()).isEqualTo("Fighting");
            assertThat(weakness.displayEnergyType()).isEqualTo("Lucha");
            assertThat(weakness.displayValue()).isEqualTo("x2");
        });
    }

    @Test
    void shouldLeaveDisplayFieldsNullWhenTranslationDoesNotExist() {
        Card card = card();
        CardResponseDto response = new CardMapper(CardTranslationService.empty(), mock(AbilityCatalogService.class), new ObjectMapper()).toDto(card);

        assertThat(response.name()).isEqualTo("Pikachu");
        assertThat(response.displayName()).isNull();
        assertThat(response.attacks()).singleElement().satisfies(attack -> assertThat(attack.displayName()).isNull());
    }

    private Card card() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId("xy1-42");
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setNumber("42");
        card.setName("Pikachu");
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setSubtype("Basic");
        card.setHp(60);
        card.setPokemonType("Lightning");
        card.setRawJson("{\"rules\":[\"Raw rule\"]}");

        Attack attack = new Attack();
        attack.setName("Quick Attack");
        attack.setDamageText("30");
        attack.setBaseDamage(30);
        attack.setAttackOrder(0);
        AttackCost cost = new AttackCost();
        cost.setEnergyType("Lightning");
        cost.setQuantity(1);
        attack.addCost(cost);
        card.addAttack(attack);

        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Fighting");
        weakness.setMultiplier("x2");
        card.addWeakness(weakness);
        return card;
    }
}
