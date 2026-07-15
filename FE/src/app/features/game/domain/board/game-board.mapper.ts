import { CardZone } from '../../../../core/models/enums/card/card-zone.enum';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameEventType } from '../../../../core/models/enums/game/game-event-type.enum';
import { GameStatus } from '../../../../core/models/enums/game/game-status.enum';
import { TurnPhase } from '../../../../core/models/enums/game/turn-phase.enum';
import { GameEvent } from '../../../../core/models/interfaces/game/game-event.interface';
import { GameResolutionType } from '../../../../core/models/interfaces/game/game-resolution.interface';
import {
  readMulliganExtraCardsGrantedPayload,
  readMulliganFlowCompletedPayload,
  readMulliganHandRevealedPayload,
  readMulliganHandValidatedPayload,
  readMulliganNewHandDrawnPayload,
  readMulliganNoticeAcknowledgedPayload,
  readMulliganRequiredPayload,
  readMulliganSequenceCompletedPayload,
  readMulliganStepPayload
} from '../../../../core/models/interfaces/game/mulligan-event.interface';
import {
  GameBoardAbility,
  GameBoardAttack,
  GameBoardCard,
  GameBoardPlayerState,
  GameBoardPokemon,
  GameBoardZoneSummary,
  GameSnapshot,
  GameSnapshotPlayerState
} from '../../../../core/models/interfaces/game/game-snapshot.interface';
import {
  VisibleBoardDto,
  VisibleBoardActionHintsDto,
  VisibleCardDto,
  VisiblePlayerBoardDto,
  VisiblePokemonDto,
  VisibleZoneDto
} from '../../../../core/models/interfaces/game/visible-board.interface';
import { BoardActionViewModel } from '../actions/board-action-view-model.interface';
import { BoardEphemeralUiState } from '../ui-state/board-ephemeral-ui-state.interface';
import { BoardCardViewModel } from '../cards/board-card-view-model.interface';
import { BoardGameViewModel } from './board-game-view-model.interface';
import {
  BoardCardZoneViewModel,
  BoardPlayerViewModel,
  BoardSlotViewModel
} from './board-player-view-model.interface';
import {
  BoardPokemonAbilityViewModel,
  BoardPokemonAttackViewModel,
  BoardPokemonViewModel
} from '../cards/board-pokemon-view-model.interface';
import { BoardResolutionViewModel } from './board-resolution-view-model.interface';
import { cardCanBeSelectedForAction } from '../interaction/game-interaction.helpers';
import { isCurrentPlayerTurn } from './game-state.helpers';
import { UiTranslateFn } from '../../../../core/services/language.service';

const BENCH_SLOT_COUNT = 5;
const INITIAL_HAND_SIZE = 7;

const STATUS_LABEL_KEYS: Record<GameStatus, string> = {
  [GameStatus.Waiting]: 'GAME.STATUS.WAITING',
  [GameStatus.Setup]: 'GAME.STATUS.SETUP',
  [GameStatus.Active]: 'GAME.STATUS.ACTIVE',
  [GameStatus.Paused]: 'GAME.STATUS.PAUSED',
  [GameStatus.Finished]: 'GAME.STATUS.FINISHED',
  [GameStatus.Cancelled]: 'GAME.STATUS.CANCELLED'
};

const PHASE_LABEL_KEYS: Record<TurnPhase, string> = {
  [TurnPhase.Draw]: 'GAME.PHASE.DRAW',
  [TurnPhase.Main]: 'GAME.PHASE.MAIN',
  [TurnPhase.Attack]: 'GAME.PHASE.ATTACK',
  [TurnPhase.BetweenTurns]: 'GAME.PHASE.BETWEEN_TURNS'
};

const ACTION_LABEL_KEYS: Partial<Record<GameActionType, string>> = {
  [GameActionType.CreateGame]: 'GAME.ACTIONS.CREATE_GAME',
  [GameActionType.JoinGame]: 'GAME.ACTIONS.JOIN_GAME',
  [GameActionType.StartGame]: 'GAME.ACTIONS.START_GAME',
  [GameActionType.AckMulliganNotice]: 'GAME.ACTIONS.ACK_MULLIGAN_NOTICE',
  [GameActionType.ChooseInitialPokemon]: 'GAME.ACTIONS.CHOOSE_INITIAL_POKEMON',
  [GameActionType.PauseGame]: 'GAME.ACTIONS.PAUSE_GAME',
  [GameActionType.ResumeGame]: 'GAME.ACTIONS.RESUME_GAME',
  [GameActionType.DrawCard]: 'GAME.ACTIONS.DRAW_CARD',
  [GameActionType.PlayBasicPokemon]: 'GAME.ACTIONS.PLAY_BASIC_POKEMON',
  [GameActionType.EvolvePokemon]: 'GAME.ACTIONS.EVOLVE_POKEMON',
  [GameActionType.AttachEnergy]: 'GAME.ACTIONS.ATTACH_ENERGY',
  [GameActionType.PlayTrainer]: 'GAME.ACTIONS.PLAY_TRAINER',
  [GameActionType.Retreat]: 'GAME.ACTIONS.RETREAT',
  [GameActionType.DeclareAttack]: 'GAME.ACTIONS.DECLARE_ATTACK',
  [GameActionType.SelectTarget]: 'GAME.ACTIONS.SELECT_TARGET',
  [GameActionType.EndTurn]: 'GAME.ACTIONS.END_TURN',
  [GameActionType.PromoteBenchPokemon]: 'GAME.ACTIONS.PROMOTE_BENCH_POKEMON',
  [GameActionType.TakePrizeCard]: 'GAME.ACTIONS.TAKE_PRIZE_CARD',
  [GameActionType.Concede]: 'GAME.ACTIONS.CONCEDE',
  [GameActionType.UseAbility]: 'GAME.ACTIONS.USE_ABILITY'
};

const DISPLAYED_GAME_EVENT_TYPES = new Set<GameEventType>([
  GameEventType.TurnStarted,
  GameEventType.CardDrawn,
  GameEventType.CardPlayed,
  GameEventType.TrainerPlayed,
  GameEventType.EnergyAttached,
  GameEventType.PokemonEvolved,
  GameEventType.RetreatDone,
  GameEventType.AttackDeclared,
  GameEventType.AttackChoiceResolved,
  GameEventType.DamageApplied,
  GameEventType.StatusApplied,
  GameEventType.AttackEffectResolved,
  GameEventType.PassiveAbilityTriggered,
  GameEventType.PokemonKnockedOut,
  GameEventType.PokemonPromoted,
  GameEventType.PrizeTaken,
  GameEventType.GameFinished,
  GameEventType.CoinFlipped
]);
const TECHNICAL_HISTORY_PATTERNS = [
  'phase changed',
  'phase change',
  'cambio de fase',
  'fase cambiada',
  'sincronizado',
  'sync',
  'version',
  'websocket',
  'state updated',
  'state sync',
  'state_sync',
  'evento visible',
  'visible event'
];

export interface MapGameSnapshotOptions {
  localPlayerId: string | null;
  playerLabels?: Record<string, string>;
  connectedByPlayerId?: Record<string, boolean>;
  avatarUrlByPlayerId?: Record<string, string>;
  eventFeed?: GameEvent[];
  uiState?: BoardEphemeralUiState;
  t?: UiTranslateFn;
}

function translate(
  options: MapGameSnapshotOptions,
  key: string,
  params?: Record<string, string | number | boolean | null | undefined>
): string {
  return options.t?.(key, params) ?? key;
}

export function mapGameSnapshotToBoardGameViewModel(
  snapshot: GameSnapshot,
  options: MapGameSnapshotOptions
): BoardGameViewModel {
  const boardView = requiredBoardView(snapshot);
  const resolvedLocalPlayerId = resolveLocalPlayerId(boardView, options.localPlayerId);
  const rivalPlayerId = resolveRivalPlayerId(boardView, resolvedLocalPlayerId);

  if (!resolvedLocalPlayerId || !rivalPlayerId) {
    throw new Error('GAME.ERRORS.BOARD_PERSPECTIVE');
  }

  const localPlayer = mapPlayer(snapshot, resolvedLocalPlayerId, options);
  const rivalPlayer = mapPlayer(snapshot, rivalPlayerId, options);
  const activePlayerLabel = snapshot.turn.activePlayerId
    ? labelForPlayer(snapshot.turn.activePlayerId, options)
    : null;
  const phaseLabel = snapshot.turn.currentPhase
    ? translate(options, PHASE_LABEL_KEYS[snapshot.turn.currentPhase])
    : translate(options, 'GAME.PHASE.WAITING_TABLE');
  const actions = mapRootActions(snapshot, localPlayer, boardView.actionHints, options);
  const eventFeed = mapEventFeed(options.eventFeed ?? [], options);
  const mulliganSetupEvents = mapMulliganSetupEvents(options.eventFeed ?? [], options);
  const localSetupState = localPlayerState(snapshot, resolvedLocalPlayerId);

  return {
    gameId: snapshot.gameId,
    status: snapshot.status,
    statusLabel: translate(options, STATUS_LABEL_KEYS[snapshot.status]),
    statusSummary: buildStatusSummary(snapshot, phaseLabel, activePlayerLabel, options),
    turnNumber: snapshot.turn.turnNumber,
    currentPhase: snapshot.turn.currentPhase,
    phaseLabel,
    activePlayerId: snapshot.turn.activePlayerId,
    activePlayerLabel,
    stateVersion: snapshot.stateVersion,
    availableActions: [...snapshot.actions.availableActions],
    localPlayer,
    rivalPlayer,
    stadium: mapStadium(boardView, options),
    actionHints: mapActionHints(boardView.actionHints),
    turnContext: {
      turnNumber: snapshot.turn.turnNumber,
      currentPhase: snapshot.turn.currentPhase,
      phaseLabel,
      activePlayerId: snapshot.turn.activePlayerId,
      activePlayerLabel,
      playerWhoWentFirstId: snapshot.turn.playerWhoWentFirstId,
      turnStartedAt: snapshot.turn.turnStartedAt,
      energyAttachedThisTurn: snapshot.turn.energyAttachedThisTurn,
      supporterPlayedThisTurn: snapshot.turn.supporterPlayedThisTurn,
      retreatedThisTurn: snapshot.turn.retreatedThisTurn,
      stateVersion: snapshot.stateVersion
    },
    eventFeed,
    mulliganSetupEvents,
    mulliganNoticePending: localSetupState.mulliganNoticePending,
    mulliganFlowActive: localSetupState.mulliganFlowActive,
    mulliganReadyForInitialSelection: localSetupState.mulliganReadyForInitialSelection,
    mulliganRoundNumber: localSetupState.mulliganRoundNumber,
    mulliganCurrentPlayer: localSetupState.mulliganCurrentPlayer,
    ui: {
      selectedCardInstanceId: options.uiState?.selectedCardInstanceId ?? null,
      selectedPokemonInPlayId: options.uiState?.selectedPokemonInPlayId ?? null,
      targetPokemonInPlayId: options.uiState?.targetPokemonInPlayId ?? null,
      activePanel: options.uiState?.activePanel ?? 'none',
      pendingAction: options.uiState?.pendingAction ?? null,
      hasSelection: Boolean(options.uiState?.selectedCardInstanceId || options.uiState?.selectedPokemonInPlayId),
      hasPendingTarget: Boolean(options.uiState?.targetPokemonInPlayId),
      eventFeedCount: eventFeed.length
    },
    actions,
    resolution: mapResolution(snapshot, options),
    missingDataNotes: buildMissingDataNotes(snapshot, options),
    availableActionLabels: actions.filter((action) => action.enabled).map((action) => action.label),
    historySummary: buildHistorySummary(eventFeed, options),
    latestActionLabel: eventFeed[0]?.label ?? translate(options, 'GAME.EVENTS.NONE')
  };
}

