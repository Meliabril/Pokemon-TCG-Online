package ar.edu.utn.frc.tup.piii.services.game.query;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GameDataService {

    Game getRequiredGame(UUID gameId);

    Game getRequiredGameForUpdate(UUID gameId);

    Game getRequiredGameDetail(UUID gameId);

    List<Game> findRecentByParticipantUserIdAndStatusIn(
            UUID userId,
            Collection<GameStatus> statuses,
            Pageable pageable);

    void assertGameExists(UUID gameId);

    Game save(Game game);
}
