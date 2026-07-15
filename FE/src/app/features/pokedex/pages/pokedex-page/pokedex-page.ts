import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { CardSupertype } from '../../../../core/models/enums/card/card-supertype.enum';
import {
  CardRelation,
  CardSummary
} from '../../../../core/models/interfaces/card/card-summary.interface';
import { AppLanguage, LanguageService } from '../../../../core/services/language.service';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { CardsApiService } from '../../../../infrastructure/api/card/cards-api.service';
import { CardDetailModalComponent } from '../../../../shared/components/card-detail-modal/card-detail-modal.component';
import {
  SharedAttackViewModel,
  SharedCardModalViewModel
} from '../../../../shared/models/card-modal.interface';
import { OakTutorialService } from '../../../home/services/oak-tutorial.service';
import { isDesktopViewport, isMobileLandscapeViewport } from '../../../../shared/utils/responsive-mode.util';

type PokedexTypeFilter = 'ALL' | CardSupertype;

const CARD_SHOWCASE_BASE =
  'pokedex-card-showcase relative flex min-h-[24rem] w-full max-w-[20rem] self-center items-center justify-center overflow-visible rounded-[2rem] border border-transparent bg-transparent p-6 shadow-none ring-0 xl:min-h-[26rem]';

const CARD_SHOWCASE_THEMES: Record<string, string> = {};

const DEFAULT_SHOWCASE_THEME = 'bg-transparent border-transparent shadow-none ring-0';

const TYPE_FILTERS: { value: PokedexTypeFilter; labelKey: string }[] = [
  { value: 'ALL', labelKey: 'POKEDEX.FILTERS.ALL' },
  { value: CardSupertype.Pokemon, labelKey: 'POKEDEX.FILTERS.POKEMON' },
  { value: CardSupertype.Energy, labelKey: 'POKEDEX.FILTERS.ENERGY' },
  { value: CardSupertype.Trainer, labelKey: 'POKEDEX.FILTERS.TRAINER' }
];


