package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "matchmaking_queue_entries", indexes = {
        @Index(name = "ux_matchmaking_queue_entries_user", columnList = "user_id", unique = true),
        @Index(name = "ix_matchmaking_queue_entries_created", columnList = "created_at")
})
@Getter
@Setter
public class MatchmakingQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "deck_id", nullable = false)
    private UUID deckId;

    @Column(name = "custom_match_code", length = 10)
    private String customMatchCode;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
