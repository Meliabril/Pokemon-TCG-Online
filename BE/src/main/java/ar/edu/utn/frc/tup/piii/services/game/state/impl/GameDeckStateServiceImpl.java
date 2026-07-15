package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.GameDeckStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameDeckStateServiceImpl implements GameDeckStateService {

    private final DeckRepository deckRepository;

    @Override
    @Transactional(readOnly = true)
    public Deck getRequiredDeckWithCards(UUID deckId, UUID ownerUserId) {
        Optional<Deck> deck = deckRepository.findByIdAndOwnerIdWithCards(deckId, ownerUserId);
        if (deck.isEmpty()) {
            throw new InvalidGameActionException("Deck not found for participant " + ownerUserId);
        }
        return deck.get();
    }
}
