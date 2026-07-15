package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameParticipantStateServiceImpl implements GameParticipantStateService {

    private final GameParticipantRepository gameParticipantRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByGameIdAndUserId(UUID gameId, UUID userId) {
        return gameParticipantRepository.existsByGame_IdAndUserId(gameId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameParticipant> findOrderedByGameId(UUID gameId) {
        return gameParticipantRepository.findByGame_IdOrderByPlayerOrderAsc(gameId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameParticipant> findByGameIdAndUserId(UUID gameId, UUID userId) {
        return gameParticipantRepository.findByGame_IdAndUserId(gameId, userId);
    }

    @Override
    @Transactional
    public GameParticipant save(GameParticipant participant) {
        return gameParticipantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPlayerIds(UUID gameId) {
        List<UUID> playerIds = new ArrayList<>();
        for (GameParticipant participant : findOrderedByGameId(gameId)) {
            playerIds.add(participant.getUserId());
        }
        return List.copyOf(playerIds);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findOpponentUserId(UUID gameId, UUID actorUserId) {
        for (GameParticipant participant : findOrderedByGameId(gameId)) {
            UUID userId = participant.getUserId();
            if (!userId.equals(actorUserId)) {
                return userId;
            }
        }
        throw new InvalidGameActionException("Cannot resolve the opponent player for this game");
    }

    @Override
    @Transactional
    public void resetConsecutiveTimeouts(UUID gameId, UUID userId) {
        findByGameIdAndUserId(gameId, userId).ifPresent(participant -> {
            if (participant.getConsecutiveTimeouts() != null && participant.getConsecutiveTimeouts() != 0) {
                participant.setConsecutiveTimeouts(0);
                gameParticipantRepository.save(participant);
            }
        });
    }
}