function requiredBoardView(snapshot: GameSnapshot): VisibleBoardDto {
  const boardView = snapshot.board.view;

  if (!boardView || boardView.players.length === 0) {
    throw new Error('GAME.ERRORS.BOARD_VIEW_MISSING');
  }

  return boardView;
}

function resolveLocalPlayerId(boardView: VisibleBoardDto, fallbackPlayerId: string | null): string | null {
  return (
    boardView.players.find((player) => player.local)?.playerId ??
    boardView.localPlayerId ??
    fallbackPlayerId ??
    null
  );
}

function resolveRivalPlayerId(boardView: VisibleBoardDto, localPlayerId: string | null): string | null {
  return boardView.players.find((player) => player.playerId !== localPlayerId)?.playerId ?? null;
}

function mapPlayer(
  snapshot: GameSnapshot,
  playerId: string,
  options: MapGameSnapshotOptions
): BoardPlayerViewModel {
  const playerState = snapshot.players[playerId] ?? emptyPlayerState();
  const visiblePlayer = visiblePlayerBoard(snapshot.board.view, playerId);
  const isLocal = visiblePlayer?.local ?? playerId === options.localPlayerId;
  const isSetup = snapshot.status === GameStatus.Setup;
  const hidePokemonIdentity = isSetup && !isLocal;
  const visibleBoardPlayer = mapVisiblePlayerToBoardPlayer(snapshot, visiblePlayer);
  const canonicalBoardPlayer = snapshot.boardPlayers?.[playerId] ?? null;
  // While this player has a pending bench->active promotion, their active slot is
  // authoritatively empty (the previous active Pokemon was KO'd). Falling back to
  // canonicalBoardPlayer here would re-render that dead "ghost" Pokemon as if it
  // were still occupying the active slot, which both looks wrong and can block the
  // drop zone from accepting the promotion drag.
  const isAwaitingOwnPromotion = snapshot.resolution?.playerToPromoteId === playerId;
  const boardPlayer = visibleBoardPlayer
      ? {
        ...visibleBoardPlayer,
        activePokemon:
          visibleBoardPlayer.activePokemon
          ?? (isAwaitingOwnPromotion ? null : canonicalBoardPlayer?.activePokemon ?? null),
        benchPokemon: visibleBoardPlayer.benchPokemon.length > 0
          ? visibleBoardPlayer.benchPokemon
          : canonicalBoardPlayer?.benchPokemon ?? [],
        hand: isLocal
          && visibleBoardPlayer.hand.count > 0
          && visibleBoardPlayer.hand.cards.length === 0
          && canonicalBoardPlayer?.hand.cards.length
            ? canonicalBoardPlayer.hand
            : visibleBoardPlayer.hand
      }
    : canonicalBoardPlayer;
  const handCards = boardPlayer
    ? zoneCards(`${playerId}-hand`, boardPlayer.hand, isLocal ? translate(options, 'GAME.CARD_FALLBACK') : translate(options, 'GAME.OPPONENT_HAND'), !isLocal)
    : legacyHandCards(snapshot, playerId, playerState, isLocal, options);
  const prizeCards = boardPlayer
    ? zoneCards(`${playerId}-prize`, boardPlayer.prizes, translate(options, 'GAME.PRIZES'), true)
    : hiddenCards(playerId, 'prize', referencesFor(snapshot, playerId, CardZone.Prize).length, options);
  const deckCards = boardPlayer
    ? zoneCards(`${playerId}-deck`, boardPlayer.deck, translate(options, 'GAME.DECK'), true)
    : hiddenCards(playerId, 'deck', referencesFor(snapshot, playerId, CardZone.Deck).length, options);
  const discardCards = boardPlayer
    ? zoneCards(`${playerId}-discard`, boardPlayer.discard, translate(options, 'GAME.DISCARD'), false)
    : referencesFor(snapshot, playerId, CardZone.Discard).map((referenceId) =>
        visibleReferenceCard(referenceId, translate(options, 'GAME.CARD_FALLBACK'))
      );
  const activePokemon = mapActiveSlot(
    snapshot,
    playerId,
    playerState,
    boardPlayer,
    visiblePlayer,
    isLocal,
    hidePokemonIdentity,
    options
  );
  const mappedBenchSlots = boardPlayer
    ? boardBenchSlots(snapshot, playerId, boardPlayer.benchPokemon, playerState, isLocal, hidePokemonIdentity, options)
    : legacyBenchSlots(snapshot, playerId, isLocal, options);
  const benchSlots = isSetup && !isLocal
    ? projectPublicSetupBenchOccupancy(
        mappedBenchSlots,
        visiblePlayer?.setupBenchOccupiedCount ?? 0,
        playerId,
        options
      )
    : mappedBenchSlots;

  return {
    id: playerId,
    label: labelForPlayer(playerId, options),
    avatarUrl: options.avatarUrlByPlayerId?.[playerId] ?? '',
    connected: options.connectedByPlayerId?.[playerId] ?? false,
    role: isLocal ? 'current' : 'opponent',
    isLocal,
    isOpponent: !isLocal,
    isActiveTurn: isCurrentPlayerTurn({
      status: snapshot.status,
      activePlayerId: snapshot.turn.activePlayerId,
      activePlayerLabel: snapshot.turn.activePlayerId
        ? labelForPlayer(snapshot.turn.activePlayerId, options)
        : null
    }, playerId),
    setupSelectionSubmitted: Boolean(
      visiblePlayer?.setupSelectionSubmitted || playerState.initialPokemonSelectionSubmitted
    ),
    mulliganCount: playerState.mulliganCount ?? 0,
    deckCount: boardPlayer?.deck.count ?? deckCards.length,
    discardCount: boardPlayer?.discard.count ?? discardCards.length,
    prizeCount: boardPlayer?.prizes.count ?? prizeCards.length,
    handCards,
    prizeCards,
    deckCards,
    discardCards,
    handZone: zone(translate(options, 'GAME.HAND'), isLocal ? 'visible' : 'hidden', handCards),
    prizeZone: zone(translate(options, 'GAME.PRIZES'), 'hidden', prizeCards),
    deckZone: zone(translate(options, 'GAME.DECK'), 'hidden', deckCards),
    discardZone: zone(translate(options, 'GAME.DISCARD'), 'visible', discardCards),
    activePokemon,
    benchSlots,
    deckPokemonOptions: isLocal
      ? (boardPlayer?.deck.cards ?? [])
          .slice(0, 7)
          .filter((c) => c.supertype === 'POKEMON' && c.cardInstanceId)
          .map((c) => ({ cardInstanceId: c.cardInstanceId!, label: c.name ?? 'Pokémon' }))
      : undefined,
    actions: isLocal
      ? snapshot.actions.availableActions
          .filter((actionType) => actionType !== GameActionType.SelectTarget)
          .map((actionType) => {
            const enabled = isActionEnabledForPlayer(snapshot, playerId, actionType);
            return mapAction(
              actionType,
              enabled,
              enabled ? null : resolutionDisabledReason(snapshot, options),
              options
            );
          })
      : []
  };
}

function mapActiveSlot(
  snapshot: GameSnapshot,
  playerId: string,
  playerState: GameSnapshotPlayerState,
  boardPlayer: GameBoardPlayerState | null,
  visiblePlayer: VisiblePlayerBoardDto | null,
  isLocal: boolean,
  hidePokemonIdentity: boolean,
  options: MapGameSnapshotOptions
): BoardSlotViewModel {
  if (boardPlayer?.activePokemon) {
    return pokemonSlot(
      `${playerId}-active`,
      translate(options, 'GAME.ACTIVE_POKEMON'),
      boardPlayer.activePokemon,
      hidePokemonIdentity,
      options
    );
  }

  if (snapshot.status === GameStatus.Setup && !isLocal && visiblePlayer?.setupActiveOccupied) {
    return hiddenSetupSlot(
      `${playerId}-active`,
      translate(options, 'GAME.ACTIVE_POKEMON'),
      options
    );
  }

  return legacyActiveSlot(snapshot, playerId, playerState, isLocal, options);
}

function projectPublicSetupBenchOccupancy(
  slots: BoardSlotViewModel[],
  occupiedCount: number,
  playerId: string,
  options: MapGameSnapshotOptions
): BoardSlotViewModel[] {
  const projectedSlots = [...slots];
  let missingOccupiedSlots = Math.max(0, occupiedCount - projectedSlots.filter((slot) => slot.occupied).length);

  for (let index = 0; index < projectedSlots.length && missingOccupiedSlots > 0; index++) {
    if (projectedSlots[index].occupied) {
      continue;
    }
    projectedSlots[index] = hiddenSetupSlot(
      `${playerId}-bench-${index + 1}`,
      translate(options, 'GAME.SLOT', { number: index + 1 }),
      options
    );
    missingOccupiedSlots--;
  }

  return projectedSlots;
}

