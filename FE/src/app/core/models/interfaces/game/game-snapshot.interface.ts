import { CardZone } from '../../enums/card/card-zone.enum';
import { GameActionType } from '../../enums/game/game-action-type.enum';
import { GameEventType } from '../../enums/game/game-event-type.enum';
import { GameStatus } from '../../enums/game/game-status.enum';
import { SpecialConditionType } from '../../enums/game/special-condition-type.enum';
import { TurnPhase } from '../../enums/game/turn-phase.enum';
import { GameResolution } from './game-resolution.interface';
import { VisibleBoardDto, VisiblePokemonEffectDto } from './visible-board.interface';

export interface GameSnapshotPlayerState {
  benchPokemonCount: number;
  activePokemonConditions: SpecialConditionType[];
  cardIdsInHand: string[];
  cardInstanceIdsInHand: string[];
  affordableAttackIds: string[];
  mulliganCount: number;
  mulliganNoticePending: boolean;
  mulliganFlowActive: boolean;
  mulliganReadyForInitialSelection: boolean;
  mulliganRoundNumber: number;
  mulliganCurrentPlayer: boolean;
  initialPokemonSelectionSubmitted: boolean;
  initialActiveCardInstanceId: string | null;
  initialBenchCardInstanceIds: string[];
}

export interface GameSnapshotTurnContext {
  currentPhase: TurnPhase | null;
  turnNumber: number;
  activePlayerId: string | null;
  playerWhoWentFirstId: string | null;
  turnStartedAt: string | null;
  energyAttachedThisTurn: boolean;
  supporterPlayedThisTurn: boolean;
  retreatedThisTurn: boolean;
}

export interface GameSnapshotBoardState {
  enteredPlayTurnByPokemonInPlayId: Record<string, number>;
  zoneByCardReferenceId: Record<string, CardZone>;
  ownerByCardReferenceId: Record<string, string>;
  view?: VisibleBoardDto | null;
}

export interface GameSnapshotActionState {
  availableActions: GameActionType[];
  processedClientActionIds: string[];
}

export interface GameBoardCard {
  cardInstanceId: string | null;
  cardId: string | null;
  externalId: string | null;
  name: string | null;
  setCode: string | null;
  number: string | null;
  supertype: string | null;
  category: string | null;
  subtype: string | null;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  hp: number | null;
  zone: CardZone | null;
  zonePosition: number | null;
  faceDown: boolean;
  playable?: boolean;
  suggestedAction?: GameActionType | null;
  disabledReason?: string | null;
  validTargetPokemonInPlayIds?: string[];
}

export interface GameBoardZoneSummary {
  count: number;
  cards: GameBoardCard[];
}

export interface GameBoardAttackCost {
  energyType: string | null;
  quantity: number;
}

export interface GameBoardAttack {
  attackId: string | null;
  name: string | null;
  damageText: string | null;
  baseDamage: number | null;
  effectText: string | null;
  attackOrder: number;
  costs: GameBoardAttackCost[];
  displayName?: string | null;
  displayCost?: string[] | null;
  displayText?: string | null;
  available: boolean;
  disabledReason: string | null;
  requiresTarget?: boolean;
  validTargetPokemonInPlayIds?: string[];
  targetsOwnPokemon?: boolean;
}

export interface GameBoardAbility {
  abilityId: string | null;
  name: string | null;
  activationType?: 'ACTIVE' | 'PASSIVE' | string | null;
  available: boolean;
  disabledReason: string | null;
  deckCardOptions?: GameBoardCard[];
}

export interface GameBoardPokemon {
  pokemonInPlayId: string | null;
  ownerUserId: string | null;
  slotPosition: number | null;
  damageCounters: number | null;
  enteredPlayTurn: number | null;
  activeCard: GameBoardCard | null;
  evolutionStack: GameBoardCard[];
  attachedCards: GameBoardCard[];
  attachedTrainerCards?: GameBoardCard[];
  specialConditions: SpecialConditionType[];
  attacks: GameBoardAttack[];
  abilities: GameBoardAbility[];
  canReceiveEnergy?: boolean;
  canReceiveTrainer?: boolean;
  canRetreatTo?: boolean;
  canPromote?: boolean;
  visualEffects?: VisiblePokemonEffectDto[];
}

export interface GameBoardPlayerState {
  playerId: string;
  hand: GameBoardZoneSummary;
  deck: GameBoardZoneSummary;
  prizes: GameBoardZoneSummary;
  discard: GameBoardZoneSummary;
  stadium: GameBoardZoneSummary;
  activePokemon: GameBoardPokemon | null;
  benchPokemon: GameBoardPokemon[];
}

export interface GameSnapshot {
  gameId: string;
  status: GameStatus;
  stateVersion: number;
  playerIds: string[];
  players: Record<string, GameSnapshotPlayerState>;
  boardPlayers?: Record<string, GameBoardPlayerState>;
  turn: GameSnapshotTurnContext;
  board: GameSnapshotBoardState;
  actions: GameSnapshotActionState;
  resolution?: GameResolution | null;
  updatedAt: string;
}

export interface GameSnapshotSync {
  gameId: string;
  eventType: GameEventType.StateSync;
  stateVersion: number;
  state: GameSnapshot;
}

/*
 * Datos no provistos por el snapshot actual del Backend:
 * - identificador del jugador local o viewer del snapshot;
 * - abilities disponibles/bloqueadas;
 * - comandos reales de USE_ABILITY en Backend.
 */
