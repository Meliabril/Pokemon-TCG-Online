package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.services.deck.DeckPersistenceService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameMatchBootstrapService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameMatchBootstrapServiceImpl implements GameMatchBootstrapService {

    private final GameDataService gameDataService;
    private final GameParticipantStateService gameParticipantStateService;
    private final DeckPersistenceService deckPersistenceService;
    private final DeckValidationService deckValidationService;
    private final GameSnapshotService gameSnapshotService;
    private final GameStateQueryService gameStateQueryService;

    @Override
    @Transactional
    public UUID createMatch(UUID userId, UUID userDeckId, UUID opponentUserId, UUID opponentDeckId) {
        Deck userDeck = loadValidDeck(userDeckId, userId);
        Deck opponentDeck = loadValidDeck(opponentDeckId, opponentUserId);

        Game game = gameDataService.save(newInitialGame());
        gameParticipantStateService.save(newParticipant(game, userId, userDeck.getId(), 1));
        gameParticipantStateService.save(newParticipant(game, opponentUserId, opponentDeck.getId(), 2));
        gameSnapshotService.saveSnapshot(
                game.getId(),
                game.getStateVersion(),
                gameStateQueryService.buildVisibleState(game),
                null);
        return game.getId();
    }

    private Game newInitialGame() {
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setStatus(GameStatus.WAITING);
        game.setTurnNumber(0);
        game.setStateVersion(0);
        return game;
    }

    private Deck loadValidDeck(UUID deckId, UUID ownerId) {
        Deck deck = deckPersistenceService.findByIdAndOwnerIdWithCards(deckId, ownerId)
                .orElseThrow(() -> new InvalidDeckException("Deck not found for user: " + ownerId));
        DeckValidationResult validationResult = deckValidationService.validate(deck);
        if (!deck.isValid() || !validationResult.valid()) {
            throw new InvalidDeckException("Deck is not valid for matchmaking");
        }
        return deck;
    }

    private GameParticipant newParticipant(Game game, UUID userId, UUID deckId, int playerOrder) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUserId(userId);
        participant.setDeckId(deckId);
        participant.setPlayerOrder(playerOrder);
        return participant;
    }
}
