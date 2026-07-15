package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.type.*;
import java.time.*;
import java.util.*;

@Entity
@jakarta.persistence.Table(name = "game_events")
@Getter
@Setter
public class GameEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private GameEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "visible_to_user_id")
    private UUID visibleToUserId;

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
