export interface EvosodaEvolutionOption {
  cardId: string;
  externalId: string;
  name: string;
  evolvesFrom: string;
  count: number;
  validTargetPokemonInPlayIds: string[];
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
}

export interface EvosodaPreview {
  options: EvosodaEvolutionOption[];
}

export interface GreatBallPokemonOption {
  cardInstanceId: string;
  cardId: string;
  name: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
}

export interface GreatBallPreview {
  cardsLookedAt: number;
  pokemonOptions: GreatBallPokemonOption[];
}

export interface ProfessorLetterEnergyOption {
  cardId: string;
  name: string;
  availableInDeck: number;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
}

export interface ProfessorLetterPreview {
  maxSelectable: number;
  options: ProfessorLetterEnergyOption[];
}
