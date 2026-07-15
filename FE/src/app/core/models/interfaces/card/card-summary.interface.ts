import { CardCategory } from '../../enums/card/card-category.enum';
import { CardSupertype } from '../../enums/card/card-supertype.enum';

export interface AttackCost {
  energyType: string;
  quantity: number;
}

export interface DisplayAbility {
  displayName: string | null;
  displayType: string | null;
  displayText: string | null;
}

export interface CardAbility {
  id: string | null;
  code?: string | null;
  name: string;
  type?: string | null;
  displayName?: string | null;
  displayType?: string | null;
  text?: string | null;
  displayText?: string | null;
  activation?: string | null;
  timing?: string | null;
  oncePerTurn?: boolean;
  implemented?: boolean;
}

export interface Attack {
  id: string;
  name: string;
  damageText: string | null;
  baseDamage: number | null;
  effectText: string | null;
  attackOrder: number;
  costs: AttackCost[];
  displayName?: string | null;
  displayCost?: string[] | null;
  displayText?: string | null;
  displayTextEn?: string | null;
}

export interface CardRelation {
  energyType: string;
  value: string;
  displayEnergyType?: string | null;
  displayValue?: string | null;
}

export interface CardSummary {
  id: string;
  externalId: string;
  setCode: string;
  setName: string;
  number: string;
  name: string;
  supertype: CardSupertype;
  category: CardCategory;
  subtype: string | null;
  evolvesFrom: string | null;
  hp: number | null;
  pokemonType: string | null;
  retreatCost: number | null;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  attacks?: Attack[];
  weaknesses?: CardRelation[];
  resistances?: CardRelation[];
  displayName?: string | null;
  displayPokemonType?: string | null;
  displaySupertype?: string | null;
  displaySubtypes?: string[] | null;
  abilities?: CardAbility[] | null;
  displayAbilities?: DisplayAbility[] | null;
  rules?: string[] | null;
  displayRules?: string[] | null;
}

export interface CardImportStatus {
  imported: boolean;
  importedAt?: string | null;
  totalCards?: number | null;
  source?: string | null;
}
