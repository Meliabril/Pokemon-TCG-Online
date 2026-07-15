export interface SharedCardContext {
  readonly canAttack?: boolean;
  readonly canUseAbility?: boolean;
  readonly canAttachEnergy?: boolean;
  readonly readonly?: boolean;
}

export interface SharedAttackViewModel {
  readonly id?: string;
  readonly name: string;
  readonly cost?: string[];
  readonly convertedEnergyCost?: number;
  readonly damage?: string | number | null;
  readonly text?: string | null;
}

export interface SharedAbilityViewModel {
  readonly id?: string;
  readonly name: string;
  readonly type?: string | null;
  readonly text?: string | null;
}

export interface SharedCardModalViewModel {
  readonly id: string;
  readonly name: string;
  readonly imageUrl?: string | null;
  readonly supertypeCode?: string | null;
  readonly supertype?: string | null;
  readonly subtypes?: string[];
  readonly types?: string[];
  readonly hp?: string | number | null;
  readonly evolvesFrom?: string | null;
  readonly rarity?: string | null;
  readonly number?: string | null;
  readonly attacks?: SharedAttackViewModel[];
  readonly abilities?: SharedAbilityViewModel[];
  readonly displayRules?: string[] | null;
  readonly weaknesses?: unknown[];
  readonly resistances?: unknown[];
  readonly retreatCost?: string[];
}
