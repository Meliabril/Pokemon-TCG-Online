package ar.edu.utn.frc.tup.piii.services.game.evolution.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.evolution.EvolutionRuleService;
import org.springframework.stereotype.Service;

@Service
public class EvolutionRuleServiceImpl implements EvolutionRuleService {

    private static final int BASIC_STACK_ORDER = 0;
    private static final int STAGE_ONE_STACK_ORDER = 1;
    private static final String SAME_TURN_EVOLUTION_MESSAGE =
            "Cannot evolve the same Pokemon twice during the same turn";

    @Override
    public void validateEvolution(
            Card evolutionCard,
            Card currentTopCard,
            PokemonEvolutionStack currentTopStack,
            int currentTurnNumber) {
        validateRequiredCards(evolutionCard, currentTopCard, currentTopStack);
        validateSupportedEvolutionCategory(evolutionCard);
        validateSameTurnEvolution(currentTopStack, currentTurnNumber);
        validateEvolvesFrom(evolutionCard, currentTopCard);
        validateStageProgression(evolutionCard, currentTopCard, currentTopStack);
    }

    private void validateRequiredCards(
            Card evolutionCard,
            Card currentTopCard,
            PokemonEvolutionStack currentTopStack) {
        if (currentTopStack == null) {
            throw new InvalidGameActionException("Evolution stack is missing for the target Pokemon");
        }
        if (evolutionCard == null) {
            throw new InvalidGameActionException("Selected evolution card was not found");
        }
        if (currentTopCard == null) {
            throw new InvalidGameActionException("Current top Pokemon card was not found");
        }
    }

    private void validateSupportedEvolutionCategory(Card evolutionCard) {
        CardCategory category = evolutionCard.getCategory();
        if (!CardCategory.STAGE_1_POKEMON.equals(category)
                && !CardCategory.STAGE_2_POKEMON.equals(category)
                && !CardCategory.MEGA_POKEMON.equals(category)) {
            throw new InvalidGameActionException("Selected card is not a valid evolution card");
        }
    }

    private void validateSameTurnEvolution(PokemonEvolutionStack currentTopStack, int currentTurnNumber) {
        Integer createdAtTurn = currentTopStack.getCreatedAtTurn();
        if (createdAtTurn == null) {
            throw new InvalidGameActionException("Evolution stack turn metadata is missing");
        }
        if (createdAtTurn.intValue() == currentTurnNumber) {
            throw new InvalidGameActionException(SAME_TURN_EVOLUTION_MESSAGE);
        }
    }

    private void validateEvolvesFrom(Card evolutionCard, Card currentTopCard) {
        String evolvesFrom = evolutionCard.getEvolvesFrom();
        if (evolvesFrom == null || evolvesFrom.isBlank()) {
            throw new InvalidGameActionException("Selected evolution card does not declare an evolvesFrom requirement");
        }
        if (currentTopCard.getName() == null || !evolvesFrom.equalsIgnoreCase(currentTopCard.getName())) {
            throw new InvalidGameActionException("Selected evolution card cannot evolve the target Pokemon");
        }
    }

    private void validateStageProgression(
            Card evolutionCard,
            Card currentTopCard,
            PokemonEvolutionStack currentTopStack) {
        Integer stackOrder = currentTopStack.getStackOrder();
        if (stackOrder == null) {
            throw new InvalidGameActionException("Evolution stack order is missing");
        }

        if (CardCategory.MEGA_POKEMON.equals(evolutionCard.getCategory())) {
            validateMegaEvolution(currentTopCard, stackOrder);
            return;
        }

        if (CardCategory.STAGE_1_POKEMON.equals(evolutionCard.getCategory())) {
            validateStageOneEvolution(currentTopCard, stackOrder);
            return;
        }

        validateStageTwoEvolution(currentTopCard, stackOrder);
    }

    private void validateStageOneEvolution(Card currentTopCard, Integer stackOrder) {
        if (stackOrder.intValue() == BASIC_STACK_ORDER && currentTopCard.isBasicStage()) {
            return;
        }

        throw new InvalidGameActionException("Selected evolution card cannot evolve the target Pokemon stage");
    }

    private void validateMegaEvolution(Card currentTopCard, Integer stackOrder) {
        if (stackOrder.intValue() == BASIC_STACK_ORDER && CardCategory.POKEMON_EX.equals(currentTopCard.getCategory())) {
            return;
        }
        throw new InvalidGameActionException("Mega Evolution requires a Pokemon-EX as the base");
    }

    private void validateStageTwoEvolution(Card currentTopCard, Integer stackOrder) {
        if (stackOrder.intValue() == STAGE_ONE_STACK_ORDER && CardCategory.STAGE_1_POKEMON.equals(currentTopCard.getCategory())) {
            return;
        }

        throw new InvalidGameActionException("Selected evolution card cannot evolve the target Pokemon stage");
    }
}
