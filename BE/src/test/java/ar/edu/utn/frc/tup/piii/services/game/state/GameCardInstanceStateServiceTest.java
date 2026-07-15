package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.repositories.GameCardInstanceRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameCardInstanceStateServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameCardInstanceStateServiceTest {

    @Test
    void shouldResequenceZoneThroughTemporaryUniquePositions() {
        GameCardInstanceRepository repository = mock(GameCardInstanceRepository.class);
        GameCardInstanceStateServiceImpl service = new GameCardInstanceStateServiceImpl(repository);
        UUID gameId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        GameCardInstance first = cardAtPosition(2);
        GameCardInstance second = cardAtPosition(3);
        List<GameCardInstance> cards = List.of(first, second);
        when(repository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                gameId,
                ownerUserId,
                CardZone.HAND)).thenReturn(cards);
        doAnswer(invocation -> {
            assertThat(cards).extracting(GameCardInstance::getZonePosition).containsExactly(-1, -2);
            return null;
        }).when(repository).flush();

        service.resequenceZone(gameId, ownerUserId, CardZone.HAND);

        assertThat(cards).extracting(GameCardInstance::getZonePosition).containsExactly(1, 2);
        org.mockito.InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
                gameId,
                ownerUserId,
                CardZone.HAND);
        inOrder.verify(repository).flush();
    }

    private GameCardInstance cardAtPosition(int position) {
        GameCardInstance card = new GameCardInstance();
        card.setId(UUID.randomUUID());
        card.setZonePosition(position);
        return card;
    }
}
