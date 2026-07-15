package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "pokemon_evolution_stack")
@Getter
@Setter
public class PokemonEvolutionStack {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pokemon_in_play_id", nullable = false)
    private PokemonInPlay pokemonInPlay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_card_instance_id", nullable = false)
    private GameCardInstance gameCardInstance;

    @Column(name = "stack_order", nullable = false)
    private Integer stackOrder;

    @Column(name = "created_at_turn", nullable = false)
    private Integer createdAtTurn;

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
