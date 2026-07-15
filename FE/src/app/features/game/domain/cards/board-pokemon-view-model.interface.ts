import { SpecialConditionType } from '../../../../core/models/enums/game/special-condition-type.enum';
import { BoardActionViewModel } from '../actions/board-action-view-model.interface';
import { BoardCardViewModel } from './board-card-view-model.interface';

export interface BoardPokemonAttackViewModel {
  id: string;
  label: string;
  name?: string | null;
  displayName?: string | null;
  damageText?: string | null;
  baseDamage?: number | null;
  effectText?: string | null;
  displayText?: string | null;
  costs: {
    energyType: string | null;
    quantity: number;
  }[];
  displayCost?: string[] | null;
  enabled: boolean;
  disabledReason?: string | null;
  requiresTarget?: boolean;
  validTargetPokemonInPlayIds?: string[];
  targetsOwnPokemon?: boolean;
}

export interface BoardPokemonAbilityViewModel {
  id: string;
  label: string;
  activationType?: 'ACTIVE' | 'PASSIVE' | string | null;
  description?: string | null;
  deckCardOptions?: {
    cardInstanceId: string;
    label: string;
    externalId?: string | null;
    cardId?: string | null;
    number?: string | null;
    imageSmallUrl?: string | null;
    imageLargeUrl?: string | null;
  }[];
  enabled: boolean;
  disabledReason?: string | null;
}

/**
 * Cosmetic-only temporary effect surfaced by the backend for a Pokemon in play (e.g. Harden's
 * damage-prevention shield). Components must only use this to decide whether to render a visual
 * indicator — never to (re)compute whether the underlying rule is active.
 */
export interface BoardPokemonEffectViewModel {
  type: string;
  source: string;
  expiresAt: string;
}

/** `BoardPokemonEffectViewModel.type` value used for Harden's damage-prevention shield. */
export const DAMAGE_PREVENTION_SHIELD_EFFECT_TYPE = 'DAMAGE_PREVENTION_SHIELD';

export interface BoardPokemonViewModel {
  id: string;
  activeCard: BoardCardViewModel;
  evolutionStack: BoardCardViewModel[];
  attachedEnergyCards: BoardCardViewModel[];
  attachedTrainerCards: BoardCardViewModel[];
  damageCounters: number | null;
  specialConditions: SpecialConditionType[];
  attacks: BoardPokemonAttackViewModel[];
  abilities: BoardPokemonAbilityViewModel[];
  actions: BoardActionViewModel[];
  canReceiveEnergy: boolean;
  canReceiveTrainer: boolean;
  canRetreatTo: boolean;
  canPromote: boolean;
  visualEffects: BoardPokemonEffectViewModel[];
}