function hiddenSetupSlot(
  id: string,
  label: string,
  options: MapGameSnapshotOptions
): BoardSlotViewModel {
  return {
    id,
    label,
    occupied: true,
    card: hiddenCard(`${id}-setup-hidden`, translate(options, 'GAME.HIDDEN_POKEMON')),
    pokemon: null
  };
}

function visiblePlayerBoard(boardView: VisibleBoardDto | null | undefined, playerId: string): VisiblePlayerBoardDto | null {
  return boardView?.players.find((player) => player.playerId === playerId) ?? null;
}

function mapVisiblePlayerToBoardPlayer(
  snapshot: GameSnapshot,
  player: VisiblePlayerBoardDto | null
): GameBoardPlayerState | null {
  if (!player) {
    return null;
  }

  return {
    playerId: player.playerId,
    hand: mapVisibleZone(player.hand),
    deck: mapVisibleZone(player.deck),
    prizes: mapVisibleZone(player.prize),
    discard: mapVisibleZone(player.discard),
    stadium: mapVisibleStadiumZone(snapshot.board.view, player.playerId),
    activePokemon: mapVisiblePokemon(player.activePokemon, CardZone.Active),
    benchPokemon: player.benchPokemon.map((pokemon) => mapVisiblePokemon(pokemon, CardZone.Bench)).filter(isDefined)
  };
}

function mapVisibleZone(zone: VisibleZoneDto): GameBoardZoneSummary {
  return {
    count: zone.count,
    cards: zone.cards.map((card, index) => mapVisibleCard(card, zone.zone, index))
  };
}

function mapVisibleStadiumZone(boardView: VisibleBoardDto | null | undefined, playerId: string): GameBoardZoneSummary {
  const stadium = boardView?.stadium;
  if (!stadium?.card || stadium.playedByPlayerId !== playerId) {
    return { count: 0, cards: [] };
  }

  return {
    count: 1,
    cards: [mapVisibleCard(stadium.card, CardZone.Stadium, 0)]
  };
}

function mapVisiblePokemon(pokemon: VisiblePokemonDto | null, zone: CardZone): GameBoardPokemon | null {
  if (!pokemon) {
    return null;
  }

  return {
    pokemonInPlayId: pokemon.pokemonInPlayId,
    ownerUserId: pokemon.ownerPlayerId,
    slotPosition: pokemon.slotPosition,
    damageCounters: pokemon.damageCounters,
    enteredPlayTurn: null,
    activeCard: pokemon.activeCard ? mapVisibleCard(pokemon.activeCard, zone, 0) : null,
    evolutionStack: pokemon.evolutionStack.map((card, index) => mapVisibleCard(card, CardZone.EvolutionStack, index)),
    attachedCards: pokemon.attachedEnergyCards.map((card, index) => mapVisibleCard(card, CardZone.Attached, index)),
    attachedTrainerCards: pokemon.attachedTrainerCards.map((card, index) => mapVisibleCard(card, CardZone.Attached, index)),
    specialConditions: pokemon.specialConditions,
    attacks: pokemon.attacks.map((attack, index) => ({
      attackId: attack.attackId,
      name: attack.name,
      damageText: attack.damageText,
      baseDamage: attack.baseDamage,
      effectText: attack.effectText,
      attackOrder: index,
      costs: attack.costs,
      displayName: attack.displayName,
      displayCost: attack.displayCost,
      displayText: attack.displayText,
      available: attack.enabled,
      disabledReason: attack.disabledReason,
      requiresTarget: attack.requiresTarget,
      validTargetPokemonInPlayIds: attack.validTargetPokemonInPlayIds,
      targetsOwnPokemon: attack.targetsOwnPokemon
    })),
    abilities: pokemon.abilities.map((ability) => ({
      abilityId: ability.abilityId,
      name: ability.name,
      activationType: ability.activationType,
      available: ability.enabled,
      disabledReason: ability.disabledReason,
      deckCardOptions: (ability.deckCardOptions ?? []).map((card, index) =>
        mapVisibleCard(card, CardZone.Deck, index)
      )
    })),
    canReceiveEnergy: pokemon.canReceiveEnergy,
    canReceiveTrainer: pokemon.canReceiveTrainer,
    canRetreatTo: pokemon.canRetreatTo,
    canPromote: pokemon.canPromote,
    visualEffects: pokemon.visualEffects ?? []
  };
}

function mapVisibleCard(card: VisibleCardDto, zone: CardZone, zonePosition: number): GameBoardCard {
  return {
    cardInstanceId: card.cardInstanceId,
    cardId: card.cardId,
    externalId: card.externalId,
    name: card.name,
    setCode: card.setCode,
    number: card.number,
    supertype: card.supertype,
    category: card.category,
    subtype: card.subtype,
    imageSmallUrl: card.imageSmallUrl,
    imageLargeUrl: card.imageLargeUrl,
    hp: card.hp,
    zone,
    zonePosition,
    faceDown: card.faceDown,
    playable: card.playable,
    suggestedAction: card.suggestedAction,
    disabledReason: card.disabledReason,
    validTargetPokemonInPlayIds: card.validTargetPokemonInPlayIds
  };
}

function isDefined<T>(value: T | null | undefined): value is T {
  return value !== null && value !== undefined;
}

function zoneCards(
  prefix: string,
  zoneSummary: GameBoardZoneSummary,
  fallbackLabel: string,
  forceHidden: boolean
): BoardCardViewModel[] {
  if (zoneSummary.cards.length === 0 && zoneSummary.count > 0) {
    return Array.from({ length: zoneSummary.count }, (_, index) =>
      hiddenCard(`${prefix}-hidden-${index + 1}`, fallbackLabel)
    );
  }

  return zoneSummary.cards.map((card, index) =>
    boardCard(card, `${prefix}-${index + 1}`, fallbackLabel, forceHidden)
  );
}

function boardCard(
  card: GameBoardCard,
  id: string,
  fallbackLabel: string,
  forceHidden: boolean
): BoardCardViewModel {
  if (forceHidden || card.faceDown || !card.cardId) {
    return hiddenCard(id, fallbackLabel, card.cardInstanceId);
  }

  const label = safeVisibleLabel(card.name, fallbackLabel, [
    card.cardInstanceId,
    card.cardId,
    card.externalId
  ]);

  return {
    id,
    cardId: card.cardId,
    cardInstanceId: card.cardInstanceId,
    label,
    visibility: 'visible',
    faceDown: false,
    rotation: 0,
    imageSmallUrl: card.imageSmallUrl,
    imageLargeUrl: card.imageLargeUrl,
    hp: card.hp ?? null,
    altText: label,
    externalId: card.externalId ?? undefined,
    setCode: card.setCode ?? undefined,
    number: card.number ?? undefined,
    supertype: card.supertype ?? undefined,
    category: card.category ?? undefined,
    subtype: card.subtype ?? undefined,
    playable: card.playable ?? false,
    suggestedAction: card.suggestedAction ?? null,
    disabledReason: card.disabledReason ?? null,
    validTargetPokemonInPlayIds: card.validTargetPokemonInPlayIds ?? []
  };
}

function hiddenCards(
  playerId: string,
  zoneName: string,
  count: number,
  options: MapGameSnapshotOptions
): BoardCardViewModel[] {
  return Array.from({ length: count }, (_, index) =>
    hiddenCard(`${playerId}-${zoneName}-hidden-${index + 1}`, translate(options, 'GAME.HIDDEN_CARD'))
  );
}

function hiddenCard(
  id: string,
  label: string,
  cardInstanceId: string | null = null
): BoardCardViewModel {
  return {
    id,
    cardId: null,
    cardInstanceId,
    label,
    visibility: 'hidden',
    faceDown: true,
    rotation: 0
  };
}

function visibleReferenceCard(
  referenceId: string,
  label: string
): BoardCardViewModel {
  return {
    id: referenceId,
    cardInstanceId: referenceId,
    label,
    visibility: 'visible',
    faceDown: false,
    rotation: 0
  };
}

function zone(
  label: string,
  visibility: BoardCardZoneViewModel['visibility'],
  cards: BoardCardViewModel[]
): BoardCardZoneViewModel {
  return {
    label,
    visibility,
    count: cards.length,
    cards
  };
}

function legacyHandCards(
  snapshot: GameSnapshot,
  playerId: string,
  playerState: GameSnapshotPlayerState,
  isLocal: boolean,
  options: MapGameSnapshotOptions
): BoardCardViewModel[] {
  const references = playerState.cardInstanceIdsInHand.length > 0
    ? playerState.cardInstanceIdsInHand
    : playerState.cardIdsInHand;

  if (!isLocal) {
    return hiddenCards(playerId, 'hand', opponentHandCount(snapshot, playerId, playerState), options);
  }

  return references.map((referenceId, index) =>
    ({
      id: referenceId,
      label: translate(options, 'GAME.CARD_NUMBER', { number: index + 1 }),
      visibility: 'visible',
      faceDown: false,
      rotation: 0
    })
  );
}

function legacyActiveSlot(
  snapshot: GameSnapshot,
  playerId: string,
  playerState: GameSnapshotPlayerState,
  isLocal: boolean,
  options: MapGameSnapshotOptions
): BoardSlotViewModel {
  const activeReference =
    referencesFor(snapshot, playerId, CardZone.Active)[0] ??
    playerState.initialActiveCardInstanceId ??
    setupSubmittedFallbackReference(snapshot, playerId, playerState, 'active');
  const card = activeReference ? legacyPokemonCard(snapshot, activeReference, isLocal, options) : null;

  return {
    id: `${playerId}-active`,
    label: translate(options, 'GAME.ACTIVE_POKEMON'),
    occupied: card !== null,
    card,
    pokemon: card ? legacyPokemon(snapshot, playerId, activeReference, card) : null
  };
}

