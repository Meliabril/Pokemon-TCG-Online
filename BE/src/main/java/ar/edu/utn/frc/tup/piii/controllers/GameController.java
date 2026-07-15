package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.game.GameDetailDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PauseGameRequestDto;
import ar.edu.utn.frc.tup.piii.exceptions.BadRequestException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GamePauseUseCase;
import ar.edu.utn.frc.tup.piii.services.game.query.GameQueryService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameResumeUseCase;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameLookupService gameLookupService;
    private final GameQueryService gameQueryService;
    private final GamePauseUseCase gamePauseUseCase;
    private final GameResumeUseCase gameResumeUseCase;
    private final GameService gameService;
    private final TrainerEffectService trainerEffectService;

    @Operation(
            summary = "Get a game by id",
            description = "Returns persisted game metadata and both participants ordered by player order")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Game found",
                    content = @Content(schema = @Schema(implementation = GameDetailDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @GetMapping("/{gameId}")
    public GameDetailDto getById(Authentication authentication, @PathVariable UUID gameId) {
        gameLookupService.assertParticipant(gameId, userId(authentication));
        return gameQueryService.getById(gameId);
    }

    @Operation(summary = "Pause a game", description = "Pauses a game and stores the last resumable snapshot")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Game paused",
                    content = @Content(schema = @Schema(implementation = GameStateDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Invalid pause request",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @PostMapping("/{gameId}/pause")
    public GameStateDto pause(
            Authentication authentication,
            @PathVariable UUID gameId,
            @Valid @RequestBody(required = false) PauseGameRequestDto request) {
        gameLookupService.assertParticipant(gameId, userId(authentication));
        return gamePauseUseCase.pause(gameId, request == null ? null : request.reason());
    }

    @Operation(summary = "Resume a game", description = "Restores a paused game from the latest snapshot")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Game resumed",
                    content = @Content(schema = @Schema(implementation = GameStateDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Game or snapshot not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Invalid resume request",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @PostMapping("/{gameId}/resume")
    public GameStateDto resume(Authentication authentication, @PathVariable UUID gameId) {
        gameLookupService.assertParticipant(gameId, userId(authentication));
        return gameResumeUseCase.resume(gameId);
    }

    @Operation(
            summary = "Execute a game action",
            description = "Entry point for game actions. It validates input and delegates orchestration to GameService.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Action processed",
                    content = @Content(schema = @Schema(implementation = GameActionResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or game id mismatch",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden action",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent game state conflict",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/{gameId}/actions")
    public ResponseEntity<GameActionResponseDto> executeAction(
            Authentication authentication,
            @Parameter(description = "Game identifier.", required = true)
            @PathVariable UUID gameId,
            @Valid @RequestBody GameActionRequestDto request) {

        UUID actorUserId = userId(authentication);

        if (!gameId.equals(request.gameId())) {
            throw new BadRequestException("Path gameId must match body gameId");
        }

        return ResponseEntity.ok(gameService.executeAction(gameId, actorUserId, request));
    }

    @Operation(
            summary = "Preview a Trainer card's choices",
            description = "Read-only lookup of the choices available for a Trainer card still in the "
                    + "player's hand (e.g. Evosoda evolutions found in the deck, Great Ball top-7 "
                    + "Pokemon, Professor's Letter basic energies). Does not mutate game state, does "
                    + "not consume the turn action, and emits no events.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preview computed"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/{gameId}/trainer-preview")
    public ResponseEntity<Map<String, Object>> previewTrainer(
            Authentication authentication,
            @Parameter(description = "Game identifier.", required = true)
            @PathVariable UUID gameId,
            @RequestBody(required = false) Map<String, Object> payload) {

        UUID actorUserId = userId(authentication);
        gameLookupService.assertParticipant(gameId, actorUserId);

        return ResponseEntity.ok(trainerEffectService.previewTrainer(gameId, actorUserId, payload));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
