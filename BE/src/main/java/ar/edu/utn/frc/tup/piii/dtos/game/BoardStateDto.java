package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;

import java.util.Map;
import java.util.UUID;

public record BoardStateDto(
        Map<UUID, Integer> enteredPlayTurnByPokemonInPlayId,
        Map<UUID, CardZone> zoneByCardReferenceId,
        Map<UUID, UUID> ownerByCardReferenceId,
        VisibleBoardDto view) {

    public BoardStateDto {
        enteredPlayTurnByPokemonInPlayId = copyIntegerMap(enteredPlayTurnByPokemonInPlayId);
        zoneByCardReferenceId = copyZoneMap(zoneByCardReferenceId);
        ownerByCardReferenceId = copyUuidMap(ownerByCardReferenceId);
        if (view == null) {
            view = VisibleBoardDto.empty();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .enteredPlayTurnByPokemonInPlayId(enteredPlayTurnByPokemonInPlayId)
                .zoneByCardReferenceId(zoneByCardReferenceId)
                .ownerByCardReferenceId(ownerByCardReferenceId)
                .view(view);
    }

    public static final class Builder {

        private Map<UUID, Integer> enteredPlayTurnByPokemonInPlayId;
        private Map<UUID, CardZone> zoneByCardReferenceId;
        private Map<UUID, UUID> ownerByCardReferenceId;
        private VisibleBoardDto view;

        private Builder() {
        }

        public Builder enteredPlayTurnByPokemonInPlayId(Map<UUID, Integer> enteredPlayTurnByPokemonInPlayId) {
            this.enteredPlayTurnByPokemonInPlayId = enteredPlayTurnByPokemonInPlayId;
            return this;
        }

        public Builder zoneByCardReferenceId(Map<UUID, CardZone> zoneByCardReferenceId) {
            this.zoneByCardReferenceId = zoneByCardReferenceId;
            return this;
        }

        public Builder ownerByCardReferenceId(Map<UUID, UUID> ownerByCardReferenceId) {
            this.ownerByCardReferenceId = ownerByCardReferenceId;
            return this;
        }

        public Builder view(VisibleBoardDto view) {
            this.view = view;
            return this;
        }

        public BoardStateDto build() {
            return new BoardStateDto(
                    enteredPlayTurnByPokemonInPlayId,
                    zoneByCardReferenceId,
                    ownerByCardReferenceId,
                    view);
        }
    }

    private static Map<UUID, Integer> copyIntegerMap(Map<UUID, Integer> source) {
        if (source == null) {
            return Map.of();
        }

        return Map.copyOf(source);
    }

    private static Map<UUID, CardZone> copyZoneMap(Map<UUID, CardZone> source) {
        if (source == null) {
            return Map.of();
        }

        return Map.copyOf(source);
    }

    private static Map<UUID, UUID> copyUuidMap(Map<UUID, UUID> source) {
        if (source == null) {
            return Map.of();
        }

        return Map.copyOf(source);
    }
}
