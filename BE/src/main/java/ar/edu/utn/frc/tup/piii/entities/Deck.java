package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "decks", indexes = {
        @Index(name = "ix_decks_owner", columnList = "owner_user_id"),
        @Index(name = "ix_decks_owner_active", columnList = "owner_user_id, is_active")
})
@Getter
@Setter
public class Deck {

    public static final String XY1_UNLIMITED_FORMAT = "XY1_UNLIMITED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 40)
    private String format;

    @Column(name = "is_valid", nullable = false)
    private boolean valid;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "validation_errors", columnDefinition = "text")
    private String validationErrors;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckCard> cards = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (format == null) {
            format = XY1_UNLIMITED_FORMAT;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addCard(DeckCard deckCard) {
        cards.add(deckCard);
        deckCard.setDeck(this);
    }

    public void clearCards() {
        cards.clear();
    }
}
