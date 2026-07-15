package ar.edu.utn.frc.tup.piii.services.game.evolution.impl;



import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvolutionRuleServiceImplTest {

    private static final int CURRENT_TURN_NUMBER = 5;
    private static final int PREVIOUS_TURN_NUMBER = 4;
    private static final int BASIC_STACK_ORDER = 0;
    private static final int STAGE_ONE_STACK_ORDER = 1;

    private final EvolutionRuleServiceImpl service = new EvolutionRuleServiceImpl();

    @Test
    void shouldAllowBasicToStageOneEvolution() {
        service.validateEvolution(
                card(CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon"),
                card(CardCategory.BASIC_POKEMON, "Base Pokemon", null),
                stack(BASIC_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER);
    }

    @Test
    void shouldAllowStageOneToStageTwoEvolution() {
        service.validateEvolution(
                card(CardCategory.STAGE_2_POKEMON, "Stage Two", "Stage One"),
                card(CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon"),
                stack(STAGE_ONE_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER);
    }

    @Test
    void shouldRejectStageTwoOnBasicPokemon() {
        assertThatThrownBy(validationCall(
                card(CardCategory.STAGE_2_POKEMON, "Stage Two", "Base Pokemon"),
                card(CardCategory.BASIC_POKEMON, "Base Pokemon", null),
                stack(BASIC_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Selected evolution card cannot evolve the target Pokemon stage");
    }

    @Test
    void shouldRejectStageOneOnStageOnePokemon() {
        assertThatThrownBy(validationCall(
                card(CardCategory.STAGE_1_POKEMON, "Another Stage One", "Stage One"),
                card(CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon"),
                stack(STAGE_ONE_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Selected evolution card cannot evolve the target Pokemon stage");
    }

    @Test
    void shouldAllowMegaEvolutionFromPokemonEx() {
        service.validateEvolution(
                card(CardCategory.MEGA_POKEMON, "M Blastoise-EX", "Blastoise-EX"),
                card(CardCategory.POKEMON_EX, "Blastoise-EX", null),
                stack(BASIC_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER);
    }

    @Test
    void shouldRejectMegaEvolutionFromBasicPokemon() {
        assertThatThrownBy(validationCall(
                card(CardCategory.MEGA_POKEMON, "M Blastoise-EX", "Blastoise"),
                card(CardCategory.BASIC_POKEMON, "Blastoise", null),
                stack(BASIC_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Mega Evolution requires a Pokemon-EX as the base");
    }

    @Test
    void shouldRejectMegaEvolutionFromStageOnePokemon() {
        assertThatThrownBy(validationCall(
                card(CardCategory.MEGA_POKEMON, "Mega Pokemon", "Stage One"),
                card(CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon"),
                stack(STAGE_ONE_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Mega Evolution requires a Pokemon-EX as the base");
    }

    @Test
    void shouldRejectEvolutionWithoutEvolvesFrom() {
        assertThatThrownBy(validationCall(
                card(CardCategory.STAGE_1_POKEMON, "Stage One", null),
                card(CardCategory.BASIC_POKEMON, "Base Pokemon", null),
                stack(BASIC_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Selected evolution card does not declare an evolvesFrom requirement");
    }

    @Test
    void shouldRejectEvolutionWithWrongEvolvesFrom() {
        assertThatThrownBy(validationCall(
                card(CardCategory.STAGE_1_POKEMON, "Stage One", "Other Pokemon"),
                card(CardCategory.BASIC_POKEMON, "Base Pokemon", null),
                stack(BASIC_STACK_ORDER, PREVIOUS_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Selected evolution card cannot evolve the target Pokemon");
    }

    @Test
    void shouldRejectEvolutionWhenTopStackWasCreatedThisTurn() {
        assertThatThrownBy(validationCall(
                card(CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon"),
                card(CardCategory.BASIC_POKEMON, "Base Pokemon", null),
                stack(BASIC_STACK_ORDER, CURRENT_TURN_NUMBER),
                CURRENT_TURN_NUMBER))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Cannot evolve the same Pokemon twice during the same turn");
    }

    private ThrowingCallable validationCall(
            Card evolutionCard,
            Card currentTopCard,
            PokemonEvolutionStack currentTopStack,
            int currentTurnNumber) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                service.validateEvolution(evolutionCard, currentTopCard, currentTopStack, currentTurnNumber);
            }
        };
    }

    private Card card(CardCategory category, String name, String evolvesFrom) {
        Card card = new Card();
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(category);
        if (category == CardCategory.BASIC_POKEMON) {
            card.setSubtype("Basic");
        }
        card.setName(name);
        card.setEvolvesFrom(evolvesFrom);
        return card;
    }

    private PokemonEvolutionStack stack(int stackOrder, int createdAtTurn) {
        PokemonEvolutionStack stack = new PokemonEvolutionStack();
        stack.setStackOrder(stackOrder);
        stack.setCreatedAtTurn(createdAtTurn);
        return stack;
    }
}
