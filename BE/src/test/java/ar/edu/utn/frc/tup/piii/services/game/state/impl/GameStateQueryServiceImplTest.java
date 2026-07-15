package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.*;
import ar.edu.utn.frc.tup.piii.dtos.game.*;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.attack.impl.AttackEffectDefinitionReader;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEnergyRequirementService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogReadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStateQueryServiceImplTest {

    @Mock private GameParticipantStateService gameParticipantStateService;
    @Mock private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock private SpecialConditionStateService specialConditionStateService;
    @Mock private GameActionLogReadRepository gameActionLogReadRepository;
    @Mock private CardService cardService;
    @Mock private CardTranslationService cardTranslationService;
    @Mock private PokemonAttachedCardStateService pokemonAttachedCardStateService;
    @Mock private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    @Mock private GameStateVisibilitySanitizer gameStateVisibilitySanitizer;
    @Mock private AttackEnergyRequirementService attackEnergyRequirementService;
    @Mock private AttackEffectDefinitionReader attackEffectDefinitionReader;
    @Mock private AbilityCatalogService abilityCatalogService;
    @Mock private AbilityUsageTracker abilityUsageTracker;
    @Mock private PassiveAbilityService passiveAbilityService;

    @InjectMocks
    private GameStateQueryServiceImpl service;

    @Test
    void shouldCoverAbilitiesAndHandPlayability() {
        UUID gameId = UUID.randomUUID();
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();
        
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setActivePlayerId(p1Id);
        game.setTurnNumber(1);
        game.setStateVersion(1);

        GameParticipant p1 = new GameParticipant();
        p1.setUserId(p1Id);
        GameParticipant p2 = new GameParticipant();
        p2.setUserId(p2Id);
        
        lenient().when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(p1, p2));
        
        // Mock Cards
        UUID mysticalFireCardId = UUID.randomUUID();
        Card mysticalFireCard = new Card();
        mysticalFireCard.setId(mysticalFireCardId);
        mysticalFireCard.setName("Delphox");
        mysticalFireCard.setCategory(CardCategory.BASIC_POKEMON);
        mysticalFireCard.setExternalId("delphox");

        UUID upsideDownCardId = UUID.randomUUID();
        Card upsideDownCard = new Card();
        upsideDownCard.setId(upsideDownCardId);
        upsideDownCard.setName("Malamar");
        upsideDownCard.setCategory(CardCategory.BASIC_POKEMON);
        upsideDownCard.setExternalId("malamar");

        UUID waterShurikenCardId = UUID.randomUUID();
        Card waterShurikenCard = new Card();
        waterShurikenCard.setId(waterShurikenCardId);
        waterShurikenCard.setName("Greninja");
        waterShurikenCard.setCategory(CardCategory.BASIC_POKEMON);
        waterShurikenCard.setExternalId("greninja");

        UUID waterEnergyCardId = UUID.randomUUID();
        Card waterEnergyCard = new Card();
        waterEnergyCard.setId(waterEnergyCardId);
        waterEnergyCard.setName("Water Energy");
        waterEnergyCard.setCategory(CardCategory.BASIC_ENERGY);

        UUID opponentPokemonCardId = UUID.randomUUID();
        Card opponentPokemonCard = new Card();
        opponentPokemonCard.setId(opponentPokemonCardId);
        opponentPokemonCard.setName("Pikachu");
        opponentPokemonCard.setCategory(CardCategory.BASIC_POKEMON);
        
        lenient().when(cardService.getCardEntityById(mysticalFireCardId)).thenReturn(mysticalFireCard);
        lenient().when(cardService.getCardEntityById(upsideDownCardId)).thenReturn(upsideDownCard);
        lenient().when(cardService.getCardEntityById(waterShurikenCardId)).thenReturn(waterShurikenCard);
        lenient().when(cardService.getCardEntityById(waterEnergyCardId)).thenReturn(waterEnergyCard);
        lenient().when(cardService.getCardEntityById(opponentPokemonCardId)).thenReturn(opponentPokemonCard);

        // Mock GameCardInstances
        GameCardInstance p1ActiveInst = new GameCardInstance();
        p1ActiveInst.setId(UUID.randomUUID());
        p1ActiveInst.setOwnerUserId(p1Id);
        p1ActiveInst.setZone(CardZone.ACTIVE);
        p1ActiveInst.setCardId(mysticalFireCardId);

        GameCardInstance p1Bench1Inst = new GameCardInstance();
        p1Bench1Inst.setId(UUID.randomUUID());
        p1Bench1Inst.setOwnerUserId(p1Id);
        p1Bench1Inst.setZone(CardZone.BENCH);
        p1Bench1Inst.setCardId(upsideDownCardId);

        GameCardInstance p1Bench2Inst = new GameCardInstance();
        p1Bench2Inst.setId(UUID.randomUUID());
        p1Bench2Inst.setOwnerUserId(p1Id);
        p1Bench2Inst.setZone(CardZone.BENCH);
        p1Bench2Inst.setCardId(waterShurikenCardId);

        GameCardInstance p2ActiveInst = new GameCardInstance();
        p2ActiveInst.setId(UUID.randomUUID());
        p2ActiveInst.setOwnerUserId(p2Id);
        p2ActiveInst.setZone(CardZone.ACTIVE);
        p2ActiveInst.setCardId(opponentPokemonCardId);

        GameCardInstance p1HandWaterInst = new GameCardInstance();
        p1HandWaterInst.setId(UUID.randomUUID());
        p1HandWaterInst.setOwnerUserId(p1Id);
        p1HandWaterInst.setZone(CardZone.HAND);
        p1HandWaterInst.setCardId(waterEnergyCardId);

        GameCardInstance p1DeckInst = new GameCardInstance();
        p1DeckInst.setId(UUID.randomUUID());
        p1DeckInst.setOwnerUserId(p1Id);
        p1DeckInst.setZone(CardZone.DECK);
        p1DeckInst.setCardId(opponentPokemonCardId); // just any card in deck

        lenient().when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                p1ActiveInst, p1Bench1Inst, p1Bench2Inst, p2ActiveInst, p1HandWaterInst, p1DeckInst
        ));
        lenient().when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, p1Id, CardZone.DECK)).thenReturn(List.of(p1DeckInst));
        lenient().when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, p1Id, CardZone.HAND)).thenReturn(List.of(p1HandWaterInst));


        // Mock PokemonInPlay
        PokemonInPlay p1Active = new PokemonInPlay();
        p1Active.setId(UUID.randomUUID());
        p1Active.setOwnerUserId(p1Id);
        p1Active.setActiveCardInstance(p1ActiveInst);
        p1Active.setEnteredPlayTurn(1);

        PokemonInPlay p1Bench1 = new PokemonInPlay();
        p1Bench1.setId(UUID.randomUUID());
        p1Bench1.setOwnerUserId(p1Id);
        p1Bench1.setActiveCardInstance(p1Bench1Inst);
        p1Bench1.setEnteredPlayTurn(1);

        PokemonInPlay p1Bench2 = new PokemonInPlay();
        p1Bench2.setId(UUID.randomUUID());
        p1Bench2.setOwnerUserId(p1Id);
        p1Bench2.setActiveCardInstance(p1Bench2Inst);
        p1Bench2.setEnteredPlayTurn(1);

        PokemonInPlay p2Active = new PokemonInPlay();
        p2Active.setId(UUID.randomUUID());
        p2Active.setOwnerUserId(p2Id);
        p2Active.setActiveCardInstance(p2ActiveInst);
        p2Active.setEnteredPlayTurn(1);

        lenient().when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(
                p1Active, p1Bench1, p1Bench2, p2Active
        ));

        // Note: the mock should return empty lists using lenient for any Pokemon UUID
        lenient().when(pokemonAttachedCardStateService.findByPokemonInPlayId(any())).thenReturn(Collections.emptyList());
        lenient().when(pokemonEvolutionStackStateService.findByPokemonInPlayId(any())).thenReturn(Collections.emptyList());
        
        lenient().when(specialConditionStateService.activeConditionsByPlayer(any(), any(), any())).thenReturn(Collections.emptyMap());
        lenient().when(specialConditionStateService.activeConditionTypes(any())).thenReturn(Collections.emptyList());

        // Mock Abilities Definition
        AbilityDefinition mfDef = new AbilityDefinition(AbilityCode.MYSTICAL_FIRE, "Mystical Fire", "Fuego Místico", "Desc", "Desc", ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType.ACTIVATED, ar.edu.utn.frc.tup.piii.dtos.enums.AbilityTiming.WHILE_IN_PLAY, true);
        AbilityDefinition udDef = new AbilityDefinition(AbilityCode.UPSIDE_DOWN_EVOLUTION, "Upside Down", "Evolución", "Desc", "Desc", ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType.ACTIVATED, ar.edu.utn.frc.tup.piii.dtos.enums.AbilityTiming.WHILE_IN_PLAY, true);
        AbilityDefinition wsDef = new AbilityDefinition(AbilityCode.WATER_SHURIKEN, "Water Shuriken", "Shuriken de Agua", "Desc", "Desc", ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType.ACTIVATED, ar.edu.utn.frc.tup.piii.dtos.enums.AbilityTiming.WHILE_IN_PLAY, true);
        
        lenient().when(abilityCatalogService.abilitiesFor("delphox")).thenReturn(List.of(mfDef));
        lenient().when(abilityCatalogService.abilitiesFor("malamar")).thenReturn(List.of(udDef));
        lenient().when(abilityCatalogService.abilitiesFor("greninja")).thenReturn(List.of(wsDef));
        lenient().when(abilityCatalogService.abilitiesFor(any())).thenReturn(Collections.emptyList());
        
        lenient().when(abilityUsageTracker.wasUsedThisTurn(any(), any(), any())).thenReturn(false);

        GameStateDto result = GameStateDto.builder().build();
        lenient().when(gameStateVisibilitySanitizer.sanitizeForViewer(any(), eq(p1Id))).thenReturn(result);

        service.buildVisibleState(game, p1Id);
    }
}
