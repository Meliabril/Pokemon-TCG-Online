package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeckMapper {

    private final CardMapper cardMapper;
    private final ObjectMapper objectMapper;

    public DeckMapper(CardMapper cardMapper, ObjectMapper objectMapper) {
        this.cardMapper = cardMapper;
        this.objectMapper = objectMapper;
    }

    public DeckResponseDto toDto(Deck deck) {
        return new DeckResponseDto(
                deck.getId(),
                deck.getOwner().getId(),
                deck.getName(),
                deck.getFormat(),
                deck.isActive(),
                deck.isValid(),
                errors(deck.getValidationErrors()),
                deck.getCards().stream()
                        .map(this::card)
                        .toList(),
                deck.getCreatedAt(),
                deck.getUpdatedAt());
    }

    private DeckCardResponseDto card(DeckCard deckCard) {
        return new DeckCardResponseDto(
                deckCard.getId(),
                deckCard.getCard().getId(),
                deckCard.getQuantity(),
                cardMapper.toDto(deckCard.getCard()));
    }

    private List<String> errors(String validationErrors) {
        if (validationErrors == null || validationErrors.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(validationErrors, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of(validationErrors);
        }
    }
}
