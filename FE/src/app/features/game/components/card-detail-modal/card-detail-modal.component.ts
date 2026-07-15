import { ChangeDetectionStrategy, Component, HostListener, computed, inject, input, output, signal } from '@angular/core';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { CardSupertype } from '../../../../core/models/enums/card/card-supertype.enum';
import { UseAbilityPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { Attack, CardAbility, CardRelation, CardSummary, DisplayAbility } from '../../../../core/models/interfaces/card/card-summary.interface';
import { AppLanguage, LanguageService } from '../../../../core/services/language.service';
import { BoardCardViewModel } from '../../domain/board/board-game-view-model.interface';
import {
  AbilityAttachedEnergyOption,
  AbilityCardOption,
  AbilitySelectionViewModel
} from '../../domain/abilities/ability-selection.helpers';
import { CardContext } from '../../domain/cards/card-context.interface';
import { BoardPokemonAbilityViewModel, BoardPokemonAttackViewModel } from '../../domain/cards/board-pokemon-view-model.interface';

const OPTIONAL_BONUS_DAMAGE_ATTACK_NAMES = new Set(['Charge Dash', 'Electron Crush']);
const PASSIVE_ABILITY_ACTIVATION_TYPE = 'PASSIVE';

const TRAINER_CATEGORY_MAP: Record<string, 'item' | 'supporter' | 'stadium' | 'tool' | 'ace_spec'> = {
  ITEM_TRAINER:        'item',
  ACE_SPEC_TRAINER:    'ace_spec',
  SUPPORTER_TRAINER:   'supporter',
  STADIUM_TRAINER:     'stadium',
  POKEMON_TOOL_TRAINER:'tool'
};

const ENERGY_COLORS: Record<string, string> = {
  Grass: '#5bbd6b',
  Fire: '#f0772b',
  Water: '#4aa3df',
  Lightning: '#f2c33a',
  Psychic: '#b772d6',
  Fighting: '#cf7236',
  Darkness: '#5a6b7b',
  Metal: '#9aa6b0',
  Fairy: '#e87fb5',
  Dragon: '#c9a227',
  Colorless: '#d8c9a8'
};

interface DisplayedAbilityViewModel extends BoardPokemonAbilityViewModel {
  description?: string | null;
}

@Component({
  selector: 'app-card-detail-modal',
  templateUrl: './card-detail-modal.component.html',
  styleUrl: './card-detail-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardDetailModalComponent {
  private readonly languageService = inject(LanguageService);
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly card = input<BoardCardViewModel | null>(null);
  readonly detail = input<CardSummary | null>(null);
  readonly isOpen = input(false);
  readonly isLoadingDetail = input(false);
  readonly detailError = input<string | null>(null);
  readonly context = input<CardContext | null>(null);
  readonly actionPending = input(false);
  readonly canDeclareAttack = input(false);
  readonly canUseAbility = input(false);
  readonly attackAvailabilityByIndex = input<Record<number, { available: boolean; reason?: string }>>({});
  readonly abilityAvailabilityByIndex = input<Record<number, { available: boolean; reason?: string }>>({});
  readonly abilitySelectionByIndex = input<Record<number, AbilitySelectionViewModel | null>>({});
  readonly ownPokemonOptions = input<{ pokemonInPlayId: string; label: string }[]>([]);
  readonly opponentPokemonOptions = input<{ pokemonInPlayId: string; label: string }[]>([]);
  readonly closed = output<void>();
  readonly attackDeclared = output<{
    attackId?: string;
    attackIndex: number;
    attackName: string;
    useBonusDamage?: boolean;
    selfTargetPokemonInPlayId?: string;
    switchTargetPokemonInPlayId?: string;
  }>();
  readonly abilityDeclared = output<{
    sourcePokemonId: string;
    ability: BoardPokemonAbilityViewModel;
    payload: UseAbilityPayload;
  }>();
  readonly selectedAttackIndex = signal<number | null>(null);
  readonly selectedAbilityIndex = signal<number | null>(null);
  readonly selectedHealTargetPokemonInPlayId = signal<string | null>(null);
  readonly selectedAbilityHandCardId = signal<string | null>(null);
  readonly selectedAbilityTargetPokemonInPlayId = signal<string | null>(null);
  readonly selectedAbilityFromPokemonInPlayId = signal<string | null>(null);
  readonly selectedAbilityToPokemonInPlayId = signal<string | null>(null);
  readonly selectedAbilitySourceEnergyCardId = signal<string | null>(null);
  readonly selectedAbilityDeckCardId = signal<string | null>(null);

  readonly cardRotateX = signal(0);
  readonly cardRotateY = signal(0);
  readonly cardGlareX = signal(50);
  readonly cardGlareY = signal(50);
  readonly typeColorStyle = computed(() => ({
    '--type-color': this.getEnergyColor(this.pokemonType())
  }));
  readonly cardTiltStyle = computed(() => ({
    'transform': `perspective(1000px) rotateX(${this.cardRotateX()}deg) rotateY(${this.cardRotateY()}deg)`,
    '--glare-x': `${this.cardGlareX()}%`,
    '--glare-y': `${this.cardGlareY()}%`
  }));

  readonly imageUrl = computed(() =>
    this.detail()?.imageLargeUrl ??
    this.detail()?.imageSmallUrl ??
    this.card()?.imageLargeUrl ??
    this.card()?.imageSmallUrl ??
    null
  );
  readonly displayName = computed(() => {
    const detail = this.detail();
    return detail
      ? this.languageService.cardName(detail, this.language())
      : this.card()?.label ?? this.t('GAME.CARD_FALLBACK');
  });
  readonly setSummary = computed(() => {
    const detail = this.detail();
    const card = this.card();
    const setName = detail?.setName ?? card?.setName ?? detail?.setCode ?? card?.setCode;
    const number = detail?.number ?? card?.number;
    if (!setName && !number) {
      return null;
    }

    return number ? `${setName ?? 'Set'} - #${number}` : setName ?? null;
  });
  readonly category = computed(() => this.detail()?.category ?? this.card()?.category ?? null);
  readonly subtype = computed(() => {
    const detail = this.detail();
    return detail ? this.languageService.subtype(detail, this.language()) : this.card()?.subtype ?? null;
  });
  readonly evolvesFrom = computed(() => this.detail()?.evolvesFrom ?? this.card()?.evolvesFrom ?? null);
  readonly hp = computed(() => this.detail()?.hp ?? this.card()?.hp ?? null);
  readonly pokemonType = computed(() =>
    this.detail()?.pokemonType ?? this.card()?.pokemonType ?? this.resolvePokemonTypeFromCategory()
  );
  readonly displayPokemonType = computed(() => {
    const detail = this.detail();
    return detail ? this.languageService.pokemonType(detail, this.language()) : this.pokemonType();
  });
  readonly retreatCost = computed(() => this.detail()?.retreatCost ?? this.card()?.retreatCost ?? null);
  readonly weaknesses = computed(() => this.detail()?.weaknesses ?? this.card()?.weaknesses ?? null);
  readonly resistances = computed(() => this.detail()?.resistances ?? this.card()?.resistances ?? null);
  readonly attacks = computed(() =>
    this.detail()?.attacks?.map((attack, index) => this.detailAttack(attack, index, this.context()?.attacks?.[index])) ??
    this.context()?.attacks ??
    this.cardAttacks()
  );
  readonly abilities = computed<DisplayedAbilityViewModel[]>(() => this.displayedAbilities());
  readonly selectedAttack = computed(() => {
    const selectedIndex = this.selectedAttackIndex();
    return selectedIndex === null ? null : this.attacks()[selectedIndex] ?? null;
  });
  readonly selectedAbility = computed(() => {
    const selectedIndex = this.selectedAbilityIndex();
    return selectedIndex === null ? null : this.abilities()[selectedIndex] ?? null;
  });
  readonly selectedAttackAvailability = computed(() => {
    const selectedIndex = this.selectedAttackIndex();
    return selectedIndex === null ? null : this.attackAvailabilityByIndex()[selectedIndex] ?? null;
  });
  readonly selectedAbilityAvailability = computed(() => {
    const selectedIndex = this.selectedAbilityIndex();
    return selectedIndex === null ? null : this.abilityAvailabilityByIndex()[selectedIndex] ?? null;
  });
  readonly selectedAbilitySelection = computed(() => {
    const selectedIndex = this.selectedAbilityIndex();
    return selectedIndex === null ? null : this.abilitySelectionByIndex()[selectedIndex] ?? null;
  });
  readonly canSelectAttacks = computed(() => this.canDeclareAttack());
  readonly canSelectAbilities = computed(() =>
    Boolean(this.context()?.owner === 'local' && this.context()?.pokemonInPlayId && this.abilities().length)
  );
  readonly canShowAbilityAction = computed(() =>
    Boolean(
      this.canSelectAbilities() &&
      this.selectedAbility() &&
      !this.isPassiveAbility(this.selectedAbility())
    )
  );
  readonly selectedAbilityHandCardOptions = computed<AbilityCardOption[]>(() =>
    this.selectedAbilitySelection()?.handCardOptions ?? []
  );
  readonly selectedAbilityTargetPokemonOptions = computed(() =>
    this.selectedAbilitySelection()?.targetPokemonOptions ?? []
  );
  readonly selectedAbilityFromPokemonOptions = computed(() =>
    this.selectedAbilitySelection()?.fromPokemonOptions ?? []
  );
  readonly selectedAbilityToPokemonOptions = computed(() => {
    const fromPokemonId = this.selectedAbilityFromPokemonInPlayId();
    const options = this.selectedAbilitySelection()?.toPokemonOptions ?? [];
    return fromPokemonId ? options.filter((option) => option.pokemonInPlayId !== fromPokemonId) : options;
  });
  readonly selectedAbilitySourceEnergyOptions = computed<AbilityAttachedEnergyOption[]>(() => {
    const fromPokemonId = this.selectedAbilityFromPokemonInPlayId();
    if (!fromPokemonId) {
      return [];
    }

    return this.selectedAbilitySelection()?.attachedEnergyOptionsByPokemonId?.[fromPokemonId] ?? [];
  });
  readonly selectedAbilityDeckCardOptions = computed<AbilityCardOption[]>(() =>
    this.selectedAbilitySelection()?.deckCardOptions ?? []
  );
  readonly healTargetOptions = computed(() => {
    const selectedAttack = this.selectedAttack();
    if (!selectedAttack?.requiresTarget) {
      return [];
    }

    const validIds = new Set(selectedAttack.validTargetPokemonInPlayIds ?? []);
    const candidates = selectedAttack.targetsOwnPokemon ? this.ownPokemonOptions() : this.opponentPokemonOptions();
    return candidates.filter((option) => validIds.has(option.pokemonInPlayId));
  });
  readonly canConfirmAttack = computed(() =>
    Boolean(
      this.canDeclareAttack() &&
      !this.actionPending() &&
      this.selectedAttack() &&
      this.selectedAttackAvailability()?.available &&
      (!this.selectedAttack()?.requiresTarget || this.selectedHealTargetPokemonInPlayId())
    )
  );
  readonly selectedAttackHasOptionalBonusDamage = computed(() => {
    const selectedAttack = this.selectedAttack();
    const name = selectedAttack?.name ?? selectedAttack?.label;
    return Boolean(name && OPTIONAL_BONUS_DAMAGE_ATTACK_NAMES.has(name));
  });
  readonly selectedAttackError = computed(() => {
    const selectedIndex = this.selectedAttackIndex();
    if (selectedIndex === null) {
      return null;
    }

    const selectedAttack = this.resolveAttackByIndex(selectedIndex);
    if (!selectedAttack?.id) {
      return 'El ataque seleccionado no existe en el Pokemon activo.';
    }

    return null;
  });
  readonly canConfirmAbility = computed(() =>
    Boolean(
      this.canUseAbility() &&
      !this.actionPending() &&
      this.selectedAbility() &&
      this.selectedAbilityAvailability()?.available &&
      !this.selectedAbilitySelection()?.unsupportedReason &&
      this.isSelectedAbilityComplete()
    )
  );
  readonly trainerType = computed(() => {
    const cat = this.category();
    return cat ? (TRAINER_CATEGORY_MAP[cat] ?? null) : null;
  });
  readonly isTrainer = computed(() => this.trainerType() !== null);
  readonly rules = computed(() => {
    const detail = this.detail();
    if (!detail) {
      return [];
    }
    return this.languageService.useSpanish(this.language())
      ? detail.displayRules ?? detail.rules ?? []
      : detail.rules ?? [];
  });
  readonly isPokemonCard = computed(() =>
    this.detail()?.supertype === CardSupertype.Pokemon || this.card()?.supertype === CardSupertype.Pokemon
  );
  readonly attackDisabledReason = computed(() => {
    if (this.actionPending()) {
      return this.t('GAME.DISABLED.ACTION_PENDING');
    }

    return this.translatedDisabledReason(this.selectedAttackAvailability()?.reason);
  });
  readonly abilityDisabledReason = computed(() => {
    if (this.actionPending()) {
      return this.t('GAME.DISABLED.ACTION_PENDING');
    }

    if (this.selectedAbilitySelection()?.unsupportedReason) {
      return this.selectedAbilitySelection()?.unsupportedReason ?? null;
    }

    if (this.selectedAbilityAvailability()?.available && !this.isSelectedAbilityComplete()) {
      return this.missingAbilitySelectionReason();
    }

    return this.languageService.gameDisabledReason(
      this.selectedAbilityAvailability()?.reason,
      'GAME.BACKEND_PENDING'
    );
  });

  trainerTypeI18nKey(): string {
    switch (this.trainerType()) {
      case 'item':      return 'GAME.TRAINER.TYPE.ITEM';
      case 'ace_spec':  return 'GAME.TRAINER.TYPE.ACE_SPEC';
      case 'supporter': return 'GAME.TRAINER.TYPE.SUPPORTER';
      case 'stadium':   return 'GAME.TRAINER.TYPE.STADIUM';
      case 'tool':      return 'GAME.TRAINER.TYPE.TOOL';
      default:          return 'GAME.TRAINER.TYPE.ITEM';
    }
  }

  readonly GameActionType = GameActionType;

  isAbilityBlocked(index: number): boolean {
    const availability = this.abilityAvailabilityByIndex()[index];
    const ability = this.abilities()[index];
    return availability?.available === false || ability?.enabled === false || this.isPassiveAbility(ability);
  }

  close(): void {
    this.clearSelections();
    this.closed.emit();
  }

  private displayedAbilities(): DisplayedAbilityViewModel[] {
    const contextAbilities = this.context()?.abilities ?? [];
    const cardAbilities = this.detail()?.abilities ?? [];
    const legacyDisplayAbilities = this.detail()?.displayAbilities ?? [];
    const detailAbilities = cardAbilities.length ? cardAbilities : legacyDisplayAbilities;
    const longestLength = Math.max(contextAbilities.length, detailAbilities.length);
    if (longestLength === 0) {
      return [];
    }

    return Array.from({ length: longestLength }, (_, index) =>
      this.mergeAbility(detailAbilities[index] ?? null, contextAbilities[index] ?? null, index)
    );
  }

  private mergeAbility(
    detailAbility: CardAbility | DisplayAbility | null,
    contextAbility: BoardPokemonAbilityViewModel | null,
    index: number
  ): DisplayedAbilityViewModel {
    const abilityName = detailAbility && 'name' in detailAbility
      ? this.languageService.abilityName(detailAbility, this.language())
      : detailAbility?.displayName ?? null;
    const abilityText = detailAbility && 'name' in detailAbility
      ? this.languageService.abilityText(detailAbility, this.language())
      : detailAbility?.displayText ?? null;

    return {
      id: contextAbility?.id ?? (detailAbility && 'id' in detailAbility ? detailAbility.id ?? undefined : undefined) ?? `ability-${index}`,
      label:
        contextAbility?.label ??
        abilityName ??
        this.t('GAME.ABILITY_FALLBACK'),
      activationType: contextAbility?.activationType ?? null,
      description: abilityText,
      enabled: contextAbility?.enabled ?? true,
      disabledReason: contextAbility?.disabledReason ?? null
    };
  }

  private isPassiveAbility(ability: BoardPokemonAbilityViewModel | null | undefined): boolean {
    return ability?.activationType?.toUpperCase() === PASSIVE_ABILITY_ACTIVATION_TYPE;
  }

  selectAttack(index: number): void {
    if (!this.canSelectAttacks()) {
      return;
    }

    const attackAvailability = this.attackAvailabilityByIndex()[index];
    if (attackAvailability && attackAvailability.available === false) {
      return;
    }

    this.selectedAttackIndex.set(this.selectedAttackIndex() === index ? null : index);
    this.selectedAbilityIndex.set(null);
    this.selectedHealTargetPokemonInPlayId.set(null);
    this.clearAbilitySelections();
  }

  selectHealTarget(pokemonInPlayId: string): void {
    this.selectedHealTargetPokemonInPlayId.set(
      this.selectedHealTargetPokemonInPlayId() === pokemonInPlayId ? null : pokemonInPlayId
    );
  }

  selectAbility(index: number): void {
    const disabled = this.isAbilityBlocked(index);

    if (!this.canSelectAbilities()) {
      return;
    }

    if (disabled) {
      return;
    }

    const nextSelectedIndex = this.selectedAbilityIndex() === index ? null : index;
    this.selectedAbilityIndex.set(nextSelectedIndex);
    this.selectedAttackIndex.set(null);
    this.selectedHealTargetPokemonInPlayId.set(null);
    this.clearAbilitySelections();
    if (nextSelectedIndex !== null) {
      this.applyDefaultAbilitySelections(nextSelectedIndex);
    }
  }

  declareSelectedAttack(useBonusDamage = false): void {
    const selectedIndex = this.selectedAttackIndex();
    const selectedAttack = selectedIndex === null ? null : this.resolveAttackByIndex(selectedIndex);
    if (selectedIndex === null || !selectedAttack || !selectedAttack.id || !this.canConfirmAttack()) {
      return;
    }

    const selectedTargetId = this.selectedHealTargetPokemonInPlayId() ?? undefined;
    const targetsOwnPokemon = selectedAttack.targetsOwnPokemon ?? false;
    this.attackDeclared.emit({
      attackId: selectedAttack.id,
      attackIndex: selectedIndex,
      attackName: selectedAttack.name ?? selectedAttack.label,
      useBonusDamage,
      selfTargetPokemonInPlayId: targetsOwnPokemon ? selectedTargetId : undefined,
      switchTargetPokemonInPlayId: targetsOwnPokemon ? undefined : selectedTargetId
    });
    this.clearSelections();
  }

  declareSelectedAbility(): void {
    const selectedIndex = this.selectedAbilityIndex();
    const selectedAbility = this.selectedAbility();
    const sourcePokemonId = this.context()?.pokemonInPlayId;
    if (selectedIndex === null || !selectedAbility || !sourcePokemonId || !this.canConfirmAbility()) {
      return;
    }

    this.abilityDeclared.emit({
      sourcePokemonId,
      ability: selectedAbility,
      payload: this.buildSelectedAbilityPayload(sourcePokemonId, selectedAbility.id)
    });
    this.clearSelections();
  }

  isAbilitySelectable(index: number): boolean {
    return this.canSelectAbilities() && !this.isAbilityBlocked(index);
  }

  selectAbilityHandCard(cardInstanceId: string): void {
    this.selectedAbilityHandCardId.set(
      this.selectedAbilityHandCardId() === cardInstanceId ? null : cardInstanceId
    );
  }

  selectAbilityTargetPokemon(pokemonInPlayId: string): void {
    this.selectedAbilityTargetPokemonInPlayId.set(
      this.selectedAbilityTargetPokemonInPlayId() === pokemonInPlayId ? null : pokemonInPlayId
    );
  }

  selectAbilityFromPokemon(pokemonInPlayId: string): void {
    const nextValue = this.selectedAbilityFromPokemonInPlayId() === pokemonInPlayId ? null : pokemonInPlayId;
    this.selectedAbilityFromPokemonInPlayId.set(nextValue);
    this.selectedAbilitySourceEnergyCardId.set(null);
    if (nextValue !== null && this.selectedAbilityToPokemonInPlayId() === nextValue) {
      this.selectedAbilityToPokemonInPlayId.set(null);
    }
  }

  selectAbilityToPokemon(pokemonInPlayId: string): void {
    this.selectedAbilityToPokemonInPlayId.set(
      this.selectedAbilityToPokemonInPlayId() === pokemonInPlayId ? null : pokemonInPlayId
    );
  }

  selectAbilitySourceEnergy(cardInstanceId: string): void {
    this.selectedAbilitySourceEnergyCardId.set(
      this.selectedAbilitySourceEnergyCardId() === cardInstanceId ? null : cardInstanceId
    );
  }

  selectAbilityDeckCard(cardInstanceId: string): void {
    this.selectedAbilityDeckCardId.set(
      this.selectedAbilityDeckCardId() === cardInstanceId ? null : cardInstanceId
    );
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    if (this.isOpen()) {
      this.close();
    }
  }

  onCardMouseMove(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();

    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const xPct = (x / rect.width) * 100;
    const yPct = (y / rect.height) * 100;

    const rotateY = ((x / rect.width) - 0.5) * 60;
    const rotateX = ((y / rect.height) - 0.5) * -60;

    this.cardRotateX.set(rotateX);
    this.cardRotateY.set(rotateY);
    this.cardGlareX.set(xPct);
    this.cardGlareY.set(yPct);
  }

  onCardMouseLeave(): void {
    this.cardRotateX.set(0);
    this.cardRotateY.set(0);
    this.cardGlareX.set(50);
    this.cardGlareY.set(50);
  }

  getEnergyColor(type: string | null | undefined): string {
    return ENERGY_COLORS[type ?? ''] ?? '#d8c9a8';
  }

  formatCategory(value: string | null | undefined): string {
    if (!value) {
      return this.t('COMMON.NO_DATA');
    }

    return value
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  relationSummary(relations: CardRelation[] | null | undefined, language: AppLanguage): string {
    if (!relations?.length) {
      return this.t('COMMON.NO_DATA');
    }

    return this.languageService.relationSummary(relations, language);
  }

  range(length: number): number[] {
    return Array.from({ length }, (_value, index) => index);
  }

  attackDamage(attack: BoardPokemonAttackViewModel): string | number {
    return attack.damageText ?? attack.baseDamage ?? '-';
  }

  attackCosts(attack: BoardPokemonAttackViewModel): BoardPokemonAttackViewModel['costs'] {
    return attack.costs ?? [];
  }

  attackCostDisplayLabel(
    attack: BoardPokemonAttackViewModel,
    costIndex: number,
    pipIndex: number,
    language: AppLanguage
  ): string {
    return this.languageService.attackCostLabel(attack, costIndex, pipIndex, language);
  }

  attackDisplayName(attack: BoardPokemonAttackViewModel, language: AppLanguage): string {
    return this.languageService.attackName({
      name: attack.name ?? attack.label,
      displayName: attack.displayName
    }, language);
  }

  attackDisplayText(attack: BoardPokemonAttackViewModel, language: AppLanguage): string | null {
    return this.languageService.attackText(attack, language);
  }

  translatedDisabledReason(reason: string | null | undefined): string {
    return this.languageService.gameDisabledReason(reason, 'GAME.DISABLED.ATTACK_UNAVAILABLE');
  }

  private resolveAttackByIndex(index: number): BoardPokemonAttackViewModel | null {
    return this.attacks()[index] ?? null;
  }

  private cardAttacks(): BoardPokemonAttackViewModel[] {
    return (this.card()?.attacks ?? []).map((attack) => ({
      id: attack.id,
      label: attack.name,
      name: attack.name,
      displayName: attack.displayName,
      damageText: attack.damageText,
      baseDamage: attack.baseDamage,
      effectText: attack.effectText,
      displayText: attack.displayText,
      costs: attack.costs ?? [],
      displayCost: attack.displayCost,
      enabled: false,
      disabledReason: null,
      requiresTarget: false,
      validTargetPokemonInPlayIds: [],
      targetsOwnPokemon: false
    }));
  }

  private detailAttack(
    attack: Attack,
    index: number,
    contextAttack: BoardPokemonAttackViewModel | undefined
  ): BoardPokemonAttackViewModel {
    return {
      id: contextAttack?.id ?? attack.id ?? `${attack.name}-${index}`,
      label: attack.name,
      name: attack.name,
      displayName: attack.displayName,
      damageText: attack.damageText,
      baseDamage: attack.baseDamage,
      effectText: attack.effectText,
      displayText: attack.displayText,
      costs: attack.costs ?? [],
      displayCost: attack.displayCost,
      enabled: contextAttack?.enabled ?? false,
      disabledReason: contextAttack?.disabledReason ?? null,
      requiresTarget: contextAttack?.requiresTarget ?? false,
      validTargetPokemonInPlayIds: contextAttack?.validTargetPokemonInPlayIds ?? [],
      targetsOwnPokemon: contextAttack?.targetsOwnPokemon ?? false
    };
  }

  private clearSelections(): void {
    this.selectedAttackIndex.set(null);
    this.selectedAbilityIndex.set(null);
    this.selectedHealTargetPokemonInPlayId.set(null);
    this.clearAbilitySelections();
  }

  private clearAbilitySelections(): void {
    this.selectedAbilityHandCardId.set(null);
    this.selectedAbilityTargetPokemonInPlayId.set(null);
    this.selectedAbilityFromPokemonInPlayId.set(null);
    this.selectedAbilityToPokemonInPlayId.set(null);
    this.selectedAbilitySourceEnergyCardId.set(null);
    this.selectedAbilityDeckCardId.set(null);
  }

  private applyDefaultAbilitySelections(index: number): void {
    const selection = this.abilitySelectionByIndex()[index];
    if (!selection) {
      return;
    }

    if (selection.requiresHandCard && selection.handCardOptions?.length === 1) {
      this.selectedAbilityHandCardId.set(selection.handCardOptions[0].cardInstanceId);
    }
    if (selection.requiresTargetPokemon && selection.targetPokemonOptions?.length === 1) {
      this.selectedAbilityTargetPokemonInPlayId.set(selection.targetPokemonOptions[0].pokemonInPlayId);
    }
  }

  private buildSelectedAbilityPayload(sourcePokemonId: string, abilityCode: string): UseAbilityPayload {
    return {
      sourcePokemonId,
      abilityCode,
      selectedHandCardId: this.selectedAbilityHandCardId() ?? undefined,
      targetPokemonId: this.selectedAbilityTargetPokemonInPlayId() ?? undefined,
      sourceEnergyCardId: this.selectedAbilitySourceEnergyCardId() ?? undefined,
      fromPokemonId: this.selectedAbilityFromPokemonInPlayId() ?? undefined,
      toPokemonId: this.selectedAbilityToPokemonInPlayId() ?? undefined,
      selectedDeckCardId: this.selectedAbilityDeckCardId() ?? undefined
    };
  }

  private isSelectedAbilityComplete(): boolean {
    const selection = this.selectedAbilitySelection();
    if (!selection) {
      return false;
    }

    if (selection.requiresHandCard && !this.selectedAbilityHandCardId()) {
      return false;
    }
    if (selection.requiresTargetPokemon && !this.selectedAbilityTargetPokemonInPlayId()) {
      return false;
    }
    if (selection.requiresFromPokemon && !this.selectedAbilityFromPokemonInPlayId()) {
      return false;
    }
    if (selection.requiresToPokemon && !this.selectedAbilityToPokemonInPlayId()) {
      return false;
    }
    if (selection.requiresSourceEnergy && !this.selectedAbilitySourceEnergyCardId()) {
      return false;
    }
    if (selection.requiresDeckCard && !this.selectedAbilityDeckCardId()) {
      return false;
    }

    return true;
  }

  private missingAbilitySelectionReason(): string {
    const selection = this.selectedAbilitySelection();
    if (!selection) {
      return this.t('GAME.BACKEND_PENDING');
    }

    if (selection.requiresHandCard && (selection.handCardOptions?.length ?? 0) === 0) {
      return 'No hay cartas validas disponibles para esta habilidad.';
    }
    if (selection.requiresHandCard && !this.selectedAbilityHandCardId()) {
      return selection.handCardPrompt ?? 'Falta elegir una carta.';
    }
    if (selection.requiresTargetPokemon && (selection.targetPokemonOptions?.length ?? 0) === 0) {
      return 'No hay Pokemon objetivo validos para esta habilidad.';
    }
    if (selection.requiresTargetPokemon && !this.selectedAbilityTargetPokemonInPlayId()) {
      return selection.targetPokemonPrompt ?? 'Falta elegir un Pokemon objetivo.';
    }
    if (selection.requiresFromPokemon && (selection.fromPokemonOptions?.length ?? 0) === 0) {
      return 'No hay Pokemon origen validos para esta habilidad.';
    }
    if (selection.requiresFromPokemon && !this.selectedAbilityFromPokemonInPlayId()) {
      return selection.fromPokemonPrompt ?? 'Falta elegir el Pokemon origen.';
    }
    if (selection.requiresSourceEnergy && this.selectedAbilityFromPokemonInPlayId() && this.selectedAbilitySourceEnergyOptions().length === 0) {
      return 'No hay Energias validas para mover desde ese Pokemon.';
    }
    if (selection.requiresSourceEnergy && !this.selectedAbilitySourceEnergyCardId()) {
      return selection.sourceEnergyPrompt ?? 'Falta elegir la Energia.';
    }
    if (selection.requiresToPokemon && (this.selectedAbilityToPokemonOptions().length ?? 0) === 0) {
      return 'No hay Pokemon destino validos para esta habilidad.';
    }
    if (selection.requiresToPokemon && !this.selectedAbilityToPokemonInPlayId()) {
      return selection.toPokemonPrompt ?? 'Falta elegir el Pokemon destino.';
    }
    if (selection.requiresDeckCard && (selection.deckCardOptions?.length ?? 0) === 0) {
      return 'No hay evoluciones validas en el mazo.';
    }
    if (selection.requiresDeckCard && !this.selectedAbilityDeckCardId()) {
      return selection.deckCardPrompt ?? 'Falta elegir una carta del mazo.';
    }

    return this.t('GAME.BACKEND_PENDING');
  }

  private resolvePokemonTypeFromCategory(): string | null {
    const category = this.category()?.toLowerCase() ?? '';
    return Object.keys(ENERGY_COLORS).find((type) => category.includes(type.toLowerCase())) ?? null;
  }
}
