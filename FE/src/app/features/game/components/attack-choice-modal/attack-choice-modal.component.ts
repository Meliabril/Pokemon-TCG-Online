import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { ResolveAttackChoicePayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { PendingAttackChoicePayload } from '../../../../core/models/interfaces/game/game-resolution.interface';
import { CardSummary } from '../../../../core/models/interfaces/card/card-summary.interface';
import { SpecialConditionType } from '../../../../core/models/enums/game/special-condition-type.enum';
import { LanguageService } from '../../../../core/services/language.service';
import { BoardAnimationService } from '../../services/board-animation.service';
import { BoardAnimationCommand } from '../../domain/animations/board-animation.types';

const SELECT_OPPONENT_BENCH_TARGET = 'SELECT_OPPONENT_BENCH_TARGET';
const SELECT_OPPONENT_ATTACK = 'SELECT_OPPONENT_ATTACK';
const YES_NO = 'YES_NO';
const REORDER_TOP_DECK = 'REORDER_TOP_DECK';
const SELECT_CARD_FROM_DISCARD = 'SELECT_CARD_FROM_DISCARD';
const SELECT_DISCARD_ITEMS_TO_HAND = 'SELECT_DISCARD_ITEMS_TO_HAND';
const SELECT_HAND_CARDS_TO_DISCARD = 'SELECT_HAND_CARDS_TO_DISCARD';
const MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH = 'MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH';
const SELECT_DECK_CARD_AND_ATTACH_TO_SELF = 'SELECT_DECK_CARD_AND_ATTACH_TO_SELF';
const SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON = 'SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON';
const SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND = 'SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND';
const CHOOSE_SPECIAL_CONDITION = 'CHOOSE_SPECIAL_CONDITION';
const LOOK_OPPONENT_DECK_TOP_CARD = 'LOOK_OPPONENT_DECK_TOP_CARD_OPTIONAL_SHUFFLE';
const MAX_ENERGY_SALON_SELECTION = 3;

interface ChoiceCardOption {
  cardInstanceId: string;
  label: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  category: string | null;
  energyType: string | null;
}

interface ChoicePokemonOption {
  pokemonInPlayId: string;
  label: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  slotPosition: number | null;
}

interface ChoiceAttackOption {
  attackOrder: number;
  attackName: string;
}

interface ChoiceEnergyOption {
  attachedCardId: string;
  label: string;
  energyType: string | null;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
}

interface ChoiceSpecialConditionOption {
  conditionType: SpecialConditionType;
  label: string;
}

type UnknownRecord = Record<string, unknown>;

@Component({
  selector: 'app-attack-choice-modal',
  templateUrl: './attack-choice-modal.component.html',
  styleUrl: './attack-choice-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttackChoiceModalComponent {
  private readonly languageService = inject(LanguageService);
  private readonly boardAnimationService = inject(BoardAnimationService);
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly isOpen = input(false);
  readonly pendingChoiceType = input<string | null>(null);
  readonly pendingChoicePayload = input<PendingAttackChoicePayload | null>(null);
  readonly card = input<CardSummary | null>(null);
  readonly isLoading = input(false);
  readonly error = input<string | null>(null);
  readonly actionPending = input(false);
  readonly resolved = output<ResolveAttackChoicePayload>();

  readonly selectedTargetPokemonId = signal<string | null>(null);
  readonly selectedAttackOrder = signal<number | null>(null);
  readonly selectedCardId = signal<string | null>(null);
  readonly selectedAttachedCardId = signal<string | null>(null);
  readonly selectedDestinationPokemonId = signal<string | null>(null);
  readonly selectedEnergySalonCardIds = signal<string[]>([]);
  readonly selectedPickupCardIds = signal<string[]>([]);
  readonly selectedHandDiscardCardIds = signal<string[]>([]);
  readonly selectedConditionType = signal<SpecialConditionType | null>(null);
  readonly reorderedCardIds = signal<string[]>([]);
  readonly draggedReorderCardId = signal<string | null>(null);
  readonly draggedEnergyCardId = signal<string | null>(null);
  readonly autoResolvedChoiceKey = signal<string | null>(null);
  private readonly pendingTrickyStepsAnimation = signal<BoardAnimationCommand | null>(null);

  readonly imageUrl = computed(() => this.card()?.imageLargeUrl ?? this.card()?.imageSmallUrl ?? null);
  readonly displayName = computed(() => {
    const card = this.card();
    return card ? this.languageService.cardName(card, this.language()) : this.t('GAME.CARD_FALLBACK');
  });
  readonly cardOptions = computed(() =>
    this.mapCardOptions(this.pendingChoicePayload()?.['cards'], this.pendingChoiceType())
  );
  readonly targetOptions = computed(() => this.mapPokemonOptions(this.pendingChoicePayload()?.['targets']));
  readonly attackOptions = computed(() => this.mapAttackOptions(this.pendingChoicePayload()?.['attacks']));
  readonly energyOptions = computed(() => this.mapEnergyOptions(this.pendingChoicePayload()?.['energies']));
  readonly conditionOptions = computed(() => this.mapSpecialConditionOptions(this.pendingChoicePayload()?.['conditionTypes']));
  readonly displayedEnergySalonOptions = computed(() => this.uniqueEnergySalonOptions(this.cardOptions()));
  // Gather Energy (and any other "pick a deck energy" choice) must show a single card per
  // energy type even though the deck may contain several physical copies of it.
  readonly displayedDeckEnergyOptions = computed(() => this.uniqueEnergySalonOptions(this.cardOptions()));
  readonly orderedCards = computed(() => {
    const cardsById = new Map(this.cardOptions().map((cardOption) => [cardOption.cardInstanceId, cardOption]));
    return this.reorderedCardIds()
      .map((cardInstanceId) => cardsById.get(cardInstanceId) ?? null)
      .filter((cardOption): cardOption is ChoiceCardOption => cardOption !== null);
  });
  readonly isLegacyTopDeckChoice = computed(() => this.pendingChoiceType() === LOOK_OPPONENT_DECK_TOP_CARD);
  readonly pickupCount = computed(() => numberValue(this.pendingChoicePayload()?.['pickupCount']) ?? 0);
  readonly handDiscardCount = computed(() => numberValue(this.pendingChoicePayload()?.['discardCount']) ?? 0);
  readonly optionalChoice = computed(() => booleanValue(this.pendingChoicePayload()?.['optional']));
  readonly autoDeckAttachChoice = computed(() => this.isAutoDeckAttachChoice());
  readonly title = computed(() => this.titleFor(this.pendingChoiceType()));
  readonly instruction = computed(() => this.instructionFor(this.pendingChoiceType()));
  readonly canConfirm = computed(() => {
    if (this.actionPending()) {
      return false;
    }

    switch (this.pendingChoiceType()) {
      case SELECT_OPPONENT_BENCH_TARGET:
        return this.selectedTargetPokemonId() !== null;
      case SELECT_OPPONENT_ATTACK:
        return this.selectedAttackOrder() !== null;
      case REORDER_TOP_DECK:
        return this.reorderedCardIds().length === this.cardOptions().length && this.cardOptions().length > 0;
      case SELECT_CARD_FROM_DISCARD:
      case SELECT_DECK_CARD_AND_ATTACH_TO_SELF:
        return this.isSelectedCardOptionValid();
      case SELECT_DISCARD_ITEMS_TO_HAND:
        return this.selectedPickupCardIds().length === this.pickupCount() && this.pickupCount() > 0;
      case SELECT_HAND_CARDS_TO_DISCARD:
        return this.selectedHandDiscardCardIds().length === this.handDiscardCount() && this.handDiscardCount() > 0;
      case MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH:
        return this.selectedAttachedCardId() !== null && this.selectedDestinationPokemonId() !== null;
      case SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON:
        return this.selectedCardId() !== null && this.selectedDestinationPokemonId() !== null;
      case SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND:
        return this.selectedEnergySalonCardIds().length > 0 && this.selectedEnergySalonCardIds().length <= MAX_ENERGY_SALON_SELECTION;
      case CHOOSE_SPECIAL_CONDITION:
        return this.selectedConditionType() !== null;
      default:
        return false;
    }
  });

  constructor() {
    effect(() => {
      const choiceType = this.pendingChoiceType();
      const cardIds = this.cardOptions().map((cardOption) => cardOption.cardInstanceId);
      const currentOrder = untracked(() => this.reorderedCardIds());
      if (choiceType === REORDER_TOP_DECK && !sameStringList(currentOrder, cardIds)) {
        this.reorderedCardIds.set(cardIds);
      }
      if (choiceType !== REORDER_TOP_DECK && currentOrder.length > 0) {
        this.reorderedCardIds.set([]);
      }
    });

    effect(() => {
      this.pendingChoiceType();
      this.pendingChoicePayload();
      untracked(() => {
        this.selectedTargetPokemonId.set(null);
        this.selectedAttackOrder.set(null);
        this.selectedCardId.set(null);
        this.selectedAttachedCardId.set(null);
        this.selectedDestinationPokemonId.set(null);
        this.selectedEnergySalonCardIds.set([]);
        this.selectedPickupCardIds.set([]);
        this.selectedHandDiscardCardIds.set([]);
        this.selectedConditionType.set(null);
        this.draggedReorderCardId.set(null);
        this.draggedEnergyCardId.set(null);
      });
    });

    effect(() => {
      const choiceType = this.pendingChoiceType();
      const cardOptions = this.cardOptions();
      const alreadyResolvedKey = this.autoResolvedChoiceKey();
      if (
        choiceType !== SELECT_DECK_CARD_AND_ATTACH_TO_SELF ||
        !this.isOpen() ||
        this.actionPending() ||
        !this.autoDeckAttachChoice() ||
        cardOptions.length === 0
      ) {
        if (choiceType !== SELECT_DECK_CARD_AND_ATTACH_TO_SELF && alreadyResolvedKey !== null) {
          this.autoResolvedChoiceKey.set(null);
        }
        return;
      }

      const choiceKey = `${choiceType}:${cardOptions.map((cardOption) => cardOption.cardInstanceId).join(',')}`;
      if (alreadyResolvedKey === choiceKey) {
        return;
      }

      this.autoResolvedChoiceKey.set(choiceKey);
      this.resolved.emit({ cardInstanceId: cardOptions[0].cardInstanceId });
    });

    // Tricky Steps (and any other energy-move choice) must only show the final travel
    // animation once the modal actually disappears from the board, otherwise the floating
    // card overlay would be hidden behind the modal backdrop. The animation is captured at
    // confirmation time and replayed as soon as `isOpen` flips back to false.
    effect(() => {
      const open = this.isOpen();
      const pendingAnimation = untracked(() => this.pendingTrickyStepsAnimation());
      if (open || !pendingAnimation) {
        return;
      }

      this.boardAnimationService.enqueue([pendingAnimation]);
      untracked(() => this.pendingTrickyStepsAnimation.set(null));
    });
  }

  resolveYesNo(confirm: boolean): void {
    if (!this.isOpen() || this.actionPending()) {
      return;
    }

    this.resolved.emit({ confirm });
  }

  skipOptionalEffect(): void {
    if (!this.isOpen() || this.actionPending() || !this.optionalChoice()) {
      return;
    }

    this.resolved.emit({ confirm: false });
  }

  selectTarget(pokemonInPlayId: string): void {
    this.selectedTargetPokemonId.set(pokemonInPlayId);
  }

  selectAttack(attackOrder: number): void {
    this.selectedAttackOrder.set(attackOrder);
  }

  selectCondition(conditionType: SpecialConditionType): void {
    if (!this.conditionOptions().some((option) => option.conditionType === conditionType)) {
      return;
    }

    this.selectedConditionType.set(conditionType);
  }

  selectCard(cardInstanceId: string): void {
    if (!this.cardOptions().some((cardOption) => cardOption.cardInstanceId === cardInstanceId)) {
      return;
    }

    this.selectedCardId.set(cardInstanceId);
  }

  selectAttachedEnergy(attachedCardId: string): void {
    this.selectedAttachedCardId.set(attachedCardId);
  }

  selectDestination(pokemonInPlayId: string): void {
    this.selectedDestinationPokemonId.set(pokemonInPlayId);
  }

  startReorderDrag(event: DragEvent, cardInstanceId: string): void {
    this.draggedReorderCardId.set(cardInstanceId);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', cardInstanceId);
    }
  }

  clearReorderDrag(): void {
    this.draggedReorderCardId.set(null);
  }

  allowDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  dropReorderedCard(targetCardInstanceId: string): void {
    const draggedCardId = this.draggedReorderCardId();
    if (!draggedCardId || draggedCardId === targetCardInstanceId) {
      return;
    }

    const currentOrder = [...this.reorderedCardIds()];
    const draggedIndex = currentOrder.indexOf(draggedCardId);
    const targetIndex = currentOrder.indexOf(targetCardInstanceId);
    if (draggedIndex < 0 || targetIndex < 0) {
      return;
    }

    currentOrder.splice(draggedIndex, 1);
    currentOrder.splice(targetIndex, 0, draggedCardId);
    this.reorderedCardIds.set(currentOrder);
    this.draggedReorderCardId.set(null);
  }

  startEnergyDrag(event: DragEvent, energyCardId: string): void {
    this.draggedEnergyCardId.set(energyCardId);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', energyCardId);
    }
  }

  clearEnergyDrag(): void {
    this.draggedEnergyCardId.set(null);
  }

  dropEnergyOnTarget(pokemonInPlayId: string): void {
    const draggedEnergyCardId = this.draggedEnergyCardId();
    if (!draggedEnergyCardId) {
      return;
    }

    this.selectEnergyForCurrentChoice(draggedEnergyCardId);
    this.selectDestination(pokemonInPlayId);
    this.draggedEnergyCardId.set(null);
  }

  selectEnergyForCurrentChoice(energyCardId: string): void {
    if (this.pendingChoiceType() === MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH) {
      this.selectAttachedEnergy(energyCardId);
      return;
    }

    this.selectCard(energyCardId);
  }

  toggleEnergySalonCard(card: ChoiceCardOption): void {
    const currentSelection = this.selectedEnergySalonCardIds();
    if (currentSelection.includes(card.cardInstanceId)) {
      this.selectedEnergySalonCardIds.set(
        currentSelection.filter((cardInstanceId) => cardInstanceId !== card.cardInstanceId)
      );
      return;
    }

    if (currentSelection.length >= MAX_ENERGY_SALON_SELECTION || this.energyTypeAlreadySelected(card)) {
      return;
    }

    this.selectedEnergySalonCardIds.set([...currentSelection, card.cardInstanceId]);
  }

  togglePickupCard(cardInstanceId: string): void {
    if (!this.cardOptions().some((cardOption) => cardOption.cardInstanceId === cardInstanceId)) {
      return;
    }

    const currentSelection = this.selectedPickupCardIds();
    if (currentSelection.includes(cardInstanceId)) {
      this.selectedPickupCardIds.set(
        currentSelection.filter((selectedCardId) => selectedCardId !== cardInstanceId)
      );
      return;
    }

    if (currentSelection.length >= this.pickupCount()) {
      return;
    }

    this.selectedPickupCardIds.set([...currentSelection, cardInstanceId]);
  }

  pickupSelectionText(): string {
    return this.t('GAME.ATTACK_CHOICE.SELECTED_COUNT', {
      count: this.selectedPickupCardIds().length,
      max: this.pickupCount()
    });
  }

  isPickupCardSelected(cardInstanceId: string): boolean {
    return this.selectedPickupCardIds().includes(cardInstanceId);
  }

  isPickupCardDisabled(cardInstanceId: string): boolean {
    return !this.isPickupCardSelected(cardInstanceId) && this.selectedPickupCardIds().length >= this.pickupCount();
  }

  toggleHandDiscardCard(cardInstanceId: string): void {
    if (!this.cardOptions().some((cardOption) => cardOption.cardInstanceId === cardInstanceId)) {
      return;
    }

    const currentSelection = this.selectedHandDiscardCardIds();
    if (currentSelection.includes(cardInstanceId)) {
      this.selectedHandDiscardCardIds.set(
        currentSelection.filter((selectedCardId) => selectedCardId !== cardInstanceId)
      );
      return;
    }

    if (currentSelection.length >= this.handDiscardCount()) {
      return;
    }

    this.selectedHandDiscardCardIds.set([...currentSelection, cardInstanceId]);
  }

  handDiscardSelectionText(): string {
    return this.t('GAME.ATTACK_CHOICE.SELECTED_COUNT', {
      count: this.selectedHandDiscardCardIds().length,
      max: this.handDiscardCount()
    });
  }

  isHandDiscardCardSelected(cardInstanceId: string): boolean {
    return this.selectedHandDiscardCardIds().includes(cardInstanceId);
  }

  isHandDiscardCardDisabled(cardInstanceId: string): boolean {
    return !this.isHandDiscardCardSelected(cardInstanceId) && this.selectedHandDiscardCardIds().length >= this.handDiscardCount();
  }

  moveReorderedCard(cardInstanceId: string, direction: -1 | 1): void {
    const currentOrder = [...this.reorderedCardIds()];
    const currentIndex = currentOrder.indexOf(cardInstanceId);
    const nextIndex = currentIndex + direction;
    if (currentIndex < 0 || nextIndex < 0 || nextIndex >= currentOrder.length) {
      return;
    }

    [currentOrder[currentIndex], currentOrder[nextIndex]] = [currentOrder[nextIndex], currentOrder[currentIndex]];
    this.reorderedCardIds.set(currentOrder);
  }

  selectedEnergySalonText(): string {
    return this.t('GAME.ATTACK_CHOICE.SELECTED_COUNT', {
      count: this.selectedEnergySalonCardIds().length,
      max: MAX_ENERGY_SALON_SELECTION
    });
  }

  isEnergySalonCardSelected(cardInstanceId: string): boolean {
    return this.selectedEnergySalonCardIds().includes(cardInstanceId);
  }

  isEnergySalonCardDisabled(card: ChoiceCardOption): boolean {
    return (
      !this.isEnergySalonCardSelected(card.cardInstanceId) &&
      (this.selectedEnergySalonCardIds().length >= MAX_ENERGY_SALON_SELECTION || this.energyTypeAlreadySelected(card))
    );
  }

  optionImage(option: ChoiceCardOption | ChoicePokemonOption): string | null {
    return option.imageLargeUrl ?? option.imageSmallUrl;
  }

  energyOptionImage(option: ChoiceEnergyOption): string | null {
    return option.imageLargeUrl ?? option.imageSmallUrl;
  }

  selectedEnergyCardId(): string | null {
    return this.pendingChoiceType() === MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH
      ? this.selectedAttachedCardId()
      : this.selectedCardId();
  }

  isTargetSelected(pokemonInPlayId: string): boolean {
    return this.selectedDestinationPokemonId() === pokemonInPlayId;
  }

  targetHasSelectedEnergy(pokemonInPlayId: string): boolean {
    return this.isTargetSelected(pokemonInPlayId) && this.selectedEnergyCardId() !== null;
  }

  confirmSelection(): void {
    if (!this.isOpen() || !this.canConfirm()) {
      return;
    }

    if (this.pendingChoiceType() === MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH) {
      this.captureTrickyStepsAnimation();
    }

    const payload = this.payloadForCurrentChoice();
    if (payload) {
      this.resolved.emit(payload);
    }
  }

  // Builds the "energy travels from the opponent's active Pokémon to the chosen bench slot"
  // animation using the exact selection confirmed by the user, and stores it so it only plays
  // once the modal closes (see the `isOpen` effect in the constructor). Changing the previewed
  // destination before confirming never calls this method, so it never fires the final animation.
  private captureTrickyStepsAnimation(): void {
    const attachedCardId = this.selectedAttachedCardId();
    const destinationPokemonId = this.selectedDestinationPokemonId();
    if (!attachedCardId || !destinationPokemonId) {
      return;
    }

    const energy = this.energyOptions().find((option) => option.attachedCardId === attachedCardId);
    const target = this.targetOptions().find((option) => option.pokemonInPlayId === destinationPokemonId);
    if (!target) {
      return;
    }

    const benchIndex = Math.max(0, Math.min(4, (target.slotPosition ?? 1) - 1));
    const toAnchor = `opponent-bench-${benchIndex}`;

    this.pendingTrickyStepsAnimation.set({
      type: 'OPPONENT_MOVE_CARD',
      fromAnchor: 'opponent-active',
      toAnchor,
      targetPulseAnchor: toAnchor,
      cardImageUrl: (energy ? this.energyOptionImage(energy) : null) ?? undefined,
      cardLabel: energy?.label ?? this.t('GAME.ATTACHED_ENERGY_FALLBACK'),
      durationMs: 780,
      travelRotationDeg: -5
    });
  }

  private payloadForCurrentChoice(): ResolveAttackChoicePayload | null {
    switch (this.pendingChoiceType()) {
      case SELECT_OPPONENT_BENCH_TARGET:
        return { targetPokemonInPlayId: this.requiredString(this.selectedTargetPokemonId()) };
      case SELECT_OPPONENT_ATTACK:
        return { attackOrder: this.requiredNumber(this.selectedAttackOrder()) };
      case REORDER_TOP_DECK:
        return { cardInstanceIds: this.reorderedCardIds() };
      case SELECT_CARD_FROM_DISCARD:
      case SELECT_DECK_CARD_AND_ATTACH_TO_SELF:
        return { cardInstanceId: this.requiredString(this.selectedCardId()) };
      case SELECT_DISCARD_ITEMS_TO_HAND:
        return { cardInstanceIds: this.selectedPickupCardIds() };
      case SELECT_HAND_CARDS_TO_DISCARD:
        return { cardInstanceIds: this.selectedHandDiscardCardIds() };
      case MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH:
        return {
          attachedCardId: this.requiredString(this.selectedAttachedCardId()),
          targetPokemonInPlayId: this.requiredString(this.selectedDestinationPokemonId())
        };
      case SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON:
        return {
          cardInstanceId: this.requiredString(this.selectedCardId()),
          pokemonInPlayId: this.requiredString(this.selectedDestinationPokemonId())
        };
      case SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND:
        return { cardInstanceIds: this.selectedEnergySalonCardIds() };
      case CHOOSE_SPECIAL_CONDITION:
        return { conditionType: this.requiredConditionType(this.selectedConditionType()) };
      default:
        return null;
    }
  }

  private titleFor(choiceType: string | null): string {
    switch (choiceType) {
      case SELECT_OPPONENT_BENCH_TARGET:
        return this.t('GAME.ATTACK_CHOICE.SELECT_TARGET');
      case SELECT_OPPONENT_ATTACK:
        return this.t('GAME.ATTACK_CHOICE.SELECT_ATTACK');
      case YES_NO:
      case LOOK_OPPONENT_DECK_TOP_CARD:
        return this.t('GAME.ATTACK_CHOICE.TITLE');
      case REORDER_TOP_DECK:
        return this.t('GAME.ATTACK_CHOICE.REORDER_CARDS');
      case SELECT_CARD_FROM_DISCARD:
      case SELECT_DECK_CARD_AND_ATTACH_TO_SELF:
      case SELECT_DISCARD_ITEMS_TO_HAND:
        return this.t('GAME.ATTACK_CHOICE.SELECT_CARD');
      case SELECT_HAND_CARDS_TO_DISCARD:
        return this.t('GAME.ATTACK_CHOICE.MENTAL_TRASH_TITLE');
      case MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH:
      case SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON:
        return this.t('GAME.ATTACK_CHOICE.SELECT_ENERGY');
      case SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND:
        return this.t('GAME.ATTACK_CHOICE.SELECT_UP_TO_THREE_ENERGIES');
      case CHOOSE_SPECIAL_CONDITION:
        return this.t('GAME.ATTACK_CHOICE.SELECT_SPECIAL_CONDITION');
      default:
        return this.t('GAME.ATTACK_CHOICE.TITLE');
    }
  }

  private instructionFor(choiceType: string | null): string {
    switch (choiceType) {
      case SELECT_OPPONENT_BENCH_TARGET:
        return this.t('GAME.ATTACK_CHOICE.SELECT_TARGET_HINT');
      case SELECT_OPPONENT_ATTACK:
        return this.t('GAME.ATTACK_CHOICE.SELECT_ATTACK_HINT');
      case YES_NO:
        return this.t('GAME.ATTACK_CHOICE.YES_NO_HINT');
      case LOOK_OPPONENT_DECK_TOP_CARD:
        return this.t('GAME.ATTACK_CHOICE.MINE_DESCRIPTION');
      case REORDER_TOP_DECK:
        return this.t('GAME.ATTACK_CHOICE.REORDER_CARDS_HINT');
      case SELECT_CARD_FROM_DISCARD:
        return this.t('GAME.ATTACK_CHOICE.SELECT_DISCARD_HINT');
      case SELECT_DISCARD_ITEMS_TO_HAND:
        return this.t('GAME.ATTACK_CHOICE.SELECT_DISCARD_ITEMS_HINT');
      case SELECT_HAND_CARDS_TO_DISCARD:
        return this.t('GAME.ATTACK_CHOICE.MENTAL_TRASH_HINT', { count: this.handDiscardCount() });
      case MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH:
        return this.t('GAME.ATTACK_CHOICE.MOVE_ENERGY_HINT');
      case SELECT_DECK_CARD_AND_ATTACH_TO_SELF:
        return this.t('GAME.ATTACK_CHOICE.SELECT_DECK_CARD_HINT');
      case SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON:
        return this.t('GAME.ATTACK_CHOICE.SELECT_ENERGY_DESTINATION_HINT');
      case SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND:
        return this.t('GAME.ATTACK_CHOICE.SELECT_UP_TO_THREE_ENERGIES_HINT');
      case CHOOSE_SPECIAL_CONDITION:
        return this.t('GAME.ATTACK_CHOICE.SELECT_SPECIAL_CONDITION_HINT');
      default:
        return this.t('GAME.ATTACK_CHOICE.UNSUPPORTED');
    }
  }

  private energyTypeAlreadySelected(card: ChoiceCardOption): boolean {
    if (!card.energyType) {
      return false;
    }

    const selectedCards = this.cardOptions().filter((cardOption) =>
      this.selectedEnergySalonCardIds().includes(cardOption.cardInstanceId)
    );
    return selectedCards.some((selectedCard) => selectedCard.energyType === card.energyType);
  }

  private isSelectedCardOptionValid(): boolean {
    const selectedCardId = this.selectedCardId();
    return selectedCardId !== null && this.cardOptions().some((cardOption) => cardOption.cardInstanceId === selectedCardId);
  }

  private mapCardOptions(value: unknown, choiceType: string | null): ChoiceCardOption[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value
      .map((entry, index) => this.mapCardOption(entry, index))
      .filter((option): option is ChoiceCardOption => option !== null)
      .filter((option) => this.isValidCardOptionForChoice(option, choiceType));
  }

  private mapCardOption(value: unknown, index: number): ChoiceCardOption | null {
    if (!isRecord(value)) {
      return null;
    }

    const cardInstanceId = stringValue(value['cardInstanceId']);
    if (!cardInstanceId) {
      return null;
    }

    return {
      cardInstanceId,
      label: stringValue(value['name']) ?? stringValue(value['label']) ?? this.t('GAME.ATTACK_CHOICE.CARD_FALLBACK', { number: index + 1 }),
      imageSmallUrl: stringValue(value['imageSmallUrl']),
      imageLargeUrl: stringValue(value['imageLargeUrl']),
      category: stringValue(value['category']),
      energyType: stringValue(value['energyType']) ?? stringValue(value['pokemonType'])
    };
  }

  private isValidCardOptionForChoice(option: ChoiceCardOption, choiceType: string | null): boolean {
    if (choiceType !== SELECT_DECK_CARD_AND_ATTACH_TO_SELF) {
      return true;
    }

    return option.category === 'BASIC_ENERGY' || option.category === 'SPECIAL_ENERGY';
  }

  private mapPokemonOptions(value: unknown): ChoicePokemonOption[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value
      .map((entry, index) => this.mapPokemonOption(entry, index))
      .filter((option): option is ChoicePokemonOption => option !== null);
  }

  private mapPokemonOption(value: unknown, index: number): ChoicePokemonOption | null {
    if (!isRecord(value)) {
      return null;
    }

    const pokemonInPlayId = stringValue(value['pokemonInPlayId']);
    if (!pokemonInPlayId) {
      return null;
    }

    const slotPosition = numberValue(value['slotPosition']);
    const fallbackSlot = slotPosition ?? index + 1;
    return {
      pokemonInPlayId,
      label: stringValue(value['name']) ?? stringValue(value['label']) ?? this.t('GAME.ATTACK_CHOICE.BENCH_SLOT', { number: fallbackSlot }),
      imageSmallUrl: stringValue(value['imageSmallUrl']),
      imageLargeUrl: stringValue(value['imageLargeUrl']),
      slotPosition
    };
  }

  private mapAttackOptions(value: unknown): ChoiceAttackOption[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value
      .map((entry, index) => this.mapAttackOption(entry, index))
      .filter((option): option is ChoiceAttackOption => option !== null);
  }

  private mapAttackOption(value: unknown, index: number): ChoiceAttackOption | null {
    if (!isRecord(value)) {
      return null;
    }

    const attackOrder = numberValue(value['attackOrder']);
    if (attackOrder === null) {
      return null;
    }

    return {
      attackOrder,
      attackName: stringValue(value['attackName']) ?? this.t('GAME.ATTACK_CHOICE.ATTACK_FALLBACK', { number: index + 1 })
    };
  }

  private mapEnergyOptions(value: unknown): ChoiceEnergyOption[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value
      .map((entry, index) => this.mapEnergyOption(entry, index))
      .filter((option): option is ChoiceEnergyOption => option !== null);
  }

  private mapEnergyOption(value: unknown, index: number): ChoiceEnergyOption | null {
    if (!isRecord(value)) {
      return null;
    }

    const attachedCardId = stringValue(value['attachedCardId']);
    if (!attachedCardId) {
      return null;
    }

    const energyType = stringValue(value['energyType']) ?? stringValue(value['pokemonType']);
    return {
      attachedCardId,
      label: stringValue(value['name']) ?? stringValue(value['label']) ?? energyType ?? this.t('GAME.ATTACK_CHOICE.ENERGY_FALLBACK', { number: index + 1 }),
      energyType,
      imageSmallUrl: stringValue(value['imageSmallUrl']),
      imageLargeUrl: stringValue(value['imageLargeUrl'])
    };
  }

  private mapSpecialConditionOptions(value: unknown): ChoiceSpecialConditionOption[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value
      .map((entry) => this.mapSpecialConditionOption(entry))
      .filter((option): option is ChoiceSpecialConditionOption => option !== null);
  }

  private mapSpecialConditionOption(value: unknown): ChoiceSpecialConditionOption | null {
    const conditionType = stringValue(value);
    if (!conditionType || !isSpecialConditionType(conditionType)) {
      return null;
    }

    return {
      conditionType,
      label: this.t(`GAME.STATUS_EFFECT.${conditionType}`)
    };
  }

  private uniqueEnergySalonOptions(cards: ChoiceCardOption[]): ChoiceCardOption[] {
    const seenTypes = new Set<string>();
    const uniqueCards: ChoiceCardOption[] = [];
    for (const card of cards) {
      const energyType = card.energyType;
      if (!energyType || seenTypes.has(energyType)) {
        continue;
      }
      seenTypes.add(energyType);
      uniqueCards.push(card);
    }
    return uniqueCards;
  }

  private isAutoDeckAttachChoice(): boolean {
    if (this.pendingChoiceType() !== SELECT_DECK_CARD_AND_ATTACH_TO_SELF) {
      return false;
    }

    const options = this.cardOptions();
    if (options.length === 0) {
      return false;
    }

    const energyTypes = new Set(options.map((option) => option.energyType).filter((energyType): energyType is string => !!energyType));
    const categories = new Set(options.map((option) => option.category).filter((category): category is string => !!category));
    return energyTypes.size === 1 && categories.size === 1;
  }

  private requiredString(value: string | null): string {
    return value ?? '';
  }

  private requiredNumber(value: number | null): number {
    return value ?? 0;
  }

  private requiredConditionType(value: SpecialConditionType | null): SpecialConditionType {
    return value ?? SpecialConditionType.Asleep;
  }
}

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function stringValue(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function numberValue(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function booleanValue(value: unknown): boolean {
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    return value.toLowerCase() === 'true';
  }
  return false;
}

function isSpecialConditionType(value: string): value is SpecialConditionType {
  return Object.values(SpecialConditionType).includes(value as SpecialConditionType);
}

function sameStringList(first: string[], second: string[]): boolean {
  return first.length === second.length && first.every((value, index) => value === second[index]);
}
