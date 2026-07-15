package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.*;

@Entity
@Table(name = "card_weaknesses", indexes = {
        @Index(name = "ix_card_weaknesses_card", columnList = "card_id")
})
@Getter
@Setter
public class CardWeakness {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotBlank
    @Column(name = "energy_type", nullable = false, length = 40)
    private String energyType;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String multiplier;
}
