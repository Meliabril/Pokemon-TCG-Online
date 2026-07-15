import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { BoardCardViewModel, BoardGameViewModel, BoardPokemonViewModel } from '../board/board-game-view-model.interface';
import {
  GameBlockingOverlayViewModel,
  GameInteractionContext,
  GameInteractionViewModel
} from './game-interaction-view-model.interface';
import { GameFinalOutcome } from '../result/game-final-outcome.type';
import { GameScreenState } from '../ui-state/game-screen-state.type';

const BASIC_POKEMON_CATEGORY = 'BASIC_POKEMON';
const BASIC_ENERGY_CATEGORY = 'BASIC_ENERGY';
const SPECIAL_ENERGY_CATEGORY = 'SPECIAL_ENERGY';
const TRAINER_CATEGORIES = new Set([
  'ITEM_TRAINER',
  'ACE_SPEC_TRAINER',
  'SUPPORTER_TRAINER',
  'STADIUM_TRAINER',
  'POKEMON_TOOL_TRAINER'
]);
const EVOLUTION_CATEGORIES = new Set(['STAGE_1_POKEMON', 'STAGE_2_POKEMON', 'MEGA_POKEMON']);
const BASIC_STAGE_SUBTYPE = 'Basic';

// category collapses stage + EX/Mega specialness into one value (a Basic
// Pokemon-EX is reported as BASIC_POKEMON_CATEGORY-less POKEMON_EX), so a
// Basic-stage Pokemon-EX must be detected via subtype instead of category.
// "Basic" is also reused as the subtype for Basic Energy cards, so energy
// categories must be excluded or an Energy card would be treated as a
// playable Basic Pokemon.
export function isBasicStagePokemon(card: BoardCardViewModel): boolean {
  if (card.category === BASIC_ENERGY_CATEGORY || card.category === SPECIAL_ENERGY_CATEGORY) {
    return false;
  }
  return card.category === BASIC_POKEMON_CATEGORY || card.subtype === BASIC_STAGE_SUBTYPE;
}

export function buildGameInteractionViewModel(
  context: GameInteractionContext
): GameInteractionViewModel {
  const { board, commandState, screenState, actionPending, awaitingSnapshot } = context;
  const activeAction = resolveActiveAction(screenState, commandState.pendingAction);
  const selectedCard =
    commandState.selectedCardInstanceId ? findHandCard(board, commandState.selectedCardInstanceId) : null;
  const selectedAttack =
    commandState.selectedAttackId && board.localPlayer.activePokemon.pokemon
      ? board.localPlayer.activePokemon.pokemon.attacks.find((attack) => attack.id === commandState.selectedAttackId) ??
        null
      : null;
  const selectableCardInstanceIds = resolveSelectableCardIds(board, screenState, activeAction, selectedAttack);
  const selectablePokemonInPlayIds = resolveSelectablePokemonIds(
    board,
    activeAction,
    selectedCard,
    selectedAttack,
    commandState.selectedAttackId
  );
  const helperBadges = resolveHelperBadges(board, commandState, selectedCard, context.t);

  return {
    pendingAction: activeAction,
    prompt: resolvePrompt(context, activeAction, selectedCard, commandState.selectedAttackId),
    confirmLabel: resolveConfirmLabel(screenState, activeAction, context.t),
    canConfirm:
      !actionPending && !awaitingSnapshot && canConfirm(board, screenState, activeAction, commandState, selectedCard),
    awaitingSnapshot,
    actionButtons: board.actions,
    attackOptions:
      activeAction === GameActionType.DeclareAttack ? resolveAttackOptions(board, commandState.selectedAttackId) : [],
    selectableCardInstanceIds,
    selectablePokemonInPlayIds,
    selectedCardInstanceId: commandState.selectedCardInstanceId,
    selectedPokemonInPlayId: commandState.selectedPokemonInPlayId,
    selectedAttackId: commandState.selectedAttackId,
    optimisticEnergyAttachment: commandState.optimisticEnergyAttachment,
    setupActiveCardInstanceId: commandState.setupActiveCardInstanceId,
    setupBenchCardInstanceIds: commandState.setupBenchCardInstanceIds,
    helperBadges
  };
}

