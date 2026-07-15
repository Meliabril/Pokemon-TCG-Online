import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CardSupertype } from '../../../../core/models/enums/card/card-supertype.enum';
import { CardSummary } from '../../../../core/models/interfaces/card/card-summary.interface';
import { DeckCardRequest } from '../../../../core/models/interfaces/deck/deck-command.interface';
import {
  DeckActivationResponse,
  DeckSummary
} from '../../../../core/models/interfaces/deck/deck-summary.interface';
import { AppLanguage, LanguageService } from '../../../../core/services/language.service';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { CardsApiService } from '../../../../infrastructure/api/card/cards-api.service';
import { DeckApiService } from '../../../../infrastructure/api/deck/deck-api.service';
import { DeckEditorComponent } from '../../components/deck-editor/deck-editor.component';
import { DeckOverviewComponent } from '../../components/deck-overview/deck-overview.component';
import {
  DeckTypeFilter,
  DeckTypeFilterOption,
  DeckValidationHint,
  DraftDeckItem
} from '../../models/deck-builder.types';
import {
  cardNamesOverCopyLimit,
  draftCardsById,
  isBasicPokemon,
  usesCopyLimitByName,
  totalDeckCards
} from '../../utils/deck-builder.utils';
import { normalizeDeckError } from '../../utils/deck-error.utils';

const MAX_DECKS = 3;
const REQUIRED_DECK_SIZE = 60;
const MAX_COPIES_BY_NAME = 4;