function legacyBenchSlots(
  snapshot: GameSnapshot,
  playerId: string,
  isLocal: boolean,
  options: MapGameSnapshotOptions
): BoardSlotViewModel[] {
  const playerState = snapshot.players[playerId] ?? emptyPlayerState();
  const references = setupBenchReferences(snapshot, playerId, playerState);

  return Array.from({ length: BENCH_SLOT_COUNT }, (_, index) => {
    const referenceId = references[index] ?? null;
    const card = referenceId ? legacyPokemonCard(snapshot, referenceId, isLocal, options) : null;

    return {
      id: `${playerId}-bench-${index + 1}`,
      label: translate(options, 'GAME.SLOT', { number: index + 1 }),
      occupied: card !== null,
      card,
      pokemon: referenceId && card ? legacyPokemon(snapshot, playerId, referenceId, card) : null
    };
  });
}

function legacyPokemonCard(
  snapshot: GameSnapshot,
  referenceId: string,
  isLocal: boolean,
  options: MapGameSnapshotOptions
): BoardCardViewModel {
  if (!isLocal && snapshot.status === GameStatus.Setup) {
    return hiddenCard(`${referenceId}-hidden`, translate(options, 'GAME.HIDDEN_POKEMON'), referenceId);
  }

  const resolvedCard = findBoardCardByReference(snapshot, referenceId);
  if (resolvedCard) {
    return boardCard(resolvedCard, referenceId, translate(options, 'GAME.POKEMON_FALLBACK'), false);
  }

  return visibleReferenceCard(referenceId, translate(options, 'GAME.POKEMON_FALLBACK'));
}

function safeVisibleLabel(
  label: string | null | undefined,
  fallbackLabel: string,
  technicalIds: Array<string | null | undefined> = []
): string {
  const normalizedLabel = label?.trim();
  if (!normalizedLabel || isTechnicalLabel(normalizedLabel, technicalIds)) {
    return fallbackLabel;
  }

  return normalizedLabel;
}

function isTechnicalLabel(
  label: string,
  technicalIds: Array<string | null | undefined>
): boolean {
  const normalizedLabel = label.trim().toLowerCase();
  const normalizedIds = technicalIds
    .filter((technicalId): technicalId is string => typeof technicalId === 'string' && technicalId.trim().length > 0)
    .map((technicalId) => technicalId.trim().toLowerCase());

  return (
    normalizedIds.includes(normalizedLabel) ||
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(label) ||
    /^pokemon-in-play-[0-9a-f-]+$/i.test(label) ||
    /^card-instance-[0-9a-f-]+$/i.test(label)
  );
}

function legacyPokemon(
  snapshot: GameSnapshot,
  playerId: string,
  referenceId: string,
  activeCard: BoardCardViewModel
): BoardPokemonViewModel {
  const playerState = snapshot.players[playerId] ?? emptyPlayerState();

  return {
    id: referenceId,
    activeCard,
    evolutionStack: [],
    attachedEnergyCards: [],
    attachedTrainerCards: [],
    damageCounters: null,
    specialConditions:
      snapshot.board.zoneByCardReferenceId[referenceId] === CardZone.Active
        ? playerState.activePokemonConditions
        : [],
    attacks: playerState.affordableAttackIds.map((attackId) => ({
      id: attackId,
      label: attackId,
      damageText: null,
      baseDamage: null,
      effectText: null,
      costs: [],
      enabled: true,
      disabledReason: null
    })),
    abilities: pendingAbility(),
    actions: [],
    canReceiveEnergy: false,
    canReceiveTrainer: false,
    canRetreatTo: false,
    canPromote: false,
    visualEffects: []
  };
}

function pokemonSlot(
  id: string,
  label: string,
  pokemon: GameBoardPokemon,
  forceHidden = false,
  options: MapGameSnapshotOptions
): BoardSlotViewModel {
  const card = pokemon.activeCard
    ? boardCard(
        pokemon.activeCard,
        `${id}-card`,
        forceHidden ? translate(options, 'GAME.HIDDEN_POKEMON') : translate(options, 'GAME.HIDDEN_CARD'),
        forceHidden
      )
    : pokemon.pokemonInPlayId
      ? hiddenCard(`${id}-hidden`, translate(options, 'GAME.HIDDEN_POKEMON'), pokemon.pokemonInPlayId)
      : null;

  return {
    id,
    label,
    occupied: card !== null || pokemon.pokemonInPlayId !== null,
    card,
    pokemon: card ? boardPokemon(pokemon, card, options) : null
  };
}

function boardPokemon(
  pokemon: GameBoardPokemon,
  activeCard: BoardCardViewModel,
  options: MapGameSnapshotOptions
): BoardPokemonViewModel {
  return {
    id: pokemon.pokemonInPlayId ?? `${activeCard.id}-pokemon`,
    activeCard,
    evolutionStack: pokemon.evolutionStack.map((card, index) =>
      boardCard(card, `${activeCard.id}-evolution-${index + 1}`, translate(options, 'GAME.EVOLUTION_FALLBACK'), false)
    ),
    attachedEnergyCards: pokemon.attachedCards.map((card, index) =>
      boardCard(card, `${activeCard.id}-attached-${index + 1}`, translate(options, 'GAME.ATTACHED_ENERGY_FALLBACK'), false)
    ),
    attachedTrainerCards: (pokemon.attachedTrainerCards ?? []).map((card, index) =>
      boardCard(card, `${activeCard.id}-tool-${index + 1}`, translate(options, 'GAME.ATTACHED_TOOL_FALLBACK'), false)
    ),
    damageCounters: pokemon.damageCounters,
    specialConditions: pokemon.specialConditions,
    attacks: pokemon.attacks.map(mapPokemonAttack),
    abilities: [...pokemon.abilities.map(mapPokemonAbility), ...pendingAbility()],
    actions: [],
    canReceiveEnergy: pokemon.canReceiveEnergy ?? false,
    canReceiveTrainer: pokemon.canReceiveTrainer ?? false,
    canRetreatTo: pokemon.canRetreatTo ?? false,
    canPromote: pokemon.canPromote ?? false,
    visualEffects: pokemon.visualEffects ?? []
  };
}

function boardBenchSlots(
  snapshot: GameSnapshot,
  playerId: string,
  benchPokemon: GameBoardPokemon[],
  playerState: GameSnapshotPlayerState,
  isLocal: boolean,
  hidePokemonIdentity: boolean,
  options: MapGameSnapshotOptions
): BoardSlotViewModel[] {
  const pokemonBySlot = Array.from<GameBoardPokemon | null>({ length: BENCH_SLOT_COUNT }).fill(null);
  const pendingPokemonWithoutSlot: GameBoardPokemon[] = [];
  const placedPokemonIdentities = new Set<string>();

  for (const pokemon of benchPokemon) {
    const pokemonIdentity = boardPokemonIdentity(pokemon);
    if (pokemonIdentity && placedPokemonIdentities.has(pokemonIdentity)) {
      continue;
    }

    const slotIndex = benchSlotIndex(pokemon.slotPosition);
    if (slotIndex !== null && pokemonBySlot[slotIndex] === null) {
      pokemonBySlot[slotIndex] = pokemon;
      if (pokemonIdentity) {
        placedPokemonIdentities.add(pokemonIdentity);
      }
      continue;
    }

    pendingPokemonWithoutSlot.push(pokemon);
    if (pokemonIdentity) {
      placedPokemonIdentities.add(pokemonIdentity);
    }
  }

  for (const pokemon of pendingPokemonWithoutSlot) {
    const emptySlotIndex = firstEmptyPokemonSlotIndex(pokemonBySlot);
    if (emptySlotIndex === null) {
      break;
    }

    pokemonBySlot[emptySlotIndex] = pokemon;
  }

  const placedCardReferences = new Set(
    pokemonBySlot
      .map((pokemon) => pokemon ? boardPokemonCardReference(pokemon) : null)
      .filter(isDefined)
  );
  const setupReferences = snapshot.status === GameStatus.Setup
    ? setupBenchReferences(snapshot, playerId, playerState)
        .filter((reference) => !placedCardReferences.has(reference))
    : [];
  let nextSetupReferenceIndex = 0;

  return Array.from({ length: BENCH_SLOT_COUNT }, (_, index) => {
    const pokemon = pokemonBySlot[index];
    if (pokemon) {
      return pokemonSlot(
        `${playerId}-bench-${index + 1}`,
        translate(options, 'GAME.SLOT', { number: index + 1 }),
        pokemon,
        hidePokemonIdentity,
        options
      );
    }

    const setupReference = setupReferences[nextSetupReferenceIndex] ?? null;
    nextSetupReferenceIndex += setupReference ? 1 : 0;
    if (setupReference) {
      const card = legacyPokemonCard(snapshot, setupReference, isLocal, options);
      return {
        id: `${playerId}-bench-${index + 1}`,
        label: translate(options, 'GAME.SLOT', { number: index + 1 }),
        occupied: true,
        card,
        pokemon: legacyPokemon(snapshot, playerId, setupReference, card)
      };
    }

    return emptyBenchSlot(playerId, index, options);
  });
}

function boardPokemonIdentity(pokemon: GameBoardPokemon): string | null {
  return pokemon.pokemonInPlayId ?? boardPokemonCardReference(pokemon);
}

function boardPokemonCardReference(pokemon: GameBoardPokemon): string | null {
  return pokemon.activeCard?.cardInstanceId ?? pokemon.activeCard?.cardId ?? pokemon.activeCard?.externalId ?? null;
}

function firstEmptyPokemonSlotIndex(slots: Array<GameBoardPokemon | null>): number | null {
  const index = slots.findIndex((slot) => slot === null);
  return index >= 0 ? index : null;
}

function setupBenchReferences(
  snapshot: GameSnapshot,
  playerId: string,
  playerState: GameSnapshotPlayerState
): string[] {
  const benchReferences = referencesFor(snapshot, playerId, CardZone.Bench);
  if (benchReferences.length > 0) {
    return benchReferences;
  }

  if (snapshot.status !== GameStatus.Setup) {
    return [];
  }

  if (playerState.initialBenchCardInstanceIds.length > 0) {
    return playerState.initialBenchCardInstanceIds;
  }

  if (!playerState.initialPokemonSelectionSubmitted) {
    return [];
  }

  return Array.from({ length: Math.min(playerState.benchPokemonCount, BENCH_SLOT_COUNT) }, (_, index) =>
    setupSubmittedFallbackReference(snapshot, playerId, playerState, `bench-${index + 1}`)
  ).filter(isDefined);
}