export function buildBlockingOverlay(
  board: BoardGameViewModel,
  screenState: GameScreenState,
  awaitingSnapshot: boolean,
  actionPending: boolean,
  t?: GameInteractionContext['t'],
  finalOutcome: GameFinalOutcome = null
): GameBlockingOverlayViewModel | null {
  void awaitingSnapshot;
  void actionPending;

  const translate = (key: string) => t?.(key) ?? key;

  switch (screenState) {
    case 'waiting-opponent':
    case 'setup-waiting-opponent':
      return null;
    case 'paused':
      return {
        title: translate('GAME.OVERLAY.PAUSED_TITLE'),
        message: translate('GAME.OVERLAY.PAUSED_MESSAGE'),
        tone: 'amber',
        variant: 'notice',
        eyebrow: '',
        showLeaveAction: false,
        leaveActionLabel: ''
      };
    case 'finished': {
      return {
        title: translate(
          finalOutcome === 'victory'
            ? 'GAME.STATUS.VICTORY'
            : finalOutcome === 'defeat'
              ? 'GAME.STATUS.DEFEAT'
              : 'GAME.OVERLAY.FINISHED_TITLE'
        ),
        message: translate(
          finalOutcome === 'victory'
            ? 'GAME.OVERLAY.VICTORY_MESSAGE'
            : finalOutcome === 'defeat'
              ? 'GAME.OVERLAY.DEFEAT_MESSAGE'
              : 'GAME.OVERLAY.FINISHED_MESSAGE'
        ),
        tone: finalOutcome === 'defeat' ? 'red' : 'green',
        variant: 'result',
        eyebrow: translate('GAME.OVERLAY.RESULT_EYEBROW'),
        showLeaveAction: true,
        leaveActionLabel: translate('GAME.OVERLAY.BACK_TO_MENU')
      };
    }
    case 'cancelled':
      return {
        title: translate('GAME.OVERLAY.CANCELLED_TITLE'),
        message: translate('GAME.OVERLAY.CANCELLED_MESSAGE'),
        tone: 'red',
        variant: 'result',
        eyebrow: translate('GAME.OVERLAY.RESULT_EYEBROW'),
        showLeaveAction: true,
        leaveActionLabel: translate('GAME.OVERLAY.BACK_TO_MENU')
      };
    case 'resolution-pending':
      return {
        title: translate('GAME.OVERLAY.RESOLUTION_TITLE'),
        message: board.resolution?.label ?? translate('GAME.PENDING_RESOLUTION'),
        tone: 'amber',
        variant: 'notice',
        eyebrow: '',
        showLeaveAction: false,
        leaveActionLabel: ''
      };
    case 'promotion-waiting-opponent':
    case 'promotion-selecting':
    case 'attack-choice-selecting':
    case 'attack-choice-waiting-opponent':
    case 'playing':
      return null;
    default:
      return null;
  }
}

export function cardReferenceId(card: BoardCardViewModel): string {
  return card.cardInstanceId ?? card.id;
}

export function cardSupportsAction(card: BoardCardViewModel, actionType: GameActionType): boolean {
  if (card.suggestedAction === actionType) {
    return true;
  }

  switch (actionType) {
    case GameActionType.PlayBasicPokemon:
      return isBasicStagePokemon(card);
    case GameActionType.AttachEnergy:
      return card.category === BASIC_ENERGY_CATEGORY || card.category === SPECIAL_ENERGY_CATEGORY;
    case GameActionType.EvolvePokemon:
      return typeof card.category === 'string' && EVOLUTION_CATEGORIES.has(card.category);
    case GameActionType.PlayTrainer:
      return typeof card.category === 'string' && TRAINER_CATEGORIES.has(card.category);
    default:
      return card.playable === true;
  }
}

export function cardCanBeSelectedForAction(card: BoardCardViewModel, actionType: GameActionType): boolean {
  if (card.visibility !== 'visible') {
    return false;
  }

  if (card.suggestedAction === actionType) {
    return true;
  }

  if (card.playable !== true) {
    return false;
  }

  return cardSupportsAction(card, actionType);
}

function resolveActiveAction(screenState: GameScreenState, pendingAction: GameActionType | null): GameActionType | null {
  if (isTerminalScreenState(screenState)) {
    return null;
  }

  if (screenState === 'setup-selecting') {
    return GameActionType.ChooseInitialPokemon;
  }

  if (screenState === 'promotion-selecting') {
    return GameActionType.PromoteBenchPokemon;
  }

  return pendingAction;
}

