package ar.edu.utn.frc.tup.piii.services.matchmaking.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.MatchmakingQueueEntry;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.exceptions.InactiveUserException;
import ar.edu.utn.frc.tup.piii.exceptions.MatchmakingConflictException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.MatchmakingDeckAccessRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchmakingGameAccessRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchmakingQueueAccessRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchmakingUserAccessRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameMatchBootstrapService;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchFoundEventPublisher;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingQueueStatusNotifier;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchmakingServiceImpl implements MatchmakingService {

    private static final EnumSet<GameStatus> HANDOFF_GAME_STATUSES = EnumSet.of(
            GameStatus.WAITING,
            GameStatus.SETUP,
            GameStatus.ACTIVE,
            GameStatus.PAUSED);
    private static final EnumSet<GameStatus> DECK_CHANGE_LOCK_GAME_STATUSES = EnumSet.of(
            GameStatus.WAITING,
            GameStatus.SETUP,
            GameStatus.ACTIVE);

    private final MatchmakingQueueAccessRepository matchmakingQueueRepository;
    private final MatchmakingGameAccessRepository matchmakingGameRepository;
    private final MatchmakingUserAccessRepository matchmakingUserRepository;
    private final MatchmakingDeckAccessRepository matchmakingDeckRepository;
    private final DeckValidationService deckValidationService;
    private final GameMatchBootstrapService gameMatchBootstrapService;
    private final MatchmakingQueueStatusNotifier queueStatusNotifier;
    private final MatchFoundEventPublisher matchFoundEventPublisher;

    @Override
    @Transactional
    public MatchmakingQueueStatusDto joinQueue(UUID userId) {
        User user = loadActiveUser(userId);

        if (matchmakingQueueRepository.existsByUser_Id(userId)) {
            throw new MatchmakingConflictException("User is already in the matchmaking queue");
        }

        Deck userDeck = loadValidDeck(userId);

        MatchmakingQueueEntry opponentEntry = matchmakingQueueRepository
                .findFirstByUser_IdNotAndDeckIdIsNotNullAndCustomMatchCodeIsNullOrderByCreatedAtAsc(userId)
                .orElse(null);

        if (opponentEntry != null) {
            if (!isQueueEntryDeckValid(opponentEntry)) {
                matchmakingQueueRepository.delete(opponentEntry);
                return queueUser(user, userDeck.getId(), null);
            }
            UUID gameId = gameMatchBootstrapService.createMatch(
                    userId,
                    userDeck.getId(),
                    opponentEntry.getUser().getId(),
                    opponentEntry.getDeckId());
            matchmakingQueueRepository.delete(opponentEntry);
            Instant matchedAt = Instant.now();
            matchFoundEventPublisher.publish(userId, opponentEntry.getUser().getId(), gameId, matchedAt);
            return new MatchmakingQueueStatusDto(false, null, 0, opponentEntry.getUser().getId(), gameId);
        }

        return queueUser(user, userDeck.getId(), null);
    }

    private MatchmakingQueueStatusDto queueUser(User user, UUID deckId, String customMatchCode) {
        MatchmakingQueueEntry queueEntry = new MatchmakingQueueEntry();
        queueEntry.setUser(user);
        queueEntry.setDeckId(deckId);
        queueEntry.setCustomMatchCode(customMatchCode);
        MatchmakingQueueEntry savedEntry = matchmakingQueueRepository.save(queueEntry);
        int queueSize = Math.toIntExact(matchmakingQueueRepository.count());
        MatchmakingQueueStatusDto status = new MatchmakingQueueStatusDto(true, savedEntry.getCreatedAt(), queueSize, null, null);
        queueStatusNotifier.notifyQueueStatus(user.getId(), status);
        return status;
    }

    @Override
    @Transactional
    public void leaveQueue(UUID userId) {
        loadActiveUser(userId);
        matchmakingQueueRepository.deleteByUser_Id(userId);
        queueStatusNotifier.notifyQueueStatus(
                userId,
                new MatchmakingQueueStatusDto(false, null, Math.toIntExact(matchmakingQueueRepository.count()), null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isQueued(UUID userId) {
        loadActiveUser(userId);
        return matchmakingQueueRepository.existsByUser_Id(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveGame(UUID userId) {
        loadActiveUser(userId);
        return matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(
                userId,
                DECK_CHANGE_LOCK_GAME_STATUSES).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchmakingQueueStatusDto getMyQueueStatus(UUID userId) {
        loadActiveUser(userId);

        return matchmakingQueueRepository.findByUser_Id(userId)
                .map(queueEntry -> new MatchmakingQueueStatusDto(
                        true,
                        queueEntry.getCreatedAt(),
                        Math.toIntExact(matchmakingQueueRepository.count()),
                        null,
                        null))
                .orElseGet(() -> findPersistedHandoffGame(userId)
                        .flatMap(game -> matchedStatusFromGame(userId, game))
                        .orElseGet(this::idleStatus));
    }

    private Optional<Game> findPersistedHandoffGame(UUID userId) {
        return matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(userId, HANDOFF_GAME_STATUSES);
    }

    private Optional<MatchmakingQueueStatusDto> matchedStatusFromGame(UUID userId, Game game) {
        return game.getParticipants().stream()
                .map(participant -> participant.getUserId())
                .filter(participantUserId -> !participantUserId.equals(userId))
                .findFirst()
                .map(matchedUserId -> new MatchmakingQueueStatusDto(false, null, 0, matchedUserId, game.getId()));
    }

    private MatchmakingQueueStatusDto idleStatus() {
        return new MatchmakingQueueStatusDto(false, null, 0, null, null);
    }

    private User loadActiveUser(UUID userId) {
        User user = matchmakingUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InactiveUserException("User is not active");
        }

        return user;
    }

    private Deck loadValidDeck(UUID userId) {
        Deck deck = matchmakingDeckRepository.findByOwnerIdAndActiveTrue(userId)
                .orElseThrow(() -> new InvalidDeckException("Active deck not found for user: " + userId));
        DeckValidationResult validationResult = deckValidationService.validate(deck);
        if (!deck.isValid() || !validationResult.valid()) {
            throw new InvalidDeckException("Deck is not valid for matchmaking");
        }
        return deck;
    }

    private boolean isQueueEntryDeckValid(MatchmakingQueueEntry queueEntry) {
        return matchmakingDeckRepository.findByIdAndOwnerIdWithCards(queueEntry.getDeckId(), queueEntry.getUser().getId())
                .map(deck -> deck.isValid() && deckValidationService.validate(deck).valid())
                .orElse(false);
    }

    @Override
    @Transactional
    public MatchmakingQueueStatusDto joinCustomQueue(UUID userId) {
        User user = loadActiveUser(userId);

        if (matchmakingQueueRepository.existsByUser_Id(userId)) {
            throw new MatchmakingConflictException("User is already in the matchmaking queue");
        }

        Deck userDeck = loadValidDeck(userId);
        String myCode = user.getId().toString().substring(0, 6).toUpperCase();

        return queueUser(user, userDeck.getId(), myCode);
    }

    @Override
    @Transactional
    public MatchmakingQueueStatusDto joinCustomGame(UUID userId, String customMatchCode) {
        User user = loadActiveUser(userId);

        if (matchmakingQueueRepository.existsByUser_Id(userId)) {
            throw new MatchmakingConflictException("User is already in the matchmaking queue");
        }

        Deck userDeck = loadValidDeck(userId);

        MatchmakingQueueEntry opponentEntry = matchmakingQueueRepository
                .findByCustomMatchCode(customMatchCode)
                .orElseThrow(() -> new ResourceNotFoundException("Custom match room not found or no longer available"));

        // Validate opponent deck is still valid
        if (!isQueueEntryDeckValid(opponentEntry)) {
            matchmakingQueueRepository.delete(opponentEntry);
            throw new ResourceNotFoundException("Opponent deck is invalid, room closed");
        }

        // Validate we aren't joining our own room
        if (opponentEntry.getUser().getId().equals(userId)) {
            throw new MatchmakingConflictException("You cannot join your own custom room");
        }

        UUID gameId = gameMatchBootstrapService.createMatch(
                userId,
                userDeck.getId(),
                opponentEntry.getUser().getId(),
                opponentEntry.getDeckId());
        
        matchmakingQueueRepository.delete(opponentEntry);
        Instant matchedAt = Instant.now();
        matchFoundEventPublisher.publish(userId, opponentEntry.getUser().getId(), gameId, matchedAt);
        
        return new MatchmakingQueueStatusDto(false, null, 0, opponentEntry.getUser().getId(), gameId);
    }
}