function setupSubmittedFallbackReference(
  snapshot: GameSnapshot,
  playerId: string,
  playerState: GameSnapshotPlayerState,
  suffix: string
): string | null {
  if (snapshot.status !== GameStatus.Setup || !playerState.initialPokemonSelectionSubmitted) {
    return null;
  }

  return `${playerId}-setup-${suffix}-hidden`;
}

function benchSlotIndex(slotPosition: number | null): number | null {
  if (slotPosition === null || slotPosition < 1 || slotPosition > BENCH_SLOT_COUNT) {
    return null;
  }

  return slotPosition - 1;
}

function emptyBenchSlot(
  playerId: string,
  index: number,
  options: MapGameSnapshotOptions
): BoardSlotViewModel {
  return {
    id: `${playerId}-bench-${index + 1}`,
    label: translate(options, 'GAME.SLOT', { number: index + 1 }),
    occupied: false,
    card: null,
    pokemon: null
  };
}

function findBoardCardByReference(
  snapshot: GameSnapshot,
  referenceId: string
): GameBoardCard | null {
  for (const boardPlayer of Object.values(snapshot.boardPlayers ?? {})) {
    const cards = [
      ...boardPlayer.hand.cards,
      ...boardPlayer.deck.cards,
      ...boardPlayer.prizes.cards,
      ...boardPlayer.discard.cards,
      ...boardPlayer.stadium.cards,
      ...pokemonCards(boardPlayer.activePokemon),
      ...boardPlayer.benchPokemon.flatMap((pokemon) => pokemonCards(pokemon))
    ];
    const resolvedCard = cards.find((card) =>
      card.cardInstanceId === referenceId ||
      card.cardId === referenceId ||
      card.externalId === referenceId
    );

    if (resolvedCard) {
      return resolvedCard;
    }
  }

  return null;
}

function pokemonCards(pokemon: GameBoardPokemon | null): GameBoardCard[] {
  if (!pokemon) {
    return [];
  }

  return [
    pokemon.activeCard,
    ...pokemon.evolutionStack,
    ...pokemon.attachedCards,
    ...(pokemon.attachedTrainerCards ?? [])
  ].filter(isDefined);
}

function mapPokemonAttack(attack: GameBoardAttack): BoardPokemonAttackViewModel {
  const name = attack.name ?? `Ataque ${attack.attackOrder + 1}`;

  return {
    id: attack.attackId ?? `attack-${attack.attackOrder}`,
    label: name,
    name,
    displayName: attack.displayName,
    damageText: attack.damageText,
    baseDamage: attack.baseDamage,
    effectText: attack.effectText,
    displayText: attack.displayText,
    costs: attack.costs,
    displayCost: attack.displayCost,
    enabled: attack.available,
    disabledReason: attack.disabledReason,
    requiresTarget: attack.requiresTarget ?? false,
    validTargetPokemonInPlayIds: attack.validTargetPokemonInPlayIds ?? [],
    targetsOwnPokemon: attack.targetsOwnPokemon ?? false
  };
}

function mapPokemonAbility(ability: GameBoardAbility): BoardPokemonAbilityViewModel {
  return {
    id: ability.abilityId ?? ability.name ?? 'ability',
    label: ability.name ?? 'Habilidad',
    activationType: ability.activationType ?? null,
    deckCardOptions: (ability.deckCardOptions ?? []).map((card) => ({
      cardInstanceId: card.cardInstanceId ?? card.cardId ?? card.externalId ?? '',
      label: card.name ?? 'Carta',
      externalId: card.externalId,
      cardId: card.cardId,
      number: card.number,
      imageSmallUrl: card.imageSmallUrl,
      imageLargeUrl: card.imageLargeUrl
    })).filter((option) => option.cardInstanceId),
    enabled: ability.available,
    disabledReason: ability.disabledReason
  };
}

function pendingAbility(): BoardPokemonAbilityViewModel[] {
  return [
    {
      id: GameActionType.UseAbility,
      label: GameActionType.UseAbility,
      enabled: false,
      disabledReason: null
    }
  ];
}

function referencesFor(snapshot: GameSnapshot, playerId: string, zoneName: CardZone): string[] {
  return Object.entries(snapshot.board.zoneByCardReferenceId)
    .filter(([referenceId, cardZone]) =>
      cardZone === zoneName && snapshot.board.ownerByCardReferenceId[referenceId] === playerId
    )
    .map(([referenceId]) => referenceId);
}

function mapAction(
  actionType: GameActionType,
  enabled: boolean,
  disabledReason: string | null = null,
  options: MapGameSnapshotOptions = { localPlayerId: null }
): BoardActionViewModel {
  return {
    actionType,
    label: ACTION_LABEL_KEYS[actionType]
      ? translate(options, ACTION_LABEL_KEYS[actionType])
      : actionType.replaceAll('_', ' ').toLowerCase(),
    enabled,
    disabledReason,
    payload: {}
  };
}

function mapRootActions(
  snapshot: GameSnapshot,
  localPlayer: BoardPlayerViewModel,
  actionHints: VisibleBoardActionHintsDto,
  options: MapGameSnapshotOptions
): BoardActionViewModel[] {
  const availableActions = snapshot.actions.availableActions.filter(
    (actionType) => actionType !== GameActionType.SelectTarget
  );
  const actions = new Map<GameActionType, BoardActionViewModel>();

  for (const actionType of availableActions) {
    const enabled = isActionEnabledForPlayer(snapshot, localPlayer.id, actionType);
    const disabledReason = enabled ? null : resolutionDisabledReason(snapshot, options);

    actions.set(actionType, mapAction(actionType, enabled, disabledReason, options));
  }

  const disabledCandidates: Array<[GameActionType, string | null, boolean]> = [
    [
      GameActionType.PlayBasicPokemon,
      firstCardReason(localPlayer.handCards, GameActionType.PlayBasicPokemon) ??
        translate(options, 'GAME.DISABLED.NO_BASIC'),
      localPlayer.handCards.some((card) => card.suggestedAction === GameActionType.PlayBasicPokemon)
    ],
    [
      GameActionType.EvolvePokemon,
      firstCardReason(localPlayer.handCards, GameActionType.EvolvePokemon) ??
        translate(options, 'GAME.DISABLED.NO_EVOLUTIONS'),
      localPlayer.handCards.some((card) => card.suggestedAction === GameActionType.EvolvePokemon)
    ],
    [
      GameActionType.AttachEnergy,
      firstCardReason(localPlayer.handCards, GameActionType.AttachEnergy) ??
        (actionHints.attachEnergyTargetPokemonInPlayIds.length === 0
          ? translate(options, 'GAME.DISABLED.NO_ENERGY_TARGETS')
          : translate(options, 'GAME.DISABLED.ENERGY_NOT_YET')),
      hasCandidate(localPlayer.handCards, GameActionType.AttachEnergy) ||
        actionHints.attachEnergyTargetPokemonInPlayIds.length > 0
    ],
    [
      GameActionType.PlayTrainer,
      firstCardReason(localPlayer.handCards, GameActionType.PlayTrainer) ??
        translate(options, 'GAME.DISABLED.NO_TRAINERS'),
      hasCandidate(localPlayer.handCards, GameActionType.PlayTrainer) ||
        actionHints.trainerTargetPokemonInPlayIds.length > 0 ||
        actionHints.trainerToolTargetPokemonInPlayIds.length > 0
    ],
    [
      GameActionType.Retreat,
      localPlayer.activePokemon.pokemon?.actions.find((action) => action.actionType === GameActionType.Retreat)
        ?.disabledReason ??
        (actionHints.retreatTargetPokemonInPlayIds.length === 0
          ? translate(options, 'GAME.DISABLED.NO_RETREAT_TARGETS')
          : translate(options, 'GAME.DISABLED.RETREAT_NOT_YET')),
      localPlayer.activePokemon.occupied
    ],
    [
      GameActionType.PromoteBenchPokemon,
      snapshot.resolution?.resolutionType === GameResolutionType.PromotionRequired
        ? translate(options, 'GAME.DISABLED.PROMOTION_REQUIRED')
        : actionHints.promoteTargetPokemonInPlayIds.length === 0
          ? translate(options, 'GAME.DISABLED.NO_PROMOTE_TARGETS')
          : translate(options, 'GAME.DISABLED.NO_PROMOTION_NOW'),
      localPlayer.benchSlots.some((slot) => slot.occupied) ||
        snapshot.resolution?.resolutionType === GameResolutionType.PromotionRequired
    ],
    [
      GameActionType.DeclareAttack,
      firstAttackReason(localPlayer) ?? translate(options, 'GAME.DISABLED.NO_ATTACKS_ENABLED'),
      (localPlayer.activePokemon.pokemon?.attacks.length ?? 0) > 0
    ]
  ];

  for (const [actionType, disabledReason, shouldExpose] of disabledCandidates) {
    if (!shouldExpose || actions.has(actionType)) {
      continue;
    }

    actions.set(actionType, mapAction(actionType, false, disabledReason, options));
  }

  if (!actions.has(GameActionType.UseAbility)) {
    actions.set(GameActionType.UseAbility, mapAction(GameActionType.UseAbility, false, null, options));
  }

  if (!actions.has(GameActionType.Concede)) {
    actions.set(GameActionType.Concede, mapAction(GameActionType.Concede, false, translate(options, 'GAME.DISABLED.CONCEDE_PHASE_2'), options));
  }

  return Array.from(actions.values());
}

