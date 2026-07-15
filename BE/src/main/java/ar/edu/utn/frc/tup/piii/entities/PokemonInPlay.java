package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "pokemon_in_play")
@Getter
@Setter
public class PokemonInPlay {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_card_instance_id", nullable = false)
    private GameCardInstance activeCardInstance;

    @Column(name = "slot_position", nullable = false)
    private Integer slotPosition;

    @Column(name = "damage_counters", nullable = false)
    private Integer damageCounters;

    @Column(name = "entered_play_turn", nullable = false)
    private Integer enteredPlayTurn;

    @Column(name = "damage_protection_turn")
    private Integer damageProtectionTurn;

    @Column(name = "damage_protection_threshold")
    private Integer damageProtectionThreshold;

    @Column(name = "attack_locked_turn")
    private Integer attackLockedTurn;

    @Column(name = "retreat_locked_turn")
    private Integer retreatLockedTurn;

    @Column(name = "blocked_attack_turn")
    private Integer blockedAttackTurn;

    @Column(name = "blocked_attack_order")
    private Integer blockedAttackOrder;

    @Column(name = "abilities_disabled_until_turn")
    private Integer abilitiesDisabledUntilTurn;

    @Column(name = "mental_panic_turn")
    private Integer mentalPanicTurn;

    @Column(name = "damage_reduction_next_turn")
    private Integer damageReductionNextTurn;

    @Column(name = "damage_reduction_amount")
    private Integer damageReductionAmount;

    @Column(name = "next_attack_bonus_turn")
    private Integer nextAttackBonusTurn;

    @Column(name = "next_attack_bonus_order")
    private Integer nextAttackBonusOrder;

    @Column(name = "next_attack_bonus_amount")
    private Integer nextAttackBonusAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (damageCounters == null) {
            damageCounters = 0;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
