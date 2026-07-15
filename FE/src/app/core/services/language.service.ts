import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { STORAGE_KEYS } from '../constants/storage/storage.constants';
import { CardRelation, CardSummary } from '../models/interfaces/card/card-summary.interface';

export type AppLanguage = 'es' | 'en';
export type TranslationParams = Record<string, string | number | boolean | null | undefined>;
export type UiTranslations = Record<string, unknown>;
export type UiTranslateFn = (key: string, params?: TranslationParams) => string;

interface DisplayableAttack {
  name: string;
  displayName?: string | null;
  effectText?: string | null;
  displayText?: string | null;
  displayTextEn?: string | null;
}

interface DisplayableAttackCost {
  costs: {
    energyType: string | null;
    quantity: number;
  }[];
  displayCost?: string[] | null;
}

interface DisplayableAbility {
  name: string;
  type?: string | null;
  text?: string | null;
  displayName?: string | null;
  displayType?: string | null;
  displayText?: string | null;
}

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);
  private readonly languageState = signal<AppLanguage>(this.readStoredLanguage());
  private readonly translationsState = signal<UiTranslations>({});
  private readonly cache = new Map<AppLanguage, UiTranslations>();

  readonly language = this.languageState.asReadonly();
  readonly translations = this.translationsState.asReadonly();
  readonly isSpanish = computed(() => this.languageState() === 'es');
  readonly label = computed(() =>
    this.isSpanish() ? this.t('COMMON.LANGUAGE_SPANISH') : this.t('COMMON.LANGUAGE_ENGLISH')
  );

  async initialize(): Promise<void> {
    await this.loadLanguage(this.languageState());
  }

  setLanguage(language: AppLanguage): void {
    void this.useLanguage(language);
  }

  async useLanguage(language: AppLanguage): Promise<void> {
    await this.loadLanguage(language);
    this.languageState.set(language);
    this.writeStoredLanguage(language);
    this.document.documentElement.lang = language;
  }

  toggleLanguage(): void {
    this.setLanguage(this.isSpanish() ? 'en' : 'es');
  }

  t(key: string, params: TranslationParams = {}): string {
    const value = this.resolveKey(this.translationsState(), key);
    const text = typeof value === 'string' ? value : key;
    return this.interpolate(text, params);
  }

  gameDisabledReason(reason: string | null | undefined, fallbackKey = 'GAME.NO_AVAILABLE'): string {
    if (!reason) {
      return this.t(fallbackKey);
    }

    const normalizedReason = reason.toLowerCase();
    const mentionsInsufficientEnergy =
      normalizedReason.includes('energia insuficiente') ||
      normalizedReason.includes('energÃƒÆ’Ã‚Â­a insuficiente') ||
      normalizedReason.includes('insufficient energy');
    const mentionsUnavailableAction =
      normalizedReason.includes('accion no disponible') ||
      normalizedReason.includes('acciÃƒÆ’Ã‚Â³n no disponible') ||
      normalizedReason.includes('action not available');

    if (mentionsInsufficientEnergy || mentionsUnavailableAction) {
      return this.t('GAME.DISABLED.INSUFFICIENT_ENERGY_OR_ACTION');
    }

    if (normalizedReason.includes('pendiente de soporte backend')) {
      return this.t('GAME.BACKEND_PENDING');
    }

    return this.t(fallbackKey);
  }

  useSpanish(language: AppLanguage = this.languageState()): boolean {
    return language === 'es';
  }

  cardName(card: Pick<CardSummary, 'name' | 'displayName'>, language = this.languageState()): string {
    return this.useSpanish(language) ? card.displayName ?? card.name : card.name;
  }

  pokemonType(
    card: Pick<CardSummary, 'pokemonType' | 'displayPokemonType'>,
    language = this.languageState()
  ): string | null {
    return this.useSpanish(language) ? card.displayPokemonType ?? card.pokemonType : card.pokemonType;
  }

  supertype(card: Pick<CardSummary, 'supertype' | 'displaySupertype'>, language = this.languageState()): string {
    return this.useSpanish(language) ? card.displaySupertype ?? card.supertype : card.supertype;
  }

  subtype(card: Pick<CardSummary, 'subtype' | 'displaySubtypes'>, language = this.languageState()): string | null {
    return this.useSpanish(language) && card.displaySubtypes?.length ? card.displaySubtypes.join(', ') : card.subtype;
  }

  category(card: Pick<CardSummary, 'category' | 'displaySubtypes'>, language = this.languageState()): string {
    return this.useSpanish(language) && card.displaySubtypes?.length ? card.displaySubtypes.join(' / ') : card.category;
  }

  attackName(attack: DisplayableAttack, language = this.languageState()): string {
    return this.useSpanish(language) ? attack.displayName ?? attack.name : attack.name;
  }

  attackText(
    attack: Pick<DisplayableAttack, 'effectText' | 'displayText' | 'displayTextEn'>,
    language = this.languageState()
  ): string | null {
    return this.useSpanish(language)
      ? attack.displayText ?? attack.effectText ?? null
      : attack.displayTextEn ?? attack.effectText ?? null;
  }

  attackCostLabel(
    attack: DisplayableAttackCost,
    costIndex: number,
    pipIndex: number,
    language = this.languageState()
  ): string {
    const flatIndex = attack.costs
      .slice(0, costIndex)
      .reduce((total, cost) => total + cost.quantity, pipIndex);

    return this.useSpanish(language)
      ? attack.displayCost?.[flatIndex] ?? attack.costs[costIndex]?.energyType ?? this.t('COMMON.ENERGY')
      : attack.costs[costIndex]?.energyType ?? this.t('COMMON.ENERGY');
  }

  abilityName(ability: DisplayableAbility, language = this.languageState()): string {
    return this.useSpanish(language) ? ability.displayName ?? ability.name : ability.name;
  }

  abilityType(ability: DisplayableAbility, language = this.languageState()): string | null {
    return this.useSpanish(language) ? ability.displayType ?? ability.type ?? null : ability.type ?? null;
  }

  abilityText(ability: DisplayableAbility, language = this.languageState()): string | null {
    return this.useSpanish(language) ? ability.displayText ?? ability.text ?? null : ability.text ?? null;
  }

  relationSummary(relations: CardRelation[] | null | undefined, language = this.languageState()): string {
    if (!relations?.length) {
      return this.t('COMMON.NO_DATA');
    }

    return relations.map((relation) => this.relationLabel(relation, language)).join(' - ');
  }

  relationLabel(relation: CardRelation, language = this.languageState()): string {
    if (!this.useSpanish(language)) {
      return `${relation.energyType} ${relation.value}`;
    }

    return `${relation.displayEnergyType ?? relation.energyType} ${relation.displayValue ?? relation.value}`;
  }

  private readStoredLanguage(): AppLanguage {
    if (typeof localStorage === 'undefined') {
      return 'es';
    }

    const storedLanguage = localStorage.getItem(STORAGE_KEYS.language);
    return storedLanguage === 'en' || storedLanguage === 'es' ? storedLanguage : 'es';
  }

  private writeStoredLanguage(language: AppLanguage): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    localStorage.setItem(STORAGE_KEYS.language, language);
  }

  private async loadLanguage(language: AppLanguage): Promise<void> {
    const cachedTranslations = this.cache.get(language);
    if (cachedTranslations) {
      this.translationsState.set(cachedTranslations);
      this.document.documentElement.lang = language;
      return;
    }

    try {
      const translations = await firstValueFrom(
        this.http.get<UiTranslations>(`assets/i18n/${language}.json`)
      );
      this.cache.set(language, translations);
      this.translationsState.set(translations);
    } catch {
      this.translationsState.set({});
    }

    this.document.documentElement.lang = language;
  }

  private resolveKey(source: UiTranslations, key: string): unknown {
    return key.split('.').reduce<unknown>((currentValue, segment) => {
      if (!currentValue || typeof currentValue !== 'object') {
        return undefined;
      }

      return (currentValue as Record<string, unknown>)[segment];
    }, source);
  }

  private interpolate(text: string, params: TranslationParams): string {
    return text.replace(/\{\{\s*([^}]+?)\s*\}\}/g, (match, name: string) => {
      const value = params[name.trim()];
      return value === null || value === undefined ? match : String(value);
    });
  }
}
