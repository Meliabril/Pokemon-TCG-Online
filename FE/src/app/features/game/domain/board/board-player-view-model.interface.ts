import { BoardActionViewModel } from '../actions/board-action-view-model.interface';
import { BoardCardViewModel } from '../cards/board-card-view-model.interface';
import { BoardPokemonViewModel } from '../cards/board-pokemon-view-model.interface';

export type BoardPlayerRole = 'opponent' | 'current';
export type BoardZoneVisibility = 'visible' | 'hidden' | 'counter';

export interface BoardCardZoneViewModel {
  label: string;
  visibility: BoardZoneVisibility;
  count: number;
  cards: BoardCardViewModel[];
}

export interface BoardSlotViewModel {
  id: string;
  label: string;
  occupied: boolean;
  card: BoardCardViewModel | null;
  pokemon?: BoardPokemonViewModel | null;
}

export interface BoardPlayerViewModel {
  id: string;
  label: string;
  avatarUrl: string;
  connected: boolean;
  role: BoardPlayerRole;
  isLocal?: boolean;
  isOpponent?: boolean;
  isActiveTurn: boolean;
  setupSelectionSubmitted: boolean;
  /** Cumulative Mulligans this player has taken, sourced from the authoritative snapshot state. */
  mulliganCount: number;
  deckCount: number;
  discardCount: number;
  prizeCount: number;
  handCards: BoardCardViewModel[];
  prizeCards: BoardCardViewModel[];
  deckCards: BoardCardViewModel[];
  discardCards: BoardCardViewModel[];
  handZone?: BoardCardZoneViewModel;
  prizeZone?: BoardCardZoneViewModel;
  deckZone?: BoardCardZoneViewModel;
  discardZone?: BoardCardZoneViewModel;
  activePokemon: BoardSlotViewModel;
  benchSlots: BoardSlotViewModel[];
  actions?: BoardActionViewModel[];
  deckPokemonOptions?: { cardInstanceId: string; label: string }[];
}
