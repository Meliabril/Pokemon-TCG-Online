package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscardCardEffectServiceImplTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @InjectMocks
    private DiscardCardEffectServiceImpl service;

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();

    @Test
    void discardFromHand_cardNotFound_throws() {
        UUID cardId = UUID.randomUUID();
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(cardId, gameId, playerId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.discardFromHand(gameId, playerId, cardId))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void discardFromHand_cardNotInHand_throws() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance card = new GameCardInstance();
        card.setId(cardId);
        card.setZone(CardZone.DECK);
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(cardId, gameId, playerId))
                .thenReturn(Optional.of(card));
        assertThatThrownBy(() -> service.discardFromHand(gameId, playerId, cardId))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not in hand");
    }

    @Test
    void discardFromHand_validCard_movesToDiscard() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance card = new GameCardInstance();
        card.setId(cardId);
        card.setZone(CardZone.HAND);
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(cardId, gameId, playerId))
                .thenReturn(Optional.of(card));
        when(gameCardInstanceStateService.nextZonePosition(gameId, playerId, CardZone.DISCARD)).thenReturn(3);
        when(gameCardInstanceStateService.save(card)).thenReturn(card);

        GameCardInstance result = service.discardFromHand(gameId, playerId, cardId);

        assertThat(result.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(result.getZonePosition()).isEqualTo(3);
        assertThat(result.getFaceDown()).isFalse();
        verify(gameCardInstanceStateService).resequenceZone(gameId, playerId, CardZone.HAND);
    }

    @Test
    void discardAttachedCard_nullCard_throws() {
        assertThatThrownBy(() -> service.discardAttachedCard(gameId, playerId, null))
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void discardAttachedCard_nullGameCardInstance_throws() {
        PokemonAttachedCard attached = new PokemonAttachedCard();
        attached.setGameCardInstance(null);
        assertThatThrownBy(() -> service.discardAttachedCard(gameId, playerId, attached))
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void discardAttachedCard_valid_movesToDiscardAndDeletesAttached() {
        GameCardInstance card = new GameCardInstance();
        PokemonAttachedCard attached = new PokemonAttachedCard();
        attached.setGameCardInstance(card);
        when(gameCardInstanceStateService.nextZonePosition(gameId, playerId, CardZone.DISCARD)).thenReturn(1);
        when(gameCardInstanceStateService.save(card)).thenReturn(card);

        service.discardAttachedCard(gameId, playerId, attached);

        assertThat(card.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(card.getFaceDown()).isFalse();
        verify(pokemonAttachedCardStateService).delete(attached);
    }
}
