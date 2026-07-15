package ar.edu.utn.frc.tup.piii.entities;

import lombok.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.BatchSize;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "ux_cards_external_id", columnList = "external_id", unique = true),
        @Index(name = "ix_cards_name", columnList = "name"),
        @Index(name = "ix_cards_set_code", columnList = "set_code"),
        @Index(name = "ix_cards_category", columnList = "category"),
        @Index(name = "ix_cards_set_category", columnList = "set_code, category"),
        @Index(name = "ix_cards_supertype", columnList = "supertype")
})
@Getter
@Setter
public class Card {

    public static final String XY1_SET_CODE = "xy1";
    public static final String CUSTOM_PROFESSORS_SET_CODE = "custom-professors";
    public static final List<String> PLAYABLE_SET_CODES = List.of(XY1_SET_CODE, CUSTOM_PROFESSORS_SET_CODE);
    public static final String SOURCE_POKEMONTCG_IO = "POKEMONTCG_IO";
    public static final String SOURCE_CUSTOM_PROFESSORS = "CUSTOM_PROFESSORS";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(name = "external_id", nullable = false, unique = true, length = 80)
    private String externalId;

    @NotBlank
    @Column(name = "set_code", nullable = false, length = 20)
    private String setCode;

    @NotBlank
    @Column(name = "set_name", nullable = false, length = 120)
    private String setName;

    @NotBlank
    @Column(nullable = false, length = 40)
    private String source = SOURCE_POKEMONTCG_IO;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String number;

    @NotBlank
    @Column(nullable = false, length = 180)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CardSupertype supertype;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CardCategory category;

    @Column(length = 80)
    private String subtype;

    @Column(name = "evolves_from", length = 180)
    private String evolvesFrom;

    private Integer hp;

    @Column(name = "pokemon_type", length = 40)
    private String pokemonType;

    @Column(name = "retreat_cost")
    private Integer retreatCost;

    @Column(name = "image_small_url")
    private String imageSmallUrl;

    @Column(name = "image_large_url")
    private String imageLargeUrl;

    @NotBlank
    @Column(name = "raw_json", nullable = false, columnDefinition = "text")
    private String rawJson;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(nullable = false)
    private Instant updatedAt;

    /*
     * Hibernate cannot fetch several List-based one-to-many associations in the
     * same EntityGraph. Sets avoid MultipleBagFetchException; DTO mappers sort
     * the values before exposing them as JSON arrays.
     */
    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attackOrder ASC")
    @BatchSize(size = 50)
    private Set<Attack> attacks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<CardWeakness> weaknesses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<CardResistance> resistances = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addAttack(Attack attack) {
        attacks.add(attack);
        attack.setCard(this);
    }

    public void addWeakness(CardWeakness weakness) {
        weaknesses.add(weakness);
        weakness.setCard(this);
    }

    public void addResistance(CardResistance resistance) {
        resistances.add(resistance);
        resistance.setCard(this);
    }

    private static final String BASIC_STAGE_SUBTYPE = "Basic";

    /*
     * category conflates evolution stage with EX/Mega specialness (e.g. a Basic
     * Pokemon-EX is categorized as POKEMON_EX, losing the "Basic" stage info).
     * subtype always holds the first raw subtype token from the source set
     * ("Basic", "Stage 1", "Stage 2"...), so it is the reliable stage check.
     */
    public boolean isBasicStage() {
        // "Basic" is reused as a subtype for both Pokemon (Basic stage) and
        // Energy cards (Basic Energy), so supertype must be checked too.
        return supertype == CardSupertype.POKEMON && BASIC_STAGE_SUBTYPE.equalsIgnoreCase(subtype);
    }

    public static boolean isPlayableSetCode(String setCode) {
        return normalizePlayableSetCode(setCode) != null;
    }

    public static String normalizePlayableSetCode(String setCode) {
        if (setCode == null || setCode.isBlank()) {
            return null;
        }
        return PLAYABLE_SET_CODES.stream()
                .filter(playableSetCode -> playableSetCode.equalsIgnoreCase(setCode))
                .findFirst()
                .orElse(null);
    }
}
