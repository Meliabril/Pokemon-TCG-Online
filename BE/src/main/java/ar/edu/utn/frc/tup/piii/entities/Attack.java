package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.BatchSize;

import java.util.*;

@Entity
@Table(name = "attacks", indexes = {
        @Index(name = "ix_attacks_card", columnList = "card_id")
})
@Getter
@Setter
public class Attack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "damage_text", length = 40)
    private String damageText;

    @Column(name = "base_damage")
    private Integer baseDamage;

    @Column(name = "effect_text")
    private String effectText;

    @Column(name = "attack_order", nullable = false)
    private int attackOrder;

    /*
     * Keep this as a Set so CardRepository can load attacks and their costs
     * together with other card relations without triggering Hibernate bag fetch
     * errors. CardMapper controls the response order.
     */
    @OneToMany(mappedBy = "attack", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("energyType ASC")
    @BatchSize(size = 50)
    private Set<AttackCost> costs = new LinkedHashSet<>();

    public void addCost(AttackCost cost) {
        costs.add(cost);
        cost.setAttack(this);
    }
}
