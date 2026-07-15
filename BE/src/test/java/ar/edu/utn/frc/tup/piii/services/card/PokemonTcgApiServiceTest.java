package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.services.card.impl.PokemonTcgApiServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PokemonTcgApiServiceTest {

    private ObjectMapper objectMapper;
    private PokemonTcgApiServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PokemonTcgApiServiceImpl(objectMapper);
    }

    @Test
    void shouldParsePokemonSupertypeWithAccent() throws Exception {
        PokemonTcgCardPayload payload = payload("""
                {
                  "id": "xy1-42",
                  "set": {"id": "xy1", "name": "XY"},
                  "number": "42",
                  "name": "Pikachu",
                  "supertype": "Pok\\u00e9mon",
                  "subtypes": ["Basic"],
                  "hp": "60",
                  "types": ["Lightning"],
                  "retreatCost": ["Colorless"],
                  "attacks": [
                    {
                      "name": "Quick Attack",
                      "cost": ["Lightning", "Colorless", "Colorless"],
                      "damage": "30+",
                      "text": "Flip a coin."
                    }
                  ],
                  "weaknesses": [{"type": "Fighting", "value": "x2"}],
                  "resistances": [{"type": "Metal", "value": "-20"}],
                  "images": {"small": "small.png", "large": "large.png"}
                }
                """);

        assertThat(payload.supertype()).isEqualTo(CardSupertype.POKEMON);
        assertThat(payload.category()).isEqualTo(CardCategory.BASIC_POKEMON);
        assertThat(payload.hp()).isEqualTo(60);
        assertThat(payload.pokemonType()).isEqualTo("Lightning");
        assertThat(payload.retreatCost()).isEqualTo(1);
        assertThat(payload.attacks()).singleElement().satisfies(attack -> {
            assertThat(attack.baseDamage()).isEqualTo(30);
            assertThat(attack.costs()).containsEntry("Lightning", 1).containsEntry("Colorless", 2);
        });
        assertThat(payload.weaknesses()).singleElement().satisfies(weakness -> {
            assertThat(weakness.energyType()).isEqualTo("Fighting");
            assertThat(weakness.value()).isEqualTo("x2");
        });
        assertThat(payload.resistances()).singleElement().satisfies(resistance -> {
            assertThat(resistance.energyType()).isEqualTo("Metal");
            assertThat(resistance.value()).isEqualTo("-20");
        });
    }

    @Test
    void shouldParsePokemonToolSubtypeWithAccent() throws Exception {
        PokemonTcgCardPayload payload = payload("""
                {
                  "id": "xy1-101",
                  "set": {"id": "xy1", "name": "XY"},
                  "number": "101",
                  "name": "Muscle Band",
                  "supertype": "Trainer",
                  "subtypes": ["Pok\\u00e9mon Tool"]
                }
                """);

        assertThat(payload.supertype()).isEqualTo(CardSupertype.TRAINER);
        assertThat(payload.category()).isEqualTo(CardCategory.POKEMON_TOOL_TRAINER);
    }

    private PokemonTcgCardPayload payload(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return ReflectionTestUtils.invokeMethod(service, "toPayload", node);
    }
}
