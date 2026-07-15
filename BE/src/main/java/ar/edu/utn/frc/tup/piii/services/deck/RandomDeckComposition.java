package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.entities.Card;

import java.util.List;

public record RandomDeckComposition(String name, List<CardEntry> cards) {

    public RandomDeckComposition {
        cards = List.copyOf(cards);
    }

    public record CardEntry(Card card, int quantity) {
    }
}
