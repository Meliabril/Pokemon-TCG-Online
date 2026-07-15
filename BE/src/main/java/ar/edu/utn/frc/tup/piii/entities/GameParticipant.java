package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "game_participants")
@Getter
@Setter
public class GameParticipant {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "deck_id", nullable = false)
    private UUID deckId;

    @Column(name = "player_order", nullable = false)
    private Integer playerOrder;

    @Column(name = "is_connected", nullable = false)
    private Boolean connected;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "supporter_locked_turn")
    private Integer supporterLockedTurn;

    @Column(name = "consecutive_timeouts", nullable = false)
    private Integer consecutiveTimeouts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (connected == null) {
            connected = Boolean.FALSE;
        }
        if (consecutiveTimeouts == null) {
            consecutiveTimeouts = 0;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
