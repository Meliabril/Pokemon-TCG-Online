import { GameEventType } from '../../enums/game/game-event-type.enum';
import { GameStatus } from '../../enums/game/game-status.enum';
import { TurnPhase } from '../../enums/game/turn-phase.enum';
import {
  GameSnapshot,
  GameSnapshotActionState,
  GameSnapshotBoardState,
  GameSnapshotPlayerState,
  GameSnapshotSync,
  GameSnapshotTurnContext
} from './game-snapshot.interface';

export type {
  GameBoardAbility,
  GameBoardAttack,
  GameBoardCard,
  GameBoardPlayerState,
  GameBoardPokemon,
  GameBoardZoneSummary
} from './game-snapshot.interface';

export type {
  VisibleAbilityDto,
  VisibleAttackCostDto,
  VisibleAttackDto,
  VisibleBoardActionHintsDto,
  VisibleBoardDto,
  VisibleCardDto,
  VisiblePlayerBoardDto,
  VisiblePokemonDto,
  VisibleStadiumDto,
  VisibleZoneDto
} from './visible-board.interface';

export interface GameParticipant {
  id: string;
  userId: string;
  username?: string | null;
  avatar?: string | null;
  deckId: string;
  playerOrder: number;
  connected: boolean;
  lastSeenAt: string | null;
  createdAt: string;
}

export interface GameDetail {
  gameId: string;
  status: GameStatus;
  currentPhase: TurnPhase | null;
  turnNumber: number;
  stateVersion: number;
  activePlayerId: string | null;
  turnStartedAt: string | null;
  winnerPlayerId: string | null;
  pauseReason: string | null;
  startedAt: string | null;
  pausedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  participants: GameParticipant[];
}

export interface GamePlayerState extends GameSnapshotPlayerState {}

export interface GameTurnContext extends GameSnapshotTurnContext {}

export interface GameBoardState extends GameSnapshotBoardState {}

export interface GameActionState extends GameSnapshotActionState {}

export interface GameState extends GameSnapshot {}

export interface GameStateSync extends Omit<GameSnapshotSync, 'state'> {
  gameId: string;
  eventType: GameEventType.StateSync;
  stateVersion: number;
  state: GameState;
}

export interface PauseGameRequest {
  reason?: string;
}