function resolveSelectableCardIds(
  board: BoardGameViewModel,
  screenState: GameScreenState,
  activeAction: GameActionType | null,
  selectedAttack: { validTargetPokemonInPlayIds?: string[] } | null
): string[] {
  if (isTerminalScreenState(screenState)) {
    return [];
  }

  if (screenState === 'setup-selecting') {
    return board.localPlayer.handCards
      .filter((card) => card.visibility === 'visible' && isBasicStagePokemon(card))
      .map((card) => cardReferenceId(card));
  }

  if (!activeAction) {
    return [];
  }

  if (activeAction === GameActionType.DeclareAttack && selectedAttack) {
    return [];
  }

  if (
    activeAction === GameActionType.PlayBasicPokemon ||
    activeAction === GameActionType.AttachEnergy ||
    activeAction === GameActionType.EvolvePokemon ||
    activeAction === GameActionType.PlayTrainer
  ) {
    return board.localPlayer.handCards
      .filter((card) => cardCanBeSelectedForAction(card, activeAction))
      .map((card) => cardReferenceId(card));
  }

  return [];
}

function resolveSelectablePokemonIds(
  board: BoardGameViewModel,
  activeAction: GameActionType | null,
  selectedCard: BoardCardViewModel | null,
  selectedAttack: { validTargetPokemonInPlayIds?: string[]; requiresTarget?: boolean } | null,
  selectedAttackId: string | null
): string[] {
  const localPokemon = collectPokemonIds(board.localPlayer.activePokemon.pokemon, board.localPlayer.benchSlots);
  const opponentPokemon = collectPokemonIds(board.rivalPlayer.activePokemon.pokemon, board.rivalPlayer.benchSlots);

  switch (activeAction) {
    case GameActionType.AttachEnergy:
      return (
        targetIdsFromCard(selectedCard) ??
        targetIdsFromActionHints(board, GameActionType.AttachEnergy, selectedCard) ??
        pokemonIdsByPredicate(board.localPlayer, (pokemon) => pokemon.canReceiveEnergy)
      );
    case GameActionType.EvolvePokemon:
      return targetIdsFromCard(selectedCard) ?? localPokemon;
    case GameActionType.PlayTrainer:
      return (
        targetIdsFromCard(selectedCard) ??
        targetIdsFromActionHints(board, GameActionType.PlayTrainer, selectedCard) ?? [
          ...pokemonIdsByPredicate(board.localPlayer, (pokemon) => pokemon.canReceiveTrainer),
          ...pokemonIdsByPredicate(board.rivalPlayer, (pokemon) => pokemon.canReceiveTrainer)
        ]
      );
    case GameActionType.Retreat:
      return (
        targetIdsFromActionHints(board, GameActionType.Retreat, selectedCard) ??
        pokemonIdsByPredicate(
          board.localPlayer,
          (pokemon) => pokemon.canRetreatTo && pokemon.id !== board.localPlayer.activePokemon.pokemon?.id
        )
      );
    case GameActionType.PromoteBenchPokemon:
      return (
        targetIdsFromActionHints(board, GameActionType.PromoteBenchPokemon, selectedCard) ??
        occupiedBenchPokemonIds(board.localPlayer) ??
        pokemonIdsByPredicate(
          board.localPlayer,
          (pokemon) => pokemon.canPromote && pokemon.id !== board.localPlayer.activePokemon.pokemon?.id
        )
      );
    case GameActionType.DeclareAttack:
      if (!selectedAttackId || !selectedAttack?.requiresTarget) {
        return [];
      }

      return selectedAttack.validTargetPokemonInPlayIds ?? opponentPokemon;
    default:
      return [];
  }
}

function resolveAttackOptions(board: BoardGameViewModel, selectedAttackId: string | null) {
  return (board.localPlayer.activePokemon.pokemon?.attacks ?? []).map((attack) => ({
    id: attack.id,
    label: attack.label,
    name: attack.name,
    displayName: attack.displayName,
    enabled: attack.enabled,
    selected: selectedAttackId === attack.id,
    requiresTarget: attack.requiresTarget ?? false,
    validTargetPokemonInPlayIds: attack.validTargetPokemonInPlayIds ?? [],
    disabledReason: attack.disabledReason ?? null
  }));
}

