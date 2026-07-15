import { CardSupertype } from '../../../core/models/enums/card/card-supertype.enum';
import { CardSummary } from '../../../core/models/interfaces/card/card-summary.interface';

export type DeckCardCounts = Record<CardSupertype, number>;

export type DeckTypeFilter = 'ALL' | CardSupertype;

export type DraftDeckItem = {
  card: CardSummary;
  quantity: number;
};

export type DeckValidationHint = {
  label: string;
  valid: boolean;
};

export type DeckTypeFilterOption = {
  value: DeckTypeFilter;
  label: string;
};
