package ar.edu.utn.frc.tup.piii.services.game.engine;




import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
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
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.repositories.GameCardInstanceRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameEventRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.PokemonAttachedCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PokemonEvolutionStackRepository;
import ar.edu.utn.frc.tup.piii.repositories.PokemonInPlayRepository;
import ar.edu.utn.frc.tup.piii.repositories.SpecialConditionRepository;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GameActionEngineFlowIntegrationTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameCardInstanceRepository gameCardInstanceRepository;

    @Autowired
    private PokemonInPlayRepository pokemonInPlayRepository;

    @Autowired
    private PokemonEvolutionStackRepository pokemonEvolutionStackRepository;

    @Autowired
    private PokemonAttachedCardRepository pokemonAttachedCardRepository;

    @Autowired
    private SpecialConditionRepository specialConditionRepository;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private GameSnapshotService gameSnapshotService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from special_conditions");
        jdbcTemplate.update("delete from pokemon_attached_cards");
        jdbcTemplate.update("delete from pokemon_evolution_stack");
        jdbcTemplate.update("delete from pokemon_in_play");
        jdbcTemplate.update("delete from game_events");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_action_logs");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from attack_costs");
        jdbcTemplate.update("delete from attacks");
        jdbcTemplate.update("delete from card_weaknesses");
        jdbcTemplate.update("delete from card_resistances");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from cards");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldStartGameAndWaitForInitialPokemonSelection() {
        TestFixture fixture = insertWaitingGameWithTwoValidDecks();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.userOneId(),
                new GameActionRequestDto(fixture.gameId(), UUID.randomUUID(), GameActionType.START_GAME, 0, java.util.Map.of()));

        assertThat(response.success()).isTrue();
        assertThat(response.newStateVersion()).isEqualTo(1);

        Game game = gameRepository.findById(fixture.gameId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.SETUP);
        assertThat(game.getCurrentPhase()).isNull();
        assertThat(game.getTurnNumber()).isZero();
        assertThat(game.getActivePlayerId()).isNull();
        assertThat(game.getPlayerWhoWentFirstId()).isNull();
        assertThat(game.getSetupState()).isNotEmpty();

        assertThat(gameCardInstanceRepository.findByGame_Id(fixture.gameId())).hasSize(120);
        assertThat(pokemonInPlayRepository.findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(fixture.gameId())).isEmpty();
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.userOneId(), CardZone.PRIZE)).hasSize(6);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.userOneId(), CardZone.HAND)).hasSize(7);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.userOneId(), CardZone.DECK)).hasSize(47);

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.userOneId()).orElseThrow();
        assertThat(state.status()).isEqualTo(GameStatus.SETUP);
        assertThat(state.actions().availableActions()).containsExactly(GameActionType.CHOOSE_INITIAL_POKEMON);
        assertThat(GameStateTestFactory.cardsInHandInstanceIdsByPlayer(state).get(fixture.userOneId())).hasSize(7);

        List<GameEvent> events = gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId());
        assertThat(events).hasSize(3);
        assertThat(events)
                .extracting(GameEvent::getEventType)
                .containsExactly(GameEventType.OPENING_HANDS_DEALT, GameEventType.STATE_SYNC, GameEventType.STATE_SYNC);
    }

    @Test
    void shouldChooseInitialPokemonForBothPlayersAndCreateInitialBoardState() {
        TestFixture fixture = insertWaitingGameWithTwoValidDecks();
        gameService.executeAction(
                fixture.gameId(),
                fixture.userOneId(),
                new GameActionRequestDto(fixture.gameId(), UUID.randomUUID(), GameActionType.START_GAME, 0, java.util.Map.of()));

        List<GameCardInstance> userOneHand = gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                fixture.gameId(),
                fixture.userOneId(),
                CardZone.HAND);
        List<GameCardInstance> userTwoHand = gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                fixture.gameId(),
                fixture.userTwoId(),
                CardZone.HAND);

        GameActionResponseDto firstChoiceResponse = gameService.executeAction(
                fixture.gameId(),
                fixture.userOneId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.CHOOSE_INITIAL_POKEMON,
                        1,
                        java.util.Map.of(
                                "activeCardInstanceId", userOneHand.get(0).getId().toString(),
                                "benchCardInstanceIds", java.util.List.of(userOneHand.get(1).getId().toString()))));

        assertThat(firstChoiceResponse.success()).isTrue();
        assertThat(firstChoiceResponse.newStateVersion()).isEqualTo(2);
        assertThat(gameRepository.findById(fixture.gameId()).orElseThrow().getStatus()).isEqualTo(GameStatus.SETUP);
        assertThat(pokemonInPlayRepository.findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(fixture.gameId())).isEmpty();

        GameActionResponseDto secondChoiceResponse = gameService.executeAction(
                fixture.gameId(),
                fixture.userTwoId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.CHOOSE_INITIAL_POKEMON,
                        2,
                        java.util.Map.of(
                                "activeCardInstanceId", userTwoHand.get(0).getId().toString(),
                                "benchCardInstanceIds", java.util.List.of())));

        assertThat(secondChoiceResponse.success()).isTrue();
        assertThat(secondChoiceResponse.newStateVersion()).isEqualTo(3);

        Game game = gameRepository.findById(fixture.gameId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.getCurrentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(game.getTurnNumber()).isEqualTo(1);
        assertThat(game.getActivePlayerId()).isNotNull();
        assertThat(game.getPlayerWhoWentFirstId()).isEqualTo(game.getActivePlayerId());
        assertThat(game.getSetupState()).isEmpty();

        List<PokemonInPlay> initialPokemon = pokemonInPlayRepository.findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(fixture.gameId());
        assertThat(initialPokemon).hasSize(3);
        for (PokemonInPlay pokemonInPlay : initialPokemon) {
            List<PokemonEvolutionStack> baseStack =
                    pokemonEvolutionStackRepository.findByPokemonInPlay_IdOrderByStackOrderAsc(pokemonInPlay.getId());
            assertThat(baseStack).hasSize(1);
            assertThat(baseStack.get(0).getStackOrder()).isZero();
            assertThat(baseStack.get(0).getCreatedAtTurn()).isEqualTo(1);
        }
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.userOneId(), CardZone.ACTIVE)).hasSize(1);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.userOneId(), CardZone.BENCH)).hasSize(1);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.userOneId(), CardZone.HAND)).hasSize(5);

        GameStateDto latestState = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.userOneId()).orElseThrow();
        assertThat(latestState.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(latestState.turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(latestState.actions().availableActions()).containsExactly(GameActionType.DRAW_CARD);

        List<GameEvent> events = gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId());
        assertThat(events)
                .extracting(GameEvent::getEventType)
                .contains(
                        GameEventType.INITIAL_POKEMON_SELECTED,
                        GameEventType.INITIAL_BOARD_REVEALED,
                        GameEventType.GAME_STARTED);
    }

    @Test
    void shouldDrawCardAndAdvancePhaseToMain() {
        TestFixture fixture = insertWaitingGameWithTwoValidDecks();
        startGameAndChooseInitialPokemon(fixture);

        Game startedGame = gameRepository.findById(fixture.gameId()).orElseThrow();
        UUID activePlayerId = startedGame.getActivePlayerId();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                activePlayerId,
                new GameActionRequestDto(fixture.gameId(), UUID.randomUUID(), GameActionType.DRAW_CARD, 3, java.util.Map.of()));

        assertThat(response.success()).isTrue();
        assertThat(response.newStateVersion()).isEqualTo(4);

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), activePlayerId).orElseThrow();
        assertThat(state.turn().currentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(state.stateVersion()).isEqualTo(4);
        assertThat(state.actions().availableActions())
                .contains(GameActionType.DECLARE_ATTACK, GameActionType.END_TURN);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), activePlayerId, CardZone.HAND)).hasSize(7);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), activePlayerId, CardZone.DECK)).hasSize(46);
    }

    @Test
    void shouldEndMainPhaseTurnDirectlyToOpponentDrawPhase() {
        TestFixture fixture = insertWaitingGameWithTwoValidDecks();
        startGameAndChooseInitialPokemon(fixture);

        Game startedGame = gameRepository.findById(fixture.gameId()).orElseThrow();
        UUID firstPlayerId = startedGame.getActivePlayerId();
        UUID opponentId;
        if (firstPlayerId.equals(fixture.userOneId())) {
            opponentId = fixture.userTwoId();
        } else {
            opponentId = fixture.userOneId();
        }

        gameService.executeAction(
                fixture.gameId(),
                firstPlayerId,
                new GameActionRequestDto(fixture.gameId(), UUID.randomUUID(), GameActionType.DRAW_CARD, 3, java.util.Map.of()));

        GameActionResponseDto drawPhaseResponse = gameService.executeAction(
                fixture.gameId(),
                firstPlayerId,
                new GameActionRequestDto(fixture.gameId(), UUID.randomUUID(), GameActionType.END_TURN, 4, java.util.Map.of()));

        assertThat(drawPhaseResponse.success()).isTrue();
        assertThat(drawPhaseResponse.newStateVersion()).isEqualTo(5);

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), opponentId).orElseThrow();
        assertThat(state.turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(state.turn().activePlayerId()).isEqualTo(opponentId);
        assertThat(state.turn().turnNumber()).isEqualTo(2);
        assertThat(state.turn().turnNumber()).isEqualTo(2);
    }

    @Test
    void shouldPlayBasicPokemonFromHandToBench() {
        ActiveMainFixture fixture = insertActiveMainFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.PLAY_BASIC_POKEMON,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("cardId", fixture.benchBasicCardId().toString())));

        assertThat(response.success()).isTrue();
        assertThat(response.newStateVersion()).isEqualTo(fixture.expectedStateVersion() + 1);

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.activePlayerId()).orElseThrow();
        assertThat(GameStateTestFactory.benchCountByPlayer(state)).containsEntry(fixture.activePlayerId(), 1);
        assertThat(pokemonInPlayRepository.findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(fixture.gameId(), fixture.activePlayerId()))
                .extracting(PokemonInPlay::getSlotPosition)
                .containsExactly(0, 1);
        List<PokemonInPlay> pokemonInPlay = pokemonInPlayRepository.findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(
                fixture.gameId(),
                fixture.activePlayerId());
        PokemonInPlay benchPokemon = pokemonInPlay.get(1);
        List<PokemonEvolutionStack> baseStack =
                pokemonEvolutionStackRepository.findByPokemonInPlay_IdOrderByStackOrderAsc(benchPokemon.getId());
        assertThat(baseStack).hasSize(1);
        assertThat(baseStack.get(0).getStackOrder()).isZero();
        assertThat(baseStack.get(0).getCreatedAtTurn()).isEqualTo(3);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.activePlayerId(), CardZone.HAND))
                .extracting(GameCardInstance::getCardId)
                .doesNotContain(fixture.benchBasicCardId());
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.activePlayerId(), CardZone.BENCH))
                .extracting(GameCardInstance::getCardId)
                .contains(fixture.benchBasicCardId());
    }

    @Test
    void shouldAttachEnergyFromHandToPokemonInPlay() {
        ActiveMainFixture fixture = insertActiveMainFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.ATTACH_ENERGY,
                        fixture.expectedStateVersion(),
                        java.util.Map.of(
                                "cardId", fixture.energyCardId().toString(),
                                "pokemonInPlayId", fixture.activePokemonInPlayId().toString())));

        assertThat(response.success()).isTrue();
        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.activePlayerId()).orElseThrow();
        assertThat(state.turn().energyAttachedThisTurn()).isTrue();
        assertThat(pokemonAttachedCardRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(fixture.activePokemonInPlayId())).hasSize(1);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.activePlayerId(), CardZone.HAND))
                .extracting(GameCardInstance::getCardId)
                .doesNotContain(fixture.energyCardId());
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.activePlayerId(), CardZone.ATTACHED))
                .extracting(GameCardInstance::getCardId)
                .contains(fixture.energyCardId());
    }

    @Test
    void shouldAttachSelectedEnergyInstanceWhenHandHasRepeatedEnergies() {
        ActiveMainFixture fixture = insertActiveMainFixture();
        UUID selectedEnergyInstanceId = UUID.randomUUID();
        insertCardInstance(
                selectedEnergyInstanceId,
                fixture.gameId(),
                fixture.activePlayerId(),
                fixture.energyCardId(),
                CardZone.HAND,
                5,
                false,
                Instant.parse("2026-05-24T20:31:00Z"));

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.ATTACH_ENERGY,
                        fixture.expectedStateVersion(),
                        java.util.Map.of(
                                "cardId", fixture.energyCardId().toString(),
                                "cardInstanceId", selectedEnergyInstanceId.toString(),
                                "pokemonInPlayId", fixture.activePokemonInPlayId().toString())));

        assertThat(response.success()).isTrue();
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                fixture.gameId(),
                fixture.activePlayerId(),
                CardZone.HAND))
                .extracting(GameCardInstance::getId)
                .contains(fixture.energyCardInstanceId())
                .doesNotContain(selectedEnergyInstanceId);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                fixture.gameId(),
                fixture.activePlayerId(),
                CardZone.ATTACHED))
                .extracting(GameCardInstance::getId)
                .contains(selectedEnergyInstanceId);
    }

    @Test
    void shouldEvolvePokemonUsingMatchingCardFromHand() {
        ActiveMainFixture fixture = insertActiveMainFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.EVOLVE_POKEMON,
                        fixture.expectedStateVersion(),
                        java.util.Map.of(
                                "cardId", fixture.evolutionCardId().toString(),
                                "pokemonInPlayId", fixture.activePokemonInPlayId().toString())));

        assertThat(response.success()).isTrue();
        List<PokemonEvolutionStack> evolutionStack =
                pokemonEvolutionStackRepository.findByPokemonInPlay_IdOrderByStackOrderAsc(fixture.activePokemonInPlayId());
        assertThat(evolutionStack).hasSize(2);
        assertThat(evolutionStack.get(0).getCreatedAtTurn()).isEqualTo(1);
        assertThat(evolutionStack.get(1).getCreatedAtTurn()).isEqualTo(3);
        UUID evolvedTopInstanceId = pokemonEvolutionStackRepository.findFirstByPokemonInPlay_IdOrderByStackOrderDesc(fixture.activePokemonInPlayId())
                .orElseThrow()
                .getGameCardInstance()
                .getId();
        UUID evolvedTopCardId = gameCardInstanceRepository.findById(evolvedTopInstanceId)
                .orElseThrow()
                .getCardId();
        UUID previousTopInstanceId = evolutionStack.get(0).getGameCardInstance().getId();
        GameCardInstance previousTopInstance = gameCardInstanceRepository.findById(previousTopInstanceId).orElseThrow();
        PokemonInPlay evolvedPokemon = pokemonInPlayRepository.findById(fixture.activePokemonInPlayId()).orElseThrow();
        assertThat(evolvedTopCardId).isEqualTo(fixture.evolutionCardId());
        assertThat(previousTopInstance.getZone()).isEqualTo(CardZone.EVOLUTION_STACK);
        assertThat(evolvedPokemon.getEnteredPlayTurn()).isEqualTo(3);
        assertThat(specialConditionRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(fixture.activePokemonInPlayId())).isEmpty();
        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.activePlayerId()).orElseThrow();
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(state).get(fixture.activePlayerId())).isEmpty();
    }

    @Test
    void shouldRejectSecondEvolutionForSamePokemonDuringSameTurn() {
        ActiveMainFixture fixture = insertActiveMainFixture();

        GameActionResponseDto firstEvolution = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.EVOLVE_POKEMON,
                        fixture.expectedStateVersion(),
                        java.util.Map.of(
                                "cardId", fixture.evolutionCardId().toString(),
                                "pokemonInPlayId", fixture.activePokemonInPlayId().toString())));

        assertThat(firstEvolution.success()).isTrue();

        assertThatThrownBy(secondEvolutionCall(fixture, firstEvolution.newStateVersion()))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Cannot evolve the same Pokemon twice during the same turn");
    }

    @Test
    void shouldAllowDifferentPokemonToEvolveDuringSameTurn() {
        MultiEvolutionFixture fixture = insertMultiEvolutionFixture();

        GameActionResponseDto firstEvolution = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.EVOLVE_POKEMON,
                        fixture.expectedStateVersion(),
                        java.util.Map.of(
                                "cardId", fixture.activeEvolutionCardId().toString(),
                                "pokemonInPlayId", fixture.activePokemonInPlayId().toString())));

        assertThat(firstEvolution.success()).isTrue();

        GameActionResponseDto secondEvolution = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.EVOLVE_POKEMON,
                        firstEvolution.newStateVersion(),
                        java.util.Map.of(
                                "cardId", fixture.benchEvolutionCardId().toString(),
                                "pokemonInPlayId", fixture.benchPokemonInPlayId().toString())));

        assertThat(secondEvolution.success()).isTrue();

        List<PokemonEvolutionStack> activeStack =
                pokemonEvolutionStackRepository.findByPokemonInPlay_IdOrderByStackOrderAsc(fixture.activePokemonInPlayId());
        List<PokemonEvolutionStack> benchStack =
                pokemonEvolutionStackRepository.findByPokemonInPlay_IdOrderByStackOrderAsc(fixture.benchPokemonInPlayId());
        assertThat(activeStack).hasSize(2);
        assertThat(benchStack).hasSize(2);
        assertThat(activeStack.get(1).getCreatedAtTurn()).isEqualTo(3);
        assertThat(benchStack.get(1).getCreatedAtTurn()).isEqualTo(3);
    }

    @Test
    void shouldRetreatActivePokemonByDiscardingEnergyAndPromotingBench() {
        RetreatFixture fixture = insertRetreatFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.RETREAT,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("targetPokemonInPlayId", fixture.benchPokemonInPlayId().toString())));

        assertThat(response.success()).isTrue();
        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.activePlayerId()).orElseThrow();
        assertThat(state.turn().retreatedThisTurn()).isTrue();
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(state).get(fixture.activePlayerId())).isEmpty();
        assertThat(pokemonAttachedCardRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(fixture.originalActivePokemonInPlayId())).isEmpty();
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.activePlayerId(), CardZone.DISCARD))
                .extracting(GameCardInstance::getCardId)
                .contains(fixture.energyCardId());
        PokemonInPlay newActive = pokemonInPlayRepository.findById(fixture.benchPokemonInPlayId()).orElseThrow();
        assertThat(newActive.getSlotPosition()).isZero();
    }

    @Test
    void shouldDeclareAttackApplyKnockoutTakePrizeAndFinishGame() {
        AttackFixture fixture = insertAttackFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(response.success()).isTrue();

        Game game = gameRepository.findById(fixture.gameId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinnerPlayerId()).isEqualTo(fixture.attackerUserId());
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(fixture.gameId(), fixture.attackerUserId(), CardZone.HAND))
                .extracting(GameCardInstance::getCardId)
                .contains(fixture.prizeCardId());
        assertThat(pokemonInPlayRepository.findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(fixture.gameId(), fixture.defenderUserId())).isEmpty();
        assertThat(gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()))
                .extracting(GameEvent::getEventType)
                .contains(GameEventType.ATTACK_DECLARED, GameEventType.DAMAGE_APPLIED, GameEventType.POKEMON_KNOCKED_OUT, GameEventType.PRIZE_TAKEN, GameEventType.GAME_FINISHED);
    }

    @Test
    void shouldDeclareAttackWithoutKnockoutApplyDamageAndPassTurnToOpponentDraw() {
        AttackFixture fixture = insertNonKnockoutAttackFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(response.success()).isTrue();

        PokemonInPlay defenderPokemon = pokemonInPlayRepository.findByGame_IdAndOwnerUserIdAndSlotPosition(
                        fixture.gameId(),
                        fixture.defenderUserId(),
                        0)
                .orElseThrow();
        assertThat(defenderPokemon.getDamageCounters()).isEqualTo(2);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                fixture.gameId(),
                fixture.defenderUserId(),
                CardZone.DISCARD)).isEmpty();

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.defenderUserId()).orElseThrow();
        assertThat(state.turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(state.turn().activePlayerId()).isEqualTo(fixture.defenderUserId());
        assertThat(state.boardPlayers().get(fixture.defenderUserId()).activePokemon().damageCounters()).isEqualTo(2);
        assertThat(gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()))
                .extracting(GameEvent::getEventType)
                .contains(GameEventType.ATTACK_DECLARED, GameEventType.DAMAGE_APPLIED)
                .doesNotContain(GameEventType.POKEMON_KNOCKED_OUT);
    }

    @Test
    void shouldDeclareAttackKnockoutWithDefenderBenchLeavePromotionPending() {
        AttackFixture fixture = insertAttackWithDefenderBenchFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(response.success()).isTrue();

        Game game = gameRepository.findById(fixture.gameId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.getCurrentPhase()).isEqualTo(TurnPhase.BETWEEN_TURNS);
        assertThat(pokemonInPlayRepository.findByGame_IdAndOwnerUserIdAndSlotPosition(
                fixture.gameId(),
                fixture.defenderUserId(),
                0)).isEmpty();
        assertThat(pokemonInPlayRepository.findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(fixture.gameId(), fixture.defenderUserId()))
                .extracting(PokemonInPlay::getSlotPosition)
                .containsExactly(1);
        assertThat(gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                fixture.gameId(),
                fixture.attackerUserId(),
                CardZone.HAND))
                .extracting(GameCardInstance::getCardId)
                .contains(fixture.prizeCardId());

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.defenderUserId()).orElseThrow();
        assertThat(state.resolution().hasPendingPromotion()).isTrue();
        assertThat(state.resolution().playerToPromoteId()).isEqualTo(fixture.defenderUserId());
        assertThat(state.boardPlayers().get(fixture.defenderUserId()).activePokemon()).isNull();
        assertThat(state.boardPlayers().get(fixture.defenderUserId()).benchPokemon()).hasSize(1);
        assertThat(gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()))
                .extracting(GameEvent::getEventType)
                .contains(GameEventType.POKEMON_KNOCKED_OUT, GameEventType.PRIZE_TAKEN, GameEventType.PROMOTION_REQUIRED)
                .doesNotContain(GameEventType.GAME_FINISHED);
    }

    @Test
    void shouldPromoteBenchPokemonSuccessfullyAfterKnockout() {
        AttackFixture fixture = insertAttackWithDefenderBenchFixture();

        GameActionResponseDto attackResponse = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(attackResponse.success()).isTrue();

        UUID benchPokemonInPlayId = pokemonInPlayRepository.findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(fixture.gameId(), fixture.defenderUserId())
                .get(0).getId();

        GameActionResponseDto promoteResponse = gameService.executeAction(
                fixture.gameId(),
                fixture.defenderUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.PROMOTE_BENCH_POKEMON,
                        attackResponse.newStateVersion(),
                        java.util.Map.of("pokemonInPlayId", benchPokemonInPlayId.toString())));

        assertThat(promoteResponse.success()).isTrue();

        Game game = gameRepository.findById(fixture.gameId()).orElseThrow();
        assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.getCurrentPhase()).isEqualTo(TurnPhase.DRAW); // It passes turn to defender since it's now their turn
        assertThat(pokemonInPlayRepository.findByGame_IdAndOwnerUserIdAndSlotPosition(
                fixture.gameId(),
                fixture.defenderUserId(),
                0)).isPresent();

        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.defenderUserId()).orElseThrow();
        assertThat(state.resolution().hasPendingPromotion()).isFalse();
        assertThat(state.boardPlayers().get(fixture.defenderUserId()).activePokemon()).isNotNull();
    }

    @Test
    void shouldFailToAttackDuringPromotionRequired() {
        AttackFixture fixture = insertAttackWithDefenderBenchFixture();

        GameActionResponseDto attackResponse = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(attackResponse.success()).isTrue();

        assertThatThrownBy(() -> gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        attackResponse.newStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString()))))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("The action is not valid during the current turn phase");
    }

    @Test
    void shouldFailToPassTurnDuringPromotionRequired() {
        AttackFixture fixture = insertAttackWithDefenderBenchFixture();

        GameActionResponseDto attackResponse = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(attackResponse.success()).isTrue();

        assertThatThrownBy(() -> gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.END_TURN,
                        attackResponse.newStateVersion(),
                        java.util.Map.of())))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("The action is not valid during the current turn phase");
    }

    @Test
    void shouldApplyPoisonFromAttackAndResolveBetweenTurnsDamage() {
        AttackFixture fixture = insertStatusAttackFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(response.success()).isTrue();
        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.defenderUserId()).orElseThrow();
        assertThat(state.turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(state.turn().activePlayerId()).isEqualTo(fixture.defenderUserId());
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(state).get(fixture.defenderUserId()))
                .containsExactly(ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.POISONED);

        PokemonInPlay defenderPokemon = pokemonInPlayRepository.findByGame_IdAndOwnerUserIdAndSlotPosition(fixture.gameId(), fixture.defenderUserId(), 0)
                .orElseThrow();
        assertThat(defenderPokemon.getDamageCounters()).isEqualTo(1);
        assertThat(gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()))
                .extracting(GameEvent::getEventType)
                .contains(GameEventType.STATUS_APPLIED);
    }

    @Test
    void shouldApplyBothParalyzedAndPoisonedFromCrisisVineMultiConditionAttack() {
        AttackFixture fixture = insertCrisisVineStatusAttackFixture();

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.attackerUserId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.DECLARE_ATTACK,
                        fixture.expectedStateVersion(),
                        java.util.Map.of("attackId", fixture.attackId().toString())));

        assertThat(response.success()).isTrue();
        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.defenderUserId()).orElseThrow();
        assertThat(state.turn().currentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(state.turn().activePlayerId()).isEqualTo(fixture.defenderUserId());

        // xy1-2 (Crisis Vine) defines two AFTER_DAMAGE operations: PARALYZED + POISONED.
        // The engine must apply BOTH to the defender. Between-turns resolution keeps both
        // because PARALYZED is only cleared when the affected player ends their own turn
        // and the condition was applied in a prior turn (appliedTurn < currentTurnNumber).
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(state).get(fixture.defenderUserId()))
                .containsExactlyInAnyOrder(
                        ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.PARALYZED,
                        ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.POISONED);

        // POISONED deals 10 damage between turns = 1 damage counter (attack baseDamage=0).
        PokemonInPlay defenderPokemon = pokemonInPlayRepository.findByGame_IdAndOwnerUserIdAndSlotPosition(fixture.gameId(), fixture.defenderUserId(), 0)
                .orElseThrow();
        assertThat(defenderPokemon.getDamageCounters()).isEqualTo(1);

        // Both special_conditions rows must be persisted on the defender.
        List<ar.edu.utn.frc.tup.piii.entities.SpecialCondition> defenderConditions =
                specialConditionRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(defenderPokemon.getId());
        assertThat(defenderConditions)
                .extracting(ar.edu.utn.frc.tup.piii.entities.SpecialCondition::getConditionType)
                .containsExactlyInAnyOrder(
                        ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.PARALYZED,
                        ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.POISONED);

        // Runtime proof that the engine applied both operations: STATUS_APPLIED events
        // for PARALYZED and POISONED were emitted during attack resolution.
        List<GameEvent> statusAppliedEvents = gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()).stream()
                .filter(event -> event.getEventType() == GameEventType.STATUS_APPLIED)
                .toList();
        assertThat(statusAppliedEvents)
                .extracting(event -> event.getPayload().get("conditionType"))
                .contains(
                        ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.PARALYZED.name(),
                        ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.POISONED.name());
        assertThat(gameEventRepository.findByGame_IdOrderByCreatedAtAsc(fixture.gameId()))
                .extracting(GameEvent::getEventType)
                .contains(GameEventType.DAMAGE_APPLIED);
    }

    @Test
    void shouldClearParalyzedConditionDuringEndTurnBetweenTurnsResolution() {
        ActiveMainFixture fixture = insertActiveMainFixture();
        insertSpecialCondition(UUID.randomUUID(), fixture.activePokemonInPlayId(), "PARALYZED", 1, Instant.parse("2026-05-24T20:31:00Z"));

        GameActionResponseDto response = gameService.executeAction(
                fixture.gameId(),
                fixture.activePlayerId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.END_TURN,
                        fixture.expectedStateVersion(),
                        java.util.Map.of()));

        assertThat(response.success()).isTrue();
        GameStateDto state = gameSnapshotService.findLatestVisibleState(fixture.gameId(), fixture.opponentPlayerId()).orElseThrow();
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(state).get(fixture.activePlayerId()))
                .containsExactly(ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.BURNED);
        assertThat(specialConditionRepository.findByPokemonInPlay_IdOrderByCreatedAtAsc(fixture.activePokemonInPlayId()))
                .extracting(ar.edu.utn.frc.tup.piii.entities.SpecialCondition::getConditionType)
                .doesNotContain(ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType.PARALYZED);
    }

    private TestFixture insertWaitingGameWithTwoValidDecks() {
        Instant now = Instant.parse("2026-05-24T20:00:00Z");
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        UUID basicCardId = UUID.randomUUID();

        insertUser(userOneId, "engineuserone", now);
        insertUser(userTwoId, "engineusertwo", now);
        insertCard(basicCardId, "POKEMON", "BASIC_POKEMON", now);
        insertDeck(deckOneId, userOneId, now);
        insertDeck(deckTwoId, userTwoId, now);
        insertDeckCard(UUID.randomUUID(), deckOneId, basicCardId, 60);
        insertDeckCard(UUID.randomUUID(), deckTwoId, basicCardId, 60);
        insertWaitingGame(gameId, now);
        insertParticipant(UUID.randomUUID(), gameId, userOneId, deckOneId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, userTwoId, deckTwoId, 2, now);
        return new TestFixture(gameId, userOneId, userTwoId);
    }

    private void startGameAndChooseInitialPokemon(TestFixture fixture) {
        gameService.executeAction(
                fixture.gameId(),
                fixture.userOneId(),
                new GameActionRequestDto(fixture.gameId(), UUID.randomUUID(), GameActionType.START_GAME, 0, java.util.Map.of()));

        UUID userOneActiveCardInstanceId = firstHandCardInstanceId(fixture.gameId(), fixture.userOneId());
        gameService.executeAction(
                fixture.gameId(),
                fixture.userOneId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.CHOOSE_INITIAL_POKEMON,
                        1,
                        java.util.Map.of(
                                "activeCardInstanceId", userOneActiveCardInstanceId.toString(),
                                "benchCardInstanceIds", java.util.List.of())));

        UUID userTwoActiveCardInstanceId = firstHandCardInstanceId(fixture.gameId(), fixture.userTwoId());
        gameService.executeAction(
                fixture.gameId(),
                fixture.userTwoId(),
                new GameActionRequestDto(
                        fixture.gameId(),
                        UUID.randomUUID(),
                        GameActionType.CHOOSE_INITIAL_POKEMON,
                        2,
                        java.util.Map.of(
                                "activeCardInstanceId", userTwoActiveCardInstanceId.toString(),
                                "benchCardInstanceIds", java.util.List.of())));
    }

    private UUID firstHandCardInstanceId(UUID gameId, UUID ownerUserId) {
        return gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                        gameId,
                        ownerUserId,
                        CardZone.HAND)
                .get(0)
                .getId();
    }

    private ThrowingCallable secondEvolutionCall(ActiveMainFixture fixture, int expectedStateVersion) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                gameService.executeAction(
                        fixture.gameId(),
                        fixture.activePlayerId(),
                        new GameActionRequestDto(
                                fixture.gameId(),
                                UUID.randomUUID(),
                                GameActionType.EVOLVE_POKEMON,
                                expectedStateVersion,
                                java.util.Map.of(
                                        "cardId", fixture.stageTwoEvolutionCardId().toString(),
                                        "pokemonInPlayId", fixture.activePokemonInPlayId().toString())));
            }
        };
    }

    private ActiveMainFixture insertActiveMainFixture() {
        Instant now = Instant.parse("2026-05-24T20:30:00Z");
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        UUID activeBasicCardId = UUID.randomUUID();
        UUID benchBasicCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();
        UUID evolutionCardId = UUID.randomUUID();
        UUID stageTwoEvolutionCardId = UUID.randomUUID();

        insertUser(userOneId, "mainuserone", now);
        insertUser(userTwoId, "mainusertwo", now);
        insertCard(activeBasicCardId, "POKEMON", "BASIC_POKEMON", "BaseMon", null, now);
        insertCard(benchBasicCardId, "POKEMON", "BASIC_POKEMON", "BenchMon", null, now);
        insertCard(energyCardId, "ENERGY", "BASIC_ENERGY", "EnergyMon", null, now);
        insertCard(evolutionCardId, "POKEMON", "STAGE_1_POKEMON", "StageMon", "BaseMon", now);
        insertCard(stageTwoEvolutionCardId, "POKEMON", "STAGE_2_POKEMON", "StageTwoMon", "StageMon", now);
        insertDeck(deckOneId, userOneId, now);
        insertDeck(deckTwoId, userTwoId, now);
        insertActiveGame(gameId, userOneId, userOneId, 3, 5, now);
        insertParticipant(UUID.randomUUID(), gameId, userOneId, deckOneId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, userTwoId, deckTwoId, 2, now);

        UUID activeInstanceId = UUID.randomUUID();
        insertCardInstance(activeInstanceId, gameId, userOneId, activeBasicCardId, CardZone.ACTIVE, 0, false, now);
        UUID benchHandInstanceId = UUID.randomUUID();
        insertCardInstance(benchHandInstanceId, gameId, userOneId, benchBasicCardId, CardZone.HAND, 1, false, now);
        UUID energyHandInstanceId = UUID.randomUUID();
        insertCardInstance(energyHandInstanceId, gameId, userOneId, energyCardId, CardZone.HAND, 2, false, now);
        UUID evolutionHandInstanceId = UUID.randomUUID();
        insertCardInstance(evolutionHandInstanceId, gameId, userOneId, evolutionCardId, CardZone.HAND, 3, false, now);
        UUID stageTwoEvolutionHandInstanceId = UUID.randomUUID();
        insertCardInstance(stageTwoEvolutionHandInstanceId, gameId, userOneId, stageTwoEvolutionCardId, CardZone.HAND, 4, false, now);

        UUID activePokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(activePokemonInPlayId, gameId, userOneId, activeInstanceId, 0, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), activePokemonInPlayId, activeInstanceId, 0, now);
        insertSpecialCondition(UUID.randomUUID(), activePokemonInPlayId, "BURNED", now);

        return new ActiveMainFixture(
                gameId,
                userOneId,
                userTwoId,
                activePokemonInPlayId,
                benchBasicCardId,
                energyCardId,
                energyHandInstanceId,
                evolutionCardId,
                stageTwoEvolutionCardId,
                5);
    }

    private MultiEvolutionFixture insertMultiEvolutionFixture() {
        Instant now = Instant.parse("2026-05-24T20:36:00Z");
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        UUID activeBasicCardId = UUID.randomUUID();
        UUID benchBasicCardId = UUID.randomUUID();
        UUID activeEvolutionCardId = UUID.randomUUID();
        UUID benchEvolutionCardId = UUID.randomUUID();

        insertUser(userOneId, "multievolutionone", now);
        insertUser(userTwoId, "multievolutiontwo", now);
        insertCard(activeBasicCardId, "POKEMON", "BASIC_POKEMON", "ActiveBaseMon", null, now);
        insertCard(benchBasicCardId, "POKEMON", "BASIC_POKEMON", "BenchBaseMon", null, now);
        insertCard(activeEvolutionCardId, "POKEMON", "STAGE_1_POKEMON", "ActiveStageMon", "ActiveBaseMon", now);
        insertCard(benchEvolutionCardId, "POKEMON", "STAGE_1_POKEMON", "BenchStageMon", "BenchBaseMon", now);
        insertDeck(deckOneId, userOneId, now);
        insertDeck(deckTwoId, userTwoId, now);
        insertActiveGame(gameId, userOneId, userOneId, 3, 9, now);
        insertParticipant(UUID.randomUUID(), gameId, userOneId, deckOneId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, userTwoId, deckTwoId, 2, now);

        UUID activeInstanceId = UUID.randomUUID();
        UUID benchInstanceId = UUID.randomUUID();
        UUID activeEvolutionInstanceId = UUID.randomUUID();
        UUID benchEvolutionInstanceId = UUID.randomUUID();
        insertCardInstance(activeInstanceId, gameId, userOneId, activeBasicCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(benchInstanceId, gameId, userOneId, benchBasicCardId, CardZone.BENCH, 1, false, now);
        insertCardInstance(activeEvolutionInstanceId, gameId, userOneId, activeEvolutionCardId, CardZone.HAND, 2, false, now);
        insertCardInstance(benchEvolutionInstanceId, gameId, userOneId, benchEvolutionCardId, CardZone.HAND, 3, false, now);

        UUID activePokemonInPlayId = UUID.randomUUID();
        UUID benchPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(activePokemonInPlayId, gameId, userOneId, activeInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(benchPokemonInPlayId, gameId, userOneId, benchInstanceId, 1, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), activePokemonInPlayId, activeInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), benchPokemonInPlayId, benchInstanceId, 0, now);

        return new MultiEvolutionFixture(
                gameId,
                userOneId,
                activePokemonInPlayId,
                benchPokemonInPlayId,
                activeEvolutionCardId,
                benchEvolutionCardId,
                9);
    }

    private RetreatFixture insertRetreatFixture() {
        Instant now = Instant.parse("2026-05-24T20:45:00Z");
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID benchCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();

        insertUser(userOneId, "retreatuserone", now);
        insertUser(userTwoId, "retreatusertwo", now);
        insertPokemonCard(activeCardId, "ActiveRetreatMon", 60, "Water", 1, now);
        insertPokemonCard(benchCardId, "BenchRetreatMon", 60, "Water", 1, now);
        insertEnergyCard(energyCardId, "Water", now);
        insertDeck(deckOneId, userOneId, now);
        insertDeck(deckTwoId, userTwoId, now);
        insertActiveGame(gameId, userOneId, userOneId, 3, 7, now);
        insertParticipant(UUID.randomUUID(), gameId, userOneId, deckOneId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, userTwoId, deckTwoId, 2, now);

        UUID activeInstanceId = UUID.randomUUID();
        UUID benchInstanceId = UUID.randomUUID();
        UUID energyInstanceId = UUID.randomUUID();
        insertCardInstance(activeInstanceId, gameId, userOneId, activeCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(benchInstanceId, gameId, userOneId, benchCardId, CardZone.BENCH, 1, false, now);
        insertCardInstance(energyInstanceId, gameId, userOneId, energyCardId, CardZone.ATTACHED, 1, false, now);

        UUID activePokemonInPlayId = UUID.randomUUID();
        UUID benchPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(activePokemonInPlayId, gameId, userOneId, activeInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(benchPokemonInPlayId, gameId, userOneId, benchInstanceId, 1, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), activePokemonInPlayId, activeInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), benchPokemonInPlayId, benchInstanceId, 0, now);
        insertAttachedCard(UUID.randomUUID(), activePokemonInPlayId, energyInstanceId, "BASIC_ENERGY", now);
        insertSpecialCondition(UUID.randomUUID(), activePokemonInPlayId, "BURNED", now);

        return new RetreatFixture(gameId, userOneId, activePokemonInPlayId, benchPokemonInPlayId, energyCardId, 7);
    }

    private AttackFixture insertAttackFixture() {
        Instant now = Instant.parse("2026-05-24T21:00:00Z");
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID attackerDeckId = UUID.randomUUID();
        UUID defenderDeckId = UUID.randomUUID();
        UUID attackerCardId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID defenderCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();
        UUID prizeCardId = UUID.randomUUID();
        UUID attackId = UUID.randomUUID();

        insertUser(attackerUserId, "attackuserone", now);
        insertUser(defenderUserId, "attackusertwo", now);
        insertPokemonCard(attackerCardId, "FireMon", 60, "Fire", 1, now);
        insertPokemonCard(defenderCardId, "GrassMon", 20, "Grass", 1, now);
        insertEnergyCard(energyCardId, "Fire", now);
        insertPokemonCard(prizeCardId, "PrizeMon", 40, "Colorless", 1, now);
        insertAttack(attackId, attackerCardId, "Flare", 20, 0);
        insertAttackCost(UUID.randomUUID(), attackId, "Fire", 1);
        insertWeakness(UUID.randomUUID(), defenderCardId, "Fire", "x2");
        insertDeck(attackerDeckId, attackerUserId, now);
        insertDeck(defenderDeckId, defenderUserId, now);
        insertActiveGame(gameId, attackerUserId, attackerUserId, TurnPhase.MAIN, 3, 9, now);
        insertParticipant(UUID.randomUUID(), gameId, attackerUserId, attackerDeckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, defenderUserId, defenderDeckId, 2, now);

        UUID attackerActiveInstanceId = UUID.randomUUID();
        UUID defenderActiveInstanceId = UUID.randomUUID();
        UUID attackerEnergyInstanceId = UUID.randomUUID();
        UUID prizeInstanceId = UUID.randomUUID();
        insertCardInstance(attackerActiveInstanceId, gameId, attackerUserId, attackerCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderActiveInstanceId, gameId, defenderUserId, defenderCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(attackerEnergyInstanceId, gameId, attackerUserId, energyCardId, CardZone.ATTACHED, 1, false, now);
        insertCardInstance(prizeInstanceId, gameId, attackerUserId, prizeCardId, CardZone.PRIZE, 1, true, now);

        UUID attackerPokemonInPlayId = UUID.randomUUID();
        UUID defenderPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(attackerPokemonInPlayId, gameId, attackerUserId, attackerActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderPokemonInPlayId, gameId, defenderUserId, defenderActiveInstanceId, 0, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), attackerPokemonInPlayId, attackerActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderPokemonInPlayId, defenderActiveInstanceId, 0, now);
        insertAttachedCard(UUID.randomUUID(), attackerPokemonInPlayId, attackerEnergyInstanceId, "BASIC_ENERGY", now);

        return new AttackFixture(gameId, attackerUserId, defenderUserId, attackId, prizeCardId, 9);
    }

    private AttackFixture insertNonKnockoutAttackFixture() {
        Instant now = Instant.parse("2026-05-24T21:20:00Z");
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID attackerDeckId = UUID.randomUUID();
        UUID defenderDeckId = UUID.randomUUID();
        UUID attackerCardId = UUID.randomUUID();
        UUID defenderCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();
        UUID attackId = UUID.randomUUID();

        insertUser(attackerUserId, "nonkoattackuserone", now);
        insertUser(defenderUserId, "nonkoattackusertwo", now);
        insertPokemonCard(attackerCardId, "StrikeMon", 60, "Fire", 1, now);
        insertPokemonCard(defenderCardId, "DurableMon", 60, "Colorless", 1, now);
        insertEnergyCard(energyCardId, "Fire", now);
        insertAttack(attackId, attackerCardId, "Flare", 20, 0);
        insertAttackCost(UUID.randomUUID(), attackId, "Fire", 1);
        insertDeck(attackerDeckId, attackerUserId, now);
        insertDeck(defenderDeckId, defenderUserId, now);
        insertActiveGame(gameId, attackerUserId, attackerUserId, TurnPhase.MAIN, 3, 13, now);
        insertParticipant(UUID.randomUUID(), gameId, attackerUserId, attackerDeckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, defenderUserId, defenderDeckId, 2, now);

        UUID attackerActiveInstanceId = UUID.randomUUID();
        UUID defenderActiveInstanceId = UUID.randomUUID();
        UUID attackerEnergyInstanceId = UUID.randomUUID();
        insertCardInstance(attackerActiveInstanceId, gameId, attackerUserId, attackerCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderActiveInstanceId, gameId, defenderUserId, defenderCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(attackerEnergyInstanceId, gameId, attackerUserId, energyCardId, CardZone.ATTACHED, 1, false, now);

        UUID attackerPokemonInPlayId = UUID.randomUUID();
        UUID defenderPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(attackerPokemonInPlayId, gameId, attackerUserId, attackerActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderPokemonInPlayId, gameId, defenderUserId, defenderActiveInstanceId, 0, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), attackerPokemonInPlayId, attackerActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderPokemonInPlayId, defenderActiveInstanceId, 0, now);
        insertAttachedCard(UUID.randomUUID(), attackerPokemonInPlayId, attackerEnergyInstanceId, "BASIC_ENERGY", now);

        return new AttackFixture(gameId, attackerUserId, defenderUserId, attackId, null, 13);
    }

    private AttackFixture insertAttackWithDefenderBenchFixture() {
        Instant now = Instant.parse("2026-05-24T21:30:00Z");
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID attackerDeckId = UUID.randomUUID();
        UUID defenderDeckId = UUID.randomUUID();
        UUID attackerCardId = UUID.randomUUID();
        UUID defenderActiveCardId = UUID.randomUUID();
        UUID defenderBenchCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();
        UUID prizeCardId = UUID.randomUUID();
        UUID secondPrizeCardId = UUID.randomUUID();
        UUID attackId = UUID.randomUUID();

        insertUser(attackerUserId, "benchkoattackuserone", now);
        insertUser(defenderUserId, "benchkoattackusertwo", now);
        insertPokemonCard(attackerCardId, "PrizeTakerMon", 60, "Fire", 1, now);
        insertPokemonCard(defenderActiveCardId, "FragileMon", 20, "Colorless", 1, now);
        insertPokemonCard(defenderBenchCardId, "BenchMon", 60, "Colorless", 1, now);
        insertEnergyCard(energyCardId, "Fire", now);
        insertPokemonCard(prizeCardId, "FirstPrizeMon", 40, "Colorless", 1, now);
        insertPokemonCard(secondPrizeCardId, "SecondPrizeMon", 40, "Colorless", 1, now);
        insertAttack(attackId, attackerCardId, "Flare", 20, 0);
        insertAttackCost(UUID.randomUUID(), attackId, "Fire", 1);
        insertDeck(attackerDeckId, attackerUserId, now);
        insertDeck(defenderDeckId, defenderUserId, now);
        insertActiveGame(gameId, attackerUserId, attackerUserId, TurnPhase.MAIN, 3, 15, now);
        insertParticipant(UUID.randomUUID(), gameId, attackerUserId, attackerDeckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, defenderUserId, defenderDeckId, 2, now);

        UUID attackerActiveInstanceId = UUID.randomUUID();
        UUID defenderActiveInstanceId = UUID.randomUUID();
        UUID defenderBenchInstanceId = UUID.randomUUID();
        UUID attackerEnergyInstanceId = UUID.randomUUID();
        UUID prizeInstanceId = UUID.randomUUID();
        UUID secondPrizeInstanceId = UUID.randomUUID();
        insertCardInstance(attackerActiveInstanceId, gameId, attackerUserId, attackerCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderActiveInstanceId, gameId, defenderUserId, defenderActiveCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderBenchInstanceId, gameId, defenderUserId, defenderBenchCardId, CardZone.BENCH, 1, false, now);
        insertCardInstance(attackerEnergyInstanceId, gameId, attackerUserId, energyCardId, CardZone.ATTACHED, 1, false, now);
        insertCardInstance(prizeInstanceId, gameId, attackerUserId, prizeCardId, CardZone.PRIZE, 1, true, now);
        insertCardInstance(secondPrizeInstanceId, gameId, attackerUserId, secondPrizeCardId, CardZone.PRIZE, 2, true, now);

        UUID attackerPokemonInPlayId = UUID.randomUUID();
        UUID defenderActivePokemonInPlayId = UUID.randomUUID();
        UUID defenderBenchPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(attackerPokemonInPlayId, gameId, attackerUserId, attackerActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderActivePokemonInPlayId, gameId, defenderUserId, defenderActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderBenchPokemonInPlayId, gameId, defenderUserId, defenderBenchInstanceId, 1, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), attackerPokemonInPlayId, attackerActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderActivePokemonInPlayId, defenderActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderBenchPokemonInPlayId, defenderBenchInstanceId, 0, now);
        insertAttachedCard(UUID.randomUUID(), attackerPokemonInPlayId, attackerEnergyInstanceId, "BASIC_ENERGY", now);

        return new AttackFixture(gameId, attackerUserId, defenderUserId, attackId, prizeCardId, 15);
    }

    private AttackFixture insertStatusAttackFixture() {
        Instant now = Instant.parse("2026-05-24T21:10:00Z");
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID attackerDeckId = UUID.randomUUID();
        UUID defenderDeckId = UUID.randomUUID();
        UUID defenderCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();

        insertUser(attackerUserId, "statusattackuserone", now);
        insertUser(defenderUserId, "statusattackusertwo", now);
        // Reuse the XY1-1 card that Xy1CardSeeder imports at startup. The seeder is
        // the only writer of "xy1-1" and the cards.external_id column has a UNIQUE
        // constraint, so re-inserting would fail when this test runs in isolation.
        // Falls back to manual insertion if the seeder is unavailable.
        UUID attackerCardId = findOrInsertSeededCard(
                "xy1-1", "VenusaurEX", 60, "Grass", 1, now);
        // Always insert a fresh "Poison Powder" attack with the test's specific
        // baseDamage=0 and Grass:1 cost. The production catalog matches by
        // card.externalId + attack.name, so a fresh attack on the same card still
        // resolves to the catalog's effect definition. The test exercises the
        // status-effect path independently from the real attack damage.
        UUID attackId = insertFixtureAttack(
                attackerCardId, "Poison Powder", "Grass", 1, now);
        insertPokemonCard(defenderCardId, "TargetMon", 60, "Colorless", 1, now);
        insertEnergyCard(energyCardId, "Grass", now);
        insertDeck(attackerDeckId, attackerUserId, now);
        insertDeck(defenderDeckId, defenderUserId, now);
        insertActiveGame(gameId, attackerUserId, attackerUserId, TurnPhase.MAIN, 3, 11, now);
        insertParticipant(UUID.randomUUID(), gameId, attackerUserId, attackerDeckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, defenderUserId, defenderDeckId, 2, now);

        UUID attackerActiveInstanceId = UUID.randomUUID();
        UUID defenderActiveInstanceId = UUID.randomUUID();
        UUID attackerEnergyInstanceId = UUID.randomUUID();
        insertCardInstance(attackerActiveInstanceId, gameId, attackerUserId, attackerCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderActiveInstanceId, gameId, defenderUserId, defenderCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(attackerEnergyInstanceId, gameId, attackerUserId, energyCardId, CardZone.ATTACHED, 1, false, now);

        UUID attackerPokemonInPlayId = UUID.randomUUID();
        UUID defenderPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(attackerPokemonInPlayId, gameId, attackerUserId, attackerActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderPokemonInPlayId, gameId, defenderUserId, defenderActiveInstanceId, 0, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), attackerPokemonInPlayId, attackerActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderPokemonInPlayId, defenderActiveInstanceId, 0, now);
        insertAttachedCard(UUID.randomUUID(), attackerPokemonInPlayId, attackerEnergyInstanceId, "BASIC_ENERGY", now);

        return new AttackFixture(gameId, attackerUserId, defenderUserId, attackId, null, 11);
    }

    private UUID findOrInsertSeededCard(
            String externalId,
            String fallbackName,
            int hp,
            String pokemonType,
            int retreatCost,
            Instant now) {
        List<UUID> existing = jdbcTemplate.queryForList(
                "select id from cards where external_id = ?",
                UUID.class,
                externalId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        UUID cardId = UUID.randomUUID();
        insertPokemonCardWithExternalId(cardId, externalId, fallbackName, hp, pokemonType, retreatCost, now);
        return cardId;
    }

    private AttackFixture insertCrisisVineStatusAttackFixture() {
        Instant now = Instant.parse("2026-05-24T21:11:00Z");
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID attackerDeckId = UUID.randomUUID();
        UUID defenderDeckId = UUID.randomUUID();
        UUID defenderCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();

        insertUser(attackerUserId, "crisisvineuserone", now);
        insertUser(defenderUserId, "crisisvineusertwo", now);
        // xy1-2 (M Venusaur-EX) is the only multi-condition entry in the production
        // catalog and the only writer of "xy1-2" is the seeder. Reuse the seeded
        // card when available, otherwise insert a basic placeholder so this test
        // can run when the external Pokemon TCG API is unreachable.
        UUID attackerCardId = findOrInsertSeededCard(
                "xy1-2", "MVenusaurEX", 60, "Grass", 1, now);
        // Always insert a fresh "Crisis Vine" attack with the test's specific
        // baseDamage=0 and Grass:1 cost. The production catalog matches by
        // card.externalId + attack.name, so a fresh attack on the seeded card
        // still resolves to the multi-condition catalog entry.
        UUID attackId = insertFixtureAttack(
                attackerCardId, "Crisis Vine", "Grass", 1, now);
        insertPokemonCard(defenderCardId, "TargetMon", 60, "Colorless", 1, now);
        insertEnergyCard(energyCardId, "Grass", now);
        insertDeck(attackerDeckId, attackerUserId, now);
        insertDeck(defenderDeckId, defenderUserId, now);
        insertActiveGame(gameId, attackerUserId, attackerUserId, TurnPhase.MAIN, 3, 12, now);
        insertParticipant(UUID.randomUUID(), gameId, attackerUserId, attackerDeckId, 1, now);
        insertParticipant(UUID.randomUUID(), gameId, defenderUserId, defenderDeckId, 2, now);

        UUID attackerActiveInstanceId = UUID.randomUUID();
        UUID defenderActiveInstanceId = UUID.randomUUID();
        UUID attackerEnergyInstanceId = UUID.randomUUID();
        insertCardInstance(attackerActiveInstanceId, gameId, attackerUserId, attackerCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(defenderActiveInstanceId, gameId, defenderUserId, defenderCardId, CardZone.ACTIVE, 0, false, now);
        insertCardInstance(attackerEnergyInstanceId, gameId, attackerUserId, energyCardId, CardZone.ATTACHED, 1, false, now);

        UUID attackerPokemonInPlayId = UUID.randomUUID();
        UUID defenderPokemonInPlayId = UUID.randomUUID();
        insertPokemonInPlay(attackerPokemonInPlayId, gameId, attackerUserId, attackerActiveInstanceId, 0, 1, 0, now);
        insertPokemonInPlay(defenderPokemonInPlayId, gameId, defenderUserId, defenderActiveInstanceId, 0, 1, 0, now);
        insertEvolutionStack(UUID.randomUUID(), attackerPokemonInPlayId, attackerActiveInstanceId, 0, now);
        insertEvolutionStack(UUID.randomUUID(), defenderPokemonInPlayId, defenderActiveInstanceId, 0, now);
        insertAttachedCard(UUID.randomUUID(), attackerPokemonInPlayId, attackerEnergyInstanceId, "BASIC_ENERGY", now);

        return new AttackFixture(gameId, attackerUserId, defenderUserId, attackId, null, 12);
    }

    private UUID insertFixtureAttack(
            UUID cardId,
            String attackName,
            String costEnergyType,
            int costQuantity,
            Instant now) {
        UUID attackId = UUID.randomUUID();
        insertAttack(attackId, cardId, attackName, 0, 0);
        insertAttackCost(UUID.randomUUID(), attackId, costEnergyType, costQuantity);
        return attackId;
    }

    private void insertUser(UUID userId, String username, Instant now) {
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                username + "@example.com",
                username,
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertCard(UUID cardId, String supertype, String category, Instant now) {
        insertCard(cardId, supertype, category, "Card " + cardId, null, now);
    }

    private void insertPokemonCard(UUID cardId, String name, int hp, String pokemonType, int retreatCost, Instant now) {
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, hp, pokemon_type, retreat_cost, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                cardId.toString(),
                "xy1",
                "XY",
                "1",
                name,
                "POKEMON",
                "BASIC_POKEMON",
                "Basic",
                hp,
                pokemonType,
                retreatCost,
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertPokemonCardWithExternalId(
            UUID cardId,
            String externalId,
            String name,
            int hp,
            String pokemonType,
            int retreatCost,
            Instant now) {
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, hp, pokemon_type, retreat_cost, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                externalId,
                "xy1",
                "XY",
                "1",
                name,
                "POKEMON",
                "BASIC_POKEMON",
                "Basic",
                hp,
                pokemonType,
                retreatCost,
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertEnergyCard(UUID cardId, String pokemonType, Instant now) {
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, pokemon_type, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                cardId.toString(),
                "xy1",
                "XY",
                "1",
                "Energy " + cardId,
                "ENERGY",
                "BASIC_ENERGY",
                pokemonType,
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertCard(UUID cardId, String supertype, String category, String name, String evolvesFrom, Instant now) {
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, subtype, evolves_from, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                cardId.toString(),
                "xy1",
                "XY",
                "1",
                name,
                supertype,
                category,
                subtypeForCategory(category),
                evolvesFrom,
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private String subtypeForCategory(String category) {
        return switch (category) {
            case "BASIC_POKEMON" -> "Basic";
            case "STAGE_1_POKEMON" -> "Stage 1";
            case "STAGE_2_POKEMON" -> "Stage 2";
            default -> null;
        };
    }

    private void insertAttack(UUID attackId, UUID cardId, String name, int baseDamage, int attackOrder) {
        jdbcTemplate.update(
                "insert into attacks (id, card_id, name, base_damage, attack_order) values (?, ?, ?, ?, ?)",
                attackId,
                cardId,
                name,
                baseDamage,
                attackOrder);
    }

    private void insertAttackCost(UUID attackCostId, UUID attackId, String energyType, int quantity) {
        jdbcTemplate.update(
                "insert into attack_costs (id, attack_id, energy_type, quantity) values (?, ?, ?, ?)",
                attackCostId,
                attackId,
                energyType,
                quantity);
    }

    private void insertWeakness(UUID weaknessId, UUID cardId, String energyType, String multiplier) {
        jdbcTemplate.update(
                "insert into card_weaknesses (id, card_id, energy_type, multiplier) values (?, ?, ?, ?)",
                weaknessId,
                cardId,
                energyType,
                multiplier);
    }

    private void insertResistance(UUID resistanceId, UUID cardId, String energyType, String resistanceValue) {
        jdbcTemplate.update(
                "insert into card_resistances (id, card_id, energy_type, resistance_value) values (?, ?, ?, ?)",
                resistanceId,
                cardId,
                energyType,
                resistanceValue);
    }

    private void insertDeck(UUID deckId, UUID ownerUserId, Instant now) {
        jdbcTemplate.update(
                "insert into decks (id, owner_user_id, name, format, is_valid, is_active, validation_errors, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deckId,
                ownerUserId,
                "Deck " + deckId,
                "XY1_UNLIMITED",
                true,
                true,
                null,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertDeckCard(UUID deckCardId, UUID deckId, UUID cardId, int quantity) {
        jdbcTemplate.update(
                "insert into deck_cards (id, deck_id, card_id, quantity) values (?, ?, ?, ?)",
                deckCardId,
                deckId,
                cardId,
                quantity);
    }

    private void insertWaitingGame(UUID gameId, Instant now) {
        jdbcTemplate.update(
                "insert into games (id, status, turn_number, state_version, created_at, updated_at) values (?, ?, ?, ?, ?, ?)",
                gameId,
                "WAITING",
                0,
                0,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertActiveGame(UUID gameId, UUID activePlayerId, UUID playerWhoWentFirstId, int turnNumber, int stateVersion, Instant now) {
        insertActiveGame(gameId, activePlayerId, playerWhoWentFirstId, TurnPhase.MAIN, turnNumber, stateVersion, now);
    }

    private void insertActiveGame(
            UUID gameId,
            UUID activePlayerId,
            UUID playerWhoWentFirstId,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            Instant now) {
        jdbcTemplate.update(
                "insert into games (id, status, current_phase, turn_number, state_version, active_player_id, player_who_went_first_id, started_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                gameId,
                "ACTIVE",
                currentPhase.name(),
                turnNumber,
                stateVersion,
                activePlayerId,
                playerWhoWentFirstId,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertParticipant(UUID participantId, UUID gameId, UUID userId, UUID deckId, int playerOrder, Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                gameId,
                userId,
                deckId,
                playerOrder,
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertCardInstance(UUID instanceId, UUID gameId, UUID ownerUserId, UUID cardId, CardZone zone, int zonePosition, boolean faceDown, Instant now) {
        jdbcTemplate.update(
                "insert into game_card_instances (id, game_id, owner_user_id, card_id, zone, zone_position, is_face_down, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                instanceId,
                gameId,
                ownerUserId,
                cardId,
                zone.name(),
                zonePosition,
                faceDown,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertPokemonInPlay(UUID pokemonInPlayId, UUID gameId, UUID ownerUserId, UUID activeCardInstanceId, int slotPosition, int enteredPlayTurn, int damageCounters, Instant now) {
        jdbcTemplate.update(
                "insert into pokemon_in_play (id, game_id, owner_user_id, active_card_instance_id, slot_position, damage_counters, entered_play_turn, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                pokemonInPlayId,
                gameId,
                ownerUserId,
                activeCardInstanceId,
                slotPosition,
                damageCounters,
                enteredPlayTurn,
                Timestamp.from(now));
    }

    private void insertEvolutionStack(UUID stackId, UUID pokemonInPlayId, UUID gameCardInstanceId, int stackOrder, Instant now) {
        insertEvolutionStack(stackId, pokemonInPlayId, gameCardInstanceId, stackOrder, 1, now);
    }

    private void insertEvolutionStack(
            UUID stackId,
            UUID pokemonInPlayId,
            UUID gameCardInstanceId,
            int stackOrder,
            int createdAtTurn,
            Instant now) {
        jdbcTemplate.update(
                "insert into pokemon_evolution_stack (id, pokemon_in_play_id, game_card_instance_id, stack_order, created_at_turn, created_at) values (?, ?, ?, ?, ?, ?)",
                stackId,
                pokemonInPlayId,
                gameCardInstanceId,
                stackOrder,
                createdAtTurn,
                Timestamp.from(now));
    }

    private void insertAttachedCard(UUID attachedCardId, UUID pokemonInPlayId, UUID gameCardInstanceId, String attachedCardType, Instant now) {
        jdbcTemplate.update(
                "insert into pokemon_attached_cards (id, pokemon_in_play_id, game_card_instance_id, attached_card_type, created_at) values (?, ?, ?, ?, ?)",
                attachedCardId,
                pokemonInPlayId,
                gameCardInstanceId,
                attachedCardType,
                Timestamp.from(now));
    }

    private void insertSpecialCondition(UUID conditionId, UUID pokemonInPlayId, String conditionType, Instant now) {
        insertSpecialCondition(conditionId, pokemonInPlayId, conditionType, 1, now);
    }

    private void insertSpecialCondition(UUID conditionId, UUID pokemonInPlayId, String conditionType, int appliedTurn, Instant now) {
        jdbcTemplate.update(
                "insert into special_conditions (id, pokemon_in_play_id, condition_type, applied_turn, created_at) values (?, ?, ?, ?, ?)",
                conditionId,
                pokemonInPlayId,
                conditionType,
                appliedTurn,
                Timestamp.from(now));
    }

    private record TestFixture(UUID gameId, UUID userOneId, UUID userTwoId) {
    }

    private record ActiveMainFixture(
            UUID gameId,
            UUID activePlayerId,
            UUID opponentPlayerId,
            UUID activePokemonInPlayId,
            UUID benchBasicCardId,
            UUID energyCardId,
            UUID energyCardInstanceId,
            UUID evolutionCardId,
            UUID stageTwoEvolutionCardId,
            int expectedStateVersion) {
    }

    private record MultiEvolutionFixture(
            UUID gameId,
            UUID activePlayerId,
            UUID activePokemonInPlayId,
            UUID benchPokemonInPlayId,
            UUID activeEvolutionCardId,
            UUID benchEvolutionCardId,
            int expectedStateVersion) {
    }

    private record RetreatFixture(
            UUID gameId,
            UUID activePlayerId,
            UUID originalActivePokemonInPlayId,
            UUID benchPokemonInPlayId,
            UUID energyCardId,
            int expectedStateVersion) {
    }

    private record AttackFixture(
            UUID gameId,
            UUID attackerUserId,
            UUID defenderUserId,
            UUID attackId,
            UUID prizeCardId,
            int expectedStateVersion) {
    }
}
