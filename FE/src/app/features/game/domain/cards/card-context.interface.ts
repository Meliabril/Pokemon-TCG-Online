import {
  BoardPokemonAbilityViewModel,
  BoardPokemonAttackViewModel
} from './board-pokemon-view-model.interface';

export type CardContextOwner = 'local' | 'rival' | 'neutral';

export type CardContextZone =
  | 'hand'
  | 'active'
  | 'bench'
  | 'discard'
  | 'stadium'
  | 'visible-board';

export interface CardContext {
  zone: CardContextZone;
  owner: CardContextOwner;
  label: string;
  slotId?: string;
  pokemonInPlayId?: string;
  damageCounters?: number | null;
  attacks?: BoardPokemonAttackViewModel[];
  abilities?: BoardPokemonAbilityViewModel[];
}
