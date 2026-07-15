import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { Attack, CardRelation } from '../../../../core/models/interfaces/card/card-summary.interface';

export type BoardCardVisibility = 'visible' | 'hidden' | 'counter';

export interface BoardCardViewModel {
  id: string;
  cardId?: string | null;
  cardInstanceId?: string | null;
  label: string;
  visibility?: BoardCardVisibility;
  faceDown: boolean;
  rotation: number;
  count?: number;
  imageSmallUrl?: string | null;
  imageLargeUrl?: string | null;
  altText?: string;
  externalId?: string;
  setCode?: string;
  setName?: string;
  number?: string;
  supertype?: string;
  category?: string | null;
  subtype?: string | null;
  evolvesFrom?: string | null;
  hp?: number | null;
  pokemonType?: string | null;
  retreatCost?: number | null;
  attacks?: Attack[];
  weaknesses?: CardRelation[];
  resistances?: CardRelation[];
  playable?: boolean;
  suggestedAction?: GameActionType | null;
  disabledReason?: string | null;
  validTargetPokemonInPlayIds?: string[];
}
