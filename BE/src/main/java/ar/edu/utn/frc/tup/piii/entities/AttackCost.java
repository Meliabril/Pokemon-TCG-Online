package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity
@Table(name = "attack_costs", indexes = {
        @Index(name = "ix_attack_costs_attack", columnList = "attack_id")
})
@Getter
@Setter
public class AttackCost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attack_id", nullable = false)
    private Attack attack;

    @NotBlank
    @Column(name = "energy_type", nullable = false, length = 40)
    private String energyType;

    @Min(1)
    @Column(nullable = false)
    private int quantity;
}
