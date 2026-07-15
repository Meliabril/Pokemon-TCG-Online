import { CardCategory } from '../../enums/card/card-category.enum';
import { CardSupertype } from '../../enums/card/card-supertype.enum';
import { CardZone } from '../../enums/card/card-zone.enum';
import { GameActionType } from '../../enums/game/game-action-type.enum';
import { SpecialConditionType } from '../../enums/game/special-condition-type.enum';

export interface VisibleAttackCostDto {
  energyType: string | null;
  quantity: number;
}

export interface VisibleAttackDto {
  attackId: string | null;
  name: string | null;
  damageText: string | null;
  baseDamage: number | null;
  effectText: string | null;
  costs: VisibleAttackCostDto[];
  displayName?: string | null;
  displayCost?: string[] | null;
  displayText?: string | null;
  enabled: boolean;
  disabledReason: string | null;
  requiresTarget: boolean;
  validTargetPokemonInPlayIds: string[];
  targetsOwnPokemon: boolean;
}

export interface VisibleAbilityDto {
  abilityId: string | null;
  name: string | null;
  activationType?: 'ACTIVE' | 'PASSIVE' | string | null;
  enabled: boolean;
  disabledReason: string | null;
  deckCardOptions?: VisibleCardDto[];
}

export interface VisibleCardDto {
  cardInstanceId: string | null;
  cardId: string | null;
  name: string | null;
  externalId: string | null;
  setCode: string | null;
  number: string | null;
  supertype: CardSupertype | null;
  category: CardCategory | null;
  subtype: string | null;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  hp: number | null;
  faceDown: boolean;
  playable: boolean;
  suggestedAction: GameActionType | null;
  disabledReason: string | null;
  validTargetPokemonInPlayIds: string[];
}

export interface VisibleZoneDto {
  zone: CardZone;
  count: number;
  cards: VisibleCardDto[];
}

/**
 * Minimal, public, purely-cosmetic description of a temporary effect currently affecting a
 * {@link VisiblePokemonDto}. Mirrors the backend's `VisiblePokemonEffectDto`. The frontend must
 * only render a visual indicator based on the presence/absence of entries here — it must never
 * recompute or guess whether the effect is active; the backend is the single source of truth.
 */
export interface VisiblePokemonEffectDto {
  type: string;
  source: string;
  expiresAt: string;
}

export interface VisiblePokemonDto {
  pokemonInPlayId: string | null;
  ownerPlayerId: string | null;
  slotPosition: number | null;
  activeCard: VisibleCardDto | null;
  evolutionStack: VisibleCardDto[];
  attachedEnergyCards: VisibleCardDto[];
  attachedTrainerCards: VisibleCardDto[];
  damageCounters: number | null;
  specialConditions: SpecialConditionType[];
  attacks: VisibleAttackDto[];
  abilities: VisibleAbilityDto[];
  canReceiveEnergy: boolean;
  canReceiveTrainer: boolean;
  canRetreatTo: boolean;
  canPromote: boolean;
  visualEffects?: VisiblePokemonEffectDto[];
}

export interface VisiblePlayerBoardDto {
  playerId: string;
  playerOrder: number;
  connected: boolean;
  local: boolean;
  mulliganCount: number;
  mulliganNoticePending: boolean;
  mulliganFlowActive: boolean;
  mulliganReadyForInitialSelection: boolean;
  mulliganRoundNumber: number;
  mulliganCurrentPlayer: boolean;
  setupSelectionSubmitted: boolean;
  setupActiveOccupied: boolean;
  setupBenchOccupiedCount: number;
  hand: VisibleZoneDto;
  deck: VisibleZoneDto;
  prize: VisibleZoneDto;
  discard: VisibleZoneDto;
  activePokemon: VisiblePokemonDto | null;
  benchPokemon: VisiblePokemonDto[];
}

export interface VisibleStadiumDto {
  card: VisibleCardDto | null;
  playedByPlayerId: string | null;
}

export interface VisibleBoardActionHintsDto {
  attachEnergyTargetPokemonInPlayIds: string[];
  retreatTargetPokemonInPlayIds: string[];
  promoteTargetPokemonInPlayIds: string[];
  trainerTargetPokemonInPlayIds: string[];
  trainerToolTargetPokemonInPlayIds: string[];
}

export interface VisibleBoardDto {
  localPlayerId: string | null;
  players: VisiblePlayerBoardDto[];
  stadium: VisibleStadiumDto | null;
  actionHints: VisibleBoardActionHintsDto;
}
