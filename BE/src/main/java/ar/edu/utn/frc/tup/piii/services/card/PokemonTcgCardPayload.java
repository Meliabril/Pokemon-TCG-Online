package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;

import java.util.List;
import java.util.Map;

public record PokemonTcgCardPayload(
        String externalId,
        String setCode,
        String setName,
        String number,
        String name,
        CardSupertype supertype,
        CardCategory category,
        String subtype,
        String evolvesFrom,
        Integer hp,
        String pokemonType,
        Integer retreatCost,
        String imageSmallUrl,
        String imageLargeUrl,
        String rawJson,
        List<AttackPayload> attacks,
        List<CardRelationPayload> weaknesses,
        List<CardRelationPayload> resistances) {

    public record AttackPayload(
            String name,
            String damageText,
            Integer baseDamage,
            String effectText,
            int attackOrder,
            Map<String, Integer> costs) {
    }

    public record CardRelationPayload(
            String energyType,
            String value) {
    }
}
