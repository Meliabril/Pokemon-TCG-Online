package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameActionPayloadReaderImplTest {

    private final GameActionPayloadReaderImpl reader = new GameActionPayloadReaderImpl();

    @Test
    void optionalUuidValidStringReturnsUuid() {
        UUID value = UUID.randomUUID();

        assertThat(reader.optionalUuid(Map.of("cardInstanceId", value.toString()), "cardInstanceId"))
                .contains(value);
    }

    @Test
    void optionalUuidMalformedStringReturnsControlledBadRequest() {
        assertThatThrownBy(() -> reader.optionalUuid(
                Map.of("cardInstanceId", "synthetic-card-reference"),
                "cardInstanceId"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Payload field 'cardInstanceId' must be a UUID");
    }

    @Test
    void requiredUuidMissingValueReturnsControlledBadRequest() {
        assertThatThrownBy(() -> reader.requiredUuid(Map.of(), "cardInstanceId"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Payload field 'cardInstanceId' must be a UUID");
    }
}
