package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutDetectionService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeValueService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.VictoryConditionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CombatResolutionServiceImpl implements CombatResolutionService {

    private final KnockoutDetectionService knockoutDetectionService;
    private final KnockoutService knockoutService;
    private final PrizeService prizeService;
    private final PrizeValueService prizeValueService;
    private final VictoryConditionService victoryConditionService;
    private final CardService cardService;
    private final GameLookupService gameLookupService;
    private final GameEventFactory gameEventFactory;
    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public CombatResolutionResult resolveKnockoutIfNeeded(
            UUID gameId,
            UUID knockedOutOwnerUserId,
            UUID rewardPlayerId,
            PokemonInPlay damagedPokemon,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            String reason) {
        if (!knockoutDetectionService.isKnockedOut(damagedPokemon)) {
            return CombatResolutionResult.noKnockout();
        }

        List<GameEventDto> events = new ArrayList<>();
        boolean wasActive = damagedPokemon.getSlotPosition() != null && damagedPokemon.getSlotPosition() == 0;
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.POKEMON_KNOCKED_OUT,
                stateVersion,
                knockoutPayload(knockedOutOwnerUserId, damagedPokemon, reason)));

        Card knockedOutTopCard = cardService.getCardEntityById(damagedPokemon.getActiveCardInstance().getCardId());
        int prizeCount = prizeValueService.prizeCardsFor(knockedOutTopCard);
        KnockoutService.KnockoutResult knockoutResult = knockoutService.resolveKnockout(
                gameId,
                knockedOutOwnerUserId,
                damagedPokemon);
        PrizeService.PrizeCardsResult prizeResult = prizeService.takePrizes(gameId, rewardPlayerId, prizeCount);
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.PRIZE_TAKEN,
                stateVersion,
                prizePayload(rewardPlayerId, prizeResult, prizeCount)));

        boolean defeatedPlayerHasPokemonAfterKnockout = true;
        boolean activePokemonAlreadyReplaced = false;
        if (wasActive) {
            defeatedPlayerHasPokemonAfterKnockout = knockoutResult.hasReplacementActivePokemon();
            activePokemonAlreadyReplaced = activePokemonAlreadyReplaced(
                    gameId,
                    knockedOutOwnerUserId,
                    damagedPokemon.getId());
            if (activePokemonAlreadyReplaced) {
                defeatedPlayerHasPokemonAfterKnockout = true;
            }
        }

        boolean playerWins = victoryConditionService.attackerWinsAfterKnockout(
                prizeResult.remainingPrizeCards(),
                defeatedPlayerHasPokemonAfterKnockout);
        if (playerWins) {
            finishGame(gameId, rewardPlayerId);
            events.add(gameEventFactory.publicEvent(
                    gameId,
                    GameEventType.GAME_FINISHED,
                    stateVersion,
                    Map.of("winnerPlayerId", rewardPlayerId.toString(), "reason", reason)));
            return new CombatResolutionResult(true, true, false, rewardPlayerId, List.copyOf(events));
        }

        if (!wasActive) {
            return new CombatResolutionResult(true, false, false, null, List.copyOf(events));
        }

        if (activePokemonAlreadyReplaced) {
            return new CombatResolutionResult(true, false, false, null, List.copyOf(events));
        }

        createPromotionResolution(gameId, knockedOutOwnerUserId, nextActivePlayerId, nextTurnNumber);
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.PROMOTION_REQUIRED,
                stateVersion,
                Map.of(
                        "playerId", knockedOutOwnerUserId.toString(),
                        "nextActivePlayerId", nextActivePlayerId.toString(),
                        "nextTurnNumber", nextTurnNumber)));
        return new CombatResolutionResult(true, false, true, null, List.copyOf(events));
    }

    private Map<String, Object> knockoutPayload(UUID knockedOutOwnerUserId, PokemonInPlay damagedPokemon, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", knockedOutOwnerUserId.toString());
        payload.put("pokemonInPlayId", damagedPokemon.getId().toString());
        if (reason != null) {
            payload.put("reason", reason);
        }
        return Map.copyOf(payload);
    }

    private Map<String, Object> prizePayload(
            UUID rewardPlayerId,
            PrizeService.PrizeCardsResult prizeResult,
            int prizeCount) {
        List<String> cardIds = new ArrayList<>();
        for (UUID cardId : prizeResult.cardIds()) {
            cardIds.add(cardId.toString());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", rewardPlayerId.toString());
        payload.put("cardIds", List.copyOf(cardIds));
        if (!prizeResult.cardIds().isEmpty()) {
            payload.put("cardId", prizeResult.cardIds().get(0).toString());
        }
        payload.put("prizeCount", prizeCount);
        payload.put("remainingPrizeCards", prizeResult.remainingPrizeCards());
        return Map.copyOf(payload);
    }

    private void finishGame(UUID gameId, UUID winnerUserId) {
        Game game = gameLookupService.getRequiredGame(gameId);
        game.setStatus(GameStatus.FINISHED);
        game.setCurrentPhase(null);
        game.setWinnerPlayerId(winnerUserId);
        game.setResolutionState(Map.of());
        game.setFinishedAt(Instant.now());
    }

    private void createPromotionResolution(
            UUID gameId,
            UUID playerToPromoteId,
            UUID nextActivePlayerId,
            int nextTurnNumber) {
        Game game = gameLookupService.getRequiredGame(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.BETWEEN_TURNS);
        game.setResolutionState(promotionResolutionMap(playerToPromoteId, nextActivePlayerId, nextTurnNumber));
        // Restart the turn clock here so the player who must promote gets a full,
        // fair timeout window measured from the moment the obligation began, instead
        // of inheriting whatever was left of the previous turn's clock. TurnTimeoutService
        // relies on this same turnStartedAt value to decide when a pending promotion
        // has timed out.
        game.setTurnStartedAt(Instant.now());
    }

    private Map<String, Object> promotionResolutionMap(
            UUID playerToPromoteId,
            UUID nextActivePlayerId,
            int nextTurnNumber) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(ResolutionStateDto.RESOLUTION_TYPE_KEY, ResolutionStateDto.PROMOTION_REQUIRED);
        values.put(ResolutionStateDto.PLAYER_TO_PROMOTE_ID_KEY, playerToPromoteId.toString());
        values.put(ResolutionStateDto.NEXT_ACTIVE_PLAYER_ID_KEY, nextActivePlayerId.toString());
        values.put(ResolutionStateDto.NEXT_TURN_NUMBER_KEY, nextTurnNumber);
        return Map.copyOf(values);
    }

    private boolean activePokemonAlreadyReplaced(UUID gameId, UUID ownerUserId, UUID knockedOutPokemonId) {
        return pokemonInPlayStateService.findActivePokemon(gameId, ownerUserId)
                .map(activePokemon -> !knockedOutPokemonId.equals(activePokemon.getId()))
                .orElse(false);
    }
}
