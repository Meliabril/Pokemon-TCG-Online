package ar.edu.utn.frc.tup.piii.controllers.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckActivationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckUpsertRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResponseDto;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/decks")
@Tag(name = "Deck", description = "Authenticated-user deck collection API")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @GetMapping
    @Operation(summary = "List the authenticated user's decks")
    public List<DeckResponseDto> getMyDecks(Authentication authentication) {
        return deckService.getMyDecks(userId(authentication));
    }

    @PostMapping
    @Operation(summary = "Create a deck for the authenticated user")
    public DeckResponseDto createDeck(
            Authentication authentication,
            @Valid @RequestBody DeckUpsertRequestDto request) {
        return deckService.createDeck(userId(authentication), request);
    }

    @PostMapping("/randomize")
    @Operation(summary = "Create a random valid XY1 deck for the authenticated user")
    public DeckResponseDto createRandomDeck(Authentication authentication) {
        return deckService.createRandomDeck(userId(authentication));
    }

    @GetMapping("/{deckId}")
    @Operation(summary = "Get one deck owned by the authenticated user")
    public DeckResponseDto getMyDeck(Authentication authentication, @PathVariable UUID deckId) {
        return deckService.getMyDeck(userId(authentication), deckId);
    }

    @PutMapping("/{deckId}")
    @Operation(summary = "Replace one deck owned by the authenticated user")
    public DeckResponseDto replaceMyDeck(
            Authentication authentication,
            @PathVariable UUID deckId,
            @Valid @RequestBody DeckUpsertRequestDto request) {
        return deckService.replaceMyDeck(userId(authentication), deckId, request);
    }

    @PostMapping("/{deckId}/cards")
    @Operation(summary = "Add a card quantity to one authenticated-user deck")
    public DeckResponseDto addCard(
            Authentication authentication,
            @PathVariable UUID deckId,
            @Valid @RequestBody DeckCardRequestDto request) {
        return deckService.addCard(userId(authentication), deckId, request);
    }

    @DeleteMapping("/{deckId}/cards/{cardId}")
    @Operation(summary = "Remove a card from one authenticated-user deck")
    public DeckResponseDto removeCard(
            Authentication authentication,
            @PathVariable UUID deckId,
            @PathVariable UUID cardId) {
        return deckService.removeCard(userId(authentication), deckId, cardId);
    }

    @GetMapping("/{deckId}/validation")
    @Operation(summary = "Validate one authenticated-user deck")
    public DeckValidationResponseDto validateMyDeck(Authentication authentication, @PathVariable UUID deckId) {
        return deckService.validateMyDeck(userId(authentication), deckId);
    }

    @PutMapping("/{deckId}/activate")
    @Operation(summary = "Activate one authenticated-user deck")
    public DeckActivationResponseDto activateDeck(Authentication authentication, @PathVariable UUID deckId) {
        return deckService.activateDeck(userId(authentication), deckId);
    }

    @PutMapping("/{deckId}/randomize")
    @Operation(summary = "Replace one authenticated-user deck with a random valid XY1 deck")
    public DeckResponseDto randomizeDeck(Authentication authentication, @PathVariable UUID deckId) {
        return deckService.randomizeDeck(userId(authentication), deckId);
    }

    @DeleteMapping("/{deckId}")
    @Operation(summary = "Delete one authenticated-user deck")
    public void deleteDeck(Authentication authentication, @PathVariable UUID deckId) {
        deckService.deleteDeck(userId(authentication), deckId);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
