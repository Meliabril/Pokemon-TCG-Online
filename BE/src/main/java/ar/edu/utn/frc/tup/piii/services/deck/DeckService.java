package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckActivationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckUpsertRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResponseDto;

import java.util.List;
import java.util.UUID;

public interface DeckService {

    List<DeckResponseDto> getMyDecks(UUID ownerUserId);

    DeckResponseDto createDeck(UUID ownerUserId, DeckUpsertRequestDto request);

    DeckResponseDto createRandomDeck(UUID ownerUserId);

    DeckResponseDto getMyDeck(UUID ownerUserId, UUID deckId);

    DeckResponseDto replaceMyDeck(UUID ownerUserId, UUID deckId, DeckUpsertRequestDto request);

    DeckResponseDto addCard(UUID ownerUserId, UUID deckId, DeckCardRequestDto request);

    DeckResponseDto removeCard(UUID ownerUserId, UUID deckId, UUID cardId);

    DeckValidationResponseDto validateMyDeck(UUID ownerUserId, UUID deckId);

    DeckActivationResponseDto activateDeck(UUID ownerUserId, UUID deckId);

    DeckResponseDto randomizeDeck(UUID ownerUserId, UUID deckId);

    void deleteDeck(UUID ownerUserId, UUID deckId);
}
