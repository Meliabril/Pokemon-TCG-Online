package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GameStateDto(
        UUID gameId,
        GameStatus status,
        int stateVersion,
        List<UUID> playerIds,
        Map<UUID, PlayerStateDto> players,
        Map<UUID, BoardPlayerStateDto> boardPlayers,
        TurnContextDto turn,
        BoardStateDto board,
        ActionStateDto actions,
        ResolutionStateDto resolution,
        Instant updatedAt) {

    public GameStateDto {
        playerIds = copyPlayerIds(playerIds);
        players = copyPlayers(players);
        boardPlayers = copyBoardPlayers(boardPlayers);
        if (turn == null) {
            turn = TurnContextDto.builder().build();
        }
        if (board == null) {
            board = BoardStateDto.builder().build();
        }
        if (actions == null) {
            actions = ActionStateDto.builder().build();
        }
        if (resolution == null) {
            resolution = ResolutionStateDto.builder().build();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .gameId(gameId)
                .status(status)
                .stateVersion(stateVersion)
                .playerIds(playerIds)
                .players(players)
                .boardPlayers(boardPlayers)
                .turn(turn)
                .board(board)
                .actions(actions)
                .resolution(resolution)
                .updatedAt(updatedAt);
    }

    public static final class Builder {

        private UUID gameId;
        private GameStatus status;
        private int stateVersion;
        private List<UUID> playerIds;
        private Map<UUID, PlayerStateDto> players;
        private Map<UUID, BoardPlayerStateDto> boardPlayers;
        private TurnContextDto turn;
        private BoardStateDto board;
        private ActionStateDto actions;
        private ResolutionStateDto resolution;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder gameId(UUID gameId) {
            this.gameId = gameId;
            return this;
        }

        public Builder status(GameStatus status) {
            this.status = status;
            return this;
        }

        public Builder stateVersion(int stateVersion) {
            this.stateVersion = stateVersion;
            return this;
        }

        public Builder playerIds(List<UUID> playerIds) {
            this.playerIds = playerIds;
            return this;
        }

        public Builder players(Map<UUID, PlayerStateDto> players) {
            this.players = players;
            return this;
        }

        public Builder boardPlayers(Map<UUID, BoardPlayerStateDto> boardPlayers) {
            this.boardPlayers = boardPlayers;
            return this;
        }

        public Builder turn(TurnContextDto turn) {
            this.turn = turn;
            return this;
        }

        public Builder board(BoardStateDto board) {
            this.board = board;
            return this;
        }

        public Builder actions(ActionStateDto actions) {
            this.actions = actions;
            return this;
        }

        public Builder resolution(ResolutionStateDto resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public GameStateDto build() {
            return new GameStateDto(
                    gameId,
                    status,
                    stateVersion,
                    playerIds,
                    players,
                    boardPlayers,
                    turn,
                    board,
                    actions,
                    resolution,
                    updatedAt);
        }
    }

    private static List<UUID> copyPlayerIds(List<UUID> source) {
        if (source == null) {
            return List.of();
        }

        return List.copyOf(source);
    }

    private static Map<UUID, PlayerStateDto> copyPlayers(Map<UUID, PlayerStateDto> source) {
        if (source == null) {
            return Map.of();
        }

        Map<UUID, PlayerStateDto> copiedPlayers = new HashMap<>();
        for (Map.Entry<UUID, PlayerStateDto> entry : source.entrySet()) {
            PlayerStateDto playerState = entry.getValue();
            if (playerState == null) {
                playerState = PlayerStateDto.builder().build();
            }
            copiedPlayers.put(entry.getKey(), playerState);
        }

        return Map.copyOf(copiedPlayers);
    }

    private static Map<UUID, BoardPlayerStateDto> copyBoardPlayers(Map<UUID, BoardPlayerStateDto> source) {
        if (source == null) {
            return Map.of();
        }

        Map<UUID, BoardPlayerStateDto> copiedPlayers = new HashMap<>();
        for (Map.Entry<UUID, BoardPlayerStateDto> entry : source.entrySet()) {
            BoardPlayerStateDto playerState = entry.getValue();
            if (playerState == null) {
                playerState = BoardPlayerStateDto.empty(entry.getKey());
            }
            copiedPlayers.put(entry.getKey(), playerState);
        }

        return Map.copyOf(copiedPlayers);
    }
}
