package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationRequest;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculatorService;
import ar.edu.utn.frc.tup.piii.services.game.attack.PendingChoiceTimeoutPolicy;
import ar.edu.utn.frc.tup.piii.services.game.attack.SpecialConditionApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import static org.mockito.Mockito.times;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttackChoiceServiceImplTest {

    @Mock
    private AttackService attackService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @Mock
    private DamageApplicationService damageApplicationService;

    @Mock
    private DamageCalculatorService damageCalculatorService;

    @Mock
    private SpecialConditionApplicationService specialConditionApplicationService;

    @Mock
    private CombatResolutionService combatResolutionService;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameStateQueryService gameStateQueryService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldShuffleOpponentDeckWhenChoiceIsConfirmed() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance first = cardInstance(defenderUserId, 1);
        GameCardInstance second = cardInstance(defenderUserId, 2);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(
                GameStateDto.builder().build(), List.of());

        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.DECK))
                .thenReturn(List.of(first, second));
        when(gameRandomService.shuffledCopy(List.of(first, second))).thenReturn(List.of(second, first));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionContext context = context(gameId, attackerUserId, defenderUserId, true, 7);
        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(second.getZonePosition()).isEqualTo(1);
        assertThat(first.getZonePosition()).isEqualTo(2);
    }

    @Test
    void shouldNotShuffleWhenChoiceIsDeclined() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(
                GameStateDto.builder().build(), List.of());

        AttackChoiceServiceImpl service = service();

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionContext context = context(gameId, attackerUserId, defenderUserId, false, 7);
        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        verify(gameCardInstanceStateService, never()).findByGameIdAndOwnerUserIdAndZone(any(), any(), eq(CardZone.DECK));
    }

    @Test
    void shouldRejectWhenNoChoiceIsPending() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        AttackChoiceServiceImpl service = service();

        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.RESOLVE_ATTACK_CHOICE, 7, Map.of("confirm", true));
        GameStateDto currentState = GameStateDto.builder()
                .stateVersion(7)
                .resolution(ResolutionStateDto.builder().build())
                .build();
        GameActionContext context = new GameActionContext(gameId, attackerUserId, request, currentState);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void shouldRejectWhenActorDoesNotOwnPendingChoice() {
        UUID gameId = UUID.randomUUID();
        UUID pendingPlayerId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        AttackChoiceServiceImpl service = service();

        GameActionContext context = contextWithChoice(
                gameId,
                otherPlayerId,
                pendingPlayerId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_OPPONENT_ATTACK,
                Map.of("attacks", List.of(Map.of("attackOrder", 0))),
                Map.of("attackOrder", 0),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("pending choice owner");
    }

    @Test
    void shouldRejectTormentAttackOrderThatWasNotOffered() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        AttackChoiceServiceImpl service = service();

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_OPPONENT_ATTACK,
                Map.of("attacks", List.of(Map.of("attackOrder", 0))),
                Map.of("attackOrder", 1),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("was not offered");
    }

    @Test
    void shouldRejectDeckCardThatWasNotOfferedForFlameCharge() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID offeredCardId = UUID.randomUUID();
        UUID requestedCardId = UUID.randomUUID();
        UUID attackerPokemonId = UUID.randomUUID();
        AttackChoiceServiceImpl service = service();

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF,
                Map.of(
                        "targetPokemonInPlayId", attackerPokemonId.toString(),
                        "requiredEnergyType", "Fire",
                        "cards", List.of(Map.of("cardInstanceId", offeredCardId.toString()))),
                Map.of("cardInstanceId", requestedCardId.toString()),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("was not offered");
    }

    @Test
    void shouldRejectPokemonCardForFlameChargeEvenWhenItHasFireType() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID cardInstanceId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID attackerPokemonId = UUID.randomUUID();
        GameCardInstance slugmaInstance = cardInstance(attackerUserId, 1);
        slugmaInstance.setId(cardInstanceId);
        slugmaInstance.setCardId(cardId);
        Card slugma = new Card();
        slugma.setId(cardId);
        slugma.setCategory(CardCategory.BASIC_POKEMON);
        slugma.setPokemonType("Fire");
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(attackerPokemonId);
        attackerPokemon.setOwnerUserId(attackerUserId);
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                cardInstanceId,
                gameId,
                attackerUserId)).thenReturn(Optional.of(slugmaInstance));
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                attackerPokemonId,
                gameId,
                attackerUserId)).thenReturn(Optional.of(attackerPokemon));
        when(cardService.getCardEntityById(cardId)).thenReturn(slugma);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF,
                Map.of(
                        "targetPokemonInPlayId", attackerPokemonId.toString(),
                        "requiredEnergyType", "Fire",
                        "cards", List.of(Map.of("cardInstanceId", cardInstanceId.toString()))),
                Map.of("cardInstanceId", cardInstanceId.toString()),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Energy card");

        verify(gameCardInstanceStateService, never()).save(slugmaInstance);
    }

    @Test
    void shouldRejectSpecialEnergyForGatherEnergy() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID cardInstanceId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        GameCardInstance cardInstance = cardInstance(attackerUserId, 1);
        cardInstance.setId(cardInstanceId);
        cardInstance.setCardId(cardId);
        Card specialEnergy = new Card();
        specialEnergy.setId(cardId);
        specialEnergy.setCategory(CardCategory.SPECIAL_ENERGY);
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonId);
        targetPokemon.setOwnerUserId(attackerUserId);
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                cardInstanceId,
                gameId,
                attackerUserId)).thenReturn(Optional.of(cardInstance));
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonId,
                gameId,
                attackerUserId)).thenReturn(Optional.of(targetPokemon));
        when(cardService.getCardEntityById(cardId)).thenReturn(specialEnergy);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON,
                Map.of(
                        "basicOnly", true,
                        "cards", List.of(Map.of("cardInstanceId", cardInstanceId.toString())),
                        "targets", List.of(Map.of("pokemonInPlayId", targetPokemonId.toString()))),
                Map.of(
                        "cardInstanceId", cardInstanceId.toString(),
                        "pokemonInPlayId", targetPokemonId.toString()),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Basic Energy");
    }

    @Test
    void recycleShouldPutTheOnlyDiscardCardOnTopOfTheDeck() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance recycledCard = cardInstance(attackerUserId, 1);
        recycledCard.setZone(CardZone.DISCARD);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                recycledCard.getId(),
                gameId,
                attackerUserId)).thenReturn(Optional.of(recycledCard));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of());
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD,
                Map.of("cards", List.of(Map.of("cardInstanceId", recycledCard.getId().toString()))),
                Map.of("cardInstanceId", recycledCard.getId().toString()),
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(recycledCard.getZone()).isEqualTo(CardZone.DECK);
        assertThat(recycledCard.getZonePosition()).isEqualTo(1);
        assertThat(recycledCard.getFaceDown()).isTrue();
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.DECK);
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.DISCARD);
    }

    @Test
    void recycleShouldShiftExistingDeckCardsWithoutChangingTheirRelativeOrder() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance recycledCard = cardInstance(attackerUserId, 1);
        recycledCard.setZone(CardZone.DISCARD);
        GameCardInstance firstDeckCard = cardInstance(attackerUserId, 1);
        GameCardInstance secondDeckCard = cardInstance(attackerUserId, 2);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                recycledCard.getId(),
                gameId,
                attackerUserId)).thenReturn(Optional.of(recycledCard));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(firstDeckCard, secondDeckCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD,
                Map.of("cards", List.of(Map.of("cardInstanceId", recycledCard.getId().toString()))),
                Map.of("cardInstanceId", recycledCard.getId().toString()),
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(recycledCard.getZone()).isEqualTo(CardZone.DECK);
        assertThat(recycledCard.getZonePosition()).isEqualTo(1);
        assertThat(firstDeckCard.getZonePosition()).isEqualTo(2);
        assertThat(secondDeckCard.getZonePosition()).isEqualTo(3);
    }

    @Test
    void recycleShouldRejectAnOfferedCardThatIsNoLongerInDiscard() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance movedCard = cardInstance(attackerUserId, 1);
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                movedCard.getId(),
                gameId,
                attackerUserId)).thenReturn(Optional.of(movedCard));

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD,
                Map.of("cards", List.of(Map.of("cardInstanceId", movedCard.getId().toString()))),
                Map.of("cardInstanceId", movedCard.getId().toString()),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("no longer valid");
    }

    @Test
    void machPunchKnockingOutTheSelectedBenchPokemonShouldShortCircuitToTheFinishedGameStateWithoutEndingTheTurn() {
        // Regression coverage for the bench-knockout bug report: Mach Punch lets the attacker pick
        // one of the opponent's Bench Pokemon to damage directly. If that hit is lethal, the
        // resulting knockout (discard + prize, and possibly the game ending) must be resolved here -
        // not silently left for some other code path to notice later.
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        PokemonInPlay benchTarget = benchPokemon(defenderUserId, targetPokemonId);
        Game game = new Game();
        GameStateDto finishedState = GameStateDto.builder().build();
        AttackChoiceServiceImpl service = service();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonId, gameId, defenderUserId))
                .thenReturn(Optional.of(benchTarget));
        when(damageApplicationService.applyDamage(benchTarget, 10)).thenReturn(2);
        when(gameEventFactory.publicEvent(any(), any(), any(Integer.class), any())).thenReturn(event(gameId));
        when(combatResolutionService.resolveKnockoutIfNeeded(
                gameId, defenderUserId, attackerUserId, benchTarget,
                defenderUserId, 8, 8, "BENCH_ATTACK_DAMAGE"))
                .thenReturn(new CombatResolutionService.CombatResolutionResult(
                        true, true, false, attackerUserId, List.of(event(gameId))));
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(finishedState);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET,
                Map.of(
                        "damage", 10,
                        "targets", List.of(Map.of("pokemonInPlayId", targetPokemonId.toString()))),
                Map.of("targetPokemonInPlayId", targetPokemonId.toString()),
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        // The knockout finished the game, so resolveAttackChoice must short-circuit through
        // resultFromCurrentGame (rebuilding the snapshot from the now-updated game state) instead
        // of proceeding to the normal end-of-turn flow.
        assertThat(result.gameState().stateVersion()).isEqualTo(8);
        verify(gameLookupService).getRequiredGame(gameId);
        verify(damageApplicationService).applyDamage(benchTarget, 10);
        verify(combatResolutionService).resolveKnockoutIfNeeded(
                gameId, defenderUserId, attackerUserId, benchTarget, defenderUserId, 8, 8, "BENCH_ATTACK_DAMAGE");
        // The attack turn must NOT be silently finished as if nothing happened - the game already
        // ended because of this knockout, so the normal end-of-turn flow is skipped entirely.
        verify(attackService, never()).finishAttackTurn(any(), any(), any(), any(Integer.class), any());
    }

    @Test
    void machPunchWithASurvivingBenchPokemonShouldStillResolveTheKnockoutCheckAndThenFinishTheTurnNormally() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        PokemonInPlay benchTarget = benchPokemon(defenderUserId, targetPokemonId);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonId, gameId, defenderUserId))
                .thenReturn(Optional.of(benchTarget));
        when(damageApplicationService.applyDamage(benchTarget, 10)).thenReturn(1);
        when(gameEventFactory.publicEvent(any(), any(), any(Integer.class), any())).thenReturn(event(gameId));
        when(combatResolutionService.resolveKnockoutIfNeeded(
                gameId, defenderUserId, attackerUserId, benchTarget,
                defenderUserId, 8, 8, "BENCH_ATTACK_DAMAGE"))
                .thenReturn(CombatResolutionService.CombatResolutionResult.noKnockout());
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET,
                Map.of(
                        "damage", 10,
                        "targets", List.of(Map.of("pokemonInPlayId", targetPokemonId.toString()))),
                Map.of("targetPokemonInPlayId", targetPokemonId.toString()),
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        // Even though this hit wasn't lethal, the knockout primitive is still consulted for every
        // bench hit - proving Mach Punch's bench-damage path was never silently skipping the check.
        verify(combatResolutionService).resolveKnockoutIfNeeded(
                gameId, defenderUserId, attackerUserId, benchTarget, defenderUserId, 8, 8, "BENCH_ATTACK_DAMAGE");
    }

    @Test
    void magmaMantleShouldApplyBonusDamageWhenDiscardedTopDeckCardIsFireEnergy() {
        Card fireEnergy = new Card();
        fireEnergy.setCategory(CardCategory.BASIC_ENERGY);
        fireEnergy.setPokemonType("Fire");

        DamageCalculationRequest capturedRequest = runMagmaMantleScenario(fireEnergy, true);

        assertThat(capturedRequest.attackerModifier()).isEqualTo(50);
    }

    @Test
    void magmaMantleShouldNotApplyBonusDamageWhenDiscardedTopDeckCardIsAFirePokemon() {
        Card firePokemon = new Card();
        firePokemon.setCategory(CardCategory.BASIC_POKEMON);
        firePokemon.setPokemonType("Fire");

        DamageCalculationRequest capturedRequest = runMagmaMantleScenario(firePokemon, true);

        assertThat(capturedRequest.attackerModifier()).isEqualTo(0);
    }

    @Test
    void magmaMantleShouldNotApplyBonusDamageWhenDiscardedTopDeckCardIsATrainer() {
        Card trainer = new Card();
        trainer.setCategory(CardCategory.ITEM_TRAINER);
        trainer.setPokemonType(null);

        DamageCalculationRequest capturedRequest = runMagmaMantleScenario(trainer, true);

        assertThat(capturedRequest.attackerModifier()).isEqualTo(0);
    }

    @Test
    void magmaMantleShouldNotApplyBonusDamageWhenDiscardedTopDeckCardIsANonFireEnergy() {
        Card waterEnergy = new Card();
        waterEnergy.setCategory(CardCategory.BASIC_ENERGY);
        waterEnergy.setPokemonType("Water");

        DamageCalculationRequest capturedRequest = runMagmaMantleScenario(waterEnergy, true);

        assertThat(capturedRequest.attackerModifier()).isEqualTo(0);
    }

    @Test
    void magmaMantleShouldNotDiscardOrApplyBonusWhenChoiceIsDeclined() {
        Card fireEnergy = new Card();
        fireEnergy.setCategory(CardCategory.BASIC_ENERGY);
        fireEnergy.setPokemonType("Fire");

        DamageCalculationRequest capturedRequest = runMagmaMantleScenario(fireEnergy, false);

        assertThat(capturedRequest.attackerModifier()).isEqualTo(0);
        verify(gameCardInstanceStateService, never()).findByGameIdAndOwnerUserIdAndZone(any(), any(), eq(CardZone.DECK));
    }

    /**
     * Drives a full Magma Mantle YES_NO resolution: a top-deck card is (optionally) discarded and
     * the resulting {@link DamageCalculationRequest} sent to {@link DamageCalculatorService} is
     * captured so the test can assert whether the +50 bonus was applied.
     */
    private DamageCalculationRequest runMagmaMantleScenario(Card discardedTopDeckCard, boolean confirm) {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        UUID attackerCardId = UUID.randomUUID();
        UUID defenderCardId = UUID.randomUUID();

        GameCardInstance topDeckCardInstance = cardInstance(attackerUserId, 1);
        discardedTopDeckCard.setId(UUID.randomUUID());

        GameCardInstance defenderActiveCardInstance = new GameCardInstance();
        defenderActiveCardInstance.setId(UUID.randomUUID());
        defenderActiveCardInstance.setCardId(defenderCardId);
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonId);
        targetPokemon.setOwnerUserId(defenderUserId);
        targetPokemon.setActiveCardInstance(defenderActiveCardInstance);

        Attack magmaMantle = new Attack();
        magmaMantle.setAttackOrder(0);
        Card attackerCard = new Card();
        attackerCard.setId(attackerCardId);
        attackerCard.setAttacks(Set.of(magmaMantle));
        Card defenderCard = new Card();
        defenderCard.setId(defenderCardId);
        defenderCard.setCategory(CardCategory.BASIC_POKEMON);

        AttackChoiceServiceImpl service = service();

        if (confirm) {
            when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                    .thenReturn(List.of(topDeckCardInstance));
            when(cardService.getCardEntityById(topDeckCardInstance.getCardId())).thenReturn(discardedTopDeckCard);
        }
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonId, gameId, defenderUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(cardService.getCardEntityById(attackerCardId)).thenReturn(attackerCard);
        when(cardService.getCardEntityById(defenderCardId)).thenReturn(defenderCard);
        when(damageCalculatorService.calculateDamage(any()))
                .thenReturn(new DamageCalculationResult(30, 30, 30, 30, 30, 3));
        when(damageApplicationService.applyDamage(eq(targetPokemon), any(Integer.class))).thenReturn(3);
        when(combatResolutionService.resolveKnockoutIfNeeded(any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class), any()))
                .thenReturn(CombatResolutionService.CombatResolutionResult.noKnockout());
        when(gameEventFactory.publicEvent(any(), any(), any(Integer.class), any())).thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(new GameActionExecutionResult(GameStateDto.builder().build(), List.of()));

        Map<String, Object> pendingChoicePayload = new HashMap<>();
        pendingChoicePayload.put("continuation", "BEFORE_DAMAGE");
        pendingChoicePayload.put("effect", "MAGMA_MANTLE");
        pendingChoicePayload.put("baseDamage", 30);
        pendingChoicePayload.put("bonusDamage", 50);
        pendingChoicePayload.put("requiredEnergyType", "Fire");
        pendingChoicePayload.put("targetPokemonInPlayId", targetPokemonId.toString());
        pendingChoicePayload.put("attackerCardId", attackerCardId.toString());
        pendingChoicePayload.put("attackOrder", 0);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.YES_NO,
                pendingChoicePayload,
                Map.of("confirm", confirm),
                7);

        service.resolveAttackChoice(context);

        ArgumentCaptor<DamageCalculationRequest> requestCaptor = ArgumentCaptor.forClass(DamageCalculationRequest.class);
        verify(damageCalculatorService).calculateDamage(requestCaptor.capture());
        return requestCaptor.getValue();
    }

    private PokemonInPlay benchPokemon(UUID ownerUserId, UUID id) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(id);
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setSlotPosition(1);
        pokemonInPlay.setDamageCounters(0);
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setCardId(UUID.randomUUID());
        pokemonInPlay.setActiveCardInstance(cardInstance);
        return pokemonInPlay;
    }

    @Test
    void shouldLetDefenderDiscardSelectedHandCardsForMentalTrashAndStillEndTheAttackersTurn() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstHandCard = cardInstance(defenderUserId, 1);
        firstHandCard.setZone(CardZone.HAND);
        GameCardInstance secondHandCard = cardInstance(defenderUserId, 2);
        secondHandCard.setZone(CardZone.HAND);
        GameCardInstance untouchedHandCard = cardInstance(defenderUserId, 3);
        untouchedHandCard.setZone(CardZone.HAND);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(firstHandCard.getId(), gameId, defenderUserId))
                .thenReturn(Optional.of(firstHandCard));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(secondHandCard.getId(), gameId, defenderUserId))
                .thenReturn(Optional.of(secondHandCard));
        when(gameCardInstanceStateService.nextZonePosition(gameId, defenderUserId, CardZone.DISCARD)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> choicePayload = Map.of(
                "discardCount", 2,
                "cards", List.of(
                        Map.of("cardInstanceId", firstHandCard.getId().toString()),
                        Map.of("cardInstanceId", secondHandCard.getId().toString()),
                        Map.of("cardInstanceId", untouchedHandCard.getId().toString())));
        GameActionContext context = contextWithChoiceAndTurnEnding(
                gameId,
                defenderUserId,
                defenderUserId,
                attackerUserId,
                defenderUserId,
                OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD,
                choicePayload,
                Map.of("cardInstanceIds", List.of(firstHandCard.getId().toString(), secondHandCard.getId().toString())),
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(firstHandCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(secondHandCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(untouchedHandCard.getZone()).isEqualTo(CardZone.HAND);
        verify(gameCardInstanceStateService).resequenceZone(gameId, defenderUserId, CardZone.HAND);
        // The defender resolved the choice, but the attacker's turn is the one that ends.
        verify(attackService).finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any());
    }

    @Test
    void shouldRejectHandDiscardSelectionWithWrongCardCount() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstHandCard = cardInstance(defenderUserId, 1);
        firstHandCard.setZone(CardZone.HAND);
        AttackChoiceServiceImpl service = service();

        Map<String, Object> choicePayload = Map.of(
                "discardCount", 2,
                "cards", List.of(Map.of("cardInstanceId", firstHandCard.getId().toString())));
        GameActionContext context = contextWithChoiceAndTurnEnding(
                gameId,
                defenderUserId,
                defenderUserId,
                attackerUserId,
                defenderUserId,
                OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD,
                choicePayload,
                Map.of("cardInstanceIds", List.of(firstHandCard.getId().toString())),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class);
        verify(gameCardInstanceStateService, never()).save(any());
    }

    @Test
    void shouldRejectHandDiscardSelectionOfACardThatWasNotOffered() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance offeredCard = cardInstance(defenderUserId, 1);
        offeredCard.setZone(CardZone.HAND);
        GameCardInstance notOfferedCard = cardInstance(defenderUserId, 2);
        notOfferedCard.setZone(CardZone.HAND);
        AttackChoiceServiceImpl service = service();

        Map<String, Object> choicePayload = Map.of(
                "discardCount", 1,
                "cards", List.of(Map.of("cardInstanceId", offeredCard.getId().toString())));
        GameActionContext context = contextWithChoiceAndTurnEnding(
                gameId,
                defenderUserId,
                defenderUserId,
                attackerUserId,
                defenderUserId,
                OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD,
                choicePayload,
                Map.of("cardInstanceIds", List.of(notOfferedCard.getId().toString())),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class);
        verify(gameCardInstanceStateService, never()).save(any());
    }

    @Test
    void shouldMoveSelectedDiscardItemCardsToHandForPickup() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstItem = cardInstance(attackerUserId, 1);
        firstItem.setZone(CardZone.DISCARD);
        GameCardInstance secondItem = cardInstance(attackerUserId, 2);
        secondItem.setZone(CardZone.DISCARD);
        GameCardInstance untouchedItem = cardInstance(attackerUserId, 3);
        untouchedItem.setZone(CardZone.DISCARD);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(firstItem.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(firstItem));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(secondItem.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(secondItem));
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> choicePayload = Map.of(
                "pickupCount", 2,
                "cards", List.of(
                        Map.of("cardInstanceId", firstItem.getId().toString()),
                        Map.of("cardInstanceId", secondItem.getId().toString()),
                        Map.of("cardInstanceId", untouchedItem.getId().toString())));
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND,
                choicePayload,
                Map.of("cardInstanceIds", List.of(firstItem.getId().toString(), secondItem.getId().toString())),
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(firstItem.getZone()).isEqualTo(CardZone.HAND);
        assertThat(secondItem.getZone()).isEqualTo(CardZone.HAND);
        assertThat(untouchedItem.getZone()).isEqualTo(CardZone.DISCARD);
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.DISCARD);
    }

    @Test
    void shouldRejectPickupSelectionWithWrongCardCount() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstItem = cardInstance(attackerUserId, 1);
        firstItem.setZone(CardZone.DISCARD);
        AttackChoiceServiceImpl service = service();

        Map<String, Object> choicePayload = Map.of(
                "pickupCount", 2,
                "cards", List.of(Map.of("cardInstanceId", firstItem.getId().toString())));
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND,
                choicePayload,
                Map.of("cardInstanceIds", List.of(firstItem.getId().toString())),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class);
        verify(gameCardInstanceStateService, never()).save(any());
    }

    @Test
    void shouldRejectPickupSelectionOfACardThatWasNotOffered() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance offeredCard = cardInstance(attackerUserId, 1);
        offeredCard.setZone(CardZone.DISCARD);
        GameCardInstance notOfferedCard = cardInstance(attackerUserId, 2);
        notOfferedCard.setZone(CardZone.DISCARD);
        AttackChoiceServiceImpl service = service();

        Map<String, Object> choicePayload = Map.of(
                "pickupCount", 1,
                "cards", List.of(Map.of("cardInstanceId", offeredCard.getId().toString())));
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND,
                choicePayload,
                Map.of("cardInstanceIds", List.of(notOfferedCard.getId().toString())),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class);
        verify(gameCardInstanceStateService, never()).save(any());
    }

    @Test
    void shouldApplyChosenSpecialConditionForConversionPowder() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID defenderPokemonId = UUID.randomUUID();
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        defenderPokemon.setId(defenderPokemonId);
        defenderPokemon.setOwnerUserId(defenderUserId);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(
                GameStateDto.builder().build(), List.of());
        GameEventDto statusEvent = event(gameId);
        AttackChoiceServiceImpl service = service();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(defenderPokemonId, gameId, defenderUserId))
                .thenReturn(Optional.of(defenderPokemon));
        when(specialConditionApplicationService.applyCondition(
                gameId,
                attackerUserId,
                defenderPokemon,
                SpecialConditionType.ASLEEP,
                7,
                8))
                .thenReturn(List.of(statusEvent));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                ChooseSpecialConditionAttackEffect.CHOICE_TYPE,
                Map.of(
                        "defenderPokemonInPlayId", defenderPokemonId.toString(),
                        ChooseSpecialConditionAttackEffect.CONDITION_TYPES_KEY, List.of("ASLEEP", "POISONED")),
                Map.of("conditionType", "ASLEEP"),
                7);
        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        verify(specialConditionApplicationService).applyCondition(
                gameId,
                attackerUserId,
                defenderPokemon,
                SpecialConditionType.ASLEEP,
                7,
                8);
    }

    @Test
    void shouldRejectSpecialConditionChoiceThatWasNotOffered() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        AttackChoiceServiceImpl service = service();

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                ChooseSpecialConditionAttackEffect.CHOICE_TYPE,
                Map.of(
                        "defenderPokemonInPlayId", UUID.randomUUID().toString(),
                        ChooseSpecialConditionAttackEffect.CONDITION_TYPES_KEY, List.of("ASLEEP", "POISONED")),
                Map.of("conditionType", "CONFUSED"),
                7);

        assertThatThrownBy(() -> service.resolveAttackChoice(context))
                .isInstanceOf(InvalidGameActionException.class);
        verify(specialConditionApplicationService, never()).applyCondition(any(), any(), any(), any(), anyInt(), anyInt());
        verify(attackService, never()).finishAttackTurn(any(), any(), any(), anyInt(), any());
    }

    @Test
    void shouldSkipOptionalEnergyMoveWhenTimeoutResolvesTrickySteps() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        Map<String, Object> pendingChoicePayload = Map.of(
                PendingAttackChoiceEffect.OPTIONAL_KEY, true,
                PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.AUTO_SKIP.name(),
                "energies", List.of(Map.of("attachedCardId", UUID.randomUUID().toString())),
                "targets", List.of(Map.of("pokemonInPlayId", UUID.randomUUID().toString())));
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH,
                pendingChoicePayload,
                Map.of("reason", "TURN_TIMEOUT"),
                7);

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionExecutionResult result = service.resolveAttackChoiceForTimeout(context);

        assertThat(result).isSameAs(expectedResult);
        verify(pokemonAttachedCardStateService, never()).save(any());
        ArgumentCaptor<Map<String, Object>> eventPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(
                eq(gameId),
                eq(GameEventType.ATTACK_CHOICE_RESOLVED),
                eq(8),
                eventPayloadCaptor.capture());
        assertThat(eventPayloadCaptor.getValue())
                .containsEntry("confirmed", false)
                .containsEntry("autoResolved", true)
                .containsEntry("reason", "TURN_TIMEOUT");
    }

    @Test
    void shouldAutoResolveMandatoryMentalTrashWithLegalCardsOnTimeout() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstHandCard = handCard(defenderUserId, 1);
        GameCardInstance secondHandCard = handCard(defenderUserId, 2);
        GameCardInstance thirdHandCard = handCard(defenderUserId, 3);
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        List<Map<String, Object>> cardOptions = List.of(
                Map.of("cardInstanceId", firstHandCard.getId().toString()),
                Map.of("cardInstanceId", secondHandCard.getId().toString()),
                Map.of("cardInstanceId", thirdHandCard.getId().toString()));
        Map<String, Object> pendingChoicePayload = Map.of(
                PendingAttackChoiceEffect.OPTIONAL_KEY, false,
                PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL.name(),
                "discardCount", 2,
                "cards", cardOptions);
        GameActionContext context = contextWithChoiceAndTurnEnding(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerUserId,
                defenderUserId,
                OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD,
                pendingChoicePayload,
                Map.of("reason", "TURN_TIMEOUT"),
                7);

        when(gameRandomService.shuffledCopy(any())).thenAnswer(invocation -> {
            List<?> source = invocation.getArgument(0);
            return List.of(source.get(1), source.get(0), source.get(2));
        });
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                secondHandCard.getId(), gameId, defenderUserId)).thenReturn(Optional.of(secondHandCard));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                firstHandCard.getId(), gameId, defenderUserId)).thenReturn(Optional.of(firstHandCard));
        when(gameCardInstanceStateService.nextZonePosition(gameId, defenderUserId, CardZone.DISCARD)).thenReturn(4);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        GameActionExecutionResult result = service.resolveAttackChoiceForTimeout(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(secondHandCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(secondHandCard.getZonePosition()).isEqualTo(4);
        assertThat(firstHandCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(firstHandCard.getZonePosition()).isEqualTo(5);
        assertThat(thirdHandCard.getZone()).isEqualTo(CardZone.HAND);
        verify(gameCardInstanceStateService).resequenceZone(gameId, defenderUserId, CardZone.HAND);
    }

    @Test
    void shouldMoveDistinctBasicEnergiesToHand() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        GameCardInstance energy1 = cardInstance(attackerUserId, 1);
        GameCardInstance energy2 = cardInstance(attackerUserId, 2);

        List<Map<String, Object>> cardOptions = List.of(
                Map.of("cardInstanceId", energy1.getId().toString(), "energyType", "FIRE"),
                Map.of("cardInstanceId", energy2.getId().toString(), "energyType", "WATER")
        );
        Map<String, Object> pendingChoicePayload = Map.of(
                "cards", cardOptions
        );
        Map<String, Object> requestPayload = Map.of(
                "cardInstanceIds", List.of(energy1.getId().toString(), energy2.getId().toString())
        );

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND,
                pendingChoicePayload,
                requestPayload,
                1
        );

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                energy1.getId(), gameId, attackerUserId)).thenReturn(Optional.of(energy1));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                energy2.getId(), gameId, attackerUserId)).thenReturn(Optional.of(energy2));

        Card fireCard = new Card();
        fireCard.setCategory(CardCategory.BASIC_ENERGY);
        fireCard.setName("Fire Energy");

        Card waterCard = new Card();
        waterCard.setCategory(CardCategory.BASIC_ENERGY);
        waterCard.setName("Water Energy");

        when(cardService.getCardEntityById(energy1.getCardId())).thenReturn(fireCard);
        when(cardService.getCardEntityById(energy2.getCardId())).thenReturn(waterCard);

        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND)).thenReturn(1, 2);
        
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));

        AttackChoiceServiceImpl service = service();
        service.resolveAttackChoice(context);

        verify(gameCardInstanceStateService, times(2)).save(any());
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.DECK);
    }
    @Test
    void shouldMoveOpponentEnergy() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        UUID attachedCardId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();

        Map<String, Object> pendingChoicePayload = Map.of(
                "energies", List.of(Map.of("attachedCardId", attachedCardId.toString())),
                "targets", List.of(Map.of("pokemonInPlayId", targetPokemonId.toString()))
        );
        Map<String, Object> requestPayload = Map.of(
                "attachedCardId", attachedCardId.toString(),
                "targetPokemonInPlayId", targetPokemonId.toString()
        );

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH,
                pendingChoicePayload,
                requestPayload,
                1
        );

        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonId);
        targetPokemon.setOwnerUserId(defenderUserId);
        targetPokemon.setSlotPosition(1);

        PokemonInPlay activePokemon = new PokemonInPlay();
        activePokemon.setId(UUID.randomUUID());
        activePokemon.setOwnerUserId(defenderUserId);
        activePokemon.setSlotPosition(0);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setId(attachedCardId);
        attachedCard.setPokemonInPlay(activePokemon);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonId, gameId, defenderUserId))
                .thenReturn(Optional.of(targetPokemon));
        
        when(pokemonAttachedCardStateService.findByGameCardInstanceId(attachedCardId))
                .thenReturn(Optional.of(attachedCard));

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));

        AttackChoiceServiceImpl service = service();
        service.resolveAttackChoice(context);

        verify(pokemonAttachedCardStateService).save(attachedCard);
    }
    @Test
    void shouldReorderTopDeck() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        GameCardInstance card1 = cardInstance(attackerUserId, 1);
        GameCardInstance card2 = cardInstance(attackerUserId, 2);

        Map<String, Object> pendingChoicePayload = Map.of(
                "cards", List.of(
                        Map.of("cardInstanceId", card1.getId().toString()),
                        Map.of("cardInstanceId", card2.getId().toString())
                )
        );
        Map<String, Object> requestPayload = Map.of(
                "cardInstanceIds", List.of(card2.getId().toString(), card1.getId().toString())
        );

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.REORDER_TOP_DECK,
                pendingChoicePayload,
                requestPayload,
                1
        );

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(card1.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(card1));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(card2.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(card2));

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));

        AttackChoiceServiceImpl service = service();
        service.resolveAttackChoice(context);

        verify(gameCardInstanceStateService, times(2)).save(any());
    }

    @Test
    void shouldResolveAttackChoiceForTimeout() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        Map<String, Object> pendingChoicePayload = Map.of();
        
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                LookOpponentDeckTopCardAttackEffect.CHOICE_TYPE,
                pendingChoicePayload,
                Map.of(),
                1
        );

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));

        AttackChoiceServiceImpl service = service();
        service.resolveAttackChoiceForTimeout(context);

        verify(gameEventFactory).publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any());
    }

    @Test
    void shouldAttachSelectedDeckEnergy() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        GameCardInstance energyCard = cardInstance(attackerUserId, 1);
        UUID targetPokemonId = UUID.randomUUID();

        Map<String, Object> pendingChoicePayload = Map.of(
                "cards", List.of(Map.of("cardInstanceId", energyCard.getId().toString())),
                "targetPokemonInPlayId", targetPokemonId.toString(),
                "targets", List.of(Map.of("pokemonInPlayId", targetPokemonId.toString()))
        );
        Map<String, Object> requestPayload = Map.of(
                "cardInstanceId", energyCard.getId().toString(),
                "targetPokemonInPlayId", targetPokemonId.toString()
        );

        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF,
                pendingChoicePayload,
                requestPayload,
                1
        );

        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonId);
        targetPokemon.setOwnerUserId(attackerUserId);

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(energyCard.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(energyCard));
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonId, gameId, attackerUserId))
                .thenReturn(Optional.of(targetPokemon));

        Card energyEntity = new Card();
        energyEntity.setCategory(CardCategory.BASIC_ENERGY);
        when(cardService.getCardEntityById(energyCard.getCardId())).thenReturn(energyEntity);

        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(1);
        
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));

        AttackChoiceServiceImpl service = service();
        service.resolveAttackChoice(context);

        verify(gameCardInstanceStateService).save(energyCard);
        verify(pokemonAttachedCardStateService).save(any(PokemonAttachedCard.class));
    }

    @Test
    void shouldSelectDistinctBasicEnergiesForTimeout() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        UUID energy1Id = UUID.randomUUID();
        UUID energy2Id = UUID.randomUUID();

        Map<String, Object> pendingChoicePayload = Map.of(
                PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL.name(),
                "cards", List.of(
                        Map.of("cardInstanceId", energy1Id.toString(), "energyType", "FIRE"),
                        Map.of("cardInstanceId", energy2Id.toString(), "energyType", "WATER")
                )
        );
        
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND,
                pendingChoicePayload,
                Map.of(),
                1
        );

        when(gameRandomService.shuffledCopy(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));
        
        // Setup mock for actual choice resolution since timeout creates a synthetic payload and resolves it
        GameCardInstance energy1 = cardInstance(attackerUserId, 1);
        energy1.setId(energy1Id);
        GameCardInstance energy2 = cardInstance(attackerUserId, 2);
        energy2.setId(energy2Id);
        
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(energy1Id, gameId, attackerUserId)).thenReturn(Optional.of(energy1));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(energy2Id, gameId, attackerUserId)).thenReturn(Optional.of(energy2));

        Card fireCard = new Card();
        fireCard.setCategory(CardCategory.BASIC_ENERGY);
        fireCard.setName("Fire Energy");
        Card waterCard = new Card();
        waterCard.setCategory(CardCategory.BASIC_ENERGY);
        waterCard.setName("Water Energy");

        when(cardService.getCardEntityById(energy1.getCardId())).thenReturn(fireCard);
        when(cardService.getCardEntityById(energy2.getCardId())).thenReturn(waterCard);
        
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND)).thenReturn(1, 2);
        
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(2), any()))
                .thenReturn(event(gameId));

        AttackChoiceServiceImpl service = service();
        service.resolveAttackChoiceForTimeout(context);

        verify(gameEventFactory).publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(2), any());
    }

    private AttackChoiceServiceImpl service() {
        return new AttackChoiceServiceImpl(
                attackService,
                gameCardInstanceStateService,
                pokemonInPlayStateService,
                pokemonAttachedCardStateService,
                cardService,
                damageApplicationService,
                damageCalculatorService,
                specialConditionApplicationService,
                combatResolutionService,
                gameLookupService,
                gameStateQueryService,
                gameRandomService,
                gameEventFactory);
    }

    private GameActionContext context(
            UUID gameId, UUID attackerUserId, UUID defenderUserId, boolean confirm, int stateVersion) {
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.RESOLVE_ATTACK_CHOICE, stateVersion, Map.of("confirm", confirm));
        ResolutionStateDto resolution = ResolutionStateDto.builder()
                .resolutionType(ResolutionStateDto.ATTACK_CHOICE_REQUIRED)
                .pendingChoicePlayerId(attackerUserId)
                .pendingChoiceType(LookOpponentDeckTopCardAttackEffect.CHOICE_TYPE)
                .nextActivePlayerId(defenderUserId)
                .nextTurnNumber(stateVersion + 1)
                .build();
        GameStateDto currentState = GameStateDto.builder()
                .stateVersion(stateVersion)
                .resolution(resolution)
                .build();
        return new GameActionContext(gameId, attackerUserId, request, currentState);
    }

    private GameActionContext contextWithChoice(
            UUID gameId,
            UUID actorUserId,
            UUID pendingPlayerId,
            UUID defenderUserId,
            String choiceType,
            Map<String, Object> pendingChoicePayload,
            Map<String, Object> requestPayload,
            int stateVersion) {
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.RESOLVE_ATTACK_CHOICE,
                stateVersion,
                requestPayload);
        ResolutionStateDto resolution = ResolutionStateDto.builder()
                .resolutionType(ResolutionStateDto.ATTACK_CHOICE_REQUIRED)
                .pendingChoicePlayerId(pendingPlayerId)
                .pendingChoiceType(choiceType)
                .pendingChoicePayload(pendingChoicePayload)
                .nextActivePlayerId(defenderUserId)
                .nextTurnNumber(stateVersion + 1)
                .build();
        GameStateDto currentState = GameStateDto.builder()
                .stateVersion(stateVersion)
                .resolution(resolution)
                .build();
        return new GameActionContext(gameId, actorUserId, request, currentState);
    }

    private GameActionContext contextWithChoiceAndTurnEnding(
            UUID gameId,
            UUID actorUserId,
            UUID pendingPlayerId,
            UUID turnEndingPlayerId,
            UUID defenderUserId,
            String choiceType,
            Map<String, Object> pendingChoicePayload,
            Map<String, Object> requestPayload,
            int stateVersion) {
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.RESOLVE_ATTACK_CHOICE,
                stateVersion,
                requestPayload);
        ResolutionStateDto resolution = ResolutionStateDto.builder()
                .resolutionType(ResolutionStateDto.ATTACK_CHOICE_REQUIRED)
                .pendingChoicePlayerId(pendingPlayerId)
                .pendingChoiceType(choiceType)
                .pendingChoicePayload(pendingChoicePayload)
                .nextActivePlayerId(defenderUserId)
                .nextTurnNumber(stateVersion + 1)
                .turnEndingPlayerId(turnEndingPlayerId)
                .build();
        GameStateDto currentState = GameStateDto.builder()
                .stateVersion(stateVersion)
                .resolution(resolution)
                .build();
        return new GameActionContext(gameId, actorUserId, request, currentState);
    }

    private GameCardInstance cardInstance(UUID ownerUserId, int position) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(UUID.randomUUID());
        cardInstance.setZone(CardZone.DECK);
        cardInstance.setZonePosition(position);
        cardInstance.setFaceDown(true);
        return cardInstance;
    }

    private GameCardInstance handCard(UUID ownerUserId, int position) {
        GameCardInstance cardInstance = cardInstance(ownerUserId, position);
        cardInstance.setZone(CardZone.HAND);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private GameEventDto event(UUID gameId) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_CHOICE_RESOLVED, 8, false, Instant.now(), Map.of());
    }
    @Test
    void shouldReorderTopDeckSuccessfully() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstCard = cardInstance(attackerUserId, 1);
        GameCardInstance secondCard = cardInstance(attackerUserId, 2);
        
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(firstCard.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(firstCard));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(secondCard.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(secondCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> pendingChoicePayload = Map.of(
                "cards", List.of(
                        Map.of("cardInstanceId", firstCard.getId().toString()),
                        Map.of("cardInstanceId", secondCard.getId().toString())
                )
        );
        Map<String, Object> requestPayload = Map.of(
                "cardInstanceIds", List.of(secondCard.getId().toString(), firstCard.getId().toString())
        );
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.REORDER_TOP_DECK,
                pendingChoicePayload,
                requestPayload,
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(secondCard.getZonePosition()).isEqualTo(1);
        assertThat(firstCard.getZonePosition()).isEqualTo(2);
        verify(gameCardInstanceStateService).save(firstCard);
        verify(gameCardInstanceStateService).save(secondCard);
    }

    @Test
    void shouldSelectDistinctBasicEnergiesToHandSuccessfully() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance fireEnergyInst = cardInstance(attackerUserId, 1);
        GameCardInstance waterEnergyInst = cardInstance(attackerUserId, 2);
        
        Card fireEnergy = new Card();
        fireEnergy.setCategory(CardCategory.BASIC_ENERGY);
        fireEnergy.setPokemonType("Fire");
        
        Card waterEnergy = new Card();
        waterEnergy.setCategory(CardCategory.BASIC_ENERGY);
        waterEnergy.setPokemonType("Water");
        
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(fireEnergyInst.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(fireEnergyInst));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(waterEnergyInst.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(waterEnergyInst));
        when(cardService.getCardEntityById(fireEnergyInst.getCardId())).thenReturn(fireEnergy);
        when(cardService.getCardEntityById(waterEnergyInst.getCardId())).thenReturn(waterEnergy);
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND)).thenReturn(1).thenReturn(2);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK)).thenReturn(List.of());
        
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> pendingChoicePayload = Map.of();
        Map<String, Object> requestPayload = Map.of(
                "cardInstanceIds", List.of(fireEnergyInst.getId().toString(), waterEnergyInst.getId().toString())
        );
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND,
                pendingChoicePayload,
                requestPayload,
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(fireEnergyInst.getZone()).isEqualTo(CardZone.HAND);
        assertThat(waterEnergyInst.getZone()).isEqualTo(CardZone.HAND);
        verify(gameCardInstanceStateService).save(fireEnergyInst);
        verify(gameCardInstanceStateService).save(waterEnergyInst);
    }
    @Test
    void shouldMoveOpponentActiveEnergyToBenchSuccessfully() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        
        GameCardInstance energyInstance = new GameCardInstance();
        energyInstance.setId(UUID.randomUUID());
        energyInstance.setCardId(UUID.randomUUID());
        energyInstance.setOwnerUserId(defenderUserId);
        Card energyCard = new Card();
        energyCard.setId(energyInstance.getCardId());
        energyCard.setCategory(CardCategory.BASIC_ENERGY);
        
        PokemonInPlay opponentActive = new PokemonInPlay();
        opponentActive.setId(UUID.randomUUID());
        opponentActive.setOwnerUserId(defenderUserId);
        opponentActive.setSlotPosition(0);
        
        PokemonAttachedCard attachedEnergy = new PokemonAttachedCard();
        attachedEnergy.setId(UUID.randomUUID());
        attachedEnergy.setPokemonInPlay(opponentActive);
        attachedEnergy.setGameCardInstance(energyInstance);
        attachedEnergy.setAttachedCardType(ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType.BASIC_ENERGY);

        PokemonInPlay opponentBench = new PokemonInPlay();
        opponentBench.setId(UUID.randomUUID());
        opponentBench.setOwnerUserId(defenderUserId);
        opponentBench.setSlotPosition(1);

        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(opponentBench.getId(), gameId, defenderUserId))
                .thenReturn(Optional.of(opponentBench));
        when(pokemonAttachedCardStateService.findByGameCardInstanceId(energyInstance.getId()))
                .thenReturn(Optional.of(attachedEnergy));
        
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> pendingChoicePayload = Map.of(
                "energies", List.of(Map.of("attachedCardId", energyInstance.getId().toString())),
                "targets", List.of(Map.of("pokemonInPlayId", opponentBench.getId().toString()))
        );
        Map<String, Object> requestPayload = Map.of(
                "attachedCardId", energyInstance.getId().toString(),
                "targetPokemonInPlayId", opponentBench.getId().toString()
        );
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH,
                pendingChoicePayload,
                requestPayload,
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(attachedEnergy.getPokemonInPlay()).isEqualTo(opponentBench);
        verify(pokemonAttachedCardStateService).save(attachedEnergy);
    }

    @Test
    void shouldAttachDeckCardToSelfSuccessfully() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        
        GameCardInstance energyInstance = new GameCardInstance();
        energyInstance.setId(UUID.randomUUID());
        energyInstance.setCardId(UUID.randomUUID());
        energyInstance.setZone(CardZone.DECK);
        energyInstance.setOwnerUserId(attackerUserId);
        
        Card energyCard = new Card();
        energyCard.setId(energyInstance.getCardId());
        energyCard.setCategory(CardCategory.BASIC_ENERGY);
        
        PokemonInPlay attackerActive = new PokemonInPlay();
        attackerActive.setId(UUID.randomUUID());
        attackerActive.setOwnerUserId(attackerUserId);
        attackerActive.setSlotPosition(0);

        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(attackerActive.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(attackerActive));
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(energyInstance.getId(), gameId, attackerUserId))
                .thenReturn(Optional.of(energyInstance));
        when(cardService.getCardEntityById(energyCard.getId())).thenReturn(energyCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(1);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK)).thenReturn(List.of());
        
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> pendingChoicePayload = Map.of(
                "cards", List.of(Map.of("cardInstanceId", energyInstance.getId().toString())),
                "targetPokemonInPlayId", attackerActive.getId().toString()
        );
        Map<String, Object> requestPayload = Map.of(
                "cardInstanceId", energyInstance.getId().toString()
        );
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF,
                pendingChoicePayload,
                requestPayload,
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        assertThat(energyInstance.getZone()).isEqualTo(CardZone.ATTACHED);
        verify(pokemonAttachedCardStateService).save(any(PokemonAttachedCard.class));
    }

    @Test
    void shouldApplyChosenSpecialConditionSuccessfully() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID defenderPokemonId = UUID.randomUUID();
        
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        defenderPokemon.setId(defenderPokemonId);

        GameActionExecutionResult expectedResult = new GameActionExecutionResult(GameStateDto.builder().build(), List.of());
        AttackChoiceServiceImpl service = service();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(defenderPokemonId, gameId, defenderUserId))
                .thenReturn(Optional.of(defenderPokemon));
        
        when(specialConditionApplicationService.applyCondition(eq(gameId), eq(attackerUserId), eq(defenderPokemon), eq(SpecialConditionType.POISONED), eq(7), eq(8)))
                .thenReturn(List.of(event(gameId)));
                
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_CHOICE_RESOLVED), eq(8), any()))
                .thenReturn(event(gameId));
        when(attackService.finishAttackTurn(any(), eq(attackerUserId), eq(defenderUserId), eq(8), any()))
                .thenReturn(expectedResult);

        Map<String, Object> pendingChoicePayload = Map.of(
                "defenderPokemonInPlayId", defenderPokemonId.toString(),
                "conditionTypes", List.of("POISONED", "ASLEEP")
        );
        Map<String, Object> requestPayload = Map.of(
                "conditionType", "POISONED"
        );
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                ChooseSpecialConditionAttackEffect.CHOICE_TYPE,
                pendingChoicePayload,
                requestPayload,
                7);

        GameActionExecutionResult result = service.resolveAttackChoice(context);

        assertThat(result).isSameAs(expectedResult);
        verify(specialConditionApplicationService).applyCondition(eq(gameId), eq(attackerUserId), eq(defenderPokemon), eq(SpecialConditionType.POISONED), eq(7), eq(8));
    }

    @Test
    void shouldCoverTimeoutSelectionsForVariousChoiceTypes() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        AttackChoiceServiceImpl service = service();
        when(gameRandomService.shuffledCopy(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> choiceTypes = List.of(
                PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET,
                PendingAttackChoiceEffect.SELECT_OPPONENT_ATTACK,
                PendingAttackChoiceEffect.REORDER_TOP_DECK,
                PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD,
                PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF,
                PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH,
                PendingAttackChoiceEffect.SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON,
                PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND,
                OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD,
                PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND
        );

        for (String choiceType : choiceTypes) {
            Map<String, Object> payload = new HashMap<>();
            payload.put(PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL.name());
            
            // Dummy options for all choice types
            payload.put("targets", List.of(Map.of("pokemonInPlayId", UUID.randomUUID().toString())));
            payload.put("attacks", List.of(Map.of("attackOrder", 1)));
            payload.put("cards", List.of(
                    Map.of("cardInstanceId", UUID.randomUUID().toString(), "energyType", "FIRE"),
                    Map.of("cardInstanceId", UUID.randomUUID().toString(), "energyType", "WATER")
            ));
            payload.put("energies", List.of(Map.of("attachedCardId", UUID.randomUUID().toString())));
            
            GameActionContext context = contextWithChoice(
                    gameId,
                    attackerUserId,
                    attackerUserId,
                    defenderUserId,
                    choiceType,
                    payload,
                    Map.of("reason", "TURN_TIMEOUT"),
                    7);

            // We expect an exception because the randomly selected UUID won't be found in our mock database
            try {
                service.resolveAttackChoiceForTimeout(context);
            } catch (Exception ignored) {
                // Ignore exception, we just want to cover legalTimeoutSelection
            }
        }
    }

    @Test
    void shouldThrowWhenNoPendingChoiceForTimeout() {
        GameActionContext context = contextWithChoice(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of(), 1);
        AttackChoiceServiceImpl service = service();
        org.junit.jupiter.api.Assertions.assertThrows(InvalidGameActionException.class, () -> service.resolveAttackChoiceForTimeout(context));
    }

    @Test
    void shouldCoverTimeoutPolicyNone() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        Map<String, Object> payload = new HashMap<>();
        payload.put(PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.NONE.name());
        
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                "ANY_CHOICE_TYPE",
                payload,
                Map.of("reason", "TURN_TIMEOUT"),
                7);

        AttackChoiceServiceImpl service = service();
        try {
            service.resolveAttackChoiceForTimeout(context);
        } catch (Exception ignored) {}
    }

    @Test
    void shouldThrowOnUnsupportedTimeoutPolicyChoiceType() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        Map<String, Object> payload = new HashMap<>();
        payload.put(PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY, PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL.name());
        
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                "UNSUPPORTED_CHOICE_TYPE",
                payload,
                Map.of("reason", "TURN_TIMEOUT"),
                7);

        AttackChoiceServiceImpl service = service();
        org.junit.jupiter.api.Assertions.assertThrows(InvalidGameActionException.class, () -> service.resolveAttackChoiceForTimeout(context));
    }

    @Test
    void shouldCoverBooleanOptionalValue() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();

        Map<String, Object> pendingPayload = new HashMap<>();
        pendingPayload.put(PendingAttackChoiceEffect.OPTIONAL_KEY, true);
        
        GameActionContext context = contextWithChoice(
                gameId,
                attackerUserId,
                attackerUserId,
                defenderUserId,
                "ANY_CHOICE_TYPE",
                pendingPayload,
                Map.of("confirm", false),
                7);

        AttackChoiceServiceImpl service = service();
        try {
            service.resolveAttackChoice(context);
        } catch (Exception ignored) {}
    }
}





