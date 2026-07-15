export interface DeckCardRequest {
  cardId: string;
  quantity: number;
}

export interface DeckUpsertRequest {
  name: string;
  cards: DeckCardRequest[];
}
