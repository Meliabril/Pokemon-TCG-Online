package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.game.GameDetailDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameParticipantDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GameDetailMapper {

    private final UserRepository userRepository;

    public GameDetailDto toDto(Game game) {
        Map<UUID, User> usersById = userRepository.findAllById(
                        game.getParticipants().stream().map(GameParticipant::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        List<GameParticipantDto> participants = game.getParticipants().stream()
                .map(participant -> toParticipantDto(participant, usersById))
                .toList();

        return new GameDetailDto(
                game.getId(),
                game.getStatus(),
                game.getCurrentPhase(),
                game.getTurnNumber(),
                game.getStateVersion(),
                game.getActivePlayerId(),
                game.getTurnStartedAt(),
                game.getWinnerPlayerId(),
                game.getPauseReason(),
                game.getStartedAt(),
                game.getPausedAt(),
                game.getFinishedAt(),
                game.getCreatedAt(),
                game.getUpdatedAt(),
                participants);
    }

    private GameParticipantDto toParticipantDto(GameParticipant participant, Map<UUID, User> usersById) {
        User user = usersById.get(participant.getUserId());

        return new GameParticipantDto(
                participant.getId(),
                participant.getUserId(),
                user != null ? user.getUsername() : null,
                user != null ? user.getAvatar() : null,
                participant.getDeckId(),
                participant.getPlayerOrder(),
                participant.getConnected(),
                participant.getLastSeenAt(),
                participant.getCreatedAt());
    }
}