function isActionEnabledForPlayer(
  snapshot: GameSnapshot,
  playerId: string,
  actionType: GameActionType
): boolean {
  if (
    snapshot.status === GameStatus.Paused ||
    snapshot.status === GameStatus.Finished ||
    snapshot.status === GameStatus.Cancelled
  ) {
    return false;
  }

  const playerState = localPlayerState(snapshot, playerId);
  if (actionType === GameActionType.AckMulliganNotice) {
    return playerState.mulliganNoticePending;
  }

  if (
    actionType === GameActionType.ChooseInitialPokemon &&
    (playerState.mulliganNoticePending ||
      playerState.mulliganFlowActive ||
      !playerState.mulliganReadyForInitialSelection)
  ) {
    return false;
  }

  if (!snapshot.resolution?.resolutionType) {
    return true;
  }

  if (snapshot.resolution.resolutionType === GameResolutionType.AttackChoiceRequired) {
    return (
      snapshot.resolution.pendingChoicePlayerId === playerId &&
      actionType === GameActionType.ResolveAttackChoice
    );
  }

  return (
    snapshot.resolution.resolutionType === GameResolutionType.PromotionRequired &&
    snapshot.resolution.playerToPromoteId === playerId &&
    actionType === GameActionType.PromoteBenchPokemon
  );
}

function resolutionDisabledReason(
  snapshot: GameSnapshot,
  options: MapGameSnapshotOptions
): string | null {
  if (
    snapshot.status === GameStatus.Paused ||
    snapshot.status === GameStatus.Finished ||
    snapshot.status === GameStatus.Cancelled
  ) {
    return translate(options, 'GAME.DISABLED.GAME_BLOCKED');
  }

  if (!snapshot.resolution?.resolutionType) {
    return null;
  }

  if (snapshot.resolution.resolutionType === GameResolutionType.AttackChoiceRequired) {
    return translate(options, 'GAME.DISABLED.ATTACK_CHOICE_REQUIRED');
  }

  return snapshot.resolution.resolutionType === GameResolutionType.PromotionRequired
    ? translate(options, 'GAME.DISABLED.PROMOTION_REQUIRED')
    : translate(options, 'GAME.PENDING_RESOLUTION');
}

function hasCandidate(cards: BoardCardViewModel[], actionType: GameActionType): boolean {
  return cards.some((card) => cardCanBeSelectedForAction(card, actionType));
}

function mapActionHints(actionHints: VisibleBoardActionHintsDto): BoardGameViewModel['actionHints'] {
  return {
    attachEnergyTargetPokemonInPlayIds: [...actionHints.attachEnergyTargetPokemonInPlayIds],
    retreatTargetPokemonInPlayIds: [...actionHints.retreatTargetPokemonInPlayIds],
    promoteTargetPokemonInPlayIds: [...actionHints.promoteTargetPokemonInPlayIds],
    trainerTargetPokemonInPlayIds: [...actionHints.trainerTargetPokemonInPlayIds],
    trainerToolTargetPokemonInPlayIds: [...actionHints.trainerToolTargetPokemonInPlayIds]
  };
}

function firstCardReason(cards: BoardCardViewModel[], actionType: GameActionType): string | null {
  return cards.find((card) => card.suggestedAction === actionType && card.disabledReason)?.disabledReason ?? null;
}

function firstAttackReason(player: BoardPlayerViewModel): string | null {
  return player.activePokemon.pokemon?.attacks.find((attack) => !attack.enabled)?.disabledReason ?? null;
}

function mapStadium(boardView: VisibleBoardDto, options: MapGameSnapshotOptions): BoardGameViewModel['stadium'] {
  if (!boardView.stadium) {
    return null;
  }

  return {
    label: translate(options, 'GAME.STADIUM'),
    card: boardView.stadium.card
      ? boardCard(
          mapVisibleCard(boardView.stadium.card, CardZone.Stadium, 0),
          'stadium-card',
          translate(options, 'GAME.STADIUM'),
          false
        )
      : null,
    playedByPlayerId: boardView.stadium.playedByPlayerId,
    playedByLabel: boardView.stadium.playedByPlayerId
      ? labelForPlayer(boardView.stadium.playedByPlayerId, options)
      : null
  };
}

function mapEventFeed(
  events: GameEvent[],
  options: MapGameSnapshotOptions
): BoardGameViewModel['eventFeed'] {
  return [...events]
    .filter((event) => DISPLAYED_GAME_EVENT_TYPES.has(event.eventType) && !hasTechnicalHistoryPattern(event))
    .sort((left, right) => right.occurredAt.localeCompare(left.occurredAt))
    .map((event) => {
      const label = buildEventLabel(event, options);
      return {
        id: event.eventId,
        eventType: event.eventType,
        label,
        occurredAt: event.occurredAt,
        stateVersion: event.stateVersion,
        privateEvent: event.privateEvent
      };
    })
    .filter((event) => !hasTechnicalText(event.label));
}

function hasTechnicalHistoryPattern(event: GameEvent): boolean {
  const payloadText = Object.values(event.payload ?? {})
    .filter((value): value is string => typeof value === 'string')
    .join(' ');

  return hasTechnicalText(`${event.eventType} ${payloadText}`);
}

function hasTechnicalText(text: string): boolean {
  const normalizedText = text.toLowerCase();
  return TECHNICAL_HISTORY_PATTERNS.some((pattern) => normalizedText.includes(pattern));
}

function buildHistorySummary(eventFeed: BoardGameViewModel['eventFeed'], options: MapGameSnapshotOptions): string {
  if (eventFeed.length === 0) {
    return translate(options, 'GAME.EVENTS.SUMMARY_EMPTY');
  }

  return translate(options, 'GAME.EVENTS.SUMMARY');
}

function buildEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const actorId = readStringPayload(event, 'actorPlayerId') ?? readStringPayload(event, 'playerId');
  const actorLabel = actorId ? labelForPlayer(actorId, options) : null;
  const subject = actorLabel ? `${actorLabel}: ` : '';

  switch (event.eventType) {
    case GameEventType.TurnStarted:
      return translate(options, 'GAME.EVENTS.TURN_STARTED', { subject });
    case GameEventType.CardDrawn: {
      const revealedEnergies = readRevealedEnergies(event);
      if (revealedEnergies.length > 0) {
        return translate(options, 'GAME.EVENTS.BASIC_ENERGIES_REVEALED', {
          subject,
          cards: revealedEnergies.map((energy) => `${energy.name} x${energy.count}`).join(', ')
        });
      }
      return translate(options, 'GAME.EVENTS.CARD_DRAWN', { subject });
    }
    case GameEventType.CardPlayed:
      return translate(options, 'GAME.EVENTS.CARD_PLAYED', { subject, card: eventCardLabel(event, options) });
    case GameEventType.TrainerPlayed:
      return translate(options, 'GAME.EVENTS.TRAINER_PLAYED', { subject, card: eventCardLabel(event, options) });
    case GameEventType.EnergyAttached:
      return translate(options, 'GAME.EVENTS.ENERGY_ATTACHED', { subject });
    case GameEventType.PokemonEvolved:
      return translate(options, 'GAME.EVENTS.POKEMON_EVOLVED', { subject });
    case GameEventType.RetreatDone:
      return translate(options, 'GAME.EVENTS.RETREAT_DONE', { subject });
    case GameEventType.AttackDeclared:
      return translate(options, 'GAME.EVENTS.ATTACK_DECLARED', { subject });
    case GameEventType.AttackChoiceResolved:
      return translate(options, 'GAME.EVENTS.ATTACK_CHOICE_RESOLVED', { subject });
    case GameEventType.DamageApplied:
      return translate(options, 'GAME.EVENTS.DAMAGE_APPLIED', {
        subject,
        damage: readNumberPayload(event, 'damage') ?? 0
      });
    case GameEventType.StatusApplied:
      return statusAppliedLabel(event, options, subject);
    case GameEventType.PokemonKnockedOut:
      return translate(options, 'GAME.EVENTS.POKEMON_KNOCKED_OUT', {
        subject,
        reason: readStringPayload(event, 'reason')
      });
    case GameEventType.PokemonPromoted:
      return translate(options, 'GAME.EVENTS.POKEMON_PROMOTED', { subject });
    case GameEventType.PrizeTaken:
      return translate(options, 'GAME.EVENTS.PRIZE_TAKEN', {
        subject,
        prizeCount: readNumberPayload(event, 'prizeCount'),
        remainingPrizeCards: readNumberPayload(event, 'remainingPrizeCards')
      });
    case GameEventType.GameFinished:
      return translate(options, 'GAME.EVENTS.GAME_FINISHED', { subject });
    case GameEventType.OpeningHandsDealt:
      return translate(options, 'GAME.EVENTS.OPENING_HANDS_DEALT', { subject });
    case GameEventType.MulliganHandRevealed:
      return buildMulliganEventLabel(event, options);
    case GameEventType.MulliganRequired:
      return buildMulliganRequiredEventLabel(event, options);
    case GameEventType.MulliganHandReturned:
      return buildMulliganStepEventLabel(event, options, 'GAME.EVENTS.MULLIGAN_HAND_RETURNED');
    case GameEventType.MulliganDeckShuffled:
      return buildMulliganStepEventLabel(event, options, 'GAME.EVENTS.MULLIGAN_DECK_SHUFFLED');
    case GameEventType.MulliganNewHandDrawn:
      return buildMulliganNewHandDrawnEventLabel(event, options);
    case GameEventType.MulliganHandValidated:
      return buildMulliganHandValidatedEventLabel(event, options);
    case GameEventType.MulliganSequenceCompleted:
      return buildOwnMulliganEventLabel(event, options);
    case GameEventType.MulliganExtraCardsGranted:
      return buildMulliganExtraCardsGrantedEventLabel(event, options);
    case GameEventType.MulliganFlowCompleted:
      return buildMulliganFlowCompletedEventLabel(event, options);
    case GameEventType.MulliganNoticeAcknowledged:
      return buildMulliganNoticeAcknowledgedEventLabel(event, options);
    case GameEventType.InvalidAction:
      return translate(options, 'GAME.EVENTS.INVALID_ACTION', { subject });
    case GameEventType.AttackEffectResolved: {
      const coinResults = readCoinResultsPayload(event);
      if (!coinResults) {
        return translate(options, 'GAME.EVENTS.GAME_ACTION', { subject });
      }

      const headsCount = coinResults.filter((coinResult) => coinResult === 'HEADS').length;
      const resultsLabel = coinResults
        .map((coinResult) => translate(options, coinResult === 'HEADS' ? 'GAME.COIN.HEADS' : 'GAME.COIN.TAILS'))
        .join(', ');

      return coinResults.length === 1
        ? translate(options, 'GAME.EVENTS.COIN_FLIP_RESULT_SINGLE', { subject, result: resultsLabel })
        : translate(options, 'GAME.EVENTS.COIN_FLIP_RESULT_MULTIPLE', {
            subject,
            results: resultsLabel,
            headsCount,
            count: coinResults.length
          });
    }
    case GameEventType.CoinFlipped: {
      const coinResults = readCoinResultsPayload(event);
      if (!coinResults) {
        return translate(options, 'GAME.EVENTS.GAME_ACTION', { subject });
      }
      const headsCount = coinResults.filter((r) => r === 'HEADS').length;
      const resultsLabel = coinResults
        .map((r) => translate(options, r === 'HEADS' ? 'GAME.COIN.HEADS' : 'GAME.COIN.TAILS'))
        .join(', ');
      return coinResults.length === 1
        ? translate(options, 'GAME.EVENTS.COIN_FLIP_RESULT_SINGLE', { subject, result: resultsLabel })
        : translate(options, 'GAME.EVENTS.COIN_FLIP_RESULT_MULTIPLE', {
            subject,
            results: resultsLabel,
            headsCount,
            count: coinResults.length
          });
    }
    case GameEventType.PassiveAbilityTriggered:
      return passiveAbilityLabel(event, options, subject);
    default:
      return translate(options, 'GAME.EVENTS.GAME_ACTION', { subject });
  }
}

function eventCardLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  return readStringPayload(event, 'displayCardName') ??
    readStringPayload(event, 'cardName') ??
    readStringPayload(event, 'cardId') ??
    translate(options, 'GAME.CARD_FALLBACK');
}

function statusAppliedLabel(event: GameEvent, options: MapGameSnapshotOptions, subject: string): string {
  const condition = readStringPayload(event, 'conditionType');
  const conditionLabel = condition
    ? translate(options, `GAME.CONDITION.${condition}`)
    : translate(options, 'GAME.CONDITION.UNKNOWN');

  return readBooleanPayload(event, 'resolved')
    ? translate(options, 'GAME.EVENTS.STATUS_RESOLVED', { subject, condition: conditionLabel })
    : translate(options, 'GAME.EVENTS.STATUS_APPLIED', { subject, condition: conditionLabel });
}

function buildMulliganRequiredEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganRequiredPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_REQUIRED', { subject: '', number: 1 });
  }

  return translate(options, 'GAME.EVENTS.MULLIGAN_REQUIRED', {
    subject: `${labelForPlayer(payload.playerId, options)} `,
    number: payload.mulliganNumber
  });
}

function buildMulliganStepEventLabel(event: GameEvent, options: MapGameSnapshotOptions, key: string): string {
  const payload = readMulliganStepPayload(event.payload);
  if (!payload) {
    return translate(options, key, { subject: '', number: 1 });
  }

  return translate(options, key, {
    subject: `${labelForPlayer(payload.playerId, options)} `,
    number: payload.mulliganNumber
  });
}

function buildMulliganNewHandDrawnEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganNewHandDrawnPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_NEW_HAND_DRAWN', { subject: '', count: INITIAL_HAND_SIZE });
  }

  return translate(options, 'GAME.EVENTS.MULLIGAN_NEW_HAND_DRAWN', {
    subject: `${labelForPlayer(payload.playerId, options)} `,
    count: payload.cardsDrawn
  });
}

function buildMulliganHandValidatedEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganHandValidatedPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_HAND_VALIDATED_REPEAT', { subject: '', number: 1 });
  }

  const key = payload.hasBasic
    ? 'GAME.EVENTS.MULLIGAN_HAND_VALIDATED_READY'
    : 'GAME.EVENTS.MULLIGAN_HAND_VALIDATED_REPEAT';

  return translate(options, key, {
    subject: `${labelForPlayer(payload.playerId, options)} `,
    number: payload.mulliganNumber
  });
}

function buildMulliganExtraCardsGrantedEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganExtraCardsGrantedPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_EXTRA_CARDS_GRANTED', { subject: '', count: 0 });
  }

  return translate(options, 'GAME.EVENTS.MULLIGAN_EXTRA_CARDS_GRANTED', {
    subject: `${labelForPlayer(payload.playerId, options)} `,
    count: payload.cardsGranted
  });
}

function buildMulliganFlowCompletedEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganFlowCompletedPayload(event.payload);
  return translate(options, 'GAME.EVENTS.MULLIGAN_FLOW_COMPLETED', {
    round: payload.roundNumber
  });
}

function buildMulliganEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganHandRevealedPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_HAND_REVEALED', {
      subject: '',
      number: 1,
      extraCards: 1
    });
  }

  return translate(options, 'GAME.EVENTS.MULLIGAN_HAND_REVEALED', {
    subject: `${labelForPlayer(payload.revealingPlayerId, options)} `,
    number: payload.mulliganNumber,
    extraCards: payload.extraCardsGranted
  });
}

function buildOwnMulliganEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganSequenceCompletedPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_SEQUENCE_COMPLETED', {
      subject: '',
      count: 1,
      extraCards: 1
    });
  }

  return translate(options, 'GAME.EVENTS.MULLIGAN_SEQUENCE_COMPLETED', {
    subject: '',
    count: payload.mulliganCount,
    extraCards: payload.extraCardsGrantedToOpponent
  });
}

function buildMulliganNoticeAcknowledgedEventLabel(event: GameEvent, options: MapGameSnapshotOptions): string {
  const payload = readMulliganNoticeAcknowledgedPayload(event.payload);
  if (!payload) {
    return translate(options, 'GAME.EVENTS.MULLIGAN_NOTICE_ACKNOWLEDGED', {
      subject: ''
    });
  }

  return translate(options, 'GAME.EVENTS.MULLIGAN_NOTICE_ACKNOWLEDGED', {
    subject: `${labelForPlayer(payload.playerId, options)} `
  });
}

function mapMulliganSetupEvents(
  events: GameEvent[],
  options: MapGameSnapshotOptions
): BoardGameViewModel['mulliganSetupEvents'] {
  // The panel is intentionally minimal: it shows only the rival's revealed (invalid) hand and the
  // own-summary (Mulligan counter + extra cards). All technical per-step events are dropped — the
  // automatic flow communicates them through animations and floating banners, not log lines.
  return [...events]
    .filter((event) =>
      event.eventType === GameEventType.MulliganHandRevealed ||
      event.eventType === GameEventType.MulliganSequenceCompleted
    )
    .sort((left, right) => right.occurredAt.localeCompare(left.occurredAt))
    .map((event) => {
      if (event.eventType === GameEventType.MulliganHandRevealed) {
        const payload = readMulliganHandRevealedPayload(event.payload);
        if (!payload) {
          return null;
        }

        return {
          kind: 'revealed-hand' as const,
          id: event.eventId,
          revealingPlayerId: payload.revealingPlayerId,
          revealingPlayerLabel: labelForPlayer(payload.revealingPlayerId, options),
          mulliganNumber: payload.mulliganNumber,
          mulliganCount: payload.mulliganCount,
          revealedCards: payload.revealedCards,
          revealedCardPlaceholderCount: payload.revealedCards.length > 0 ? 0 : payload.revealedCardIds.length,
          extraCardsGranted: payload.extraCardsGranted,
          pendingExtraCardsForViewer: payload.pendingExtraCardsForViewer,
          occurredAt: event.occurredAt
        };
      }

      const payload = readMulliganSequenceCompletedPayload(event.payload);
      if (!payload) {
        return null;
      }

      return {
        kind: 'own-summary' as const,
        id: event.eventId,
        playerId: payload.playerId,
        playerLabel: labelForPlayer(payload.playerId, options),
        mulliganCount: payload.mulliganCount,
        extraCardsGrantedToOpponent: payload.extraCardsGrantedToOpponent,
        opponentPlayerId: payload.opponentPlayerId,
        opponentPlayerLabel: labelForPlayer(payload.opponentPlayerId, options),
        automatic: payload.automatic,
        occurredAt: event.occurredAt
      };
    })
    .filter(isDefined);
}

function isMulliganStepEvent(eventType: GameEventType): boolean {
  return eventType === GameEventType.MulliganRequired ||
    eventType === GameEventType.MulliganHandReturned ||
    eventType === GameEventType.MulliganDeckShuffled ||
    eventType === GameEventType.MulliganNewHandDrawn ||
    eventType === GameEventType.MulliganHandValidated ||
    eventType === GameEventType.MulliganExtraCardsGranted ||
    eventType === GameEventType.MulliganFlowCompleted ||
    eventType === GameEventType.MulliganNoticeAcknowledged;
}

function mapMulliganStepSetupEvent(
  event: GameEvent,
  options: MapGameSnapshotOptions
): BoardGameViewModel['mulliganSetupEvents'][number] | null {
  const label = buildEventLabel(event, options);
  const step = readMulliganStepPayload(event.payload);
  const acknowledged = event.eventType === GameEventType.MulliganNoticeAcknowledged
    ? readMulliganNoticeAcknowledgedPayload(event.payload)
    : null;
  const required = event.eventType === GameEventType.MulliganRequired
    ? readMulliganRequiredPayload(event.payload)
    : null;
  const extra = event.eventType === GameEventType.MulliganExtraCardsGranted
    ? readMulliganExtraCardsGrantedPayload(event.payload)
    : null;
  const completed = event.eventType === GameEventType.MulliganFlowCompleted
    ? readMulliganFlowCompletedPayload(event.payload)
    : null;
  const playerId = step?.playerId ?? acknowledged?.playerId ?? required?.playerId ?? extra?.playerId ?? null;
  const roundNumber = step?.roundNumber ?? acknowledged?.roundNumber ?? required?.roundNumber ?? completed?.roundNumber ?? 0;
  const mulliganNumber = step?.mulliganNumber ?? required?.mulliganNumber ?? null;

  return {
    kind: 'step',
    id: event.eventId,
    eventType: event.eventType,
    playerId,
    playerLabel: playerId ? labelForPlayer(playerId, options) : null,
    label,
    roundNumber,
    mulliganNumber,
    status: mulliganStepStatus(event),
    occurredAt: event.occurredAt
  };
}

