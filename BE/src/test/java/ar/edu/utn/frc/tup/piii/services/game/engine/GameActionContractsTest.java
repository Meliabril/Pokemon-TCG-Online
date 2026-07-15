package ar.edu.utn.frc.tup.piii.services.game.engine;




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
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.ConcurrentGameStateException;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameActionContractsTest {

    @Test
    void gameActionRequestDefaultsNullParametersToImmutableEmptyMap() {
        GameActionRequestDto request = new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameActionType.DRAW_CARD,
                0,
                null);

        assertTrue(request.parameters().isEmpty());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                request.parameters().put("cardId", UUID.randomUUID());
            }
        });
    }

    @Test
    void gameActionRequestCopiesParameters() {
        Map<String, Object> parameters = Map.<String, Object>of("cardId", UUID.randomUUID());

        GameActionRequestDto request = new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameActionType.PLAY_BASIC_POKEMON,
                2,
                parameters);

        assertEquals(parameters, request.parameters());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                request.parameters().put("zone", CardZone.BENCH);
            }
        });
    }

    @Test
    void gameActionResponseGroupsActionResultData() {
        GameActionResponseDto response = new GameActionResponseDto(
                true,
                "Action processed successfully",
                2,
                null);

        assertTrue(response.success());
        assertEquals("Action processed successfully", response.message());
        assertEquals(2, response.newStateVersion());
        assertEquals(null, response.data());
    }

    @Test
    void gameStateDefaultsNullActionsToImmutableEmptyList() {
        GameStateDto state = GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.DRAW,
                1,
                3,
                UUID.randomUUID(),
                null,
                Instant.now());

        assertTrue(state.actions().availableActions().isEmpty());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                state.actions().availableActions().add(GameActionType.END_TURN);
            }
        });
    }

    @Test
    void gameStateCopiesNewRuleValidatorStructuresAsImmutableValues() {
        UUID actorUserId = UUID.randomUUID();
        UUID processedActionId = UUID.randomUUID();
        UUID lateActionId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();
        UUID lateCardId = UUID.randomUUID();
        Map<UUID, List<UUID>> cardsInHandByPlayer = new HashMap<>();
        Map<UUID, Set<UUID>> affordableAttacksByPlayer = new HashMap<>();
        Set<UUID> processedClientActionIds = new HashSet<>();
        Map<UUID, String> cardZones = new HashMap<>();
        Map<UUID, UUID> cardOwners = new HashMap<>();
        Map<UUID, Integer> mulliganCountByPlayer = new HashMap<>();
        Map<UUID, Boolean> setupSelectionSubmittedByPlayer = new HashMap<>();
        Map<UUID, UUID> setupActiveCardInstanceIdByPlayer = new HashMap<>();
        Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer = new HashMap<>();
        Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer = new HashMap<>();

        cardsInHandByPlayer.put(actorUserId, List.of(targetCardId));
        cardsInHandInstanceIdsByPlayer.put(actorUserId, List.of(targetCardId));
        affordableAttacksByPlayer.put(actorUserId, Set.of(UUID.randomUUID()));
        processedClientActionIds.add(processedActionId);
        cardZones.put(targetCardId, CardZone.ACTIVE.name());
        cardOwners.put(targetCardId, actorUserId);
        mulliganCountByPlayer.put(actorUserId, 1);
        setupSelectionSubmittedByPlayer.put(actorUserId, Boolean.TRUE);
        setupActiveCardInstanceIdByPlayer.put(actorUserId, targetCardId);
        setupBenchCardInstanceIdsByPlayer.put(actorUserId, List.of(targetCardId));

        GameStateDto state = GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                1,
                3,
                actorUserId,
                List.of(actorUserId),
                false,
                false,
                false,
                Map.<UUID, Integer>of(),
                Map.<UUID, List<SpecialConditionType>>of(),
                1,
                actorUserId,
                Map.<UUID, Integer>of(),
                cardsInHandByPlayer,
                affordableAttacksByPlayer,
                processedClientActionIds,
                cardZones,
                cardOwners,
                mulliganCountByPlayer,
                setupSelectionSubmittedByPlayer,
                setupActiveCardInstanceIdByPlayer,
                setupBenchCardInstanceIdsByPlayer,
                cardsInHandInstanceIdsByPlayer,
                List.of(GameActionType.END_TURN),
                Instant.now());

        processedClientActionIds.add(lateActionId);
        cardZones.put(lateCardId, CardZone.HAND.name());
        cardOwners.put(lateCardId, actorUserId);
        mulliganCountByPlayer.put(actorUserId, 2);
        setupSelectionSubmittedByPlayer.put(actorUserId, Boolean.FALSE);
        setupActiveCardInstanceIdByPlayer.put(actorUserId, lateCardId);
        setupBenchCardInstanceIdsByPlayer.put(actorUserId, List.of(lateCardId));
        cardsInHandInstanceIdsByPlayer.put(actorUserId, List.of(lateCardId));

        assertTrue(state.actions().processedClientActionIds().contains(processedActionId));
        assertFalse(state.actions().processedClientActionIds().contains(lateActionId));
        assertEquals(CardZone.ACTIVE, state.board().zoneByCardReferenceId().get(targetCardId));
        assertFalse(state.board().zoneByCardReferenceId().containsKey(lateCardId));
        assertEquals(actorUserId, state.board().ownerByCardReferenceId().get(targetCardId));
        assertFalse(state.board().ownerByCardReferenceId().containsKey(lateCardId));
        assertEquals(1, GameStateTestFactory.mulliganCountByPlayer(state).get(actorUserId));
        assertTrue(GameStateTestFactory.setupSelectionSubmittedByPlayer(state).get(actorUserId));
        assertEquals(targetCardId, GameStateTestFactory.setupActiveCardInstanceIdByPlayer(state).get(actorUserId));
        assertEquals(List.of(targetCardId), GameStateTestFactory.setupBenchCardInstanceIdsByPlayer(state).get(actorUserId));
        assertEquals(List.of(targetCardId), GameStateTestFactory.cardsInHandInstanceIdsByPlayer(state).get(actorUserId));
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                state.actions().processedClientActionIds().add(UUID.randomUUID());
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                state.board().zoneByCardReferenceId().put(UUID.randomUUID(), CardZone.HAND);
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                state.board().ownerByCardReferenceId().put(UUID.randomUUID(), actorUserId);
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                GameStateTestFactory.setupSelectionSubmittedByPlayer(state).put(actorUserId, Boolean.FALSE);
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                GameStateTestFactory.cardsInHandInstanceIdsByPlayer(state).get(actorUserId).add(UUID.randomUUID());
            }
        });
    }

    @Test
    void gameEventDefaultsNullPayloadToImmutableEmptyMap() {
        GameEventDto event = new GameEventDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameEventType.STATE_SYNC,
                4,
                false,
                Instant.now(),
                null);

        assertTrue(event.payload().isEmpty());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                event.payload().put("stateVersion", 4);
            }
        });
    }

    @Test
    void executionResultDefaultsNullEventsToImmutableEmptyList() {
        GameActionExecutionResult result = new GameActionExecutionResult(sampleState(), null);

        assertTrue(result.emittedEvents().isEmpty());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                result.emittedEvents().add(sampleEvent());
            }
        });
    }

    @Test
    void gameActionContextGroupsActionData() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        GameActionRequestDto request = new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameActionType.END_TURN,
                7,
                Map.<String, Object>of());
        GameStateDto state = sampleState();

        GameActionContext context = new GameActionContext(gameId, actorUserId, request, state);

        assertEquals(gameId, context.gameId());
        assertEquals(actorUserId, context.actorUserId());
        assertEquals(request, context.request());
        assertEquals(state, context.currentState());
    }

    @Test
    void gameActionHandlerDeclaresSupportedActionAndExecutesValidatedContext() {
        GameActionExecutionResult expected = new GameActionExecutionResult(sampleState(), List.of(sampleEvent()));
        GameActionHandler handler = new GameActionHandler() {
            @Override
            public GameActionType supportedAction() {
                return GameActionType.DRAW_CARD;
            }

            @Override
            public GameActionExecutionResult execute(GameActionContext context) {
                return expected;
            }
        };

        GameActionContext context = new GameActionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new GameActionRequestDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        GameActionType.DRAW_CARD,
                        0,
                        Map.<String, Object>of()),
                sampleState());

        assertEquals(GameActionType.DRAW_CARD, handler.supportedAction());
        assertEquals(expected, handler.execute(context));
    }

    @Test
    void gameActionExceptionsExposeDomainCodesAndHttpStatuses() {
        InvalidGameActionException invalid = new InvalidGameActionException("invalid action");
        ForbiddenActionException forbidden = new ForbiddenActionException("forbidden action");
        ConcurrentGameStateException concurrent = new ConcurrentGameStateException("version conflict");

        assertEquals("INVALID_GAME_ACTION", invalid.getErrorCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, invalid.getStatus());
        assertEquals("invalid action", invalid.getMessage());

        assertEquals("FORBIDDEN_ACTION", forbidden.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertEquals("forbidden action", forbidden.getMessage());

        assertEquals("CONCURRENT_GAME_STATE", concurrent.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, concurrent.getStatus());
        assertEquals("version conflict", concurrent.getMessage());
    }

    @Test
    void gameEnumsExposeExpectedContractValues() {
        assertArrayEquals(new GameActionType[] {
                GameActionType.CREATE_GAME,
                GameActionType.JOIN_GAME,
                GameActionType.START_GAME,
                GameActionType.ACK_MULLIGAN_NOTICE,
                GameActionType.CHOOSE_INITIAL_POKEMON,
                GameActionType.PAUSE_GAME,
                GameActionType.RESUME_GAME,
                GameActionType.DRAW_CARD,
                GameActionType.PLAY_BASIC_POKEMON,
                GameActionType.EVOLVE_POKEMON,
                GameActionType.ATTACH_ENERGY,
                GameActionType.PLAY_TRAINER,
                GameActionType.USE_ABILITY,
                GameActionType.RETREAT,
                GameActionType.DECLARE_ATTACK,
                GameActionType.SELECT_TARGET,
                GameActionType.END_TURN,
                GameActionType.PROMOTE_BENCH_POKEMON,
                GameActionType.RESOLVE_ATTACK_CHOICE,
                GameActionType.TAKE_PRIZE_CARD,
                GameActionType.CONCEDE
        }, GameActionType.values());

        assertArrayEquals(new GameStatus[] {
                GameStatus.WAITING,
                GameStatus.SETUP,
                GameStatus.ACTIVE,
                GameStatus.PAUSED,
                GameStatus.FINISHED,
                GameStatus.CANCELLED
        }, GameStatus.values());

        assertArrayEquals(new TurnPhase[] {
                TurnPhase.DRAW,
                TurnPhase.MAIN,
                TurnPhase.ATTACK,
                TurnPhase.BETWEEN_TURNS
        }, TurnPhase.values());

        assertArrayEquals(new CardZone[] {
                CardZone.DECK,
                CardZone.HAND,
                CardZone.PRIZE,
                CardZone.DISCARD,
                CardZone.ACTIVE,
                CardZone.BENCH,
                CardZone.EVOLUTION_STACK,
                CardZone.ATTACHED,
                CardZone.STADIUM,
                CardZone.LOST
        }, CardZone.values());

        assertArrayEquals(new GameEventType[] {
                GameEventType.PLAYER_JOINED,
                GameEventType.PARTICIPANT_PRESENCE_CHANGED,
                GameEventType.GAME_STARTED,
                GameEventType.OPENING_HANDS_DEALT,
                GameEventType.MULLIGAN_REQUIRED,
                GameEventType.MULLIGAN_HAND_REVEALED,
                GameEventType.MULLIGAN_HAND_RETURNED,
                GameEventType.MULLIGAN_DECK_SHUFFLED,
                GameEventType.MULLIGAN_NEW_HAND_DRAWN,
                GameEventType.MULLIGAN_HAND_VALIDATED,
                GameEventType.MULLIGAN_SEQUENCE_COMPLETED,
                GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED,
                GameEventType.MULLIGAN_FLOW_COMPLETED,
                GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED,
                GameEventType.INITIAL_POKEMON_SELECTED,
                GameEventType.INITIAL_BOARD_REVEALED,
                GameEventType.GAME_PAUSED,
                GameEventType.GAME_RESUMED,
                GameEventType.TURN_STARTED,
                GameEventType.PHASE_CHANGED,
                GameEventType.CARD_DRAWN,
                GameEventType.CARD_PLAYED,
                GameEventType.POKEMON_EVOLVED,
                GameEventType.ENERGY_ATTACHED,
                GameEventType.TRAINER_PLAYED,
                GameEventType.RETREAT_DONE,
                GameEventType.ATTACK_DECLARED,
                GameEventType.DAMAGE_APPLIED,
                GameEventType.STATUS_APPLIED,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                GameEventType.PASSIVE_ABILITY_TRIGGERED,
                GameEventType.POKEMON_KNOCKED_OUT,
                GameEventType.PROMOTION_REQUIRED,
                GameEventType.POKEMON_PROMOTED,
                GameEventType.ATTACK_CHOICE_REQUIRED,
                GameEventType.ATTACK_CHOICE_RESOLVED,
                GameEventType.PRIZE_TAKEN,
                GameEventType.SUDDEN_DEATH_REQUIRED,
                GameEventType.GAME_FINISHED,
                GameEventType.INVALID_ACTION,
                GameEventType.COIN_FLIPPED,
                GameEventType.CHAT_MESSAGE,
                GameEventType.STATE_SYNC
        }, GameEventType.values());

        assertArrayEquals(new SpecialConditionType[] {
                SpecialConditionType.ASLEEP,
                SpecialConditionType.BURNED,
                SpecialConditionType.CONFUSED,
                SpecialConditionType.PARALYZED,
                SpecialConditionType.POISONED
        }, SpecialConditionType.values());

        assertArrayEquals(new CardSupertype[] {
                CardSupertype.POKEMON,
                CardSupertype.TRAINER,
                CardSupertype.ENERGY
        }, CardSupertype.values());

        assertArrayEquals(new CardCategory[] {
                CardCategory.BASIC_POKEMON,
                CardCategory.STAGE_1_POKEMON,
                CardCategory.STAGE_2_POKEMON,
                CardCategory.POKEMON_EX,
                CardCategory.MEGA_POKEMON,
                CardCategory.BASIC_ENERGY,
                CardCategory.SPECIAL_ENERGY,
                CardCategory.ITEM_TRAINER,
                CardCategory.SUPPORTER_TRAINER,
                CardCategory.STADIUM_TRAINER,
                CardCategory.POKEMON_TOOL_TRAINER
        }, CardCategory.values());

        assertArrayEquals(new AttachedCardType[] {
                AttachedCardType.BASIC_ENERGY,
                AttachedCardType.SPECIAL_ENERGY,
                AttachedCardType.POKEMON_TOOL
        }, AttachedCardType.values());
    }

    private GameStateDto sampleState() {
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                5,
                UUID.randomUUID(),
                List.of(GameActionType.END_TURN),
                Instant.now());
    }

    private GameEventDto sampleEvent() {
        return new GameEventDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameEventType.STATE_SYNC,
                5,
                false,
                Instant.now(),
                Map.<String, Object>of("stateVersion", 5));
    }
}
