package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

/**
 * Engine-owned Trainer effect metadata read from the card catalog payload.
 */
public record TrainerEffectDefinition(String type, int amount) {

    public static TrainerEffectDefinition empty() {
        return new TrainerEffectDefinition(null, 0);
    }
}
