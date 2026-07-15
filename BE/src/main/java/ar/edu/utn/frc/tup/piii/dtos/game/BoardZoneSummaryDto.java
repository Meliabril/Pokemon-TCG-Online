package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;

import java.util.List;

public record BoardZoneSummaryDto(
        int count,
        List<BoardCardDto> cards) {

    public BoardZoneSummaryDto {
        if (count < 0) {
            count = 0;
        }
        if (cards == null) {
            cards = List.of();
        } else {
            cards = List.copyOf(cards);
        }
    }

    public static BoardZoneSummaryDto empty() {
        return new BoardZoneSummaryDto(0, List.of());
    }

    public static BoardZoneSummaryDto hidden(CardZone zone, int count) {
        if (count <= 0) {
            return empty();
        }

        return new BoardZoneSummaryDto(count, hiddenCards(zone, count));
    }

    private static List<BoardCardDto> hiddenCards(CardZone zone, int count) {
        java.util.ArrayList<BoardCardDto> cards = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(BoardCardDto.hidden(zone));
        }

        return List.copyOf(cards);
    }
}
