package ar.edu.utn.frc.tup.piii.services.game.evolution;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;

public interface EvolutionRuleService {

    void validateEvolution(
            Card evolutionCard,
            Card currentTopCard,
            PokemonEvolutionStack currentTopStack,
            int currentTurnNumber);
}
