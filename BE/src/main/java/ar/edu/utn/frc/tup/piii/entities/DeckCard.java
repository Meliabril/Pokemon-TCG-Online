package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity
@Table(name = "deck_cards", indexes = {
        @Index(name = "ix_deck_cards_deck", columnList = "deck_id"),
        @Index(name = "ix_deck_cards_card", columnList = "card_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "ux_deck_card", columnNames = {"deck_id", "card_id"})
})
@Getter
@Setter
public class DeckCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Min(1)
    @Column(nullable = false)
    private int quantity;
}
