package ar.edu.utn.frc.tup.piii.services.game.query.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameDataServiceImpl implements GameDataService {

    private final GameRepository gameRepository;

    @Override
    @Transactional(readOnly = true)
    public Game getRequiredGame(UUID gameId) {
        Optional<Game> game = gameRepository.findById(gameId);
        if (game.isEmpty()) {
            throw new ResourceNotFoundException("Game with id " + gameId + " was not found");
        }
        return game.get();
    }

    @Override
    @Transactional
    public Game getRequiredGameForUpdate(UUID gameId) {
        Optional<Game> game = gameRepository.findDetailByIdForUpdate(gameId);
        if (game.isEmpty()) {
            throw new ResourceNotFoundException("Game with id " + gameId + " was not found");
        }
        return game.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Game getRequiredGameDetail(UUID gameId) {
        Optional<Game> game = gameRepository.findDetailById(gameId);
        if (game.isEmpty()) {
            throw new ResourceNotFoundException("Game with id " + gameId + " was not found");
        }
        return game.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findRecentByParticipantUserIdAndStatusIn(
            UUID userId,
            Collection<GameStatus> statuses,
            Pageable pageable) {
        return gameRepository.findRecentByParticipantUserIdAndStatusIn(userId, statuses, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertGameExists(UUID gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new ResourceNotFoundException("Game with id " + gameId + " was not found");
        }
    }

    @Override
    @Transactional
    public Game save(Game game) {
        return gameRepository.save(game);
    }
}