function mulliganStepStatus(event: GameEvent): 'info' | 'success' | 'warning' {
  if (
    event.eventType === GameEventType.MulliganRequired ||
    event.eventType === GameEventType.MulliganHandValidated
  ) {
    const validation = event.eventType === GameEventType.MulliganHandValidated
      ? readMulliganHandValidatedPayload(event.payload)
      : null;
    return validation?.hasBasic ? 'success' : 'warning';
  }

  if (
    event.eventType === GameEventType.MulliganFlowCompleted ||
    event.eventType === GameEventType.MulliganExtraCardsGranted ||
    event.eventType === GameEventType.MulliganNoticeAcknowledged
  ) {
    return 'success';
  }

  return 'info';
}

function passiveAbilityLabel(event: GameEvent, options: MapGameSnapshotOptions, subject: string): string {
  const abilityId = readStringPayload(event, 'abilityId') ?? 'PASSIVE_ABILITY';
  const damageCounters = readNumberPayload(event, 'damageCounters') ?? 0;
  const coinFlip = readStringPayload(event, 'coinFlip');
  const abilityLabel = abilityId.replaceAll('_', ' ').toLowerCase();

  if (coinFlip) {
    const coinLabel = translate(options, coinFlip === 'HEADS' ? 'GAME.COIN.HEADS' : 'GAME.COIN.TAILS');
    return translate(options, 'GAME.EVENTS.PASSIVE_ABILITY_TRIGGERED_WITH_COIN', {
      subject,
      ability: abilityLabel,
      result: coinLabel,
      damageCounters
    });
  }

  return translate(options, 'GAME.EVENTS.PASSIVE_ABILITY_TRIGGERED', {
    subject,
    ability: abilityLabel,
    damageCounters
  });
}

function readStringPayload(event: GameEvent, key: string): string | null {
  const value = event.payload[key];
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function readNumberPayload(event: GameEvent, key: string): number | null {
  const value = event.payload[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function readBooleanPayload(event: GameEvent, key: string): boolean {
  return event.payload[key] === true;
}

function readRevealedEnergies(event: GameEvent): { name: string; count: number }[] {
  const rawRevealedEnergies = event.payload['revealedEnergies'];
  if (!Array.isArray(rawRevealedEnergies)) {
    return [];
  }

  return rawRevealedEnergies.flatMap((rawEnergy) => {
    if (!rawEnergy || typeof rawEnergy !== 'object') {
      return [];
    }

    const energy = rawEnergy as Record<string, unknown>;
    const name = energy['name'];
    const count = energy['count'];
    if (typeof name !== 'string' || typeof count !== 'number' || !Number.isFinite(count) || count <= 0) {
      return [];
    }

    return [{ name, count }];
  });
}

function readCoinResultsPayload(event: GameEvent): string[] | null {
  const payloadCoinResults = event.payload['coinResults'];
  return Array.isArray(payloadCoinResults) &&
    payloadCoinResults.length > 0 &&
    payloadCoinResults.every((coinResult) => typeof coinResult === 'string')
    ? payloadCoinResults
    : null;
}

const LOOK_OPPONENT_DECK_TOP_CARD_EFFECT_TYPE = 'LOOK_OPPONENT_DECK_TOP_CARD_OPTIONAL_SHUFFLE';

function mapResolution(
  snapshot: GameSnapshot,
  options: MapGameSnapshotOptions
): BoardResolutionViewModel | null {
  const resolution = snapshot.resolution;

  if (!resolution?.resolutionType) {
    return null;
  }

  return {
    resolutionType: resolution.resolutionType,
    label: resolutionLabel(resolution.resolutionType, resolution.playerToPromoteId, options),
    playerToPromoteId: resolution.playerToPromoteId,
    nextActivePlayerId: resolution.nextActivePlayerId,
    nextTurnNumber: resolution.nextTurnNumber || null,
    pendingChoicePlayerId: resolution.pendingChoicePlayerId ?? null,
    pendingChoiceType: resolution.pendingChoiceType ?? null,
    pendingChoicePayload: resolution.pendingChoicePayload ?? null,
    revealedCardId: resolution.resolutionType === GameResolutionType.AttackChoiceRequired
      ? revealedOpponentDeckTopCardId(options.eventFeed ?? [])
      : null
  };
}

function revealedOpponentDeckTopCardId(eventFeed: GameEvent[]): string | null {
  for (let index = eventFeed.length - 1; index >= 0; index--) {
    const event = eventFeed[index];
    if (
      event.eventType === GameEventType.AttackEffectResolved &&
      event.payload?.['effectType'] === LOOK_OPPONENT_DECK_TOP_CARD_EFFECT_TYPE &&
      typeof event.payload?.['revealedCardId'] === 'string'
    ) {
      return event.payload['revealedCardId'] as string;
    }
  }

  return null;
}

function resolutionLabel(
  resolutionType: GameResolutionType,
  playerToPromoteId: string | null,
  options: MapGameSnapshotOptions
): string {
  switch (resolutionType) {
    case GameResolutionType.PromotionRequired:
      return playerToPromoteId
        ? translate(options, 'GAME.RESOLUTION.PROMOTE_PLAYER', { player: labelForPlayer(playerToPromoteId, options) })
        : translate(options, 'GAME.RESOLUTION.PROMOTION_REQUIRED');
    case GameResolutionType.SuddenDeathRequired:
      return translate(options, 'GAME.RESOLUTION.SUDDEN_DEATH');
    case GameResolutionType.AttackChoiceRequired:
      return translate(options, 'GAME.RESOLUTION.ATTACK_CHOICE_REQUIRED');
  }
}

function buildStatusSummary(
  snapshot: GameSnapshot,
  phaseLabel: string,
  activePlayerLabel: string | null,
  options: MapGameSnapshotOptions
): string {
  if (snapshot.status === GameStatus.Active && snapshot.actions.availableActions.includes(GameActionType.DrawCard)) {
    return translate(options, 'GAME.STATUS_SUMMARY.DRAW');
  }

  if (snapshot.status === GameStatus.Active && activePlayerLabel) {
    return translate(options, 'GAME.STATUS_SUMMARY.ACTIVE', {
      turn: snapshot.turn.turnNumber,
      phase: phaseLabel,
      player: activePlayerLabel
    });
  }

  if (snapshot.status === GameStatus.Setup) {
    return translate(options, 'GAME.STATUS_SUMMARY.SETUP');
  }

  return translate(options, STATUS_LABEL_KEYS[snapshot.status]);
}

function buildMissingDataNotes(snapshot: GameSnapshot, options: MapGameSnapshotOptions): string[] {
  const notes: string[] = [];
  const boardPlayers = Object.values(snapshot.boardPlayers ?? {});
  const boardView = snapshot.board.view ?? null;
  const usesLegacyFallback = !snapshot.boardPlayers && !boardView;

  if (usesLegacyFallback) {
    notes.push(translate(options, 'GAME.NO_DATA_LEGACY'));
  }

  if (boardPlayers.length > 0 && boardPlayers.every((player) => !player.activePokemon && player.benchPokemon.length === 0)) {
    notes.push(translate(options, 'GAME.NO_POKEMON_DATA'));
  }
  if (boardPlayers.length > 0 && boardPlayers.every((player) => player.discard.count === 0)) {
    notes.push(translate(options, 'GAME.NO_DISCARD_DATA'));
  }
  if ((boardPlayers.length > 0 && boardPlayers.every((player) => player.stadium.count === 0)) && !boardView?.stadium?.card) {
    notes.push(translate(options, 'GAME.NO_STADIUM_DATA'));
  }
  if (boardPlayers.every((player) => [
    player.activePokemon,
    ...player.benchPokemon
  ].filter((pokemon): pokemon is GameBoardPokemon => pokemon !== null).every((pokemon) => pokemon.abilities.length === 0))) {
    notes.push(translate(options, 'GAME.NO_ABILITIES_DATA'));
  }

  return notes;
}

function labelForPlayer(playerId: string, options: MapGameSnapshotOptions): string {
  return options.playerLabels?.[playerId] ?? playerId;
}

function localPlayerState(snapshot: GameSnapshot, playerId: string): GameSnapshotPlayerState {
  return snapshot.players[playerId] ?? emptyPlayerState();
}

function emptyPlayerState(): GameSnapshotPlayerState {
  return {
    benchPokemonCount: 0,
    activePokemonConditions: [],
    cardIdsInHand: [],
    cardInstanceIdsInHand: [],
    affordableAttackIds: [],
    mulliganCount: 0,
    mulliganNoticePending: false,
    mulliganFlowActive: false,
    mulliganReadyForInitialSelection: true,
    mulliganRoundNumber: 0,
    mulliganCurrentPlayer: false,
    initialPokemonSelectionSubmitted: false,
    initialActiveCardInstanceId: null,
    initialBenchCardInstanceIds: []
  };
}

function handCount(cardInstanceIds: string[] | undefined, cardIds: string[] | undefined): number {
  return cardInstanceIds && cardInstanceIds.length > 0 ? cardInstanceIds.length : cardIds?.length ?? 0;
}

function opponentHandCount(
  snapshot: GameSnapshot,
  playerId: string,
  playerState: GameSnapshot['players'][string] | undefined
): number {
  const sanitizedHandCount = handCount(playerState?.cardInstanceIdsInHand, playerState?.cardIdsInHand);
  if (sanitizedHandCount > 0) {
    return sanitizedHandCount;
  }

  const boardHandCount = referencesFor(snapshot, playerId, CardZone.Hand).length;
  if (boardHandCount > 0) {
    return boardHandCount;
  }

  if (snapshot.status !== GameStatus.Setup) {
    return 0;
  }

  const localPlayerId = snapshot.playerIds.find((snapshotPlayerId) => snapshotPlayerId !== playerId);
  const localMulliganCount = localPlayerId ? snapshot.players[localPlayerId]?.mulliganCount ?? 0 : 0;

  return INITIAL_HAND_SIZE + localMulliganCount;
}
