package ar.edu.utn.frc.tup.piii.services.matchmaking;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.MatchmakingQueueEntry;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
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
import ar.edu.utn.frc.tup.piii.services.matchmaking.impl.MatchmakingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MatchmakingServiceTest {

    private MatchmakingQueueAccessRepository matchmakingQueueRepository;
    private MatchmakingGameAccessRepository matchmakingGameRepository;
    private MatchmakingUserAccessRepository matchmakingUserRepository;
    private MatchmakingDeckAccessRepository matchmakingDeckRepository;
    private DeckValidationService deckValidationService;
    private GameMatchBootstrapService gameMatchBootstrapService;
    private MatchmakingQueueStatusNotifier queueStatusNotifier;
    private MatchFoundEventPublisher matchFoundEventPublisher;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchmakingQueueRepository = mock(MatchmakingQueueAccessRepository.class);
        matchmakingGameRepository = mock(MatchmakingGameAccessRepository.class);
        matchmakingUserRepository = mock(MatchmakingUserAccessRepository.class);
        matchmakingDeckRepository = mock(MatchmakingDeckAccessRepository.class);
        deckValidationService = mock(DeckValidationService.class);
        gameMatchBootstrapService = mock(GameMatchBootstrapService.class);
        queueStatusNotifier = mock(MatchmakingQueueStatusNotifier.class);
        matchFoundEventPublisher = mock(MatchFoundEventPublisher.class);
        matchmakingService = new MatchmakingServiceImpl(
                matchmakingQueueRepository,
                matchmakingGameRepository,
                matchmakingUserRepository,
                matchmakingDeckRepository,
                deckValidationService,
                gameMatchBootstrapService,
                queueStatusNotifier,
                matchFoundEventPublisher);
    }

    @Test
    void shouldQueueUserWithActiveDeckWhenNoOpponentExists() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        User user = activeUser(userId);
        Deck deck = validDeck(deckId, user);
        MatchmakingQueueEntry savedEntry = queueEntry(user, Instant.parse("2026-05-20T10:00:00Z"));
        savedEntry.setDeckId(deckId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingDeckRepository.findByOwnerIdAndActiveTrue(userId)).thenReturn(Optional.of(deck));
        when(deckValidationService.validate(deck)).thenReturn(new DeckValidationResult(true, List.of()));
        when(matchmakingQueueRepository.existsByUser_Id(userId)).thenReturn(false);
        when(matchmakingQueueRepository.findFirstByUser_IdNotAndDeckIdIsNotNullAndCustomMatchCodeIsNullOrderByCreatedAtAsc(userId)).thenReturn(Optional.empty());
        when(matchmakingQueueRepository.save(any(MatchmakingQueueEntry.class))).thenReturn(savedEntry);
        when(matchmakingQueueRepository.count()).thenReturn(1L);

        MatchmakingQueueStatusDto result = matchmakingService.joinQueue(userId);

        assertThat(result.queued()).isTrue();
        ArgumentCaptor<MatchmakingQueueEntry> captor = ArgumentCaptor.forClass(MatchmakingQueueEntry.class);
        verify(matchmakingQueueRepository).save(captor.capture());
        assertThat(captor.getValue().getDeckId()).isEqualTo(deckId);
        verify(queueStatusNotifier).notifyQueueStatus(userId, result);
    }

    @Test
    void shouldMatchUsersUsingTheirActiveDecks() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        User user = activeUser(userId);
        User opponent = activeUser(opponentUserId);
        Deck deck = validDeck(deckId, user);
        Deck opponentDeck = validDeck(opponentDeckId, opponent);
        MatchmakingQueueEntry opponentEntry = queueEntry(opponent, Instant.parse("2026-05-20T09:59:00Z"));
        opponentEntry.setDeckId(opponentDeckId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingDeckRepository.findByOwnerIdAndActiveTrue(userId)).thenReturn(Optional.of(deck));
        when(matchmakingDeckRepository.findByIdAndOwnerIdWithCards(opponentDeckId, opponentUserId)).thenReturn(Optional.of(opponentDeck));
        when(deckValidationService.validate(deck)).thenReturn(new DeckValidationResult(true, List.of()));
        when(deckValidationService.validate(opponentDeck)).thenReturn(new DeckValidationResult(true, List.of()));
        when(matchmakingQueueRepository.existsByUser_Id(userId)).thenReturn(false);
        when(matchmakingQueueRepository.findFirstByUser_IdNotAndDeckIdIsNotNullAndCustomMatchCodeIsNullOrderByCreatedAtAsc(userId))
                .thenReturn(Optional.of(opponentEntry));
        when(gameMatchBootstrapService.createMatch(userId, deckId, opponentUserId, opponentDeckId)).thenReturn(gameId);

        MatchmakingQueueStatusDto result = matchmakingService.joinQueue(userId);

        assertThat(result.queued()).isFalse();
        assertThat(result.matchedUserId()).isEqualTo(opponentUserId);
        assertThat(result.gameId()).isEqualTo(gameId);
        verify(gameMatchBootstrapService).createMatch(userId, deckId, opponentUserId, opponentDeckId);
        verify(matchFoundEventPublisher).publish(eq(userId), eq(opponentUserId), eq(gameId), any(Instant.class));
        verifyNoInteractions(queueStatusNotifier);
    }

    @Test
    void shouldReturnPersistedGameWhenUserIsNoLongerQueued() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        User user = activeUser(userId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingQueueRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(matchmakingQueueRepository.count()).thenReturn(0L);
        when(matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(any(), any()))
                .thenReturn(Optional.of(game(gameId, userId, opponentUserId)));

        MatchmakingQueueStatusDto result = matchmakingService.getMyQueueStatus(userId);

        assertThat(result.queued()).isFalse();
        assertThat(result.queueSize()).isZero();
        assertThat(result.gameId()).isEqualTo(gameId);
        assertThat(result.matchedUserId()).isEqualTo(opponentUserId);
    }

    @Test
    void shouldReturnMatchedStatusWithZeroQueueSizeEvenWhenOtherUsersRemainQueued() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        User user = activeUser(userId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingQueueRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(matchmakingQueueRepository.count()).thenReturn(4L);
        when(matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(any(), any()))
                .thenReturn(Optional.of(game(gameId, userId, opponentUserId)));

        MatchmakingQueueStatusDto result = matchmakingService.getMyQueueStatus(userId);

        assertThat(result.queued()).isFalse();
        assertThat(result.queueSize()).isZero();
        assertThat(result.gameId()).isEqualTo(gameId);
        assertThat(result.matchedUserId()).isEqualTo(opponentUserId);
    }

    @Test
    void shouldNotReturnGameIdWithoutMatchedUserId() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        User user = activeUser(userId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingQueueRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(matchmakingQueueRepository.count()).thenReturn(2L);
        when(matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(any(), any()))
                .thenReturn(Optional.of(game(gameId, userId)));

        MatchmakingQueueStatusDto result = matchmakingService.getMyQueueStatus(userId);

        assertThat(result.queued()).isFalse();
        assertThat(result.queueSize()).isZero();
        assertThat(result.gameId()).isNull();
        assertThat(result.matchedUserId()).isNull();
    }

    @Test
    void shouldReturnQueuedStatusWithoutGameIdWhenUserStillInQueue() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);
        MatchmakingQueueEntry queueEntry = queueEntry(user, Instant.parse("2026-05-20T10:00:00Z"));

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingQueueRepository.findByUser_Id(userId)).thenReturn(Optional.of(queueEntry));
        when(matchmakingQueueRepository.count()).thenReturn(1L);

        MatchmakingQueueStatusDto result = matchmakingService.getMyQueueStatus(userId);

        assertThat(result.queued()).isTrue();
        assertThat(result.gameId()).isNull();
        assertThat(result.matchedUserId()).isNull();
    }

    @Test
    void shouldReturnIdleStatusWithZeroQueueSizeWhenUserHasNoActiveMatch() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingQueueRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(matchmakingQueueRepository.count()).thenReturn(3L);
        when(matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(any(), any())).thenReturn(Optional.empty());

        MatchmakingQueueStatusDto result = matchmakingService.getMyQueueStatus(userId);

        assertThat(result.queued()).isFalse();
        assertThat(result.queueSize()).isZero();
        assertThat(result.gameId()).isNull();
        assertThat(result.matchedUserId()).isNull();
    }

    @Test
    void shouldReportActiveGameWhenUserHasDeckLockedGame() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingGameRepository.findLatestByParticipantUserIdAndStatusIn(any(), any()))
                .thenReturn(Optional.of(game(UUID.randomUUID(), userId, UUID.randomUUID())));

        assertThat(matchmakingService.hasActiveGame(userId)).isTrue();
    }

    @Test
    void shouldRejectJoinWhenActiveDeckIsMissing() {
        UUID userId = UUID.randomUUID();
        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(matchmakingQueueRepository.existsByUser_Id(userId)).thenReturn(false);
        when(matchmakingDeckRepository.findByOwnerIdAndActiveTrue(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchmakingService.joinQueue(userId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("Active deck not found");
    }

    @Test
    void shouldRejectJoinWhenActiveDeckIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        User user = activeUser(userId);
        Deck invalidDeck = validDeck(deckId, user);
        invalidDeck.setValid(false);

        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(matchmakingQueueRepository.existsByUser_Id(userId)).thenReturn(false);
        when(matchmakingDeckRepository.findByOwnerIdAndActiveTrue(userId)).thenReturn(Optional.of(invalidDeck));
        when(deckValidationService.validate(invalidDeck)).thenReturn(new DeckValidationResult(true, List.of()));

        assertThatThrownBy(() -> matchmakingService.joinQueue(userId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("not valid for matchmaking");

        verify(matchmakingQueueRepository, never()).save(any(MatchmakingQueueEntry.class));
    }

    @Test
    void shouldRejectJoinWhenUserAlreadyQueued() {
        UUID userId = UUID.randomUUID();
        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(matchmakingQueueRepository.existsByUser_Id(userId)).thenReturn(true);

        assertThatThrownBy(() -> matchmakingService.joinQueue(userId))
                .isInstanceOf(MatchmakingConflictException.class)
                .hasMessageContaining("already in the matchmaking queue");
    }

    @Test
    void shouldRejectJoinWhenUserIsInactive() {
        UUID userId = UUID.randomUUID();
        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(blockedUser(userId)));

        assertThatThrownBy(() -> matchmakingService.joinQueue(userId))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldRejectJoinWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchmakingService.joinQueue(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void shouldLeaveQueueAndNotifyUser() {
        UUID userId = UUID.randomUUID();
        when(matchmakingUserRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));
        when(matchmakingQueueRepository.count()).thenReturn(0L);

        matchmakingService.leaveQueue(userId);

        verify(matchmakingQueueRepository).deleteByUser_Id(userId);
        verify(queueStatusNotifier).notifyQueueStatus(userId, new MatchmakingQueueStatusDto(false, null, 0, null, null));
    }

    private Game game(UUID gameId, UUID firstUserId, UUID secondUserId) {
        Game game = new Game();
        game.setId(gameId);
        game.setParticipants(List.of(
                participant(game, firstUserId, 1),
                participant(game, secondUserId, 2)));
        return game;
    }

    private Game game(UUID gameId, UUID onlyUserId) {
        Game game = new Game();
        game.setId(gameId);
        game.setParticipants(List.of(participant(game, onlyUserId, 1)));
        return game;
    }

    private ar.edu.utn.frc.tup.piii.entities.GameParticipant participant(Game game, UUID userId, int playerOrder) {
        ar.edu.utn.frc.tup.piii.entities.GameParticipant participant = new ar.edu.utn.frc.tup.piii.entities.GameParticipant();
        participant.setGame(game);
        participant.setUserId(userId);
        participant.setPlayerOrder(playerOrder);
        return participant;
    }

    private MatchmakingQueueEntry queueEntry(User user, Instant createdAt) {
        MatchmakingQueueEntry entry = new MatchmakingQueueEntry();
        entry.setId(UUID.randomUUID());
        entry.setUser(user);
        entry.setCreatedAt(createdAt);
        return entry;
    }

    private Deck validDeck(UUID deckId, User owner) {
        Deck deck = new Deck();
        deck.setId(deckId);
        deck.setOwner(owner);
        deck.setName("Deck");
        deck.setValid(true);
        deck.setActive(true);
        return deck;
    }

    private User activeUser(UUID userId) {
        return user(userId, UserStatus.ACTIVE);
    }

    private User blockedUser(UUID userId) {
        return user(userId, UserStatus.BLOCKED);
    }

    private User user(UUID userId, UserStatus status) {
        User user = new User();
        user.setId(userId);
        user.setEmail("trainer@example.com");
        user.setUsername("trainer");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(status);
        user.setCreatedAt(Instant.parse("2026-05-20T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-20T10:00:00Z"));
        return user;
    }
}
