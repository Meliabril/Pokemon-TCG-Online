package ar.edu.utn.frc.tup.piii.services.card.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.card.CustomProfessorCardJsonSeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import ar.edu.utn.frc.tup.piii.services.card.CardCatalogModifiedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomProfessorCardJsonSeedServiceImpl implements CustomProfessorCardJsonSeedService {

    private static final String CUSTOM_PROFESSOR_CARDS_RESOURCE = "cards/custom-professors.json";

    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public int seedCustomProfessorCards() {
        CustomProfessorCardsPayload payload = readPayload();
        for (CustomProfessorCardPayload cardPayload : payload.cards()) {
            cardRepository.save(toEntity(cardPayload));
        }
        eventPublisher.publishEvent(new CardCatalogModifiedEvent());
        return payload.cards().size();
    }

    private CustomProfessorCardsPayload readPayload() {
        ClassPathResource resource = new ClassPathResource(CUSTOM_PROFESSOR_CARDS_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, CustomProfessorCardsPayload.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read custom professor cards JSON seed", exception);
        }
    }

    private Card toEntity(CustomProfessorCardPayload payload) {
        Card card = cardRepository.findByExternalId(payload.externalId()).orElseGet(Card::new);
        card.setExternalId(payload.externalId());
        card.setSetCode(payload.setCode());
        card.setSetName(payload.setName());
        card.setSource(payload.source());
        card.setNumber(payload.number());
        card.setName(payload.name());
        card.setSupertype(payload.supertype());
        card.setCategory(payload.category());
        card.setSubtype(payload.subtype());
        card.setEvolvesFrom(payload.evolvesFrom());
        card.setHp(payload.hp());
        card.setPokemonType(payload.pokemonType());
        card.setRetreatCost(payload.retreatCost());
        card.setImageSmallUrl(payload.imageSmallUrl());
        card.setImageLargeUrl(payload.imageLargeUrl());
        card.setRawJson(rawJson(payload));
        card.getAttacks().clear();
        card.getWeaknesses().clear();
        card.getResistances().clear();
        payload.attacks().forEach(attackPayload -> card.addAttack(attack(attackPayload)));
        payload.weaknesses().forEach(weaknessPayload -> card.addWeakness(weakness(weaknessPayload)));
        payload.resistances().forEach(resistancePayload -> card.addResistance(resistance(resistancePayload)));
        return card;
    }

    private String rawJson(CustomProfessorCardPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize custom professor card raw JSON", exception);
        }
    }

    private Attack attack(CustomProfessorAttackPayload payload) {
        Attack attack = new Attack();
        attack.setName(payload.name());
        attack.setDamageText(payload.damageText());
        attack.setBaseDamage(payload.baseDamage());
        attack.setEffectText(payload.effectText());
        attack.setAttackOrder(payload.attackOrder());
        payload.costs().forEach(costPayload -> attack.addCost(attackCost(costPayload)));
        return attack;
    }

    private AttackCost attackCost(CustomProfessorCostPayload payload) {
        AttackCost attackCost = new AttackCost();
        attackCost.setEnergyType(payload.energyType());
        attackCost.setQuantity(payload.quantity());
        return attackCost;
    }

    private CardWeakness weakness(CustomProfessorRelationPayload payload) {
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType(payload.energyType());
        weakness.setMultiplier(payload.value());
        return weakness;
    }

    private CardResistance resistance(CustomProfessorRelationPayload payload) {
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType(payload.energyType());
        resistance.setValue(payload.value());
        return resistance;
    }

    private record CustomProfessorCardsPayload(List<CustomProfessorCardPayload> cards) {
    }

    private record CustomProfessorCardPayload(
            String externalId,
            String setCode,
            String setName,
            String source,
            String rarity,
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
            List<CustomProfessorAttackPayload> attacks,
            List<CustomProfessorRelationPayload> weaknesses,
            List<CustomProfessorRelationPayload> resistances) {
    }

    private record CustomProfessorAttackPayload(
            String name,
            String damageText,
            Integer baseDamage,
            String effectText,
            int attackOrder,
            List<CustomProfessorCostPayload> costs) {
    }

    private record CustomProfessorCostPayload(String energyType, int quantity) {
    }

    private record CustomProfessorRelationPayload(String energyType, String value) {
    }
}
