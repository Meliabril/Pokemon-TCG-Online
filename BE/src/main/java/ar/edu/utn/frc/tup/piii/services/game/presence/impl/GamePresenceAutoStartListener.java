package ar.edu.utn.frc.tup.piii.services.game.presence.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.presence.GameParticipantConnectedEvent;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GamePresenceAutoStartListener {

    private final GameParticipantRepository gameParticipantRepository;
    private final GameRepository gameRepository;
    private final GameService gameService;

    @EventListener
    public void onParticipantConnected(GameParticipantConnectedEvent event) {
        Optional<Game> gameCandidate = gameRepository.findDetailByIdForUpdate(event.gameId());
        if (gameCandidate.isEmpty()) {
            throw new ResourceNotFoundException("Game not found with id: " + event.gameId());
        }
        Game game = gameCandidate.get();

        if (game.getStatus() != GameStatus.WAITING) {
            return;
        }

        List<GameParticipant> participants = gameParticipantRepository.findByGame_IdOrderByPlayerOrderAsc(event.gameId());
        if (participants.size() != 2 || hasDisconnectedParticipant(participants)) {
            return;
        }

        UUID actorUserId = participants.get(0).getUserId();
        GameActionRequestDto request = new GameActionRequestDto(
                event.gameId(),
                UUID.randomUUID(),
                GameActionType.START_GAME,
                game.getStateVersion(),
                Map.of());

        gameService.executeAction(event.gameId(), actorUserId, request);
    }

    private boolean hasDisconnectedParticipant(List<GameParticipant> participants) {
        for (GameParticipant participant : participants) {
            if (!Boolean.TRUE.equals(participant.getConnected())) {
                return true;
            }
        }
        return false;
    }
}
