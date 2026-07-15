package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameCardInstanceStateService {

    List<GameCardInstance> findByGameId(UUID gameId);

    List<GameCardInstance> findByGameIdAndOwnerUserIdAndZone(UUID gameId, UUID ownerUserId, CardZone zone);

    Optional<GameCardInstance> findByIdAndGameIdAndOwnerUserId(UUID id, UUID gameId, UUID ownerUserId);

    Optional<GameCardInstance> findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
            UUID gameId,
            UUID ownerUserId,
            UUID cardId,
            CardZone zone);

    GameCardInstance save(GameCardInstance gameCardInstance);

    List<GameCardInstance> saveAll(Iterable<GameCardInstance> gameCardInstances);

    void flush();

    int swapActiveWithBench(UUID activeInstanceId, UUID benchInstanceId, Integer benchSlot);

    int nextZonePosition(UUID gameId, UUID ownerUserId, CardZone zone);

    void resequenceZone(UUID gameId, UUID ownerUserId, CardZone zone);

    void reorderAndPersistZone(UUID gameId, UUID ownerUserId, CardZone targetZone, List<GameCardInstance> cards);
}
