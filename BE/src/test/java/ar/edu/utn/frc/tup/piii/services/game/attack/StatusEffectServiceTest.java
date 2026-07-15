package ar.edu.utn.frc.tup.piii.services.game.attack;



import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameEventFactoryImpl;
import ar.edu.utn.frc.tup.piii.services.game.attack.impl.StatusEffectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusEffectServiceTest {

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameParticipantStateService gameParticipantStateService;

    @Mock
    private CardService cardService;

    @Mock
    private KnockoutService knockoutService;

    @Mock
    private PrizeService prizeService;

    @Mock
    private VictoryConditionService victoryConditionService;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameRandomService gameRandomService;

    private final GameEventFactory gameEventFactory = new GameEventFactoryImpl();

    private StatusEffectService service() {
        return new StatusEffectServiceImpl(
                specialConditionStateService,
                pokemonInPlayStateService,
                gameParticipantStateService,
                cardService,
                knockoutService,
                prizeService,
                victoryConditionService,
                gameEventFactory,
                gameLookupService,
                gameRandomService);
    }

    @Test
    void shouldReturnNoEventsWhenAttackAppliesNoConditions() {
        StatusEffectService service = service();
        PokemonInPlay targetPokemon = pokemonInPlay(UUID.randomUUID(), UUID.randomUUID(), 0);

        StatusEffectService.AppliedConditionsResult result = service.applyAttackConditions(
                UUID.randomUUID(),
                UUID.randomUUID(),
                targetPokemon,
                Map.of(),
                2,
                5);

        assertThat(result.events()).isEmpty();
        assertThat(result.updatedConditionsByPlayer()).isEmpty();
    }

    @Test
    void shouldReplaceExclusiveConditionAndPersistAppliedTurn() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID sourcePlayerId = UUID.randomUUID();
        UUID targetPlayerId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = pokemonInPlay(targetPlayerId, pokemonInPlayId, 0);

        SpecialCondition asleep = new SpecialCondition();
        asleep.setPokemonInPlay(targetPokemon);
        asleep.setConditionType(SpecialConditionType.ASLEEP);
        asleep.setAppliedTurn(1);

        when(specialConditionStateService.save(any(SpecialCondition.class))).thenAnswer(returnsFirstArg());
        when(specialConditionStateService.activeConditionTypes(pokemonInPlayId))
                .thenReturn(List.of(SpecialConditionType.PARALYZED));

        StatusEffectService.AppliedConditionsResult result = service.applyAttackConditions(
                gameId,
                sourcePlayerId,
                targetPokemon,
                Map.of("statusCondition", "PARALYZED"),
                4,
                9);

        ArgumentCaptor<SpecialCondition> savedCondition = ArgumentCaptor.forClass(SpecialCondition.class);
        verify(specialConditionStateService).clearConditions(pokemonInPlayId, java.util.EnumSet.of(
                SpecialConditionType.ASLEEP,
                SpecialConditionType.CONFUSED,
                SpecialConditionType.PARALYZED));
        verify(specialConditionStateService).save(savedCondition.capture());
        assertThat(savedCondition.getValue().getConditionType()).isEqualTo(SpecialConditionType.PARALYZED);
        assertThat(savedCondition.getValue().getAppliedTurn()).isEqualTo(4);
        assertThat(result.updatedConditionsByPlayer().get(targetPlayerId)).containsExactly(SpecialConditionType.PARALYZED);
        assertThat(result.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .containsExactly(GameEventType.STATUS_APPLIED);
    }

    @Test
    void shouldApplyMultipleConditionsAndSkipAlreadyActiveCondition() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID sourcePlayerId = UUID.randomUUID();
        UUID targetPlayerId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = pokemonInPlay(targetPlayerId, pokemonInPlayId, 0);
        SpecialCondition poisoned = savedCondition(targetPokemon, SpecialConditionType.POISONED, 1);

        when(specialConditionStateService.findByPokemonInPlayIdAndConditionType(pokemonInPlayId, SpecialConditionType.BURNED))
                .thenReturn(Optional.empty());
        when(specialConditionStateService.findByPokemonInPlayIdAndConditionType(pokemonInPlayId, SpecialConditionType.POISONED))
                .thenReturn(Optional.of(poisoned));
        when(specialConditionStateService.save(any(SpecialCondition.class))).thenAnswer(returnsFirstArg());
        when(specialConditionStateService.activeConditionTypes(pokemonInPlayId))
                .thenReturn(List.of(SpecialConditionType.BURNED, SpecialConditionType.POISONED));

        StatusEffectService.AppliedConditionsResult result = service.applyAttackConditions(
                gameId,
                sourcePlayerId,
                targetPokemon,
                Map.of("statusConditions", List.of(SpecialConditionType.BURNED, "POISONED")),
                3,
                6);

        ArgumentCaptor<SpecialCondition> savedCondition = ArgumentCaptor.forClass(SpecialCondition.class);
        verify(specialConditionStateService).save(savedCondition.capture());
        assertThat(savedCondition.getValue().getConditionType()).isEqualTo(SpecialConditionType.BURNED);
        assertThat(result.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .containsExactly(GameEventType.STATUS_APPLIED);
        assertThat(result.updatedConditionsByPlayer().get(targetPlayerId))
                .containsExactly(SpecialConditionType.BURNED, SpecialConditionType.POISONED);
    }

    @Test
    void shouldRejectUnsupportedConditionPayloadValue() {
        StatusEffectService service = service();
        PokemonInPlay targetPokemon = pokemonInPlay(UUID.randomUUID(), UUID.randomUUID(), 0);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                service.applyAttackConditions(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        targetPokemon,
                        Map.of("statusCondition", 123),
                        1,
                        2);
            }
        })
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Unsupported special condition value");
    }

    @Test
    void shouldFailAttackAndApplySelfDamageWhenConfusionCoinIsTails() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID attackerPokemonId = UUID.randomUUID();
        PokemonInPlay attackerPokemon = pokemonInPlay(attackerUserId, attackerPokemonId, 0);
        Card attackerCard = new Card();
        attackerCard.setHp(60);

        when(specialConditionStateService.findByPokemonInPlayIdAndConditionType(attackerPokemonId, SpecialConditionType.CONFUSED))
                .thenReturn(Optional.of(savedCondition(attackerPokemon, SpecialConditionType.CONFUSED, 2)));
        when(gameRandomService.flipCoin()).thenReturn(false);
        when(cardService.getCardEntityById(attackerPokemon.getActiveCardInstance().getCardId())).thenReturn(attackerCard);
        when(pokemonInPlayStateService.save(any(PokemonInPlay.class))).thenAnswer(returnsFirstArg());

        StatusEffectService.ConfusionResolution resolution = service.resolveConfusionBeforeAttack(
                gameId,
                attackerUserId,
                attackerPokemon,
                3,
                7);

        assertThat(resolution.attackCanProceed()).isFalse();
        assertThat(resolution.winnerUserId()).isNull();
        assertThat(attackerPokemon.getDamageCounters()).isEqualTo(3);
        assertThat(resolution.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .containsExactly(GameEventType.DAMAGE_APPLIED);
    }

    @Test
    void shouldAllowAttackWhenPokemonIsNotConfusedOrCoinIsHeads() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID attackerPokemonId = UUID.randomUUID();
        PokemonInPlay attackerPokemon = pokemonInPlay(attackerUserId, attackerPokemonId, 0);

        when(specialConditionStateService.findByPokemonInPlayIdAndConditionType(attackerPokemonId, SpecialConditionType.CONFUSED))
                .thenReturn(Optional.empty(), Optional.of(savedCondition(attackerPokemon, SpecialConditionType.CONFUSED, 2)));

        StatusEffectService.ConfusionResolution noConfusion = service.resolveConfusionBeforeAttack(
                gameId,
                attackerUserId,
                attackerPokemon,
                3,
                7);

        when(gameRandomService.flipCoin()).thenReturn(true);
        StatusEffectService.ConfusionResolution heads = service.resolveConfusionBeforeAttack(
                gameId,
                attackerUserId,
                attackerPokemon,
                3,
                8);

        assertThat(noConfusion.attackCanProceed()).isTrue();
        assertThat(heads.attackCanProceed()).isTrue();
        assertThat(noConfusion.events()).isEmpty();
        assertThat(heads.events()).isEmpty();
    }

    @Test
    void shouldFinishGameWhenConfusionSelfDamageKnocksOutAttacker() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID attackerPokemonId = UUID.randomUUID();
        UUID prizeCardId = UUID.randomUUID();
        PokemonInPlay attackerPokemon = pokemonInPlay(attackerUserId, attackerPokemonId, 3);
        Card attackerCard = new Card();
        attackerCard.setHp(60);
        Game game = new Game();

        when(specialConditionStateService.findByPokemonInPlayIdAndConditionType(attackerPokemonId, SpecialConditionType.CONFUSED))
                .thenReturn(Optional.of(savedCondition(attackerPokemon, SpecialConditionType.CONFUSED, 2)));
        when(gameRandomService.flipCoin()).thenReturn(false);
        when(cardService.getCardEntityById(attackerPokemon.getActiveCardInstance().getCardId())).thenReturn(attackerCard);
        when(pokemonInPlayStateService.save(any(PokemonInPlay.class))).thenAnswer(returnsFirstArg());
        when(gameParticipantStateService.findOpponentUserId(gameId, attackerUserId)).thenReturn(defenderUserId);
        when(knockoutService.resolveKnockout(gameId, attackerUserId, attackerPokemon))
                .thenReturn(new KnockoutService.KnockoutResult(null, false));
        when(prizeService.takeSinglePrize(gameId, defenderUserId))
                .thenReturn(new PrizeService.PrizeResult(prizeCardId, 0));
        when(victoryConditionService.attackerWinsAfterKnockout(0, false)).thenReturn(true);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);

        StatusEffectService.ConfusionResolution resolution = service.resolveConfusionBeforeAttack(
                gameId,
                attackerUserId,
                attackerPokemon,
                3,
                7);

        assertThat(resolution.attackCanProceed()).isFalse();
        assertThat(resolution.winnerUserId()).isEqualTo(defenderUserId);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getWinnerPlayerId()).isEqualTo(defenderUserId);
        assertThat(resolution.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .containsExactly(
                        GameEventType.DAMAGE_APPLIED,
                        GameEventType.POKEMON_KNOCKED_OUT,
                        GameEventType.PRIZE_TAKEN,
                        GameEventType.GAME_FINISHED);
    }

    @Test
    void shouldApplyPoisonDamageAndClearParalysisDuringBetweenTurns() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemonInPlay(playerId, pokemonInPlayId, 0);
        SpecialCondition poisoned = savedCondition(activePokemon, SpecialConditionType.POISONED, 1);
        SpecialCondition paralyzed = savedCondition(activePokemon, SpecialConditionType.PARALYZED, 1);

        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerId), participant(opponentId)));
        when(pokemonInPlayStateService.findActivePokemon(gameId, playerId)).thenReturn(Optional.of(activePokemon));
        when(pokemonInPlayStateService.findActivePokemon(gameId, opponentId)).thenReturn(Optional.empty());
        when(specialConditionStateService.findByPokemonInPlayId(pokemonInPlayId))
                .thenReturn(List.of(poisoned, paralyzed), List.of(poisoned));
        when(pokemonInPlayStateService.save(any(PokemonInPlay.class))).thenAnswer(returnsFirstArg());
        Card activeCard = new Card();
        activeCard.setHp(60);
        when(cardService.getCardEntityById(activePokemon.getActiveCardInstance().getCardId())).thenReturn(activeCard);
        when(specialConditionStateService.activeConditionsByPlayer(gameId, List.of(playerId, opponentId), pokemonInPlayStateService))
                .thenReturn(Map.of(playerId, List.of(SpecialConditionType.POISONED), opponentId, List.of()));
        when(pokemonInPlayStateService.benchCountByPlayer(gameId, List.of(playerId, opponentId)))
                .thenReturn(Map.of(playerId, 0, opponentId, 0));

        StatusEffectService.BetweenTurnsResolution resolution = service.processBetweenTurns(
                gameId,
                playerId,
                3,
                8);

        verify(specialConditionStateService).delete(paralyzed);
        assertThat(activePokemon.getDamageCounters()).isEqualTo(1);
        assertThat(resolution.activeConditionsByPlayer().get(playerId)).containsExactly(SpecialConditionType.POISONED);
        assertThat(resolution.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .contains(GameEventType.DAMAGE_APPLIED, GameEventType.STATUS_APPLIED);
    }

    @Test
    void shouldApplyBurnDamageAndResolveAsleepDuringBetweenTurns() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemonInPlay(playerId, pokemonInPlayId, 0);
        SpecialCondition burned = savedCondition(activePokemon, SpecialConditionType.BURNED, 1);
        SpecialCondition asleep = savedCondition(activePokemon, SpecialConditionType.ASLEEP, 1);

        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerId), participant(opponentId)));
        when(pokemonInPlayStateService.findActivePokemon(gameId, playerId)).thenReturn(Optional.of(activePokemon));
        when(pokemonInPlayStateService.findActivePokemon(gameId, opponentId)).thenReturn(Optional.empty());
        when(specialConditionStateService.findByPokemonInPlayId(pokemonInPlayId)).thenReturn(List.of(burned, asleep));
        when(gameRandomService.flipCoin()).thenReturn(true, false);
        when(pokemonInPlayStateService.save(any(PokemonInPlay.class))).thenAnswer(returnsFirstArg());
        Card activeCard = new Card();
        activeCard.setHp(60);
        when(cardService.getCardEntityById(activePokemon.getActiveCardInstance().getCardId())).thenReturn(activeCard);
        when(specialConditionStateService.activeConditionsByPlayer(gameId, List.of(playerId, opponentId), pokemonInPlayStateService))
                .thenReturn(Map.of(playerId, List.of(SpecialConditionType.BURNED), opponentId, List.of()));
        when(pokemonInPlayStateService.benchCountByPlayer(gameId, List.of(playerId, opponentId)))
                .thenReturn(Map.of(playerId, 0, opponentId, 0));

        StatusEffectService.BetweenTurnsResolution resolution = service.processBetweenTurns(
                gameId,
                playerId,
                3,
                8);

        verify(specialConditionStateService).delete(asleep);
        assertThat(activePokemon.getDamageCounters()).isEqualTo(2);
        assertThat(resolution.gameFinished()).isFalse();
        assertThat(resolution.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .contains(GameEventType.DAMAGE_APPLIED, GameEventType.STATUS_APPLIED);
    }

    @Test
    void shouldFinishGameWhenBetweenTurnsDamageKnocksOutPokemon() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID defeatedPlayerId = UUID.randomUUID();
        UUID winningPlayerId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        UUID prizeCardId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemonInPlay(defeatedPlayerId, pokemonInPlayId, 5);
        SpecialCondition poisoned = savedCondition(activePokemon, SpecialConditionType.POISONED, 1);
        Card activeCard = new Card();
        activeCard.setHp(60);
        Game game = new Game();

        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(defeatedPlayerId), participant(winningPlayerId)));
        when(pokemonInPlayStateService.findActivePokemon(gameId, defeatedPlayerId)).thenReturn(Optional.of(activePokemon));
        when(specialConditionStateService.findByPokemonInPlayId(pokemonInPlayId)).thenReturn(List.of(poisoned));
        when(pokemonInPlayStateService.save(any(PokemonInPlay.class))).thenAnswer(returnsFirstArg());
        when(cardService.getCardEntityById(activePokemon.getActiveCardInstance().getCardId())).thenReturn(activeCard);
        when(gameParticipantStateService.findOpponentUserId(gameId, defeatedPlayerId)).thenReturn(winningPlayerId);
        when(knockoutService.resolveKnockout(gameId, defeatedPlayerId, activePokemon))
                .thenReturn(new KnockoutService.KnockoutResult(null, false));
        when(prizeService.takeSinglePrize(gameId, winningPlayerId))
                .thenReturn(new PrizeService.PrizeResult(prizeCardId, 0));
        when(victoryConditionService.attackerWinsAfterKnockout(0, false)).thenReturn(true);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(specialConditionStateService.activeConditionsByPlayer(gameId, List.of(defeatedPlayerId, winningPlayerId), pokemonInPlayStateService))
                .thenReturn(Map.of(defeatedPlayerId, List.of(SpecialConditionType.POISONED), winningPlayerId, List.of()));
        when(pokemonInPlayStateService.benchCountByPlayer(gameId, List.of(defeatedPlayerId, winningPlayerId)))
                .thenReturn(Map.of(defeatedPlayerId, 0, winningPlayerId, 0));

        StatusEffectService.BetweenTurnsResolution resolution = service.processBetweenTurns(
                gameId,
                defeatedPlayerId,
                3,
                8);

        assertThat(resolution.gameFinished()).isTrue();
        assertThat(resolution.winnerUserId()).isEqualTo(winningPlayerId);
        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(resolution.events()).extracting(ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto::eventType)
                .contains(GameEventType.POKEMON_KNOCKED_OUT, GameEventType.PRIZE_TAKEN, GameEventType.GAME_FINISHED);
    }

    @Test
    void shouldSnapshotConditionsAndBenchCountsForRequestedPlayers() {
        StatusEffectService service = service();
        UUID gameId = UUID.randomUUID();
        UUID firstPlayerId = UUID.randomUUID();
        UUID secondPlayerId = UUID.randomUUID();
        UUID missingPlayerId = UUID.randomUUID();

        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(firstPlayerId), participant(secondPlayerId)));
        when(specialConditionStateService.activeConditionsByPlayer(gameId, List.of(firstPlayerId, secondPlayerId), pokemonInPlayStateService))
                .thenReturn(Map.of(firstPlayerId, List.of(SpecialConditionType.POISONED)));
        when(pokemonInPlayStateService.benchCountByPlayer(gameId, List.of(firstPlayerId, secondPlayerId)))
                .thenReturn(Map.of(secondPlayerId, 2));

        Map<UUID, List<SpecialConditionType>> conditions = service.snapshotActiveConditions(
                gameId,
                List.of(firstPlayerId, missingPlayerId));
        Map<UUID, Integer> benchCounts = service.snapshotBenchCounts(
                gameId,
                List.of(secondPlayerId, missingPlayerId));

        assertThat(conditions).containsEntry(firstPlayerId, List.of(SpecialConditionType.POISONED));
        assertThat(conditions).containsEntry(missingPlayerId, List.of());
        assertThat(benchCounts).containsEntry(secondPlayerId, 2);
        assertThat(benchCounts).containsEntry(missingPlayerId, 0);
    }

    private PokemonInPlay pokemonInPlay(UUID ownerUserId, UUID pokemonInPlayId, int damageCounters) {
        GameCardInstance activeCard = new GameCardInstance();
        activeCard.setId(UUID.randomUUID());
        activeCard.setCardId(UUID.randomUUID());

        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(pokemonInPlayId);
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCard);
        pokemonInPlay.setSlotPosition(0);
        pokemonInPlay.setDamageCounters(damageCounters);
        pokemonInPlay.setEnteredPlayTurn(1);
        return pokemonInPlay;
    }

    private SpecialCondition savedCondition(PokemonInPlay pokemonInPlay, SpecialConditionType type, int appliedTurn) {
        SpecialCondition condition = new SpecialCondition();
        condition.setPokemonInPlay(pokemonInPlay);
        condition.setConditionType(type);
        condition.setAppliedTurn(appliedTurn);
        return condition;
    }

    private GameParticipant participant(UUID userId) {
        GameParticipant participant = new GameParticipant();
        participant.setUserId(userId);
        return participant;
    }
}
