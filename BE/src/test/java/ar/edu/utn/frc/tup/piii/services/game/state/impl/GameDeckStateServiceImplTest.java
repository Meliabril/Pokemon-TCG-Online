package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDeckStateServiceImplTest {

    @Mock
    private DeckRepository deckRepository;

    @InjectMocks
    private GameDeckStateServiceImpl service;

    @Test
    void getRequiredDeckWithCards_found_returnsDeck() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Deck deck = new Deck();
        deck.setId(deckId);
        when(deckRepository.findByIdAndOwnerIdWithCards(deckId, ownerId)).thenReturn(Optional.of(deck));

        assertThat(service.getRequiredDeckWithCards(deckId, ownerId)).isSameAs(deck);
    }

    @Test
    void getRequiredDeckWithCards_notFound_throws() {
        UUID deckId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(deckRepository.findByIdAndOwnerIdWithCards(deckId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequiredDeckWithCards(deckId, ownerId))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Deck not found");
    }
}
