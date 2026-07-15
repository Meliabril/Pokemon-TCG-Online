package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrawCardsEffectServiceImplTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @InjectMocks
    private DrawCardsEffectServiceImpl service;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @Test
    void drawCards_zeroAmount_returnsEmpty() {
        assertThat(service.drawCards(gameId, playerId, 0)).isEmpty();
    }

    @Test
    void drawCards_negativeAmount_returnsEmpty() {
        assertThat(service.drawCards(gameId, playerId, -1)).isEmpty();
    }

    @Test
    void drawCards_notEnoughCardsInDeck_throws() {
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.DECK))
                .thenReturn(List.of(deckCard()));
        assertThatThrownBy(() -> service.drawCards(gameId, playerId, 3))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("enough cards");
    }

    @Test
    void drawCards_sufficientDeck_movesCardsToHand() {
        GameCardInstance card1 = deckCard();
        GameCardInstance card2 = deckCard();
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.DECK))
                .thenReturn(List.of(card1, card2));
        when(gameCardInstanceStateService.nextZonePosition(gameId, playerId, CardZone.HAND))
                .thenReturn(1).thenReturn(2);
        when(gameCardInstanceStateService.save(card1)).thenReturn(card1);
        when(gameCardInstanceStateService.save(card2)).thenReturn(card2);

        List<GameCardInstance> drawn = service.drawCards(gameId, playerId, 2);

        assertThat(drawn).hasSize(2);
        assertThat(card1.getZone()).isEqualTo(CardZone.HAND);
        assertThat(card1.getFaceDown()).isFalse();
        assertThat(card2.getZone()).isEqualTo(CardZone.HAND);
        verify(gameCardInstanceStateService).resequenceZone(gameId, playerId, CardZone.DECK);
    }

    @Test
    void drawUntilHandSize_zeroTarget_returnsEmpty() {
        assertThat(service.drawUntilHandSize(gameId, playerId, 0)).isEmpty();
    }

    @Test
    void drawUntilHandSize_handAlreadyFull_returnsEmpty() {
        GameCardInstance h1 = handCard(); GameCardInstance h2 = handCard(); GameCardInstance h3 = handCard();
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.HAND))
                .thenReturn(List.of(h1, h2, h3));
        assertThat(service.drawUntilHandSize(gameId, playerId, 3)).isEmpty();
    }

    @Test
    void drawUntilHandSize_drawsToTargetSize() {
        GameCardInstance handCard = handCard();
        GameCardInstance deckCard = deckCard();
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.HAND))
                .thenReturn(List.of(handCard));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.DECK))
                .thenReturn(List.of(deckCard));
        when(gameCardInstanceStateService.nextZonePosition(gameId, playerId, CardZone.HAND)).thenReturn(2);
        when(gameCardInstanceStateService.save(deckCard)).thenReturn(deckCard);

        service.drawUntilHandSize(gameId, playerId, 2);

        assertThat(deckCard.getZone()).isEqualTo(CardZone.HAND);
    }

    private GameCardInstance deckCard() {
        GameCardInstance c = new GameCardInstance();
        c.setId(UUID.randomUUID());
        c.setZone(CardZone.DECK);
        c.setFaceDown(true);
        return c;
    }

    private GameCardInstance handCard() {
        GameCardInstance c = new GameCardInstance();
        c.setId(UUID.randomUUID());
        c.setZone(CardZone.HAND);
        c.setFaceDown(false);
        return c;
    }
}
