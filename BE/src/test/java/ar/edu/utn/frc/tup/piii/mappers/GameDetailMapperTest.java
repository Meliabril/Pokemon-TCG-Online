package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDetailMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameDetailMapper gameDetailMapper;

    @Test
    void shouldExposeParticipantUsernameWhenUserExists() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-31T14:00:00Z");

        User user = new User();
        user.setId(userId);
        user.setUsername("Misty");

        GameParticipant participant = new GameParticipant();
        participant.setId(UUID.randomUUID());
        participant.setUserId(userId);
        participant.setDeckId(deckId);
        participant.setPlayerOrder(2);
        participant.setConnected(Boolean.TRUE);
        participant.setCreatedAt(createdAt);

        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setStatus(GameStatus.WAITING);
        game.setTurnNumber(0);
        game.setStateVersion(0);
        game.setCreatedAt(createdAt);
        game.setUpdatedAt(createdAt);
        game.setParticipants(List.of(participant));

        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        var dto = gameDetailMapper.toDto(game);

        assertThat(dto.participants()).singleElement().satisfies(player -> {
            assertThat(player.userId()).isEqualTo(userId);
            assertThat(player.username()).isEqualTo("Misty");
        });
    }

    @Test
    void shouldLeaveParticipantUsernameNullWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-31T14:00:00Z");

        GameParticipant participant = new GameParticipant();
        participant.setId(UUID.randomUUID());
        participant.setUserId(userId);
        participant.setDeckId(deckId);
        participant.setPlayerOrder(2);
        participant.setConnected(Boolean.FALSE);
        participant.setCreatedAt(createdAt);

        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setStatus(GameStatus.WAITING);
        game.setTurnNumber(0);
        game.setStateVersion(0);
        game.setCreatedAt(createdAt);
        game.setUpdatedAt(createdAt);
        game.setParticipants(List.of(participant));

        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of());

        var dto = gameDetailMapper.toDto(game);

        assertThat(dto.participants()).singleElement().satisfies(player -> {
            assertThat(player.userId()).isEqualTo(userId);
            assertThat(player.username()).isNull();
        });
    }
}
