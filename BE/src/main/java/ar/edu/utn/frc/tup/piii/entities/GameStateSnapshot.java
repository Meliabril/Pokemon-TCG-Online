package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.type.*;
import java.time.*;
import java.util.*;

@Entity
@jakarta.persistence.Table(name = "game_state_snapshots")
@Getter
@Setter
public class GameStateSnapshot {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_json", nullable = false)
    private Map<String, Object> stateJson;

    @Column(nullable = false, length = 128)
    private String checksum;

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