@Component({
  selector: 'app-pokedex-page',
  imports: [CardDetailModalComponent],
  templateUrl: './pokedex-page.html',
  styleUrl: './pokedex-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PokedexPage implements OnInit, OnDestroy {
  private readonly cardsApi = inject(CardsApiService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tutorialService = inject(OakTutorialService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly failedImages = signal(new Set<string>());
  private requestedCardId: string | null = null;
  private shouldAutoStartTutorial = false;
  private autoTutorialStarted = false;
  private openedCardFromRouteId: string | null = null;
  private readonly handleViewportChange = (): void => {
    window.setTimeout(() => this.updatePokedexViewportMode(), 150);
  };

  private readonly energyColors: Record<string, string> = {
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

  readonly cards = signal<CardSummary[]>([]);
  readonly isLightTheme = computed(() => this.appTheme.theme() === 'LIGHT');
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly selectedId = signal<string | null>(null);
  readonly search = signal('');
  readonly typeFilter = signal<PokedexTypeFilter>('ALL');
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');
  readonly typeFilters = TYPE_FILTERS;
  readonly totalCards = computed(() => this.cards().length);
  readonly isPokedexMobile = signal(false);
  readonly isMobileCardModalOpen = signal(false);
  readonly selectedMobileCard = signal<SharedCardModalViewModel | null>(null);

  readonly cardRotateX = signal(0);
  readonly cardRotateY = signal(0);
  readonly cardGlareX = signal(50);
  readonly cardGlareY = signal(50);
  readonly cardStyle = computed(() => {
    const card = this.selectedCard();
    const color = this.getEnergyColor(card?.pokemonType);

    return {
      transform: `perspective(1000px) rotateX(${this.cardRotateX()}deg) rotateY(${this.cardRotateY()}deg)`,
      '--glare-x': `${this.cardGlareX()}%`,
      '--glare-y': `${this.cardGlareY()}%`,
      '--glare-color': color
    };
  });

  readonly filteredCards = computed(() => {
    const search = this.search().trim().toLowerCase();
    const type = this.typeFilter();

    return this.cards().filter((card) => {
      const displayName = card.displayName?.toLowerCase() ?? '';
      const matchesSearch =
        !search || card.name.toLowerCase().includes(search) || displayName.includes(search);
      const matchesType = type === 'ALL' || card.supertype === type;
      return matchesSearch && matchesType;
    });
  });

  readonly selectedCard = computed((): CardSummary | null => {
    const selectedId = this.selectedId();
    const cards = this.filteredCards();
    return cards.find((card) => card.id === selectedId) ?? cards[0] ?? null;
  });

  constructor() {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.requestedCardId = params.get('cardId');
        this.shouldAutoStartTutorial =
          params.get('startTutorial') === 'pokedex' &&
          params.get('fromDeckTutorial') === 'true';
        this.applyTutorialNavigationRequest();
      });

    this.loadCards();
  }

  ngOnInit(): void {
    this.updatePokedexViewportMode();

    window.addEventListener('resize', this.handleViewportChange);
    window.addEventListener('orientationchange', this.handleViewportChange);
    window.visualViewport?.addEventListener('resize', this.handleViewportChange);
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.handleViewportChange);
    window.removeEventListener('orientationchange', this.handleViewportChange);
    window.visualViewport?.removeEventListener('resize', this.handleViewportChange);
  }

  loadCards(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.cardsApi.getCards().subscribe({
      next: (cards) => {
        this.cards.set(cards);
        
        const cardIdParam = this.requestedCardId;
        if (cardIdParam && cards.some(c => c.id === cardIdParam)) {
          this.selectedId.set(cardIdParam);
        } else {
          this.selectedId.set(cards[0]?.id ?? null);
        }
        
        this.isLoading.set(false);
        this.applyTutorialNavigationRequest();
      },
      error: () => {
        this.cards.set([]);
        this.selectedId.set(null);
        this.errorMessage.set('POKEDEX.LOAD_ERROR');
        this.isLoading.set(false);
      }
    });
  }

  private applyTutorialNavigationRequest(): void {
    if (this.isLoading() || this.cards().length === 0) {
      return;
    }

    if (this.requestedCardId && this.cards().some((card) => card.id === this.requestedCardId)) {
      this.selectedId.set(this.requestedCardId);
      this.maybeOpenCardModalFromRoute(this.requestedCardId);
    }

    if (!this.shouldAutoStartTutorial || this.autoTutorialStarted) {
      return;
    }

    this.autoTutorialStarted = true;
    window.setTimeout(() => {
      this.tutorialService.start();
      void this.clearTutorialQueryParams();
    });
  }

  /**
   * Cuando se navega a la Pokédex con un cardId por query param (p. ej. desde
   * las cartas del mazo activo en Mazo), en mobile debe abrirse el modal de
   * detalle de esa carta exacta. En desktop, el cardId ya selecciona la carta
   * en el panel principal (selectedId) y no requiere modal.
   */
  private maybeOpenCardModalFromRoute(cardId: string): void {
    if (!this.isPokedexMobile()) {
      return;
    }

    if (this.openedCardFromRouteId === cardId) {
      return;
    }

    const card = this.cards().find((c) => c.id === cardId);
    if (!card) {
      return;
    }

    this.openedCardFromRouteId = cardId;
    this.openMobileCardModal(card);
  }

  private clearTutorialQueryParams(): Promise<boolean> {
    return this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        startTutorial: null,
        fromDeckTutorial: null
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  selectCard(id: string): void {
    this.selectedId.set(id);
  }

  openMobileCardModal(card: CardSummary): void {
    this.selectedMobileCard.set(this.mapCardToModalViewModel(card));
    this.isMobileCardModalOpen.set(true);
  }

  closeMobileCardModal(): void {
    this.isMobileCardModalOpen.set(false);
    this.selectedMobileCard.set(null);
    this.openedCardFromRouteId = null;
  }

  setTypeFilter(filter: PokedexTypeFilter): void {
    this.typeFilter.set(filter);
  }

  updateSearch(event: Event): void {
    this.search.set(event.target instanceof HTMLInputElement ? event.target.value : '');
  }

  onImgError(key: string): void {
    this.failedImages.update((failedImages) => new Set([...failedImages, key]));
  }

  isImageFailed(key: string): boolean {
    return this.failedImages().has(key);
  }

  getEnergyColor(type: string | null | undefined): string {
    return this.energyColors[type ?? ''] ?? '#d8c9a8';
  }

  cardDisplayName(card: CardSummary, language: AppLanguage): string {
    return this.languageService.cardName(card, language);
  }

  cardDisplayPokemonType(card: CardSummary, language: AppLanguage): string | null {
    return this.languageService.pokemonType(card, language);
  }

  cardDisplaySubtype(card: CardSummary, language: AppLanguage): string | null {
    return this.languageService.subtype(card, language);
  }

  attackDisplayName(attack: NonNullable<CardSummary['attacks']>[number], language: AppLanguage): string {
    return this.languageService.attackName(attack, language);
  }

  attackDisplayText(attack: NonNullable<CardSummary['attacks']>[number], language: AppLanguage): string | null {
    return this.languageService.attackText(attack, language);
  }

  abilityDisplayName(ability: NonNullable<CardSummary['abilities']>[number], language: AppLanguage): string {
    return this.languageService.abilityName(ability, language);
  }

  abilityDisplayType(ability: NonNullable<CardSummary['abilities']>[number], language: AppLanguage): string | null {
    return this.languageService.abilityType(ability, language);
  }

  abilityDisplayText(ability: NonNullable<CardSummary['abilities']>[number], language: AppLanguage): string | null {
    return this.languageService.abilityText(ability, language);
  }

  cardDisplayRules(card: CardSummary, language: AppLanguage): string[] {
    return this.languageService.useSpanish(language)
      ? card.displayRules ?? card.rules ?? []
      : card.rules ?? [];
  }

  isPokemonCard(card: CardSummary): boolean {
    return card.supertype === CardSupertype.Pokemon;
  }

  isTrainerCard(card: CardSummary): boolean {
    return card.supertype === CardSupertype.Trainer;
  }

  attackCostDisplayLabel(
    attack: NonNullable<CardSummary['attacks']>[number],
    costIndex: number,
    pipIndex: number,
    language: AppLanguage
  ): string {
    return this.languageService.attackCostLabel(attack, costIndex, pipIndex, language);
  }

  cardShowcaseClass(type: string | null | undefined): string {
    const theme = CARD_SHOWCASE_THEMES[type ?? ''] ?? DEFAULT_SHOWCASE_THEME;
    void this.isLightTheme();
    return `${CARD_SHOWCASE_BASE} ${theme}`;
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

  formatCategory(value: string | null, language: AppLanguage): string {
    void language;
    if (!value) {
      return this.t('COMMON.NO_DATA');
    }

    const translatedValue = this.t(`POKEDEX.CATEGORIES.${value}`);
    return translatedValue !== `POKEDEX.CATEGORIES.${value}` ? translatedValue : value
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

  text(key: string, language: AppLanguage): string {
    void language;
    return this.t(key);
  }

  typeFilterLabel(filter: (typeof TYPE_FILTERS)[number], language: AppLanguage): string {
    void language;
    return this.t(filter.labelKey);
  }

  range(length: number): number[] {
    return Array.from({ length }, (_value, index) => index);
  }

  private updatePokedexViewportMode(): void {
    this.isPokedexMobile.set(!isDesktopViewport() && isMobileLandscapeViewport());
  }

  private mapCardToModalViewModel(card: CardSummary): SharedCardModalViewModel {
    return {
      id: card.id,
      name: this.cardDisplayName(card, this.language()),
      imageUrl: card.imageLargeUrl ?? card.imageSmallUrl,
      supertypeCode: card.supertype,
      supertype: card.displaySupertype ?? card.supertype,
      subtypes: card.displaySubtypes ?? (card.subtype ? [this.cardDisplaySubtype(card, this.language()) ?? card.subtype] : []),
      types: card.displayPokemonType ?? card.pokemonType ? [this.cardDisplayPokemonType(card, this.language()) ?? card.pokemonType ?? ''] : [],
      hp: card.hp,
      evolvesFrom: card.evolvesFrom,
      rarity: card.setName,
      number: card.number,
      attacks: (card.attacks ?? []).map((attack): SharedAttackViewModel => ({
        id: attack.id,
        name: this.attackDisplayName(attack, this.language()),
        cost: this.attackCostList(attack),
        convertedEnergyCost: this.attackCostList(attack).length,
        damage: attack.damageText ?? attack.baseDamage,
        text: this.attackDisplayText(attack, this.language())
      })),
      abilities: (card.abilities ?? []).map((ability) => ({
        id: ability.id ?? ability.code ?? undefined,
        name: this.abilityDisplayName(ability, this.language()),
        type: this.abilityDisplayType(ability, this.language()),
        text: this.abilityDisplayText(ability, this.language())
      })),
      displayRules: this.cardDisplayRules(card, this.language()),
      weaknesses: card.weaknesses ?? [],
      resistances: card.resistances ?? [],
      retreatCost: Array.from({ length: card.retreatCost ?? 0 }, () => 'Colorless')
    };
  }

  private attackCostList(attack: NonNullable<CardSummary['attacks']>[number]): string[] {
    if (attack.displayCost?.length) {
      return attack.displayCost;
    }

    return (attack.costs ?? []).flatMap((cost) =>
      Array.from({ length: cost.quantity }, () => cost.energyType)
    );
  }
}
