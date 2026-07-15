import { CardCategory } from '../../../core/models/enums/card/card-category.enum';
import { CardSupertype } from '../../../core/models/enums/card/card-supertype.enum';
import { CardSummary } from '../../../core/models/interfaces/card/card-summary.interface';
import { DeckCardRequest } from '../../../core/models/interfaces/deck/deck-command.interface';
import { DeckCardSummary } from '../../../core/models/interfaces/deck/deck-summary.interface';
import { DeckCardCounts, DraftDeckItem } from '../models/deck-builder.types';

const EMPTY_COUNTS: DeckCardCounts = {
  [CardSupertype.Pokemon]: 0,
  [CardSupertype.Energy]: 0,
  [CardSupertype.Trainer]: 0
};

export function totalDeckCards(cards: Pick<DeckCardSummary, 'quantity'>[]): number {
  return cards.reduce((total, card) => total + card.quantity, 0);
}

export function countCardsBySupertype(cards: DeckCardSummary[]): DeckCardCounts {
  return cards.reduce<DeckCardCounts>(
    (counts, card) => ({
      ...counts,
      [card.card.supertype]: counts[card.card.supertype] + card.quantity
    }),
    { ...EMPTY_COUNTS }
  );
}

export function draftCardsById(cards: DeckCardRequest[]): Map<string, number> {
  return new Map(cards.map((card) => [card.cardId, card.quantity]));
}

const BASIC_STAGE_SUBTYPE = 'Basic';

// category collapses stage + EX/Mega specialness into one value (a Basic
// Pokemon-EX is reported as a non-BasicPokemon category), so Basic-stage
// EX Pokemon must be detected via subtype instead of category. "Basic" is
// also reused as the subtype for Basic Energy cards, so supertype must be
// checked too or an Energy card would be treated as a Basic Pokemon.
export function isBasicPokemon(card: CardSummary): boolean {
  if (card.supertype !== CardSupertype.Pokemon) {
    return false;
  }
  return card.category === CardCategory.BasicPokemon || card.subtype === BASIC_STAGE_SUBTYPE;
}

export function isPokemon(card: CardSummary): boolean {
  return card.supertype === CardSupertype.Pokemon;
}

export function usesCopyLimitByName(card: CardSummary): boolean {
  return (
    card.supertype === CardSupertype.Pokemon ||
    card.supertype === CardSupertype.Trainer ||
    card.category === CardCategory.SpecialEnergy
  );
}

export function cardNamesOverCopyLimit(
  items: DraftDeckItem[],
  maxCopiesByName: number
): string[] {
  const copiesByName = items.reduce<Map<string, number>>((copies, item) => {
    if (usesCopyLimitByName(item.card)) {
      copies.set(item.card.name, (copies.get(item.card.name) ?? 0) + item.quantity);
    }
    return copies;
  }, new Map());

  return [...copiesByName.entries()]
    .filter(([, copies]) => copies > maxCopiesByName)
    .map(([name]) => name)
    .sort((left, right) => left.localeCompare(right));
}
