import { GameEventType } from '../../../../core/models/enums/game/game-event-type.enum';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameStatus } from '../../../../core/models/enums/game/game-status.enum';
import { TurnPhase } from '../../../../core/models/enums/game/turn-phase.enum';
import { BoardActionViewModel } from '../actions/board-action-view-model.interface';
import { BoardEphemeralUiState } from '../ui-state/board-ephemeral-ui-state.interface';
import { BoardCardViewModel } from '../cards/board-card-view-model.interface';
import { BoardPlayerViewModel } from './board-player-view-model.interface';
import { BoardResolutionViewModel } from './board-resolution-view-model.interface';
import { MulliganRevealedCard } from '../../../../core/models/interfaces/game/mulligan-event.interface';

export type { BoardActionViewModel } from '../actions/board-action-view-model.interface';
export type { BoardCardViewModel, BoardCardVisibility } from '../cards/board-card-view-model.interface';
export type { CardContext, CardContextOwner, CardContextZone } from '../cards/card-context.interface';
export type {
  BoardPokemonAbilityViewModel,
  BoardPokemonAttackViewModel,
  BoardPokemonViewModel
} from '../cards/board-pokemon-view-model.interface';
export type {
  BoardCardZoneViewModel,
  BoardPlayerRole,
  BoardPlayerViewModel,
  BoardSlotViewModel,
  BoardZoneVisibility
} from './board-player-view-model.interface';
export type { BoardResolutionViewModel } from './board-resolution-view-model.interface';

export interface BoardStadiumViewModel {
  label: string;
  card: BoardCardViewModel | null;
  playedByPlayerId: string | null;
  playedByLabel: string | null;
}

export interface BoardTurnContextViewModel {
  turnNumber: number;
  currentPhase: TurnPhase | null;
  phaseLabel: string;
  activePlayerId: string | null;
  activePlayerLabel: string | null;
  playerWhoWentFirstId: string | null;
  turnStartedAt: string | null;
  energyAttachedThisTurn: boolean;
  supporterPlayedThisTurn: boolean;
  retreatedThisTurn: boolean;
  stateVersion: number;
}

export interface BoardEventFeedItemViewModel {
  id: string;
  eventType: GameEventType;
  label: string;
  occurredAt: string;
  stateVersion: number;
  privateEvent: boolean;
}

export interface BoardMulliganRevealSetupEventViewModel {
  kind: 'revealed-hand';
  id: string;
  revealingPlayerId: string;
  revealingPlayerLabel: string;
  mulliganNumber: number;
  mulliganCount: number;
  revealedCards: MulliganRevealedCard[];
  revealedCardPlaceholderCount: number;
  extraCardsGranted: number;
  pendingExtraCardsForViewer: number;
  occurredAt: string;
}

export interface BoardMulliganOwnSummarySetupEventViewModel {
  kind: 'own-summary';
  id: string;
  playerId: string;
  playerLabel: string;
  mulliganCount: number;
  extraCardsGrantedToOpponent: number;
  opponentPlayerId: string;
  opponentPlayerLabel: string;
  automatic: boolean;
  occurredAt: string;
}

export interface BoardMulliganStepSetupEventViewModel {
  kind: 'step';
  id: string;
  eventType: GameEventType;
  playerId: string | null;
  playerLabel: string | null;
  label: string;
  roundNumber: number;
  mulliganNumber: number | null;
  status: 'info' | 'success' | 'warning';
  occurredAt: string;
}

export type BoardMulliganSetupEventViewModel =
  | BoardMulliganRevealSetupEventViewModel
  | BoardMulliganOwnSummarySetupEventViewModel
  | BoardMulliganStepSetupEventViewModel;

export interface BoardUiStateViewModel extends BoardEphemeralUiState {
  hasSelection: boolean;
  hasPendingTarget: boolean;
  eventFeedCount: number;
}

export interface BoardActionHintsViewModel {
  attachEnergyTargetPokemonInPlayIds: string[];
  retreatTargetPokemonInPlayIds: string[];
  promoteTargetPokemonInPlayIds: string[];
  trainerTargetPokemonInPlayIds: string[];
  trainerToolTargetPokemonInPlayIds: string[];
}

export interface BoardGameViewModel {
  gameId: string;
  status: GameStatus;
  statusLabel: string;
  statusSummary: string;
  turnNumber: number;
  currentPhase: TurnPhase | null;
  phaseLabel: string;
  activePlayerId: string | null;
  activePlayerLabel: string | null;
  stateVersion: number;
  availableActions: GameActionType[];
  localPlayer: BoardPlayerViewModel;
  rivalPlayer: BoardPlayerViewModel;
  stadium: BoardStadiumViewModel | null;
  actionHints: BoardActionHintsViewModel;
  turnContext: BoardTurnContextViewModel;
  eventFeed: BoardEventFeedItemViewModel[];
  mulliganSetupEvents: BoardMulliganSetupEventViewModel[];
  mulliganNoticePending: boolean;
  mulliganFlowActive: boolean;
  mulliganReadyForInitialSelection: boolean;
  mulliganRoundNumber: number;
  mulliganCurrentPlayer: boolean;
  ui: BoardUiStateViewModel;
  actions: BoardActionViewModel[];
  resolution: BoardResolutionViewModel | null;
  missingDataNotes: string[];
  availableActionLabels: string[];
  historySummary: string;
  latestActionLabel: string;
}
