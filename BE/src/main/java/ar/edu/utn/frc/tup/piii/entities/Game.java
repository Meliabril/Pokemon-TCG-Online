package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.type.*;
import java.time.*;
import java.util.*;

@Entity
@jakarta.persistence.Table(name = "games")
@Getter
@Setter
public class Game {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameStatus status;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "current_phase", length = 30)
    private TurnPhase currentPhase;

    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;

    @Column(name = "state_version", nullable = false)
    private Integer stateVersion;

    @Column(name = "active_player_id")
    private UUID activePlayerId;

    @Column(name = "turn_started_at")
    private Instant turnStartedAt;

    @Column(name = "player_who_went_first_id")
    private UUID playerWhoWentFirstId;

    @Column(name = "winner_player_id")
    private UUID winnerPlayerId;

    @Column(name = "pause_reason", length = 120)
    private String pauseReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setup_state")
    private Map<String, Object> setupState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolution_state")
    private Map<String, Object> resolutionState;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "game", cascade = jakarta.persistence.CascadeType.ALL)
    @jakarta.persistence.OrderBy("playerOrder ASC")
    private List<GameParticipant> participants = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = GameStatus.WAITING;
        }
        if (turnNumber == null) {
            turnNumber = 0;
        }
        if (stateVersion == null) {
            stateVersion = 0;
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
