package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.type.*;
import java.time.*;
import java.util.*;

@Entity
@jakarta.persistence.Table(name = "game_action_logs")
@Getter
@Setter
public class GameActionLog {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private GameActionType actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> result;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "client_action_id")
    private UUID clientActionId;

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
