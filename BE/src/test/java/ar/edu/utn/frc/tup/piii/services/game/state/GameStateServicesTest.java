package ar.edu.utn.frc.tup.piii.services.game.state;




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
import ar.edu.utn.frc.tup.piii.services.game.ability.*;
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
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardCardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogReadRepository;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateQueryServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateRestorerImpl;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateVisibilitySanitizer;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStateServicesTest {

    @Mock
    private GameParticipantStateService gameParticipantStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Mock
    private GameActionLogReadRepository gameActionLogReadRepository;

    @Mock
    private AbilityCatalogService abilityCatalogService;

    @Mock
    private AbilityUsageTracker abilityUsageTracker;

    @Mock
    private PassiveAbilityService passiveAbilityService;

    @Mock
    private CardService cardService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;

    @Test
    void shouldBuildVisibleStateUsingPorts() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID handCardId = UUID.randomUUID();
        UUID processedActionId = UUID.randomUUID();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerTwo);
        game.setPlayerWhoWentFirstId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                benchCard(playerOne),
                benchCard(playerOne),
                activeCard(playerTwo),
                handCard(playerOne, handCardId)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of(actionLog(processedActionId)));

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        assertThat(visibleState.playerIds()).containsExactly(playerOne, playerTwo);
        assertThat(visibleState.turn().playerWhoWentFirstId()).isEqualTo(playerOne);
        assertThat(GameStateTestFactory.benchCountByPlayer(visibleState)).containsEntry(playerOne, 2).containsEntry(playerTwo, 0);
        assertThat(GameStateTestFactory.cardsInHandByPlayer(visibleState).get(playerOne)).containsExactly(handCardId);
        assertThat(visibleState.board().zoneByCardReferenceId()).containsEntry(handCardId, CardZone.HAND);
        assertThat(visibleState.board().ownerByCardReferenceId()).containsEntry(handCardId, playerOne);
        assertThat(visibleState.actions().processedClientActionIds()).containsExactly(processedActionId);
    }

    @Test
    void shouldHideOpponentHandWhenBuildingViewerVisibleState() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID viewerHandCardId = UUID.randomUUID();
        UUID opponentHandCardId = UUID.randomUUID();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(viewerUserId), participant(opponentUserId)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                handCard(viewerUserId, viewerHandCardId),
                handCard(opponentUserId, opponentHandCardId)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game, viewerUserId);

        assertThat(GameStateTestFactory.cardsInHandByPlayer(visibleState)).containsEntry(viewerUserId, List.of(viewerHandCardId));
        assertThat(GameStateTestFactory.cardsInHandByPlayer(visibleState)).containsEntry(opponentUserId, List.of());
        assertThat(visibleState.board().zoneByCardReferenceId()).containsEntry(viewerHandCardId, CardZone.HAND);
        assertThat(visibleState.board().zoneByCardReferenceId()).doesNotContainKey(opponentHandCardId);
        assertThat(visibleState.board().ownerByCardReferenceId()).containsEntry(viewerHandCardId, viewerUserId);
        assertThat(visibleState.board().ownerByCardReferenceId()).doesNotContainKey(opponentHandCardId);
    }

    @Test
    void shouldBuildVisualBoardSnapshotFromGameState() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID activeCardInstanceId = UUID.randomUUID();
        UUID attackId = UUID.randomUUID();
        GameCardInstance activeCardInstance = cardInstance(playerOne, activeCardId, CardZone.ACTIVE, 0, false);
        activeCardInstance.setId(activeCardInstanceId);
        PokemonInPlay activePokemon = pokemonInPlay(playerOne, activeCardInstance, 0);
        Card card = pokemonCard(activeCardId, "Pikachu", attack(attackId, "Thunder Shock"));
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.ATTACK);
        game.setTurnNumber(4);
        game.setStateVersion(7);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(activeCardInstance));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(activePokemon));
        when(specialConditionStateService.activeConditionTypes(activePokemon.getId())).thenReturn(List.of(SpecialConditionType.POISONED));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(cardService.getCardEntityById(activeCardId)).thenReturn(card);
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        assertThat(visibleState.boardPlayers()).containsKeys(playerOne, playerTwo);
        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().pokemonInPlayId()).isEqualTo(activePokemon.getId());
        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().activeCard().cardId()).isEqualTo(activeCardId);
        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().activeCard().name()).isEqualTo("Pikachu");
        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().specialConditions()).containsExactly(SpecialConditionType.POISONED);
        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().attacks())
                .extracting("attackId")
                .containsExactly(attackId);
        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().attacks().get(0).available()).isTrue();
    }

    @Test
    void shouldExposeOwnBenchTargetRequirementForHealAttacks() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID benchCardId = UUID.randomUUID();
        UUID massageAttackId = UUID.randomUUID();
        GameCardInstance activeCardInstance = cardInstance(playerOne, activeCardId, CardZone.ACTIVE, 0, false);
        GameCardInstance benchCardInstance = cardInstance(playerOne, benchCardId, CardZone.BENCH, 0, false);
        PokemonInPlay activePokemon = pokemonInPlay(playerOne, activeCardInstance, 0);
        PokemonInPlay benchPokemon = pokemonInPlay(playerOne, benchCardInstance, 1);
        Attack massage = attack(massageAttackId, "Massage");
        massage.setAttackOrder(0);
        Card mrMime = pokemonCard(activeCardId, "Mr. Mime", massage);
        mrMime.setExternalId("xy1-91");
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(4);
        game.setStateVersion(7);
        game.setActivePlayerId(playerOne);
        activePokemon.setGame(game);
        benchPokemon.setGame(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(activeCardInstance, benchCardInstance));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(activePokemon, benchPokemon));
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, playerOne))
                .thenReturn(List.of(activePokemon, benchPokemon));
        when(specialConditionStateService.activeConditionTypes(activePokemon.getId())).thenReturn(List.of());
        when(specialConditionStateService.activeConditionTypes(benchPokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(benchPokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(benchPokemon.getId())).thenReturn(List.of());
        when(cardService.getCardEntityById(activeCardId)).thenReturn(mrMime);
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        var playerOneBoard = visibleState.board().view().players().stream()
                .filter(board -> playerOne.equals(board.playerId()))
                .findFirst()
                .orElseThrow();
        var massageDto = playerOneBoard.activePokemon().attacks().stream()
                .filter(dto -> massageAttackId.equals(dto.attackId()))
                .findFirst()
                .orElseThrow();
        assertThat(massageDto.requiresTarget()).isTrue();
        assertThat(massageDto.validTargetPokemonInPlayIds()).containsExactly(benchPokemon.getId());

        var legacyMassageDto = visibleState.boardPlayers().get(playerOne).activePokemon().attacks().stream()
                .filter(dto -> massageAttackId.equals(dto.attackId()))
                .findFirst()
                .orElseThrow();
        assertThat(legacyMassageDto.requiresTarget()).isTrue();
        assertThat(legacyMassageDto.validTargetPokemonInPlayIds()).containsExactly(benchPokemon.getId());
    }

    @Test
    void shouldExposeOpponentBenchTargetRequirementForForcedSwitchAttacks() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID opponentActiveCardId = UUID.randomUUID();
        UUID opponentBenchCardId = UUID.randomUUID();
        UUID luringGlowAttackId = UUID.randomUUID();
        GameCardInstance activeCardInstance = cardInstance(playerOne, activeCardId, CardZone.ACTIVE, 0, false);
        GameCardInstance opponentActiveCardInstance = cardInstance(playerTwo, opponentActiveCardId, CardZone.ACTIVE, 0, false);
        GameCardInstance opponentBenchCardInstance = cardInstance(playerTwo, opponentBenchCardId, CardZone.BENCH, 0, false);
        PokemonInPlay activePokemon = pokemonInPlay(playerOne, activeCardInstance, 0);
        PokemonInPlay opponentActivePokemon = pokemonInPlay(playerTwo, opponentActiveCardInstance, 0);
        PokemonInPlay opponentBenchPokemon = pokemonInPlay(playerTwo, opponentBenchCardInstance, 1);
        Attack luringGlow = attack(luringGlowAttackId, "Luring Glow");
        luringGlow.setAttackOrder(0);
        Card volbeat = pokemonCard(activeCardId, "Volbeat", luringGlow);
        volbeat.setExternalId("xy1-8");
        Card activeOpponent = pokemonCard(opponentActiveCardId, "Chansey", attack(UUID.randomUUID(), "Pound"));
        Card benchOpponent = pokemonCard(opponentBenchCardId, "Skitty", attack(UUID.randomUUID(), "Tackle"));
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.ATTACK);
        game.setTurnNumber(4);
        game.setStateVersion(7);
        game.setActivePlayerId(playerOne);
        activePokemon.setGame(game);
        opponentActivePokemon.setGame(game);
        opponentBenchPokemon.setGame(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameParticipantStateService.findPlayerIds(gameId)).thenReturn(List.of(playerOne, playerTwo));
        when(gameCardInstanceStateService.findByGameId(gameId))
                .thenReturn(List.of(activeCardInstance, opponentActiveCardInstance, opponentBenchCardInstance));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId))
                .thenReturn(List.of(activePokemon, opponentActivePokemon, opponentBenchPokemon));
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, playerTwo))
                .thenReturn(List.of(opponentActivePokemon, opponentBenchPokemon));
        when(specialConditionStateService.activeConditionTypes(activePokemon.getId())).thenReturn(List.of());
        when(specialConditionStateService.activeConditionTypes(opponentActivePokemon.getId())).thenReturn(List.of());
        when(specialConditionStateService.activeConditionTypes(opponentBenchPokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(opponentActivePokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(opponentBenchPokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(opponentActivePokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(opponentBenchPokemon.getId())).thenReturn(List.of());
        when(cardService.getCardEntityById(activeCardId)).thenReturn(volbeat);
        when(cardService.getCardEntityById(opponentActiveCardId)).thenReturn(activeOpponent);
        when(cardService.getCardEntityById(opponentBenchCardId)).thenReturn(benchOpponent);
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        var playerOneBoard = visibleState.board().view().players().stream()
                .filter(board -> playerOne.equals(board.playerId()))
                .findFirst()
                .orElseThrow();
        var luringGlowDto = playerOneBoard.activePokemon().attacks().stream()
                .filter(dto -> luringGlowAttackId.equals(dto.attackId()))
                .findFirst()
                .orElseThrow();
        assertThat(luringGlowDto.requiresTarget()).isTrue();
        assertThat(luringGlowDto.targetsOwnPokemon()).isFalse();
        assertThat(luringGlowDto.validTargetPokemonInPlayIds()).containsExactly(opponentBenchPokemon.getId());

        var legacyLuringGlowDto = visibleState.boardPlayers().get(playerOne).activePokemon().attacks().stream()
                .filter(dto -> luringGlowAttackId.equals(dto.attackId()))
                .findFirst()
                .orElseThrow();
        assertThat(legacyLuringGlowDto.requiresTarget()).isTrue();
        assertThat(legacyLuringGlowDto.targetsOwnPokemon()).isFalse();
        assertThat(legacyLuringGlowDto.validTargetPokemonInPlayIds()).containsExactly(opponentBenchPokemon.getId());
    }

    @Test
    void shouldNotRequireTargetForAutomaticBenchSweepAttacks() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID benchCardId = UUID.randomUUID();
        UUID earthquakeAttackId = UUID.randomUUID();
        GameCardInstance activeCardInstance = cardInstance(playerOne, activeCardId, CardZone.ACTIVE, 0, false);
        GameCardInstance benchCardInstance = cardInstance(playerOne, benchCardId, CardZone.BENCH, 0, false);
        PokemonInPlay activePokemon = pokemonInPlay(playerOne, activeCardInstance, 0);
        PokemonInPlay benchPokemon = pokemonInPlay(playerOne, benchCardInstance, 1);
        Attack earthquake = attack(earthquakeAttackId, "Earthquake");
        earthquake.setAttackOrder(0);
        Card dugtrio = pokemonCard(activeCardId, "Dugtrio", earthquake);
        dugtrio.setExternalId("xy1-59");
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(4);
        game.setStateVersion(7);
        game.setActivePlayerId(playerOne);
        activePokemon.setGame(game);
        benchPokemon.setGame(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameParticipantStateService.findPlayerIds(gameId)).thenReturn(List.of(playerOne, playerTwo));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(activeCardInstance, benchCardInstance));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(activePokemon, benchPokemon));
        when(pokemonInPlayStateService.findActivePokemon(gameId, playerTwo)).thenReturn(Optional.empty());
        when(specialConditionStateService.activeConditionTypes(activePokemon.getId())).thenReturn(List.of());
        when(specialConditionStateService.activeConditionTypes(benchPokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(benchPokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(benchPokemon.getId())).thenReturn(List.of());
        when(cardService.getCardEntityById(activeCardId)).thenReturn(dugtrio);
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        var playerOneBoard = visibleState.board().view().players().stream()
                .filter(board -> playerOne.equals(board.playerId()))
                .findFirst()
                .orElseThrow();
        var earthquakeDto = playerOneBoard.activePokemon().attacks().stream()
                .filter(dto -> earthquakeAttackId.equals(dto.attackId()))
                .findFirst()
                .orElseThrow();
        assertThat(earthquakeDto.requiresTarget()).isFalse();
        assertThat(earthquakeDto.validTargetPokemonInPlayIds()).isEmpty();

        var legacyEarthquakeDto = visibleState.boardPlayers().get(playerOne).activePokemon().attacks().stream()
                .filter(dto -> earthquakeAttackId.equals(dto.attackId()))
                .findFirst()
                .orElseThrow();
        assertThat(legacyEarthquakeDto.requiresTarget()).isFalse();
        assertThat(legacyEarthquakeDto.validTargetPokemonInPlayIds()).isEmpty();
    }

    @Test
    void shouldSanitizeVisualBoardSnapshotForViewer() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID viewerHandCardId = UUID.randomUUID();
        UUID opponentHandCardId = UUID.randomUUID();
        UUID viewerDeckCardId = UUID.randomUUID();
        UUID viewerPrizeCardId = UUID.randomUUID();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(viewerUserId), participant(opponentUserId)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                cardInstance(viewerUserId, viewerHandCardId, CardZone.HAND, 0, false),
                cardInstance(opponentUserId, opponentHandCardId, CardZone.HAND, 0, false),
                cardInstance(viewerUserId, viewerDeckCardId, CardZone.DECK, 1, false),
                cardInstance(viewerUserId, viewerPrizeCardId, CardZone.PRIZE, 1, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(cardService.getCardEntityById(viewerHandCardId)).thenReturn(pokemonCard(viewerHandCardId, "Charmander"));
        when(cardService.getCardEntityById(opponentHandCardId)).thenReturn(pokemonCard(opponentHandCardId, "Squirtle"));
        when(cardService.getCardEntityById(viewerDeckCardId)).thenReturn(pokemonCard(viewerDeckCardId, "Bulbasaur"));
        when(cardService.getCardEntityById(viewerPrizeCardId)).thenReturn(pokemonCard(viewerPrizeCardId, "Mew"));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game, viewerUserId);

        assertThat(visibleState.boardPlayers().get(viewerUserId).hand().cards().get(0).cardId()).isEqualTo(viewerHandCardId);
        assertThat(visibleState.boardPlayers().get(opponentUserId).hand().count()).isEqualTo(1);
        assertThat(visibleState.boardPlayers().get(opponentUserId).hand().cards().get(0).cardId()).isNull();
        assertThat(visibleState.boardPlayers().get(opponentUserId).hand().cards().get(0).cardInstanceId()).isNull();
        assertThat(visibleState.boardPlayers().get(viewerUserId).deck().count()).isEqualTo(1);
        assertThat(visibleState.boardPlayers().get(viewerUserId).deck().cards().get(0).cardId()).isNull();
        assertThat(visibleState.boardPlayers().get(viewerUserId).prizes().count()).isEqualTo(1);
        assertThat(visibleState.boardPlayers().get(viewerUserId).prizes().cards().get(0).cardInstanceId()).isNull();
    }

    @Test
    void shouldMarkStage1AndStage2HandCardsAsPlayableThroughBoardPlayers() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID stage1CardId = UUID.randomUUID();
        UUID stage2CardId = UUID.randomUUID();
        UUID basicCardId = UUID.randomUUID();
        UUID basicCardInstanceId = UUID.randomUUID();
        UUID basicPokemonInPlayId = UUID.randomUUID();
        UUID stage1InPlayCardId = UUID.randomUUID();
        UUID stage1InPlayCardInstanceId = UUID.randomUUID();
        UUID stage1PokemonInPlayId = UUID.randomUUID();
        GameCardInstance basicCardInstance = cardInstance(playerOne, basicCardId, CardZone.ACTIVE, 0, false);
        basicCardInstance.setId(basicCardInstanceId);
        GameCardInstance stage1InPlayCardInstance = cardInstance(playerOne, stage1InPlayCardId, CardZone.BENCH, 1, false);
        stage1InPlayCardInstance.setId(stage1InPlayCardInstanceId);
        PokemonInPlay basicPokemon = pokemonInPlay(playerOne, basicCardInstance, 0);
        basicPokemon.setId(basicPokemonInPlayId);
        basicPokemon.setEnteredPlayTurn(1);
        PokemonInPlay stage1InPlayPokemon = pokemonInPlay(playerOne, stage1InPlayCardInstance, 1);
        stage1InPlayPokemon.setId(stage1PokemonInPlayId);
        stage1InPlayPokemon.setEnteredPlayTurn(2);
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                basicCardInstance,
                stage1InPlayCardInstance,
                cardInstance(playerOne, stage1CardId, CardZone.HAND, 0, false),
                cardInstance(playerOne, stage2CardId, CardZone.HAND, 1, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(basicPokemon, stage1InPlayPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(basicPokemonInPlayId))
                .thenReturn(Optional.of(evolutionStack(basicPokemon, basicCardInstance, 0, 1)));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(stage1PokemonInPlayId))
                .thenReturn(Optional.of(evolutionStack(stage1InPlayPokemon, stage1InPlayCardInstance, 1, 2)));
        when(cardService.getCardEntityById(stage1CardId)).thenReturn(stage1CardWithEvolvesFrom(stage1CardId, "Ivysaur", "Charmander"));
        when(cardService.getCardEntityById(stage2CardId)).thenReturn(stage2CardWithEvolvesFrom(stage2CardId, "Venusaur", "Ivysaur"));
        when(cardService.getCardEntityById(basicCardId)).thenReturn(pokemonCard(basicCardId, "Charmander"));
        when(cardService.getCardEntityById(stage1InPlayCardId)).thenReturn(stage1CardWithEvolvesFrom(stage1InPlayCardId, "Ivysaur", "Charmander"));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        List<BoardCardDto> handCards = visibleState.boardPlayers().get(playerOne).hand().cards();
        assertThat(handCards).hasSize(2);
        BoardCardDto stage1Dto = findByCardId(handCards, stage1CardId);
        BoardCardDto stage2Dto = findByCardId(handCards, stage2CardId);
        assertThat(stage1Dto.playable()).isTrue();
        assertThat(stage1Dto.suggestedAction()).isEqualTo(GameActionType.EVOLVE_POKEMON);
        assertThat(stage1Dto.disabledReason()).isNull();
        assertThat(stage1Dto.validTargetPokemonInPlayIds()).containsExactly(basicPokemonInPlayId);
        assertThat(stage2Dto.playable()).isTrue();
        assertThat(stage2Dto.suggestedAction()).isEqualTo(GameActionType.EVOLVE_POKEMON);
        assertThat(stage2Dto.disabledReason()).isNull();
        assertThat(stage2Dto.validTargetPokemonInPlayIds()).containsExactly(stage1PokemonInPlayId);
    }

    @Test
    void shouldKeepEvolutionHandCardUnplayableWhenNoValidTargetInPlay() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID stage1CardId = UUID.randomUUID();
        UUID wrongBasicCardId = UUID.randomUUID();
        UUID wrongBasicCardInstanceId = UUID.randomUUID();
        UUID wrongPokemonInPlayId = UUID.randomUUID();
        GameCardInstance wrongBasicCardInstance = cardInstance(playerOne, wrongBasicCardId, CardZone.ACTIVE, 0, false);
        wrongBasicCardInstance.setId(wrongBasicCardInstanceId);
        PokemonInPlay wrongPokemon = pokemonInPlay(playerOne, wrongBasicCardInstance, 0);
        wrongPokemon.setId(wrongPokemonInPlayId);
        wrongPokemon.setEnteredPlayTurn(1);
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                wrongBasicCardInstance,
                cardInstance(playerOne, stage1CardId, CardZone.HAND, 0, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(wrongPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(wrongPokemonInPlayId))
                .thenReturn(Optional.of(evolutionStack(wrongPokemon, wrongBasicCardInstance, 0, 1)));
        when(cardService.getCardEntityById(stage1CardId)).thenReturn(stage1CardWithEvolvesFrom(stage1CardId, "Ivysaur", "Charmander"));
        when(cardService.getCardEntityById(wrongBasicCardId)).thenReturn(pokemonCard(wrongBasicCardId, "Squirtle"));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        List<BoardCardDto> handCards = visibleState.boardPlayers().get(playerOne).hand().cards();
        assertThat(handCards).hasSize(1);
        BoardCardDto stage1Dto = findByCardId(handCards, stage1CardId);
        assertThat(stage1Dto.playable()).isFalse();
        assertThat(stage1Dto.suggestedAction()).isNull();
        assertThat(stage1Dto.disabledReason()).isNotNull();
        assertThat(stage1Dto.validTargetPokemonInPlayIds()).isEmpty();
    }

    @Test
    void shouldKeepEvolutionHandCardUnplayableWhenStageProgressionMismatch() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID stage2CardId = UUID.randomUUID();
        UUID basicCardId = UUID.randomUUID();
        UUID basicCardInstanceId = UUID.randomUUID();
        UUID basicPokemonInPlayId = UUID.randomUUID();
        GameCardInstance basicCardInstance = cardInstance(playerOne, basicCardId, CardZone.ACTIVE, 0, false);
        basicCardInstance.setId(basicCardInstanceId);
        PokemonInPlay basicPokemon = pokemonInPlay(playerOne, basicCardInstance, 0);
        basicPokemon.setId(basicPokemonInPlayId);
        basicPokemon.setEnteredPlayTurn(1);
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                basicCardInstance,
                cardInstance(playerOne, stage2CardId, CardZone.HAND, 0, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(basicPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(basicPokemonInPlayId))
                .thenReturn(Optional.of(evolutionStack(basicPokemon, basicCardInstance, 0, 1)));
        when(cardService.getCardEntityById(stage2CardId)).thenReturn(stage2CardWithEvolvesFrom(stage2CardId, "Venusaur", "Charmander"));
        when(cardService.getCardEntityById(basicCardId)).thenReturn(pokemonCard(basicCardId, "Charmander"));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        List<BoardCardDto> handCards = visibleState.boardPlayers().get(playerOne).hand().cards();
        BoardCardDto stage2Dto = findByCardId(handCards, stage2CardId);
        assertThat(stage2Dto.playable()).isFalse();
        assertThat(stage2Dto.suggestedAction()).isNull();
        assertThat(stage2Dto.disabledReason()).isNotNull();
        assertThat(stage2Dto.validTargetPokemonInPlayIds()).isEmpty();
    }

    @Test
    void shouldKeepEvolutionHandCardUnplayableWhenTargetEvolvedThisTurn() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID stage2CardId = UUID.randomUUID();
        UUID stage1InPlayCardId = UUID.randomUUID();
        UUID stage1InPlayCardInstanceId = UUID.randomUUID();
        UUID stage1PokemonInPlayId = UUID.randomUUID();
        GameCardInstance stage1InPlayCardInstance = cardInstance(playerOne, stage1InPlayCardId, CardZone.ACTIVE, 0, false);
        stage1InPlayCardInstance.setId(stage1InPlayCardInstanceId);
        PokemonInPlay stage1InPlayPokemon = pokemonInPlay(playerOne, stage1InPlayCardInstance, 0);
        stage1InPlayPokemon.setId(stage1PokemonInPlayId);
        stage1InPlayPokemon.setEnteredPlayTurn(game.getTurnNumber() == null ? 3 : game.getTurnNumber());
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                stage1InPlayCardInstance,
                cardInstance(playerOne, stage2CardId, CardZone.HAND, 0, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(stage1InPlayPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(stage1PokemonInPlayId))
                .thenReturn(Optional.of(evolutionStack(stage1InPlayPokemon, stage1InPlayCardInstance, 1, 3)));
        when(cardService.getCardEntityById(stage2CardId)).thenReturn(stage2CardWithEvolvesFrom(stage2CardId, "Venusaur", "Ivysaur"));
        when(cardService.getCardEntityById(stage1InPlayCardId)).thenReturn(stage1CardWithEvolvesFrom(stage1InPlayCardId, "Ivysaur", "Charmander"));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        List<BoardCardDto> handCards = visibleState.boardPlayers().get(playerOne).hand().cards();
        BoardCardDto stage2Dto = findByCardId(handCards, stage2CardId);
        assertThat(stage2Dto.playable()).isFalse();
        assertThat(stage2Dto.suggestedAction()).isNull();
        assertThat(stage2Dto.disabledReason()).isNotNull();
        assertThat(stage2Dto.validTargetPokemonInPlayIds()).isEmpty();
    }

    @Test
    void shouldKeepEvolutionHandCardsUnplayableOutsideMainPhase() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID stage1CardId = UUID.randomUUID();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.DRAW);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                cardInstance(playerOne, stage1CardId, CardZone.HAND, 0, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(cardService.getCardEntityById(stage1CardId)).thenReturn(stage1Card(stage1CardId, "Ivysaur"));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        List<BoardCardDto> handCards = visibleState.boardPlayers().get(playerOne).hand().cards();
        assertThat(handCards).hasSize(1);
        BoardCardDto stage1Dto = findByCardId(handCards, stage1CardId);
        assertThat(stage1Dto.playable()).isFalse();
        assertThat(stage1Dto.suggestedAction()).isNull();
        assertThat(stage1Dto.disabledReason()).isNotNull();
    }

    @Test
    void shouldNotExposePlayabilityForOpponentHandCards() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID opponentStage1CardId = UUID.randomUUID();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(viewerUserId);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(viewerUserId), participant(opponentUserId)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                cardInstance(opponentUserId, opponentStage1CardId, CardZone.HAND, 0, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game, viewerUserId);

        List<BoardCardDto> opponentHand = visibleState.boardPlayers().get(opponentUserId).hand().cards();
        assertThat(opponentHand).hasSize(1);
        BoardCardDto hidden = opponentHand.get(0);
        assertThat(hidden.cardId()).isNull();
        assertThat(hidden.playable()).isFalse();
        assertThat(hidden.suggestedAction()).isNull();
        assertThat(hidden.disabledReason()).isNull();
        assertThat(hidden.validTargetPokemonInPlayIds()).isEmpty();
    }

    @Test
    void shouldKeepBasicAndEnergyHandCardsPlayable() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID basicCardId = UUID.randomUUID();
        UUID energyCardId = UUID.randomUUID();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(3);
        game.setStateVersion(5);
        game.setActivePlayerId(playerOne);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(
                cardInstance(playerOne, basicCardId, CardZone.HAND, 0, false),
                cardInstance(playerOne, energyCardId, CardZone.HAND, 1, false)));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(cardService.getCardEntityById(basicCardId)).thenReturn(pokemonCard(basicCardId, "Charmander"));
        when(cardService.getCardEntityById(energyCardId)).thenReturn(basicEnergyCard(energyCardId));
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        List<BoardCardDto> handCards = visibleState.boardPlayers().get(playerOne).hand().cards();
        BoardCardDto basicDto = findByCardId(handCards, basicCardId);
        BoardCardDto energyDto = findByCardId(handCards, energyCardId);
        assertThat(basicDto.playable()).isTrue();
        assertThat(basicDto.suggestedAction()).isEqualTo(GameActionType.PLAY_BASIC_POKEMON);
        assertThat(energyDto.playable()).isFalse();
        assertThat(energyDto.suggestedAction()).isNull();
    }

    @Test
    void shouldExposeTormentBlockedAttackAsDisabledForTheAffectedTurn() {
        GameStateQueryService gameStateQueryService = gameStateQueryService();
        Game game = new Game();
        UUID gameId = UUID.randomUUID();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID blockedAttackId = UUID.randomUUID();
        UUID otherAttackId = UUID.randomUUID();
        GameCardInstance activeCardInstance = cardInstance(playerOne, activeCardId, CardZone.ACTIVE, 0, false);
        PokemonInPlay activePokemon = pokemonInPlay(playerOne, activeCardInstance, 0);
        Attack blockedAttack = attack(blockedAttackId, "Blocked Attack");
        blockedAttack.setAttackOrder(0);
        Attack otherAttack = attack(otherAttackId, "Other Attack");
        otherAttack.setAttackOrder(1);
        Card card = pokemonCard(activeCardId, "Simisage", blockedAttack, otherAttack);
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.MAIN);
        game.setTurnNumber(4);
        game.setStateVersion(7);
        game.setActivePlayerId(playerOne);
        activePokemon.setGame(game);
        activePokemon.setBlockedAttackTurn(4);
        activePokemon.setBlockedAttackOrder(0);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(List.of(participant(playerOne), participant(playerTwo)));
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(activeCardInstance));
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of(activePokemon));
        when(specialConditionStateService.activeConditionTypes(activePokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(activePokemon.getId())).thenReturn(List.of());
        when(cardService.getCardEntityById(activeCardId)).thenReturn(card);
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of());

        GameStateDto visibleState = gameStateQueryService.buildVisibleState(game);

        assertThat(visibleState.boardPlayers().get(playerOne).activePokemon().attacks())
                .extracting("attackId", "available", "disabledReason")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(blockedAttackId, false, "Este ataque esta bloqueado este turno."),
                        org.assertj.core.groups.Tuple.tuple(otherAttackId, true, null));
    }

    @Test
    void shouldRestoreMutableFieldsFromSnapshot() {
        GameStateRestorer gameStateRestorer = new GameStateRestorerImpl();
        Game game = new Game();
        UUID activePlayerId = UUID.randomUUID();
        GameStateDto snapshot = GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.PAUSED,
                TurnPhase.ATTACK,
                8,
                13,
                activePlayerId,
                List.of(activePlayerId),
                List.of(),
                java.time.Instant.parse("2026-05-23T20:00:00Z"));

        gameStateRestorer.restoreFromSnapshot(game, snapshot);

        assertThat(game.getStatus()).isEqualTo(GameStatus.PAUSED);
        assertThat(game.getCurrentPhase()).isEqualTo(TurnPhase.ATTACK);
        assertThat(game.getTurnNumber()).isEqualTo(8);
        assertThat(game.getActivePlayerId()).isEqualTo(activePlayerId);
    }

    private GameParticipant participant(UUID userId) {
        GameParticipant participant = new GameParticipant();
        participant.setUserId(userId);
        return participant;
    }

    private GameStateQueryService gameStateQueryService() {
        return new GameStateQueryServiceImpl(
                gameParticipantStateService,
                gameCardInstanceStateService,
                pokemonInPlayStateService,
                specialConditionStateService,
                gameActionLogReadRepository,
                cardService,
                CardTranslationService.empty(),
                pokemonAttachedCardStateService,
                pokemonEvolutionStackStateService,
                new GameStateVisibilitySanitizer(),
                new AttackEnergyRequirementServiceImpl(pokemonAttachedCardStateService, cardService),
                new AttackEffectDefinitionReader(new ObjectMapper()),
                abilityCatalogService,
                abilityUsageTracker,
                passiveAbilityService);
    }

    private GameCardInstance benchCard(UUID ownerUserId) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setZone(CardZone.BENCH);
        return cardInstance;
    }

    private GameCardInstance activeCard(UUID ownerUserId) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setZone(CardZone.ACTIVE);
        return cardInstance;
    }

    private GameCardInstance handCard(UUID ownerUserId, UUID cardId) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(CardZone.HAND);
        return cardInstance;
    }

    private GameCardInstance cardInstance(
            UUID ownerUserId,
            UUID cardId,
            CardZone zone,
            Integer zonePosition,
            boolean faceDown) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(zonePosition);
        cardInstance.setFaceDown(faceDown);
        return cardInstance;
    }

    private PokemonInPlay pokemonInPlay(UUID ownerUserId, GameCardInstance activeCardInstance, Integer slotPosition) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(UUID.randomUUID());
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCardInstance);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setDamageCounters(20);
        pokemonInPlay.setEnteredPlayTurn(2);
        return pokemonInPlay;
    }

    private Card pokemonCard(UUID cardId, String name, Attack... attacks) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("xy1-" + cardId.toString().substring(0, 8));
        card.setSetCode("xy1");
        card.setNumber("1");
        card.setName(name);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setSubtype("Basic");
        for (Attack attack : attacks) {
            card.addAttack(attack);
        }
        return card;
    }

    private Attack attack(UUID attackId, String name) {
        Attack attack = new Attack();
        attack.setId(attackId);
        attack.setName(name);
        attack.setDamageText("30");
        attack.setBaseDamage(30);
        attack.setEffectText("Flip a coin.");
        attack.setAttackOrder(1);
        return attack;
    }

    private GameActionLog actionLog(UUID clientActionId) {
        GameActionLog actionLog = new GameActionLog();
        actionLog.setClientActionId(clientActionId);
        return actionLog;
    }

    private Card stage1Card(UUID cardId, String name) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("xy1-" + cardId.toString().substring(0, 8));
        card.setSetCode("xy1");
        card.setNumber("1");
        card.setName(name);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.STAGE_1_POKEMON);
        return card;
    }

    private Card stage2Card(UUID cardId, String name) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("xy1-" + cardId.toString().substring(0, 8));
        card.setSetCode("xy1");
        card.setNumber("2");
        card.setName(name);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.STAGE_2_POKEMON);
        return card;
    }

    private Card stage1CardWithEvolvesFrom(UUID cardId, String name, String evolvesFrom) {
        Card card = stage1Card(cardId, name);
        card.setEvolvesFrom(evolvesFrom);
        return card;
    }

    private Card stage2CardWithEvolvesFrom(UUID cardId, String name, String evolvesFrom) {
        Card card = stage2Card(cardId, name);
        card.setEvolvesFrom(evolvesFrom);
        return card;
    }

    private PokemonEvolutionStack evolutionStack(
            PokemonInPlay pokemon,
            GameCardInstance cardInstance,
            int stackOrder,
            int createdAtTurn) {
        PokemonEvolutionStack stack = new PokemonEvolutionStack();
        stack.setId(UUID.randomUUID());
        stack.setPokemonInPlay(pokemon);
        stack.setGameCardInstance(cardInstance);
        stack.setStackOrder(stackOrder);
        stack.setCreatedAtTurn(createdAtTurn);
        return stack;
    }

    private Card basicEnergyCard(UUID cardId) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("xy1-" + cardId.toString().substring(0, 8));
        card.setSetCode("xy1");
        card.setNumber("100");
        card.setName("Fire Energy");
        card.setSupertype(CardSupertype.ENERGY);
        card.setCategory(CardCategory.BASIC_ENERGY);
        return card;
    }

    private BoardCardDto findByCardId(List<BoardCardDto> cards, UUID cardId) {
        return cards.stream()
                .filter(card -> cardId.equals(card.cardId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected hand card with id " + cardId));
    }
}
