package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityTiming;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.effect.DamageCounterEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameEventFactoryImpl;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassiveAbilityServiceImplTest {

    private static final int NEXT_TURN_NUMBER = 8;
    private static final int STATE_VERSION = 12;

    @Mock private AbilityCatalogService abilityCatalogService;
    @Mock private CardService cardService;
    @Mock private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock private PokemonAttachedCardStateService pokemonAttachedCardStateService;
    @Mock private SpecialConditionStateService specialConditionStateService;
    @Mock private DamageCounterEffectService damageCounterEffectService;
    @Mock private GameRandomService gameRandomService;

    private PassiveAbilityService service;
    private Game game;
    private UUID attackerUserId;
    private UUID defenderUserId;
    private PokemonInPlay attackerPokemon;
    private PokemonInPlay defenderPokemon;
    private Card attackerCard;
    private Card defenderCard;

    @BeforeEach
    void setUp() {
        GameEventFactory gameEventFactory = new GameEventFactoryImpl();
        service = new PassiveAbilityServiceImpl(
                abilityCatalogService,
                cardService,
                pokemonInPlayStateService,
                pokemonAttachedCardStateService,
                specialConditionStateService,
                damageCounterEffectService,
                gameRandomService,
                gameEventFactory);

        game = new Game();
        game.setId(UUID.randomUUID());
        game.setTurnNumber(7);

        attackerUserId = UUID.randomUUID();
        defenderUserId = UUID.randomUUID();
        attackerCard = card("xy1-1", 100);
        defenderCard = card("xy1-14", 160);
        attackerPokemon = pokemon(attackerUserId, attackerCard, 0, 0);
        defenderPokemon = pokemon(defenderUserId, defenderCard, 0, 0);

        lenient().when(abilityCatalogService.find(any(), any())).thenReturn(Optional.empty());
        lenient().when(cardService.getCardEntityById(attackerPokemon.getActiveCardInstance().getCardId()))
                .thenReturn(attackerCard);
        lenient().when(cardService.getCardEntityById(defenderPokemon.getActiveCardInstance().getCardId()))
                .thenReturn(defenderCard);
        lenient().when(damageCounterEffectService.placeDamageCounters(any(), any(), anyInt(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new DamageCounterEffectService.DamageCounterResult(0, 0, false, false, null, List.of()));
    }

    @Test
    void shouldTriggerSpikyShieldAfterActiveChesnaughtTakesAttackDamage() {
        givenAbility(defenderCard, AbilityCode.SPIKY_SHIELD);

        PassiveAbilityService.PassiveAbilityResolution result = service.applyAfterAttackDamage(
                game,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                90,
                NEXT_TURN_NUMBER,
                STATE_VERSION);

        assertThat(result.events()).extracting(GameEventDto::eventType)
                .contains(GameEventType.PASSIVE_ABILITY_TRIGGERED);
        GameEventDto passiveEvent = result.events().get(0);
        assertThat(passiveEvent.payload())
                .containsEntry("abilityId", AbilityCode.SPIKY_SHIELD.name())
                .containsEntry("sourcePokemonId", defenderPokemon.getId().toString())
                .containsEntry("targetPokemonId", attackerPokemon.getId().toString())
                .containsEntry("damageCounters", 3);
        verify(damageCounterEffectService).placeDamageCounters(
                game.getId(),
                attackerPokemon,
                3,
                defenderUserId,
                defenderUserId,
                NEXT_TURN_NUMBER,
                STATE_VERSION,
                AbilityCode.SPIKY_SHIELD.name());
    }

    @Test
    void shouldNotTriggerSpikyShieldFromBench() {
        givenAbility(defenderCard, AbilityCode.SPIKY_SHIELD);
        defenderPokemon.setSlotPosition(2);

        PassiveAbilityService.PassiveAbilityResolution result = service.applyAfterAttackDamage(
                game,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                90,
                NEXT_TURN_NUMBER,
                STATE_VERSION);

        assertThat(result.events()).isEmpty();
        verify(damageCounterEffectService, never()).placeDamageCounters(any(), any(), anyInt(), any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    void shouldTriggerDestinyBurstAndPlaceFiveCountersOnHeadsWhenVoltorbIsKnockedOut() {
        defenderCard = card("xy1-44", 50);
        defenderPokemon = pokemon(defenderUserId, defenderCard, 0, 5);
        when(cardService.getCardEntityById(defenderPokemon.getActiveCardInstance().getCardId()))
                .thenReturn(defenderCard);
        givenAbility(defenderCard, AbilityCode.DESTINY_BURST);
        when(gameRandomService.flipCoin()).thenReturn(true);

        PassiveAbilityService.PassiveAbilityResolution result = service.applyAfterAttackDamage(
                game,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                60,
                NEXT_TURN_NUMBER,
                STATE_VERSION);

        GameEventDto passiveEvent = result.events().get(0);
        assertThat(passiveEvent.eventType()).isEqualTo(GameEventType.PASSIVE_ABILITY_TRIGGERED);
        assertThat(passiveEvent.payload())
                .containsEntry("abilityId", AbilityCode.DESTINY_BURST.name())
                .containsEntry("coinFlip", "HEADS")
                .containsEntry("damageCounters", 5);
        verify(damageCounterEffectService).placeDamageCounters(
                game.getId(),
                attackerPokemon,
                5,
                defenderUserId,
                defenderUserId,
                NEXT_TURN_NUMBER,
                STATE_VERSION,
                AbilityCode.DESTINY_BURST.name());
    }

    @Test
    void shouldTriggerDestinyBurstButNotPlaceCountersOnTails() {
        defenderCard = card("xy1-44", 50);
        defenderPokemon = pokemon(defenderUserId, defenderCard, 0, 5);
        when(cardService.getCardEntityById(defenderPokemon.getActiveCardInstance().getCardId()))
                .thenReturn(defenderCard);
        givenAbility(defenderCard, AbilityCode.DESTINY_BURST);
        when(gameRandomService.flipCoin()).thenReturn(false);

        PassiveAbilityService.PassiveAbilityResolution result = service.applyAfterAttackDamage(
                game,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                60,
                NEXT_TURN_NUMBER,
                STATE_VERSION);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).payload())
                .containsEntry("abilityId", AbilityCode.DESTINY_BURST.name())
                .containsEntry("coinFlip", "TAILS")
                .containsEntry("damageCounters", 0);
        verify(damageCounterEffectService, never()).placeDamageCounters(any(), any(), anyInt(), any(), any(), anyInt(), anyInt(), any());
    }

    @Test
    void shouldNotTriggerPassivesWhenAttackDamageIsZero() {
        PassiveAbilityService.PassiveAbilityResolution result = service.applyAfterAttackDamage(
                game,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                0,
                NEXT_TURN_NUMBER,
                STATE_VERSION);

        assertThat(result.events()).isEmpty();
        verify(gameRandomService, never()).flipCoin();
        verify(damageCounterEffectService, never()).placeDamageCounters(any(), any(), anyInt(), any(), any(), anyInt(), anyInt(), any());
    }

    private void givenAbility(Card card, AbilityCode abilityCode) {
        lenient().when(abilityCatalogService.find(eq(card.getExternalId()), eq(abilityCode)))
                .thenReturn(Optional.of(new AbilityDefinition(
                        abilityCode,
                        abilityCode.name(),
                        abilityCode.name(),
                        "",
                        "",
                        AbilityActivationType.PASSIVE,
                        AbilityTiming.WHEN_DAMAGED,
                        false)));
    }

    private Card card(String externalId, int hp) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(externalId);
        card.setHp(hp);
        return card;
    }

    private PokemonInPlay pokemon(UUID ownerUserId, Card card, int slotPosition, int damageCounters) {
        GameCardInstance activeCardInstance = new GameCardInstance();
        activeCardInstance.setId(UUID.randomUUID());
        activeCardInstance.setCardId(card.getId());

        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setGame(game);
        pokemon.setOwnerUserId(ownerUserId);
        pokemon.setActiveCardInstance(activeCardInstance);
        pokemon.setSlotPosition(slotPosition);
        pokemon.setDamageCounters(damageCounters);
        return pokemon;
    }
}
