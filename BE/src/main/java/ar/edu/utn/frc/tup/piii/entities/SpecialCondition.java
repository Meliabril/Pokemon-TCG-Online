package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "special_conditions")
@Getter
@Setter
public class SpecialCondition {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pokemon_in_play_id", nullable = false)
    private PokemonInPlay pokemonInPlay;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 40)
    private SpecialConditionType conditionType;

    @Column(name = "applied_turn", nullable = false)
    private Integer appliedTurn;

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
        if (appliedTurn == null) {
            appliedTurn = 0;
        }
    }
}
