export type EmptyActionPayload = Record<string, never>;

export interface ChooseInitialPokemonPayload {
  activeCardInstanceId: string;
  benchCardInstanceIds: string[];
}

export interface PlayBasicPokemonPayload {
  cardId: string;
}

export interface AttachEnergyPayload {
  cardId: string;
  cardInstanceId?: string;
  pokemonInPlayId: string;
}

export interface DeclareAttackPayload {
  attackId: string;
  targetPokemonInPlayId?: string;
  useBonusDamage?: boolean;
  selfTargetPokemonInPlayId?: string;
  switchTargetPokemonInPlayId?: string;
}

export type EndTurnPayload = EmptyActionPayload;

export interface PromoteBenchPokemonPayload {
  pokemonInPlayId: string;
}

export type ResolveAttackChoicePayload =
  | { targetPokemonInPlayId: string }
  | { attackOrder: number }
  | { confirm: boolean }
  | { cardInstanceIds: string[] }
  | { cardInstanceId: string }
  | { conditionType: string }
  | { attachedCardId: string; targetPokemonInPlayId: string }
  | { cardInstanceId: string; pokemonInPlayId: string };

export interface EvolvePokemonPayload {
  cardId: string;
  pokemonInPlayId: string;
}

export interface PlayTrainerPayload {
  cardId?: string;
  cardInstanceId?: string;
  targetCardId?: string;
  targetPokemonInPlayId?: string;
  targetCardInstanceId?: string;
  targetEnergyCardInstanceId?: string;
  /** Evosoda: grouped card identity; backend resolves one matching deck instance. */
  selectedEvolutionExternalId?: string;
  /** Evosoda: which evolution card (deck card-type id) to evolve the target Pokemon into. */
  selectedCardInstanceId?: string;
  /** Professor's Letter: card-type ids of the up-to-2 Basic Energy cards chosen from the deck (repeat an id for quantity 2). */
  selectedCardIds?: string[];
}

export interface RetreatPayload {
  targetPokemonInPlayId: string;
}

export interface SelectTargetPayload {
  targetPokemonInPlayId: string;
}

export type DrawCardPayload = EmptyActionPayload;
export type TakePrizeCardPayload = EmptyActionPayload;
export type ConcedePayload = EmptyActionPayload;

export interface UseAbilityPayload {
  sourcePokemonId: string;
  abilityCode: string;
  selectedHandCardId?: string;
  targetPokemonId?: string;
  sourceEnergyCardId?: string;
  fromPokemonId?: string;
  toPokemonId?: string;
  selectedDeckCardId?: string;
}

export type GameActionPayload =
  | EmptyActionPayload
  | ChooseInitialPokemonPayload
  | PlayBasicPokemonPayload
  | AttachEnergyPayload
  | DeclareAttackPayload
  | EndTurnPayload
  | PromoteBenchPokemonPayload
  | ResolveAttackChoicePayload
  | EvolvePokemonPayload
  | PlayTrainerPayload
  | RetreatPayload
  | SelectTargetPayload
  | DrawCardPayload
  | TakePrizeCardPayload
  | ConcedePayload
  | UseAbilityPayload;
