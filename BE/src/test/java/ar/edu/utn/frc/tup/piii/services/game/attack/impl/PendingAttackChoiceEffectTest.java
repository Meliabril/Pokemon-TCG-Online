package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PendingAttackChoiceEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void flameChargePayloadShouldOnlyOfferFireEnergyCards() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance fireEnergyInstance = deckCard(attackerUserId, UUID.randomUUID(), 1);
        GameCardInstance slugmaInstance = deckCard(attackerUserId, UUID.randomUUID(), 2);
        Card fireEnergy = card(fireEnergyInstance.getCardId(), "Fire Energy", CardCategory.BASIC_ENERGY, null);
        Card slugma = card(slugmaInstance.getCardId(), "Slugma", CardCategory.BASIC_POKEMON, "Fire");
        PendingAttackChoiceEffect effect = effect();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(fireEnergyInstance, slugmaInstance));
        when(cardService.getCardEntityById(fireEnergyInstance.getCardId())).thenReturn(fireEnergy);
        when(cardService.getCardEntityById(slugmaInstance.getCardId())).thenReturn(slugma);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choiceType()).isEqualTo(PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF);
        assertThat(result.choicePayload().get("cards")).asList().singleElement().satisfies(option -> {
            assertThat(option).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> cardOption = (Map<String, Object>) option;
            assertThat(cardOption.get("cardInstanceId")).isEqualTo(fireEnergyInstance.getId().toString());
            assertThat(cardOption.get("name")).isEqualTo("Fire Energy");
            assertThat(cardOption.get("category")).isEqualTo(CardCategory.BASIC_ENERGY.name());
        });
    }

    @Test
    void flameChargePayloadShouldNotOpenChoiceWithoutFireEnergyCards() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance slugmaInstance = deckCard(attackerUserId, UUID.randomUUID(), 1);
        Card slugma = card(slugmaInstance.getCardId(), "Slugma", CardCategory.BASIC_POKEMON, "Fire");
        PendingAttackChoiceEffect effect = effect();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(slugmaInstance));
        when(cardService.getCardEntityById(slugmaInstance.getCardId())).thenReturn(slugma);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.choiceRequired()).isFalse();
        assertThat(result.choicePayload()).isEmpty();
    }

    @Test
    void pickupPayloadShouldOnlyOfferItemCardsCappedAtTheOperationAmount() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance itemOne = discardCard(attackerUserId, UUID.randomUUID(), 1);
        GameCardInstance itemTwo = discardCard(attackerUserId, UUID.randomUUID(), 2);
        GameCardInstance itemThree = discardCard(attackerUserId, UUID.randomUUID(), 3);
        GameCardInstance supporter = discardCard(attackerUserId, UUID.randomUUID(), 4);
        PendingAttackChoiceEffect effect = effect();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DISCARD))
                .thenReturn(List.of(itemOne, itemTwo, itemThree, supporter));
        when(cardService.getCardEntityById(itemOne.getCardId()))
                .thenReturn(card(itemOne.getCardId(), "Potion", CardCategory.ITEM_TRAINER, null));
        when(cardService.getCardEntityById(itemTwo.getCardId()))
                .thenReturn(card(itemTwo.getCardId(), "Switch", CardCategory.ITEM_TRAINER, null));
        when(cardService.getCardEntityById(itemThree.getCardId()))
                .thenReturn(card(itemThree.getCardId(), "Net Ball", CardCategory.ITEM_TRAINER, null));
        when(cardService.getCardEntityById(supporter.getCardId()))
                .thenReturn(card(supporter.getCardId(), "N", CardCategory.SUPPORTER_TRAINER, null));

        AttackEffectResult result = effect.apply(pickupContext(gameId, attackerUserId, 2));

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choiceType()).isEqualTo(PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND);
        assertThat(result.choicePayload().get("cards")).asList().hasSize(3);
        assertThat(result.choicePayload().get("pickupCount")).isEqualTo(2);
    }

    @Test
    void pickupPayloadShouldCapCountToTheNumberOfEligibleItemCards() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance onlyItem = discardCard(attackerUserId, UUID.randomUUID(), 1);
        PendingAttackChoiceEffect effect = effect();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DISCARD))
                .thenReturn(List.of(onlyItem));
        when(cardService.getCardEntityById(onlyItem.getCardId()))
                .thenReturn(card(onlyItem.getCardId(), "Potion", CardCategory.ITEM_TRAINER, null));

        AttackEffectResult result = effect.apply(pickupContext(gameId, attackerUserId, 2));

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choicePayload().get("pickupCount")).isEqualTo(1);
    }

    @Test
    void pickupPayloadShouldNotOpenChoiceWithoutItemCardsInDiscard() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance supporter = discardCard(attackerUserId, UUID.randomUUID(), 1);
        PendingAttackChoiceEffect effect = effect();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DISCARD))
                .thenReturn(List.of(supporter));
        when(cardService.getCardEntityById(supporter.getCardId()))
                .thenReturn(card(supporter.getCardId(), "N", CardCategory.SUPPORTER_TRAINER, null));

        AttackEffectResult result = effect.apply(pickupContext(gameId, attackerUserId, 2));

        assertThat(result.choiceRequired()).isFalse();
        assertThat(result.choicePayload()).isEmpty();
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldSupportSelectDiscardItemsToHandOperationType() {
        PendingAttackChoiceEffect effect = effect();
        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND,
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                2,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);

        assertThat(effect.supports(operation)).isTrue();
        assertThat(effect.supports(null)).isFalse();
    }

    private AttackEffectContext pickupContext(UUID gameId, UUID attackerUserId, int amount) {
        UUID defenderUserId = UUID.randomUUID();
        PokemonInPlay attackerPokemon = pokemonInPlay(attackerUserId);
        PokemonInPlay defenderPokemon = pokemonInPlay(defenderUserId);
        Card attackerCard = card(UUID.randomUUID(), "Diggersby", CardCategory.STAGE_1_POKEMON, null);
        Card defenderCard = card(UUID.randomUUID(), "Defender", CardCategory.BASIC_POKEMON, null);
        Attack attack = new Attack();
        attack.setAttackOrder(0);
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                attackerCard,
                defenderCard,
                attack);
        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND,
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                amount,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, defenderPokemon, operation, Map.of(), 1, 1, 0);
    }

    private GameCardInstance discardCard(UUID ownerUserId, UUID cardId, int zonePosition) {
        GameCardInstance instance = new GameCardInstance();
        instance.setId(UUID.randomUUID());
        instance.setOwnerUserId(ownerUserId);
        instance.setCardId(cardId);
        instance.setZone(CardZone.DISCARD);
        instance.setZonePosition(zonePosition);
        instance.setFaceDown(false);
        return instance;
    }

    private PendingAttackChoiceEffect effect() {
        lenient().when(gameEventFactory.publicEvent(any(), any(), anyInt(), any()))
                .thenReturn(mock(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto.class));
        return new PendingAttackChoiceEffect(
                pokemonInPlayStateService,
                pokemonAttachedCardStateService,
                gameCardInstanceStateService,
                cardService,
                gameEventFactory);
    }

    private AttackEffectContext context(UUID gameId, UUID attackerUserId) {
        UUID defenderUserId = UUID.randomUUID();
        PokemonInPlay attackerPokemon = pokemonInPlay(attackerUserId);
        PokemonInPlay defenderPokemon = pokemonInPlay(defenderUserId);
        Card attackerCard = card(UUID.randomUUID(), "Fletchinder", CardCategory.STAGE_1_POKEMON, "Fire");
        Card defenderCard = card(UUID.randomUUID(), "Froakie", CardCategory.BASIC_POKEMON, "Water");
        Attack attack = new Attack();
        attack.setAttackOrder(0);
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                attackerCard,
                defenderCard,
                attack);
        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF,
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                "Fire",
                null,
                null);
        return new AttackEffectContext(resolutionContext, defenderPokemon, operation, Map.of(), 2, 1, 0);
    }

    private PokemonInPlay pokemonInPlay(UUID ownerUserId) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(ownerUserId);
        return pokemon;
    }

    private GameCardInstance deckCard(UUID ownerUserId, UUID cardId, int zonePosition) {
        GameCardInstance instance = new GameCardInstance();
        instance.setId(UUID.randomUUID());
        instance.setOwnerUserId(ownerUserId);
        instance.setCardId(cardId);
        instance.setZone(CardZone.DECK);
        instance.setZonePosition(zonePosition);
        instance.setFaceDown(true);
        return instance;
    }

    private Card card(UUID cardId, String name, CardCategory category, String pokemonType) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId(name.toLowerCase().replace(" ", "-"));
        card.setName(name);
        card.setCategory(category);
        card.setPokemonType(pokemonType);
        return card;
    }

    @Test
    void shouldSupportDeckEnergyAttachToOwnPokemonPayload() {
        UUID attackerUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        PokemonInPlay activePokemon = new PokemonInPlay();
        activePokemon.setId(UUID.randomUUID());
        activePokemon.setOwnerUserId(attackerUserId);
        activePokemon.setSlotPosition(0);

        GameCardInstance deckEnergy = new GameCardInstance();
        deckEnergy.setId(UUID.randomUUID());
        deckEnergy.setCardId(UUID.randomUUID());

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.attackerUserId()).thenReturn(attackerUserId);
        PokemonInPlay defPip = new PokemonInPlay(); defPip.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(activePokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(defPip);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.defenderCard()).thenReturn(card);
        Attack attack = new Attack(); attack.setAttackOrder(0);
        when(resolutionContext.selectedAttack()).thenReturn(attack);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(any(), eq(attackerUserId), eq(CardZone.DECK)))
                .thenReturn(List.of(deckEnergy));
        
        Card energyCard = new Card();
        energyCard.setCategory(CardCategory.BASIC_ENERGY);
        energyCard.setName("Water Energy");
        when(cardService.getCardEntityById(deckEnergy.getCardId())).thenReturn(energyCard);

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(any(), eq(attackerUserId)))
                .thenReturn(List.of(activePokemon));

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 1, null, null, false, 0, null);
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }
    
    @Test
    void shouldSupportDistinctBasicEnergiesPayload() {
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance deckEnergy = new GameCardInstance();
        deckEnergy.setId(UUID.randomUUID());
        deckEnergy.setCardId(UUID.randomUUID());

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.attackerUserId()).thenReturn(attackerUserId);
        PokemonInPlay attackerPokemon = new PokemonInPlay(); attackerPokemon.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay(); defenderPokemon.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(attackerPokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(defenderPokemon);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.defenderCard()).thenReturn(card);
        Attack attack = new Attack(); attack.setAttackOrder(0);
        when(resolutionContext.selectedAttack()).thenReturn(attack);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(any(), eq(attackerUserId), eq(CardZone.DECK)))
                .thenReturn(List.of(deckEnergy));

        Card energyCard = new Card();
        energyCard.setCategory(CardCategory.BASIC_ENERGY);
        energyCard.setName("Water Energy");
        when(cardService.getCardEntityById(deckEnergy.getCardId())).thenReturn(energyCard);

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 1, null, null, false, 0, null);
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }
    
    @Test
    void shouldSupportOpponentBenchPayload() {
        UUID defenderUserId = UUID.randomUUID();
        PokemonInPlay benchPokemon = new PokemonInPlay();
        benchPokemon.setId(UUID.randomUUID());
        benchPokemon.setOwnerUserId(defenderUserId);
        benchPokemon.setSlotPosition(1);

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.defenderUserId()).thenReturn(defenderUserId);
        PokemonInPlay attackerPokemon = new PokemonInPlay(); attackerPokemon.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay(); defenderPokemon.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(attackerPokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(defenderPokemon);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.defenderCard()).thenReturn(card);
        Attack attack = new Attack(); attack.setAttackOrder(0);
        when(resolutionContext.selectedAttack()).thenReturn(attack);

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(any(), eq(defenderUserId)))
                .thenReturn(List.of(benchPokemon));

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 30, null, null, false, 0, null);
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }
    
    @Test
    void shouldSupportOpponentAttackPayload() {
        Card defenderCard = new Card();
        defenderCard.setId(UUID.randomUUID());
        Attack attack = new Attack();
        attack.setAttackOrder(1);
        attack.setName("Scratch");
        attack.setBaseDamage(10);
        attack.setDamageText("10");
        defenderCard.setAttacks(new java.util.LinkedHashSet<>(List.of(attack)));

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.defenderCard()).thenReturn(defenderCard);
        PokemonInPlay attackerPokemon = new PokemonInPlay(); attackerPokemon.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay(); defenderPokemon.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(attackerPokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(defenderPokemon);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.selectedAttack()).thenReturn(attack);

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.SELECT_OPPONENT_ATTACK,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 1, null, null, false, 0, null);
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }

    @Test
    void shouldSupportYesNoPayload() {
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance deckCard = new GameCardInstance();
        deckCard.setId(UUID.randomUUID());

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.attackerUserId()).thenReturn(attackerUserId);
        PokemonInPlay attackerPokemon = new PokemonInPlay(); attackerPokemon.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay(); defenderPokemon.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(attackerPokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(defenderPokemon);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.defenderCard()).thenReturn(card);
        Attack selectedAttack = new Attack();
        selectedAttack.setBaseDamage(30);
        selectedAttack.setDamageText("30");
        when(resolutionContext.selectedAttack()).thenReturn(selectedAttack);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(any(), eq(attackerUserId), eq(CardZone.DECK)))
                .thenReturn(List.of(deckCard));
        
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(UUID.randomUUID());

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.YES_NO,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 1, null, null, false, 0, null, null, "FIRE");
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);
        when(context.targetPokemon()).thenReturn(targetPokemon);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }

    @Test
    void shouldSupportReorderTopDeckPayload() {
        UUID attackerUserId = UUID.randomUUID();
        UUID cardId1 = UUID.randomUUID();
        UUID cardId2 = UUID.randomUUID();
        GameCardInstance deckCard1 = new GameCardInstance();
        deckCard1.setId(UUID.randomUUID());
        deckCard1.setCardId(cardId1);
        deckCard1.setZonePosition(1);
        GameCardInstance deckCard2 = new GameCardInstance();
        deckCard2.setId(UUID.randomUUID());
        deckCard2.setCardId(cardId2);
        deckCard2.setZonePosition(2);
        Card blankCard1 = new Card(); blankCard1.setId(cardId1);
        Card blankCard2 = new Card(); blankCard2.setId(cardId2);
        lenient().when(cardService.getCardEntityById(cardId1)).thenReturn(blankCard1);
        lenient().when(cardService.getCardEntityById(cardId2)).thenReturn(blankCard2);

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.attackerUserId()).thenReturn(attackerUserId);
        PokemonInPlay attackerPokemon = new PokemonInPlay(); attackerPokemon.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay(); defenderPokemon.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(attackerPokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(defenderPokemon);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.defenderCard()).thenReturn(card);
        Attack attack = new Attack(); attack.setAttackOrder(0);
        when(resolutionContext.selectedAttack()).thenReturn(attack);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(any(), eq(attackerUserId), eq(CardZone.DECK)))
                .thenReturn(List.of(deckCard1, deckCard2));

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.REORDER_TOP_DECK,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 2, null, null, false, 0, null);
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }

    @Test
    void shouldSupportMoveOpponentEnergyPayload() {
        UUID defenderUserId = UUID.randomUUID();
        PokemonInPlay activePokemon = new PokemonInPlay();
        activePokemon.setId(UUID.randomUUID());
        activePokemon.setOwnerUserId(defenderUserId);
        activePokemon.setSlotPosition(0);

        PokemonInPlay benchPokemon = new PokemonInPlay();
        benchPokemon.setId(UUID.randomUUID());
        benchPokemon.setOwnerUserId(defenderUserId);
        benchPokemon.setSlotPosition(1);
        
        UUID energyCardId = UUID.randomUUID();
        PokemonAttachedCard attachedEnergy = new PokemonAttachedCard();
        attachedEnergy.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        GameCardInstance energyInstance = new GameCardInstance();
        energyInstance.setId(UUID.randomUUID());
        energyInstance.setCardId(energyCardId);
        attachedEnergy.setGameCardInstance(energyInstance);
        Card energyCard = new Card(); energyCard.setCategory(CardCategory.BASIC_ENERGY);
        lenient().when(cardService.getCardEntityById(energyCardId)).thenReturn(energyCard);

        AttackResolutionContext resolutionContext = mock(AttackResolutionContext.class);
        when(resolutionContext.defenderUserId()).thenReturn(defenderUserId);
        PokemonInPlay attackerPokemon = new PokemonInPlay(); attackerPokemon.setId(UUID.randomUUID());
        when(resolutionContext.attackerPokemon()).thenReturn(attackerPokemon);
        when(resolutionContext.defenderPokemon()).thenReturn(activePokemon);
        Card card = new Card(); card.setId(UUID.randomUUID());
        when(resolutionContext.attackerCard()).thenReturn(card);
        when(resolutionContext.defenderCard()).thenReturn(card);
        Attack attack = new Attack(); attack.setAttackOrder(0);
        when(resolutionContext.selectedAttack()).thenReturn(attack);

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(any(), eq(defenderUserId)))
                .thenReturn(List.of(activePokemon, benchPokemon));

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId()))
                .thenReturn(List.of(attachedEnergy));

        AttackEffectOperation operation = new AttackEffectOperation(
                PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH,
                AttackEffectPhase.AFTER_DAMAGE,
                null, 1, null, null, false, 0, null);
        AttackEffectContext context = mock(AttackEffectContext.class);
        when(context.operation()).thenReturn(operation);
        when(context.resolutionContext()).thenReturn(resolutionContext);

        PendingAttackChoiceEffect effect = effect();
        AttackEffectResult result = effect.apply(context);
        assertThat(result.choiceRequired()).isTrue();
    }

}
