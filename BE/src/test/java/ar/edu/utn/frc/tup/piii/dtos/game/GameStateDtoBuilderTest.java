package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameStateDtoBuilderTest {

    @Test
    void builderShouldUseNestedDefaults() {
        UUID gameId = UUID.randomUUID();
        Instant updatedAt = Instant.now();

        GameStateDto state = GameStateDto.builder()
                .gameId(gameId)
                .status(GameStatus.ACTIVE)
                .stateVersion(8)
                .updatedAt(updatedAt)
                .build();

        assertThat(state.gameId()).isEqualTo(gameId);
        assertThat(state.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(state.stateVersion()).isEqualTo(8);
        assertThat(state.playerIds()).isEmpty();
        assertThat(state.players()).isEmpty();
        assertThat(state.boardPlayers()).isEmpty();
        assertThat(state.turn().turnNumber()).isZero();
        assertThat(state.board().zoneByCardReferenceId()).isEmpty();
        assertThat(state.actions().availableActions()).isEmpty();
        assertThat(state.resolution().resolutionType()).isNull();
        assertThat(state.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toBuilderShouldPreserveEveryField() {
        GameStateDto sourceState = fullState();

        GameStateDto copiedState = sourceState.toBuilder().build();

        assertThat(copiedState).isEqualTo(sourceState);
    }

    @Test
    void toBuilderShouldOverrideOnlyRequestedFields() {
        GameStateDto sourceState = fullState();
        Instant updatedAt = Instant.now();

        TurnContextDto newTurn = sourceState.turn().toBuilder()
                .supporterPlayedThisTurn(false)
                .build();

        GameStateDto copiedState = sourceState.toBuilder()
                .stateVersion(99)
                .turn(newTurn)
                .updatedAt(updatedAt)
                .build();

        assertThat(copiedState.stateVersion()).isEqualTo(99);
        assertThat(copiedState.turn().supporterPlayedThisTurn()).isFalse();
        assertThat(copiedState.updatedAt()).isEqualTo(updatedAt);
        assertThat(copiedState.gameId()).isEqualTo(sourceState.gameId());
        assertThat(copiedState.board().zoneByCardReferenceId()).isEqualTo(sourceState.board().zoneByCardReferenceId());
        assertThat(sourceState.turn().supporterPlayedThisTurn()).isTrue();
    }

    @Test
    void nestedCollectionsShouldRemainImmutable() {
        UUID playerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID attackId = UUID.randomUUID();
        List<UUID> playerIds = new ArrayList<>();
        playerIds.add(playerId);
        List<SpecialConditionType> conditions = new ArrayList<>();
        conditions.add(SpecialConditionType.ASLEEP);
        Set<UUID> affordableAttacks = new HashSet<>();
        affordableAttacks.add(attackId);

        PlayerStateDto playerState = PlayerStateDto.builder()
                .activePokemonConditions(conditions)
                .affordableAttackIds(affordableAttacks)
                .build();
        Map<UUID, PlayerStateDto> players = new HashMap<>();
        players.put(playerId, playerState);

        BoardStateDto board = BoardStateDto.builder()
                .zoneByCardReferenceId(Map.of(cardId, CardZone.HAND))
                .build();

        GameStateDto state = GameStateDto.builder()
                .gameId(UUID.randomUUID())
                .status(GameStatus.ACTIVE)
                .stateVersion(1)
                .playerIds(playerIds)
                .players(players)
                .turn(TurnContextDto.builder()
                        .currentPhase(TurnPhase.MAIN)
                        .activePlayerId(playerId)
                        .build())
                .board(board)
                .actions(ActionStateDto.builder()
                        .availableActions(List.of(GameActionType.END_TURN))
                        .build())
                .updatedAt(Instant.now())
                .build();

        playerIds.add(UUID.randomUUID());
        conditions.add(SpecialConditionType.PARALYZED);
        affordableAttacks.add(UUID.randomUUID());

        assertThat(state.playerIds()).containsExactly(playerId);
        assertThat(state.players().get(playerId).activePokemonConditions()).containsExactly(SpecialConditionType.ASLEEP);
        assertThat(state.players().get(playerId).affordableAttackIds()).containsExactly(attackId);
        assertThrows(UnsupportedOperationException.class, addPlayerId(state));
        assertThrows(UnsupportedOperationException.class, addCondition(state, playerId));
        assertThrows(UnsupportedOperationException.class, addAffordableAttack(state, playerId));
    }

    private GameStateDto fullState() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID cardInstanceId = UUID.randomUUID();
        UUID attackId = UUID.randomUUID();
        UUID clientActionId = UUID.randomUUID();
        Instant updatedAt = Instant.now();

        PlayerStateDto playerState = PlayerStateDto.builder()
                .benchPokemonCount(2)
                .activePokemonConditions(List.of(SpecialConditionType.POISONED))
                .cardIdsInHand(List.of(cardId))
                .cardInstanceIdsInHand(List.of(cardInstanceId))
                .affordableAttackIds(Set.of(attackId))
                .mulliganCount(1)
                .initialPokemonSelectionSubmitted(true)
                .initialActiveCardInstanceId(cardInstanceId)
                .initialBenchCardInstanceIds(List.of(cardInstanceId))
                .build();

        return GameStateDto.builder()
                .gameId(gameId)
                .status(GameStatus.ACTIVE)
                .stateVersion(12)
                .playerIds(List.of(playerOneId, playerTwoId))
                .players(Map.of(playerOneId, playerState))
                .boardPlayers(Map.of(playerOneId, BoardPlayerStateDto.empty(playerOneId)))
                .turn(TurnContextDto.builder()
                        .currentPhase(TurnPhase.MAIN)
                        .turnNumber(6)
                        .activePlayerId(playerOneId)
                        .playerWhoWentFirstId(playerTwoId)
                        .energyAttachedThisTurn(true)
                        .supporterPlayedThisTurn(true)
                        .retreatedThisTurn(true)
                        .build())
                .board(BoardStateDto.builder()
                        .enteredPlayTurnByPokemonInPlayId(Map.of(pokemonInPlayId, 3))
                        .zoneByCardReferenceId(Map.of(cardId, CardZone.HAND, pokemonInPlayId, CardZone.ACTIVE))
                        .ownerByCardReferenceId(Map.of(cardId, playerOneId, pokemonInPlayId, playerOneId))
                        .build())
                .actions(ActionStateDto.builder()
                        .availableActions(List.of(GameActionType.PLAY_TRAINER, GameActionType.END_TURN))
                        .processedClientActionIds(Set.of(clientActionId))
                        .build())
                .resolution(ResolutionStateDto.builder()
                        .resolutionType(ResolutionStateDto.PROMOTION_REQUIRED)
                        .playerToPromoteId(playerTwoId)
                        .nextActivePlayerId(playerOneId)
                        .nextTurnNumber(7)
                        .build())
                .updatedAt(updatedAt)
                .build();
    }

    private Executable addPlayerId(GameStateDto state) {
        return new Executable() {
            @Override
            public void execute() {
                state.playerIds().add(UUID.randomUUID());
            }
        };
    }

    private Executable addCondition(GameStateDto state, UUID playerId) {
        return new Executable() {
            @Override
            public void execute() {
                state.players().get(playerId).activePokemonConditions().add(SpecialConditionType.PARALYZED);
            }
        };
    }

    private Executable addAffordableAttack(GameStateDto state, UUID playerId) {
        return new Executable() {
            @Override
            public void execute() {
                state.players().get(playerId).affordableAttackIds().add(UUID.randomUUID());
            }
        };
    }
}
