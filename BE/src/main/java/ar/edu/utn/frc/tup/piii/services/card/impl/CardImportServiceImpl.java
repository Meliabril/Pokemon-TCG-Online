package ar.edu.utn.frc.tup.piii.services.card.impl;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.card.CardImportService;
import ar.edu.utn.frc.tup.piii.services.card.PokemonTcgApiService;
import ar.edu.utn.frc.tup.piii.services.card.PokemonTcgCardPayload;
import ar.edu.utn.frc.tup.piii.services.card.Xy1CardImportValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import ar.edu.utn.frc.tup.piii.services.card.CardCatalogModifiedEvent;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardImportServiceImpl implements CardImportService {

    private final PokemonTcgApiService pokemonTcgApiService;
    private final Xy1CardImportValidator validator;
    private final CardRepository cardRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CardImportResultDto importXy1() {
        List<PokemonTcgCardPayload> payloads = pokemonTcgApiService.fetchXy1Cards();
        // Import is all-or-nothing from a business perspective: XY1 must have exactly 146 cards.
        validator.validateFetchedCards(payloads);
        for (PokemonTcgCardPayload payload : payloads) {
            cardRepository.save(toEntity(payload));
        }
        eventPublisher.publishEvent(new CardCatalogModifiedEvent());
        long importedCards = cardRepository.countBySetCode(Card.XY1_SET_CODE);
        boolean complete = validator.isComplete(importedCards);
        return new CardImportResultDto(
                Card.XY1_SET_CODE,
                importedCards,
                complete,
                complete ? "XY1 import completed" : "XY1 import is incomplete");
    }

    @Override
    @Transactional(readOnly = true)
    public CardImportStatusDto getXy1Status() {
        long importedCards = cardRepository.countBySetCode(Card.XY1_SET_CODE);
        return new CardImportStatusDto(
                Card.XY1_SET_CODE,
                importedCards,
                Xy1CardImportValidator.EXPECTED_XY1_CARDS,
                validator.isComplete(importedCards));
    }

    private Card toEntity(PokemonTcgCardPayload payload) {
        Card card = cardRepository.findByExternalId(payload.externalId()).orElseGet(Card::new);
        card.setExternalId(payload.externalId());
        card.setSetCode(payload.setCode());
        card.setSetName(payload.setName());
        card.setSource(Card.SOURCE_POKEMONTCG_IO);
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
        card.setRawJson(payload.rawJson());
        card.getAttacks().clear();
        card.getWeaknesses().clear();
        card.getResistances().clear();
        payload.attacks().forEach(attack -> card.addAttack(attack(attack)));
        payload.weaknesses().forEach(weakness -> card.addWeakness(weakness(weakness)));
        payload.resistances().forEach(resistance -> card.addResistance(resistance(resistance)));
        return card;
    }

    private Attack attack(PokemonTcgCardPayload.AttackPayload payload) {
        Attack attack = new Attack();
        attack.setName(payload.name());
        attack.setDamageText(payload.damageText());
        attack.setBaseDamage(payload.baseDamage());
        attack.setEffectText(payload.effectText());
        attack.setAttackOrder(payload.attackOrder());
        for (Map.Entry<String, Integer> cost : payload.costs().entrySet()) {
            AttackCost attackCost = new AttackCost();
            attackCost.setEnergyType(cost.getKey());
            attackCost.setQuantity(cost.getValue());
            attack.addCost(attackCost);
        }
        return attack;
    }

    private CardWeakness weakness(PokemonTcgCardPayload.CardRelationPayload payload) {
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType(payload.energyType());
        weakness.setMultiplier(payload.value());
        return weakness;
    }

    private CardResistance resistance(PokemonTcgCardPayload.CardRelationPayload payload) {
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType(payload.energyType());
        resistance.setValue(payload.value());
        return resistance;
    }
}
