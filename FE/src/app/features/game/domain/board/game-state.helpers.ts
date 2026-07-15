import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameStatus } from '../../../../core/models/enums/game/game-status.enum';
import { GameResolutionType } from '../../../../core/models/interfaces/game/game-resolution.interface';
import { BoardGameViewModel } from './board-game-view-model.interface';
import { GameScreenState } from '../ui-state/game-screen-state.type';

type PlayerTurnState = Pick<BoardGameViewModel, 'status' | 'activePlayerId' | 'activePlayerLabel'>;
type ResolutionState = {
  resolution?: {
    resolutionType: GameResolutionType | null;
    playerToPromoteId: string | null;
    pendingChoicePlayerId?: string | null;
  } | null;
};
type BlockableGameState = ResolutionState & {
  status: GameStatus;
  availableActions?: GameActionType[];
  actions?: {
    availableActions: GameActionType[];
  };
};

export function hasAction(
  availableActions: GameActionType[],
  actionType: GameActionType
): boolean {
  return availableActions.includes(actionType);
}

export function isCurrentPlayerTurn(state: PlayerTurnState, playerId: string): boolean {
  return state.activePlayerId === playerId;
}

export function isResolutionPending(state: ResolutionState): boolean {
  return Boolean(state.resolution?.resolutionType);
}

export function isPromotionRequired(state: ResolutionState): boolean {
  return state.resolution?.resolutionType === GameResolutionType.PromotionRequired;
}

export function isLocalPromotionRequired(state: ResolutionState, playerId: string | null): boolean {
  return Boolean(
    playerId &&
    isPromotionRequired(state) &&
    state.resolution?.playerToPromoteId === playerId
  );
}

export function isAttackChoiceRequired(state: ResolutionState): boolean {
  return state.resolution?.resolutionType === GameResolutionType.AttackChoiceRequired;
}

export function isLocalAttackChoiceRequired(state: ResolutionState, playerId: string | null): boolean {
  return Boolean(
    playerId &&
    isAttackChoiceRequired(state) &&
    state.resolution?.pendingChoicePlayerId === playerId
  );
}

export function promotionScreenState(
  state: ResolutionState,
  playerId: string | null
): Extract<GameScreenState, 'promotion-selecting' | 'promotion-waiting-opponent'> | null {
  if (!isPromotionRequired(state)) {
    return null;
  }

  return isLocalPromotionRequired(state, playerId)
    ? 'promotion-selecting'
    : 'promotion-waiting-opponent';
}

export function attackChoiceScreenState(
  state: ResolutionState,
  playerId: string | null
): Extract<GameScreenState, 'attack-choice-selecting' | 'attack-choice-waiting-opponent'> | null {
  if (!isAttackChoiceRequired(state)) {
    return null;
  }

  return isLocalAttackChoiceRequired(state, playerId)
    ? 'attack-choice-selecting'
    : 'attack-choice-waiting-opponent';
}

export function isGameBlocked(state: BlockableGameState): boolean {
  if (isResolutionPending(state)) {
    return true;
  }

  return (
    state.status === GameStatus.Paused ||
    state.status === GameStatus.Finished ||
    state.status === GameStatus.Cancelled
  );
}

export function canExecuteGameAction(
  state: BlockableGameState,
  playerId: string | null,
  actionType: GameActionType
): boolean {
  if (!isGameBlocked(state)) {
    return true;
  }

  if (
    state.status === GameStatus.Paused ||
    state.status === GameStatus.Finished ||
    state.status === GameStatus.Cancelled
  ) {
    return false;
  }

  const availableActions = state.availableActions ?? state.actions?.availableActions ?? [];

  if (
    actionType === GameActionType.PromoteBenchPokemon &&
    isLocalPromotionRequired(state, playerId) &&
    availableActions.includes(GameActionType.PromoteBenchPokemon)
  ) {
    return true;
  }

  return (
    actionType === GameActionType.ResolveAttackChoice &&
    isLocalAttackChoiceRequired(state, playerId) &&
    availableActions.includes(GameActionType.ResolveAttackChoice)
  );
}
