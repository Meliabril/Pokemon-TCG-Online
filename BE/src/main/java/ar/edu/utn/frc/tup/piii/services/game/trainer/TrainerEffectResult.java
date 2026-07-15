package ar.edu.utn.frc.tup.piii.services.game.trainer;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.Map;

/**
 * Result produced by a Trainer effect strategy before the played card is discarded.
 */
public record TrainerEffectResult(
        Map<String, Object> effectData,
        List<GameEventDto> emittedEvents) {

    public TrainerEffectResult {
        if (effectData == null) {
            effectData = Map.of();
        } else {
            effectData = Map.copyOf(effectData);
        }
        if (emittedEvents == null) {
            emittedEvents = List.of();
        } else {
            emittedEvents = List.copyOf(emittedEvents);
        }
    }

    public static TrainerEffectResult empty() {
        return new TrainerEffectResult(Map.of(), List.of());
    }
}