function resolvePrompt(
  context: GameInteractionContext,
  activeAction: GameActionType | null,
  selectedCard: BoardCardViewModel | null,
  selectedAttackId: string | null
): string {
  const { board, screenState } = context;
  const t = context.t ?? ((key: string) => key);

  if (screenState === 'setup-selecting') {
    return t('GAME.INTERACTION.SETUP_PROMPT');
  }

  switch (activeAction) {
    case GameActionType.PlayBasicPokemon:
      return selectedCard ? t('GAME.INTERACTION.PLAY_BASIC_SELECTED') : t('GAME.INTERACTION.PLAY_BASIC');
    case GameActionType.AttachEnergy:
      return selectedCard ? t('GAME.INTERACTION.ATTACH_SELECTED') : t('GAME.INTERACTION.ATTACH');
    case GameActionType.EvolvePokemon:
      return selectedCard ? t('GAME.INTERACTION.EVOLVE_SELECTED') : t('GAME.INTERACTION.EVOLVE');
    case GameActionType.PlayTrainer:
      return selectedCard ? t('GAME.INTERACTION.TRAINER_SELECTED') : t('GAME.INTERACTION.TRAINER');
    case GameActionType.Retreat:
      return t('GAME.INTERACTION.RETREAT');
    case GameActionType.PromoteBenchPokemon:
      return t('GAME.INTERACTION.PROMOTE');
    case GameActionType.DeclareAttack:
      return selectedAttackId ? t('GAME.INTERACTION.ATTACK_SELECTED') : t('GAME.INTERACTION.ATTACK');
    case GameActionType.DrawCard:
      return t('GAME.INTERACTION.DRAW');
    case GameActionType.EndTurn:
      return t('GAME.INTERACTION.END_TURN');
    default:
      return board.statusSummary;
  }
}

function resolveConfirmLabel(
  screenState: GameScreenState,
  activeAction: GameActionType | null,
  t: ((key: string) => string) = (key) => key
): string {
  if (screenState === 'setup-selecting') {
    return t('GAME.INTERACTION.SEND_SETUP');
  }

  switch (activeAction) {
    case GameActionType.EndTurn:
      return t('GAME.END_TURN');
    case GameActionType.DrawCard:
      return t('GAME.INTERACTION.DRAW_LABEL');
    case GameActionType.DeclareAttack:
      return t('GAME.INTERACTION.DECLARE_ATTACK');
    case GameActionType.PromoteBenchPokemon:
      return t('GAME.INTERACTION.PROMOTE_LABEL');
    default:
      return t('COMMON.CONFIRM');
  }
}

function canConfirm(
  board: BoardGameViewModel,
  screenState: GameScreenState,
  activeAction: GameActionType | null,
  commandState: GameInteractionContext['commandState'],
  selectedCard: BoardCardViewModel | null
): boolean {
  if (isTerminalScreenState(screenState)) {
    return false;
  }

  if (screenState === 'setup-selecting') {
    return typeof commandState.setupActiveCardInstanceId === 'string';
  }

  switch (activeAction) {
    case GameActionType.PlayBasicPokemon:
      return Boolean(selectedCard?.cardId);
    case GameActionType.AttachEnergy:
    case GameActionType.EvolvePokemon:
      return Boolean(selectedCard?.cardId && commandState.selectedPokemonInPlayId);
    case GameActionType.PlayTrainer:
      if (!selectedCard?.cardId) {
        return false;
      }

      return requiresPokemonTarget(board, selectedCard) ? Boolean(commandState.selectedPokemonInPlayId) : true;
    case GameActionType.Retreat:
    case GameActionType.PromoteBenchPokemon:
      return Boolean(commandState.selectedPokemonInPlayId);
    case GameActionType.DeclareAttack: {
      const selectedAttack = board.localPlayer.activePokemon.pokemon?.attacks.find(
        (attack) => attack.id === commandState.selectedAttackId
      );

      if (!selectedAttack?.enabled) {
        return false;
      }

      return selectedAttack.requiresTarget ? Boolean(commandState.selectedPokemonInPlayId) : true;
    }
    case GameActionType.DrawCard:
    case GameActionType.EndTurn:
      return true;
    default:
      return false;
  }
}

function isTerminalScreenState(screenState: GameScreenState): boolean {
  return screenState === 'finished' || screenState === 'paused' || screenState === 'cancelled';
}

