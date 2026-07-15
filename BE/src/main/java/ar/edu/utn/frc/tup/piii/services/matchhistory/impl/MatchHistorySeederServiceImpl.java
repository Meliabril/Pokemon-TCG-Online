package ar.edu.utn.frc.tup.piii.services.matchhistory.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.GenericMessageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import ar.edu.utn.frc.tup.piii.services.matchhistory.MatchHistorySeederService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Profile("!prod")
@RequiredArgsConstructor
public class MatchHistorySeederServiceImpl implements MatchHistorySeederService {

    private static final int MATCHES_TO_CREATE = 15;
    private static final int MIN_TURNS = 5;
    private static final int MAX_TURNS = 30;
    private static final int PLAYER_ONE_ORDER = 1;
    private static final int PLAYER_TWO_ORDER = 2;
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String SEED_PASSWORD = "SeedPassword123!";
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_AVATAR = "seed-admin";
    private static final String SEED_EMAIL_DOMAIN = "@match-history-seed.local";
    private static final int[] FINISHED_DAYS_AGO = {0, 3, 15, 40};
    private static final List<String> OPPONENT_NAMES = List.of(
            "Ash Ketchum",
            "Gary Oak",
            "Cynthia",
            "Misty",
            "Brock",
            "Lance",
            "Dawn",
            "Serena",
            "Leon",
            "Red",
            "Blue",
            "May",
            "Iris",
            "Steven Stone",
            "Professor Oak"
    );

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final GameRepository gameRepository;
    private final PasswordService passwordService;

    @Override
    @Transactional
    public GenericMessageResponseDto seedFinishedMatches() {
        User adminUser = findOrCreateAdminUser();
        Deck adminUserDeck = findOrCreateSeedDeck(adminUser);
        Instant now = Instant.now();

        for (int index = 0; index < MATCHES_TO_CREATE; index++) {
            User opponent = findOrCreateOpponent(OPPONENT_NAMES.get(index));
            Deck opponentDeck = findOrCreateSeedDeck(opponent);
            Game game = buildFinishedGame(adminUser, opponent, index, now);
            game.getParticipants().add(buildParticipant(game, adminUser, adminUserDeck, PLAYER_ONE_ORDER));
            game.getParticipants().add(buildParticipant(game, opponent, opponentDeck, PLAYER_TWO_ORDER));
            gameRepository.save(game);
        }

        return new GenericMessageResponseDto("Se generaron 15 partidas finalizadas para admin.");
    }

    private User findOrCreateAdminUser() {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(ADMIN_EMAIL, ADMIN_USERNAME)
                .orElseGet(this::createAdminUser);
    }

    private User createAdminUser() {
        User user = new User();
        user.setEmail(ADMIN_EMAIL);
        user.setUsername(ADMIN_USERNAME);
        user.setAvatar(ADMIN_AVATAR);
        user.setPasswordHash(passwordService.hash(ADMIN_PASSWORD));
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Game buildFinishedGame(User adminUser, User opponent, int index, Instant now) {
        Instant finishedAt = resolveFinishedAt(index, now);
        UUID winnerPlayerId = resolveWinnerPlayerId(adminUser, opponent, index);

        Game game = new Game();
        game.setStatus(GameStatus.FINISHED);
        game.setCurrentPhase(TurnPhase.ATTACK);
        game.setTurnNumber(randomTurns());
        game.setStateVersion(index + 1);
        game.setWinnerPlayerId(winnerPlayerId);
        game.setPlayerWhoWentFirstId(adminUser.getId());
        game.setStartedAt(finishedAt.minus(Duration.ofHours(1)));
        game.setFinishedAt(finishedAt);
        return game;
    }

    private UUID resolveWinnerPlayerId(User adminUser, User opponent, int index) {
        if (index == 0 || index == 1) {
            return adminUser.getId();
        }

        if (index == 2) {
            return opponent.getId();
        }

        boolean adminUserWon = ThreadLocalRandom.current().nextBoolean();
        if (adminUserWon) {
            return adminUser.getId();
        }

        return opponent.getId();
    }

    private int randomTurns() {
        return ThreadLocalRandom.current().nextInt(MIN_TURNS, MAX_TURNS + 1);
    }

    private Instant resolveFinishedAt(int index, Instant now) {
        int daysAgo = FINISHED_DAYS_AGO[index % FINISHED_DAYS_AGO.length];
        return now.minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours(index));
    }

    private GameParticipant buildParticipant(Game game, User user, Deck deck, int playerOrder) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUserId(user.getId());
        participant.setDeckId(deck.getId());
        participant.setPlayerOrder(playerOrder);
        participant.setConnected(Boolean.FALSE);
        participant.setLastSeenAt(game.getFinishedAt());
        return participant;
    }

    private User findOrCreateOpponent(String opponentName) {
        return userRepository.findByUsernameIgnoreCase(opponentName)
                .orElseGet(() -> createOpponent(opponentName));
    }

    private User createOpponent(String opponentName) {
        String identifier = normalizeIdentifier(opponentName);

        User user = new User();
        user.setEmail(identifier + SEED_EMAIL_DOMAIN);
        user.setUsername(opponentName);
        user.setAvatar("seed-" + identifier);
        user.setPasswordHash(passwordService.hash(SEED_PASSWORD));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Deck findOrCreateSeedDeck(User owner) {
        return deckRepository.findActiveMetadataByOwnerId(owner.getId())
                .orElseGet(() -> createSeedDeck(owner));
    }

    private Deck createSeedDeck(User owner) {
        Deck deck = new Deck();
        deck.setOwner(owner);
        deck.setName("Seed Deck - " + owner.getUsername());
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        deck.setValid(true);
        deck.setActive(true);
        deck.setValidationErrors("");
        return deckRepository.save(deck);
    }

    private String normalizeIdentifier(String value) {
        return value.toLowerCase()
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.|\\.$", "");
    }
}
