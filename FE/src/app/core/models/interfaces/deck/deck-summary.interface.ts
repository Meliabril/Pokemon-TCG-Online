import { CardSummary } from '../card/card-summary.interface';

export interface DeckSummary {
  id: string;
  ownerUserId: string;
  name: string;
  format: string;
  active: boolean;
  valid: boolean;
  validationErrors: string[];
  cards: DeckCardSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface DeckCardSummary {
  id: string;
  cardId: string;
  quantity: number;
  card: CardSummary;
}

export interface DeckValidationResponse {
  deckId: string;
  valid: boolean;
  errors: string[];
}

export interface DeckActivationResponse {
  id: string;
  active: boolean;
  valid: boolean;
  validationErrors: string[];
}