function resolveHelperBadges(
  board: BoardGameViewModel,
  commandState: GameInteractionContext['commandState'],
  selectedCard: BoardCardViewModel | null,
  t?: GameInteractionContext['t']
): string[] {
  const badges: string[] = [];
  const translate = (key: string, params?: Record<string, string | number>) => t?.(key, params) ?? key;

  if (commandState.setupActiveCardInstanceId) {
    const activeCard = findHandCard(board, commandState.setupActiveCardInstanceId);
    if (activeCard) {
      badges.push(translate('GAME.INTERACTION.INITIAL_ACTIVE', { name: activeCard.label }));
    }
  }

  if (commandState.setupBenchCardInstanceIds.length > 0) {
    badges.push(translate('GAME.INTERACTION.INITIAL_BENCH', { count: commandState.setupBenchCardInstanceIds.length }));
  }

  if (selectedCard?.label) {
    badges.push(translate('GAME.INTERACTION.CARD_SELECTED', { name: selectedCard.label }));
  }

  return badges;
}

function collectPokemonIds(
  activePokemon: BoardGameViewModel['localPlayer']['activePokemon']['pokemon'] | null | undefined,
  benchSlots: BoardGameViewModel['localPlayer']['benchSlots']
): string[] {
  return [activePokemon?.id ?? null, ...benchSlots.map((slot) => slot.pokemon?.id ?? null)].filter(
    (pokemonId): pokemonId is string => typeof pokemonId === 'string'
  );
}

function pokemonIdsByPredicate(
  player: BoardGameViewModel['localPlayer'],
  predicate: (pokemon: BoardPokemonViewModel) => boolean
): string[] {
  const pokemon = [player.activePokemon.pokemon, ...player.benchSlots.map((slot) => slot.pokemon)].filter(
    (entry): entry is BoardPokemonViewModel => Boolean(entry)
  );

  return pokemon.filter(predicate).map((entry) => entry.id);
}

function occupiedBenchPokemonIds(player: BoardGameViewModel['localPlayer']): string[] | null {
  const occupiedIds = player.benchSlots
    .map((slot) => slot.pokemon?.id ?? null)
    .filter((pokemonId): pokemonId is string => typeof pokemonId === 'string');

  return occupiedIds.length > 0 ? occupiedIds : null;
}

function targetIdsFromCard(card: BoardCardViewModel | null): string[] | null {
  if (!card) {
    return null;
  }

  return card.validTargetPokemonInPlayIds && card.validTargetPokemonInPlayIds.length > 0
    ? card.validTargetPokemonInPlayIds
    : null;
}

function requiresPokemonTarget(board: BoardGameViewModel, card: BoardCardViewModel): boolean {
  return (
    (card.validTargetPokemonInPlayIds?.length ?? 0) > 0 ||
    (targetIdsFromActionHints(board, GameActionType.PlayTrainer, card)?.length ?? 0) > 0
  );
}

function targetIdsFromActionHints(
  board: BoardGameViewModel,
  actionType: GameActionType,
  selectedCard: BoardCardViewModel | null
): string[] | null {
  switch (actionType) {
    case GameActionType.AttachEnergy:
      return nonEmptyTargetIds(board.actionHints.attachEnergyTargetPokemonInPlayIds);
    case GameActionType.PlayTrainer:
      if (selectedCard?.category === 'POKEMON_TOOL_TRAINER') {
        return nonEmptyTargetIds(board.actionHints.trainerToolTargetPokemonInPlayIds);
      }

      return nonEmptyTargetIds(board.actionHints.trainerTargetPokemonInPlayIds);
    case GameActionType.Retreat:
      return nonEmptyTargetIds(board.actionHints.retreatTargetPokemonInPlayIds);
    case GameActionType.PromoteBenchPokemon:
      return nonEmptyTargetIds(board.actionHints.promoteTargetPokemonInPlayIds);
    default:
      return null;
  }
}

function nonEmptyTargetIds(targetIds: string[]): string[] | null {
  return targetIds.length > 0 ? targetIds : null;
}

function findHandCard(board: BoardGameViewModel, cardInstanceId: string): BoardCardViewModel | null {
  return (
    board.localPlayer.handCards.find((card) => card.cardInstanceId === cardInstanceId) ??
    board.localPlayer.handCards.find((card) => card.id === cardInstanceId) ??
    null
  );
}

function cardMatchesAction(card: BoardCardViewModel, actionType: GameActionType): boolean {
  return cardCanBeSelectedForAction(card, actionType);
}