@Component({
  selector: 'app-deck-page',
  imports: [DeckOverviewComponent, DeckEditorComponent],
  templateUrl: './deck-page.component.html',
  styleUrl: './deck-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly deckApi = inject(DeckApiService);
  private readonly cardsApi = inject(CardsApiService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);

  readonly decks = signal<DeckSummary[]>([]);
  readonly isLightTheme = computed(() => this.appTheme.theme() === 'LIGHT');
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly availableCards = signal<CardSummary[]>([]);
  readonly editingDeckId = signal<string | null>(null);
  readonly isLoadingDecks = signal(true);
  readonly isLoadingCards = signal(false);
  readonly isSaving = signal(false);
  readonly isRandomizing = signal(false);
  readonly errorMessage = signal('');
  readonly cardsErrorMessage = signal('');
  readonly searchTerm = signal('');
  readonly typeFilter = signal<DeckTypeFilter>('ALL');
  readonly draftName = signal('');
  readonly draftCards = signal<DeckCardRequest[]>([]);
  readonly maxDecks = MAX_DECKS;
  readonly requiredDeckSize = REQUIRED_DECK_SIZE;
  readonly miniPreviewSlots = [0, 1, 2, 3, 4];
  readonly typeFilters = computed<DeckTypeFilterOption[]>(() => {
    this.languageService.translations();

    return [
      { value: 'ALL', label: this.t('DECK.FILTERS.ALL') },
      { value: CardSupertype.Pokemon, label: this.t('DECK.FILTERS.POKEMON') },
      { value: CardSupertype.Energy, label: this.t('DECK.FILTERS.ENERGY') },
      { value: CardSupertype.Trainer, label: this.t('DECK.FILTERS.TRAINER') }
    ];
  });

  readonly activeDeck = computed(() => this.decks().find((deck) => deck.active) ?? null);
  readonly isLoading = computed(() => this.isLoadingDecks());
  readonly isEditing = computed(() => this.editingDeckId() !== null);
  readonly emptySlots = computed(() => Math.max(MAX_DECKS - this.decks().length, 0));
  readonly emptySlotIndexes = computed(() =>
    Array.from({ length: this.emptySlots() }, (_, index) => index)
  );
  readonly filteredCards = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();
    const type = this.typeFilter();

    return this.availableCards().filter((card) => {
      const displayName = card.displayName?.toLowerCase() ?? '';
      const matchesSearch =
        !search || card.name.toLowerCase().includes(search) || displayName.includes(search);
      const matchesType = type === 'ALL' || card.supertype === type;
      return matchesSearch && matchesType;
    });
  });
  readonly draftTotal = computed(() => totalDeckCards(this.draftCards()));
  readonly canSaveDraft = computed(
    () => !this.isSaving() && !this.isRandomizing()
  );
  readonly canRandomize = computed(
    () =>
      !this.isSaving() &&
      !this.isRandomizing() &&
      !(this.editingDeckId() === 'new' && this.decks().length >= MAX_DECKS)
  );
  readonly draftCardCounts = computed(() => draftCardsById(this.draftCards()));
  readonly draftDeckItems = computed<DraftDeckItem[]>(() => {
    const cardsById = new Map(this.availableCards().map((card) => [card.id, card]));
    const language = this.language();

    return this.draftCards()
      .map((draftCard) => {
        const card = cardsById.get(draftCard.cardId);
        return card ? { card, quantity: draftCard.quantity } : null;
      })
      .filter((item): item is DraftDeckItem => item !== null)
      .sort((left, right) => this.cardDisplayName(left.card, language).localeCompare(this.cardDisplayName(right.card, language)));
  });
  readonly draftValidationHints = computed<DeckValidationHint[]>(() => {
    this.languageService.translations();
    const items = this.draftDeckItems();
    const overLimitNames = cardNamesOverCopyLimit(items, MAX_COPIES_BY_NAME);

    return [
      {
        label: this.t('DECK.VALIDATION_TOTAL', {
          total: this.draftTotal(),
          required: REQUIRED_DECK_SIZE
        }),
        valid: this.draftTotal() === REQUIRED_DECK_SIZE
      },
      {
        label:
          overLimitNames.length === 0
            ? this.t('DECK.VALIDATION_MAX_COPIES')
            : this.t('DECK.VALIDATION_REDUCE', { names: overLimitNames.join(', ') }),
        valid: overLimitNames.length === 0
      },
      {
        label: this.t('DECK.VALIDATION_BASIC'),
        valid: items.some((item) => isBasicPokemon(item.card))
      }
    ];
  });

  readonly cardQuantity = (cardId: string): number => {
    return this.draftCardCounts().get(cardId) ?? 0;
  };
  readonly isCopyLimitReached = (card: CardSummary): boolean => {
    return usesCopyLimitByName(card) && this.cardCopiesByName(card.name) >= MAX_COPIES_BY_NAME;
  };
  readonly canAddCardToDraft = (card: CardSummary): boolean => {
    return this.draftTotal() < REQUIRED_DECK_SIZE && !this.isCopyLimitReached(card);
  };

  constructor() {
    this.loadDeckBuilderData();
    this.route.queryParamMap.subscribe((params) => {
      if (params.get('mode') === 'new' && !this.isEditing()) {
        this.startCreating();
      }
    });
  }

  startCreating(): void {
    this.editingDeckId.set('new');
    this.draftName.set('');
    this.draftCards.set([]);
    this.searchTerm.set('');
    this.typeFilter.set('ALL');
    this.errorMessage.set('');
    this.ensureAvailableCards();
  }

  startEditing(deck: DeckSummary): void {
    this.editingDeckId.set(deck.id);
    this.applyDeckToDraft(deck);
    this.searchTerm.set('');
    this.typeFilter.set('ALL');
    this.errorMessage.set('');
    this.ensureAvailableCards();
  }

  closeEditor(): void {
    this.editingDeckId.set(null);
    this.draftName.set('');
    this.draftCards.set([]);
    this.searchTerm.set('');
    this.typeFilter.set('ALL');
    this.errorMessage.set('');
  }

  updateDraftName(name: string): void {
    this.draftName.set(name);
  }

  updateSearchTerm(searchTerm: string): void {
    this.searchTerm.set(searchTerm);
  }

  changeTypeFilter(filter: DeckTypeFilter): void {
    this.typeFilter.set(filter);
  }

  addCardToDraft(card: CardSummary): void {
    if (this.isCopyLimitReached(card)) {
      this.errorMessage.set(
        this.t('DECK.ERROR_MAX_COPIES_CARD', {
          name: this.cardDisplayName(card)
        })
      );
      return;
    }

    if (!this.canAddCardToDraft(card)) {
      return;
    }

    this.errorMessage.set('');
    this.draftCards.update((cards) => {
      const existing = cards.find((draftCard) => draftCard.cardId === card.id);

      if (!existing) {
        return [...cards, { cardId: card.id, quantity: 1 }];
      }

      return cards.map((draftCard) =>
        draftCard.cardId === card.id
          ? { ...draftCard, quantity: draftCard.quantity + 1 }
          : draftCard
      );
    });
  }

  removeCardFromDraft(cardId: string): void {
    this.draftCards.update((cards) =>
      cards
        .map((draftCard) =>
          draftCard.cardId === cardId
            ? { ...draftCard, quantity: draftCard.quantity - 1 }
            : draftCard
        )
        .filter((draftCard) => draftCard.quantity > 0)
    );
  }

  clearDraft(): void {
    this.draftCards.set([]);
    this.errorMessage.set('');
  }

  dismissError(): void {
    this.errorMessage.set('');
  }

  saveDraft(): void {
    const draftErrorMessage = this.getDraftSaveErrorMessage();

    if (draftErrorMessage) {
      this.errorMessage.set(draftErrorMessage);
      return;
    }

    const request = {
      name: this.draftName().trim(),
      cards: this.draftCards()
    };
    const editingId = this.editingDeckId();

    if (!editingId) {
      this.errorMessage.set(this.t('DECK.ERROR_NO_EDITING_SAVE'));
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set('');

    const saveRequest =
      editingId === 'new'
        ? this.deckApi.createDeck(request)
        : this.deckApi.replaceDeck(editingId, request);

    saveRequest.subscribe({
      next: (deck) => {
        this.upsertDeckInState(deck);
        this.editingDeckId.set(null);
        this.draftName.set('');
        this.draftCards.set([]);
        this.isSaving.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(normalizeDeckError(error, this.t));
        this.isSaving.set(false);
      }
    });
  }

  randomizeEditingDeck(): void {
    const editingId = this.editingDeckId();

    if (!editingId) {
      this.errorMessage.set(this.t('DECK.ERROR_NO_EDITING_RANDOM'));
      return;
    }

    if (editingId === 'new' && this.decks().length >= MAX_DECKS) {
      this.errorMessage.set(
        this.t('DECK.ERROR_MAX_DECKS', { max: MAX_DECKS })
      );
      return;
    }

    this.isRandomizing.set(true);
    this.errorMessage.set('');

    const randomizeRequest =
      editingId === 'new' ? this.deckApi.createRandomDeck() : this.deckApi.randomizeDeck(editingId);

    randomizeRequest.subscribe({
      next: (deck) => {
        this.upsertDeckInState(deck);
        this.editingDeckId.set(deck.id);
        this.applyDeckToDraft(deck);
        this.isRandomizing.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(normalizeDeckError(error, this.t));
        this.isRandomizing.set(false);
      }
    });
  }

  activateDeck(deckId: string): void {
    const previousDecks = this.decks();
    const selectedDeck = previousDecks.find((deck) => deck.id === deckId);
    this.isSaving.set(true);
    this.errorMessage.set('');
    if (selectedDeck) {
      this.applyDeckActivation({
        id: deckId,
        active: true,
        valid: selectedDeck.valid,
        validationErrors: selectedDeck.validationErrors
      });
    }

    this.deckApi.activateDeck(deckId).subscribe({
      next: (activation) => {
        this.applyDeckActivation(activation);
        this.isSaving.set(false);
      },
      error: (error: unknown) => {
        this.decks.set(previousDecks);
        this.errorMessage.set(normalizeDeckError(error, this.t));
        this.isSaving.set(false);
      }
    });
  }

  deleteDeck(deckId: string): void {
    this.isSaving.set(true);
    this.errorMessage.set('');

    this.deckApi.deleteDeck(deckId).subscribe({
      next: () => {
        const nextDecks = this.decks().filter((deck) => deck.id !== deckId);
        this.decks.set(nextDecks);
        this.isSaving.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(normalizeDeckError(error, this.t));
        this.isSaving.set(false);
      }
    });
  }

  private loadDeckBuilderData(): void {
    this.loadDecks();
    this.loadAvailableCards();
  }

  private loadDecks(): void {
    this.isLoadingDecks.set(true);
    this.errorMessage.set('');

    this.deckApi.getDecks().subscribe({
      next: (decks) => {
        this.decks.set(decks);
        this.isLoadingDecks.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(normalizeDeckError(error, this.t));
        this.isLoadingDecks.set(false);
      }
    });
  }

  loadAvailableCards(): void {
    this.isLoadingCards.set(true);
    this.cardsErrorMessage.set('');

    this.cardsApi.getCards().subscribe({
      next: (cards) => {
        this.availableCards.set(cards);
        this.isLoadingCards.set(false);
      },
      error: (error: unknown) => {
        this.cardsErrorMessage.set(normalizeDeckError(error, this.t));
        this.isLoadingCards.set(false);
      }
    });
  }

  private ensureAvailableCards(): void {
    if (!this.isLoadingCards() && this.availableCards().length === 0) {
      this.loadAvailableCards();
    }
  }

  private applyDeckToDraft(deck: DeckSummary): void {
    this.draftName.set(deck.name);
    this.draftCards.set(deck.cards.map((card) => ({ cardId: card.cardId, quantity: card.quantity })));
  }

  private getDraftSaveErrorMessage(): string {
    if (!this.editingDeckId()) {
      return this.t('DECK.ERROR_NO_EDITING_SAVE');
    }

    if (!this.draftName().trim()) {
      return this.t('DECK.ERROR_NAME_REQUIRED');
    }

    const invalidHints = this.draftValidationHints().filter((hint) => !hint.valid);

    if (invalidHints.length > 0) {
      return this.t('DECK.ERROR_SAVE_INVALID', {
        reasons: invalidHints.map((hint) => hint.label).join('. ')
      });
    }

    return '';
  }

  private cardCopiesByName(cardName: string): number {
    return this.draftDeckItems()
      .filter((item) => item.card.name === cardName)
      .reduce((total, item) => total + item.quantity, 0);
  }

  private cardDisplayName(card: CardSummary, language: AppLanguage = this.language()): string {
    return this.languageService.cardName(card, language);
  }

  private upsertDeckInState(updatedDeck: DeckSummary): void {
    this.decks.update((decks) => {
      const nextDecks = decks.some((deck) => deck.id === updatedDeck.id)
        ? decks
        : [...decks, updatedDeck];

      return nextDecks.map((deck) => ({
        ...deck,
        active: deck.id === updatedDeck.id ? updatedDeck.active : updatedDeck.active ? false : deck.active,
        ...(deck.id === updatedDeck.id ? updatedDeck : {})
      }));
    });
  }

  private applyDeckActivation(activation: DeckActivationResponse): void {
    this.decks.update((decks) =>
      decks.map((deck) => ({
        ...deck,
        active: deck.id === activation.id,
        ...(deck.id === activation.id
          ? {
              valid: activation.valid,
              validationErrors: activation.validationErrors
            }
          : {})
      }))
    );
  }
}
