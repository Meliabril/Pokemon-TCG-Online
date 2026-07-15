package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "pokemon_attached_cards")
@Getter
@Setter
public class PokemonAttachedCard {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pokemon_in_play_id", nullable = false)
    private PokemonInPlay pokemonInPlay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_card_instance_id", nullable = false)
    private GameCardInstance gameCardInstance;

    @Enumerated(EnumType.STRING)
    @Column(name = "attached_card_type", nullable = false, length = 40)
    private AttachedCardType attachedCardType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
