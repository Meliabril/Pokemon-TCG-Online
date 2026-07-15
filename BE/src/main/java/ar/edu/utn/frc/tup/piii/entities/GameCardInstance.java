package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "game_card_instances")
@Getter
@Setter
public class GameCardInstance {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CardZone zone;

    @Column(name = "zone_position")
    private Integer zonePosition;

    @Column(name = "is_face_down", nullable = false)
    private Boolean faceDown;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (faceDown == null) {
            faceDown = Boolean.FALSE;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
