package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameChatMessageDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.services.game.chat.GameChatService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameHistoryQueryService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameVisibleEventQueryService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameHistoryController {

    private final GameLookupService gameLookupService;
    private final GameHistoryQueryService gameHistoryQueryService;
    private final GameVisibleEventQueryService gameVisibleEventQueryService;
    private final GameSnapshotService gameSnapshotService;
    private final GameChatService gameChatService;

    @Operation(summary = "Get game history", description = "Returns accepted actions in chronological order")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "History found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GameActionLogEntryDto.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @GetMapping("/{gameId}/history")
    public List<GameActionLogEntryDto> getHistory(Authentication authentication, @PathVariable UUID gameId) {
        gameLookupService.assertParticipant(gameId, userId(authentication));
        return gameHistoryQueryService.getHistory(gameId);
    }

    @Operation(summary = "Get visible game events", description = "Returns the visible event feed for the authenticated player")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Events found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GameEventDto.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @GetMapping("/{gameId}/events")
    public List<GameEventDto> getVisibleEvents(Authentication authentication, @PathVariable UUID gameId) {
        gameLookupService.assertParticipant(gameId, userId(authentication));
        return gameVisibleEventQueryService.getVisibleEvents(gameId, userId(authentication));
    }

    @Operation(summary = "Get latest game snapshot", description = "Returns the latest visible stored game state")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Snapshot found",
                    content = @Content(schema = @Schema(implementation = GameStateDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Snapshot not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @GetMapping("/{gameId}/snapshot/latest")
    public GameStateDto getLatestSnapshot(Authentication authentication, @PathVariable UUID gameId) {
        return gameSnapshotService.findLatestVisibleState(gameId, userId(authentication))
                .orElseThrow(() -> new ResourceNotFoundException("No snapshot found for game " + gameId));
    }

    @Operation(summary = "Get visible game chat history", description = "Returns the persisted public chat history for the game")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Chat history found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GameChatMessageDto.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @GetMapping("/{gameId}/chat")
    public List<GameChatMessageDto> getChatHistory(Authentication authentication, @PathVariable UUID gameId) {
        return gameChatService.getVisibleChatHistory(gameId, userId(authentication));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
