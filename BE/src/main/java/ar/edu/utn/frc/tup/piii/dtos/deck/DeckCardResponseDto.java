package ar.edu.utn.frc.tup.piii.dtos.deck;

import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;

import java.util.UUID;

public record DeckCardResponseDto(
        UUID id,
        UUID cardId,
        int quantity,
        CardResponseDto card) {
}
