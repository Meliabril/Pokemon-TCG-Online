package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEnergyRequirementService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackLockService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContextFactory;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackTargetResolverService;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.ConfusionResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculatorService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageProtectionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.MentalPanicService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.OutgoingDamageReductionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.PokemonToolModifierService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttackServiceImplTest {

    @Mock
    private AttackResolutionContextFactory attackResolutionContextFactory;

    @Mock
    private AttackTargetResolverService attackTargetResolverService;

    @Mock
    private AttackEnergyRequirementService attackEnergyRequirementService;

    @Mock
    private ConfusionResolutionService confusionResolutionService;

    @Mock
    private AttackEffectService attackEffectService;

    @Mock
    private DamageCalculatorService damageCalculatorService;

    @Mock
    private DamageApplicationService damageApplicationService;

    @Mock
    private DamageProtectionService damageProtectionService;

    @Mock
    private AttackLockService attackLockService;

    @Mock
    private MentalPanicService mentalPanicService;

    @Mock
    private OutgoingDamageReductionService outgoingDamageReductionService;

    @Mock
    private PokemonToolModifierService pokemonToolModifierService;

    @Mock
    private CombatResolutionService combatResolutionService;

    @Mock
    private BetweenTurnsResolutionService betweenTurnsResolutionService;

    @Mock
    private CardService cardService;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private PassiveAbilityService passiveAbilityService;

    @Mock
    private AbilityUsageTracker abilityUsageTracker;

    @Test
    void declareAttackShouldWarnAndKeepTurnWhenAttackerIsLocked() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID attackerPokemonId = UUID.randomUUID();
        int currentStateVersion = 8;
        int nextStateVersion = currentStateVersion + 1;
        int currentTurnNumber = 5;
        PokemonInPlay attackerPokemon = pokemonInPlay(attackerPokemonId);
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                pokemonInPlay(UUID.randomUUID()),
                new Card(),
                new Card(),
                new Attack());
        GameStateDto currentState = state(gameId, attackerUserId, currentTurnNumber, currentStateVersion);
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(currentTurnNumber);
        game.setActivePlayerId(attackerUserId);
        GameEventDto lockEvent = event(gameId, nextStateVersion, attackerPokemonId);
        GameActionContext context = new GameActionContext(
                gameId,
                attackerUserId,
                new GameActionRequestDto(gameId, UUID.randomUUID(), GameActionType.DECLARE_ATTACK, currentStateVersion, Map.of()),
                currentState);

        when(attackResolutionContextFactory.create(context)).thenReturn(resolutionContext);
        when(attackLockService.consumeLock(attackerPokemon, currentTurnNumber)).thenReturn(true);
        when(gameEventFactory.publicEvent(
                eq(gameId),
                eq(GameEventType.ATTACK_EFFECT_RESOLVED),
                eq(nextStateVersion),
                anyMap()))
                .thenReturn(lockEvent);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        GameActionExecutionResult result = service().declareAttack(context);

        assertThat(result.gameState().stateVersion()).isEqualTo(nextStateVersion);
        assertThat(result.gameState().turn().activePlayerId()).isEqualTo(attackerUserId);
        assertThat(result.gameState().turn().currentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(result.gameState().turn().turnNumber()).isEqualTo(currentTurnNumber);
        assertThat(result.emittedEvents()).containsExactly(lockEvent);
        assertThat(lockEvent.payload()).containsEntry("effectType", "ATTACK_LOCKED");
        verifyNoInteractions(betweenTurnsResolutionService);
        verify(attackEnergyRequirementService, never()).hasRequiredEnergy(resolutionContext.attackerPokemon(), resolutionContext.selectedAttack());
    }

    private AttackServiceImpl service() {
        return new AttackServiceImpl(
                attackResolutionContextFactory,
                attackTargetResolverService,
                attackEnergyRequirementService,
                confusionResolutionService,
                attackEffectService,
                damageCalculatorService,
                damageApplicationService,
                damageProtectionService,
                attackLockService,
                mentalPanicService,
                outgoingDamageReductionService,
                pokemonToolModifierService,
                combatResolutionService,
                betweenTurnsResolutionService,
                passiveAbilityService,
                cardService,
                gameLookupService,
                gameEventFactory,
                abilityUsageTracker);
    }

    private GameStateDto state(UUID gameId, UUID activePlayerId, int turnNumber, int stateVersion) {
        return GameStateDto.builder()
                .gameId(gameId)
                .status(GameStatus.ACTIVE)
                .stateVersion(stateVersion)
                .turn(TurnContextDto.builder()
                        .activePlayerId(activePlayerId)
                        .currentPhase(TurnPhase.MAIN)
                        .turnNumber(turnNumber)
                        .build())
                .build();
    }

    private PokemonInPlay pokemonInPlay(UUID id) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(id);
        return pokemonInPlay;
    }

    private GameEventDto event(UUID gameId, int stateVersion, UUID pokemonInPlayId) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                false,
                Instant.now(),
                Map.of(
                        "effectType", "ATTACK_LOCKED",
                        "pokemonInPlayId", pokemonInPlayId.toString()));
    }
}
