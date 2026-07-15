import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  computed,
  effect,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameStatus } from '../../../../core/models/enums/game/game-status.enum';
import { ResolveAttackChoicePayload, UseAbilityPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { CardSummary } from '../../../../core/models/interfaces/card/card-summary.interface';
import { GameChatMessage } from '../../../../core/models/interfaces/game/game-chat-message.interface';
import { LanguageService } from '../../../../core/services/language.service';
import { CardsApiService } from '../../../../infrastructure/api/card/cards-api.service';
import {
  ActivePokemonCardDrop,
  ActivePokemonSlotComponent
} from '../active-pokemon-slot/active-pokemon-slot.component';
import { AbilityEnergyTransferModalComponent } from '../ability-energy-transfer-modal/ability-energy-transfer-modal.component';
import { BenchRowComponent } from '../bench-row/bench-row.component';
import { AttackChoiceModalComponent } from '../attack-choice-modal/attack-choice-modal.component';
import { CardDetailModalComponent } from '../card-detail-modal/card-detail-modal.component';
import { CardZoneStackComponent } from '../card-zone-stack/card-zone-stack.component';
import { DiscardPileModalComponent } from '../discard-pile-modal/discard-pile-modal.component';
import { HandCardPointerDrop, HandFanComponent } from '../hand-fan/hand-fan.component';
import { TrainerSelectionModalComponent } from '../trainer-selection-modal/trainer-selection-modal.component';
import { PlayerStatusCardComponent } from '../player-status-card/player-status-card.component';
import { TurnHudComponent } from '../turn-hud/turn-hud.component';
import { MulliganSetupPanelComponent } from '../mulligan-setup-panel/mulligan-setup-panel.component';
import { MulliganModalComponent } from '../mulligan-modal/mulligan-modal.component';
import {
  BoardCardViewModel,
  BoardGameViewModel,
  BoardPlayerViewModel
} from '../../domain/board/board-game-view-model.interface';
import { BenchCardDrop } from '../bench-row/bench-row.component';
import { BoardAnimationLayerComponent } from '../board-animation-layer/board-animation-layer.component';
import { CardHoverVisualTarget } from '../../domain/cards/card-hover-visual-target.interface';
import { GameInteractionViewModel } from '../../domain/interaction/game-interaction-view-model.interface';
import { cardCanBeSelectedForAction, cardReferenceId, cardSupportsAction, isBasicStagePokemon } from '../../domain/interaction/game-interaction.helpers';
import { CardContext } from '../../domain/cards/card-context.interface';
import { BoardPokemonAbilityViewModel } from '../../domain/cards/board-pokemon-view-model.interface';
import { GameChatDrawerComponent } from '../game-chat-drawer/game-chat-drawer.component';
import { GameLogFeedComponent } from '../game-log-feed/game-log-feed.component';
import { MulliganSettledHand, MulliganVisualService } from '../../services/mulligan-visual.service';
import { MulliganRevealedCard } from '../../../../core/models/interfaces/game/mulligan-event.interface';
import { MOBILE_LANDSCAPE_MEDIA_QUERY } from '../../../../shared/utils/responsive-mode.util';
import { AbilitySelectionViewModel, buildAbilitySelectionViewModel } from '../../domain/abilities/ability-selection.helpers';
import {
  EvosodaEvolutionOption,
  GreatBallPokemonOption,
  ProfessorLetterEnergyOption
} from '../../domain/trainers/trainer-preview.interface';
import { TrainerPreviewService } from '../../services/trainer-preview.service';

const TURN_TIMEOUT_SECONDS = 120;
const URGENT_THRESHOLD_SECONDS = 20;
const SELECT_HAND_CARDS_TO_DISCARD = 'SELECT_HAND_CARDS_TO_DISCARD';
const FAIRY_TRANSFER_ABILITY_ID = 'FAIRY_TRANSFER';
const ITEM_TRAINER_CATEGORY = 'ITEM_TRAINER';
const ACE_SPEC_TRAINER_CATEGORY = 'ACE_SPEC_TRAINER';
const SUPPORTER_TRAINER_CATEGORY = 'SUPPORTER_TRAINER';
const STADIUM_TRAINER_CATEGORY = 'STADIUM_TRAINER';
const POKEMON_TOOL_TRAINER_CATEGORY = 'POKEMON_TOOL_TRAINER';
const POKEMON_TOOL_SUBTYPE = 'POKEMON_TOOL';
const TRAINER_CATEGORY_MAP_BOARD: Record<string, true> = {
  [ITEM_TRAINER_CATEGORY]: true,
  [ACE_SPEC_TRAINER_CATEGORY]: true,
  [SUPPORTER_TRAINER_CATEGORY]: true,
  [STADIUM_TRAINER_CATEGORY]: true,
  [POKEMON_TOOL_TRAINER_CATEGORY]: true
};
const PASSIVE_ABILITY_ACTIVATION_TYPE = 'PASSIVE';
type TrainerDragMode = 'TRAINER_PLAY_AREA' | 'STADIUM_AREA' | 'POKEMON_TOOL_TARGET';

@Component({
  selector: 'app-game-board',
  templateUrl: './game-board.component.html',
  styleUrl: './game-board.component.css',
  imports: [
    ActivePokemonSlotComponent,
    AbilityEnergyTransferModalComponent,
    AttackChoiceModalComponent,
    BenchRowComponent,
    BoardAnimationLayerComponent,
    CardDetailModalComponent,
    CardZoneStackComponent,
    DiscardPileModalComponent,
    GameChatDrawerComponent,
    GameLogFeedComponent,
    HandFanComponent,
    PlayerStatusCardComponent,
    TrainerSelectionModalComponent,
    TurnHudComponent,
    MulliganSetupPanelComponent,
    MulliganModalComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameBoardComponent implements AfterViewInit, OnDestroy {
  private readonly cardsApiService = inject(CardsApiService);
  private readonly trainerPreviewService = inject(TrainerPreviewService);
  private readonly languageService = inject(LanguageService);
  private readonly mulliganVisual = inject(MulliganVisualService);
  private readonly nowMillis = signal(Date.now());
  private readonly intervalId = window.setInterval(() => this.nowMillis.set(Date.now()), 1000);
  private statusPanelBaseHeight: number | null = null;
  private statusPanelPositionFrameId: number | null = null;
  private statusPanelResizeObserver: ResizeObserver | null = null;
  @ViewChild('boardSurface') private boardSurfaceElement?: ElementRef<HTMLElement>;
  @ViewChild('statusOverlay') private statusOverlayElement?: ElementRef<HTMLElement>;
  @ViewChild('opponentBenchZone') private opponentBenchElement?: ElementRef<HTMLElement>;
  @ViewChild('currentResourcesZone') private currentResourcesElement?: ElementRef<HTMLElement>;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  private readonly cardDetailCache = new Map<string, CardSummary>();
  readonly mobileLogsOpen = signal(false);
  readonly statusPanelPosition = signal<{ top: number; left: number } | null>(null);
  private readonly mobileViewportMediaQuery = window.matchMedia(MOBILE_LANDSCAPE_MEDIA_QUERY);
  /** Reactive mobile-landscape flag; the Mulligan modal is a mobile-only presentation. */
  readonly isMobileViewport = signal(this.mobileViewportMediaQuery.matches);
  private readonly mobileViewportListener = (event: MediaQueryListEvent): void =>
    this.isMobileViewport.set(event.matches);

  readonly board = input.required<BoardGameViewModel>();
  readonly actionPending = input(false);
  readonly interaction = input<GameInteractionViewModel | null>(null);
  readonly statusLabel = input.required<string>();
  readonly turnNumber = input.required<number>();
  readonly phaseLabel = input.required<string>();
  readonly activePlayerLabel = input<string | null>(null);
  readonly stateVersion = input.required<number>();
  readonly eventFeed = input.required<BoardGameViewModel['eventFeed']>();
  readonly historySummary = input.required<string>();
  readonly latestActionLabel = input.required<string>();
  readonly availableActionLabels = input.required<string[]>();
  readonly statusSummary = input.required<string>();
  readonly remoteHoveredCardKey = input<string | null>(null);
  readonly localHandHidden = input(false);
  readonly opponentHandHidden = input(false);
  readonly chatMessages = input<GameChatMessage[]>([]);
  readonly chatCurrentUserId = input<string | null>(null);
  readonly chatConnected = input(false);
  readonly chatOpen = signal(false);

  toggleMobileLogs(): void {
    this.mobileLogsOpen.update((isOpen) => !isOpen);
  }

  closeMobileLogs(): void {
    this.mobileLogsOpen.set(false);
  }

  ngAfterViewInit(): void {
    this.statusPanelResizeObserver = new ResizeObserver(() => this.scheduleStatusPanelPosition());

    [
      this.boardSurfaceElement?.nativeElement,
      this.opponentBenchElement?.nativeElement,
      this.currentResourcesElement?.nativeElement
    ]
      .filter((element): element is HTMLElement => element !== undefined)
      .forEach((element) => this.statusPanelResizeObserver?.observe(element));

    window.addEventListener('resize', this.handleWindowResize);
    this.scheduleStatusPanelPosition();
  }

  private readonly handleWindowResize = (): void => {
    this.statusPanelBaseHeight = null;
    this.scheduleStatusPanelPosition();
  };

  private scheduleStatusPanelPosition(): void {
    if (typeof window === 'undefined') {
      return;
    }

    if (this.statusPanelPositionFrameId !== null) {
      window.cancelAnimationFrame(this.statusPanelPositionFrameId);
    }

    this.statusPanelPositionFrameId = window.requestAnimationFrame(() => {
      this.statusPanelPositionFrameId = null;
      this.updateStatusPanelPosition();
    });
  }

  private updateStatusPanelPosition(): void {
    if (this.board().status === GameStatus.Setup) {
      this.statusPanelPosition.set(null);
      this.statusPanelBaseHeight = null;
      return;
    }

    const boardElement = this.boardSurfaceElement?.nativeElement;
    const panelElement = this.statusOverlayElement?.nativeElement;
    const opponentBenchElement = this.opponentBenchElement?.nativeElement;
    const currentResourcesElement = this.currentResourcesElement?.nativeElement;

    if (!boardElement || !panelElement || !opponentBenchElement || !currentResourcesElement) {
      return;
    }

    const boardRect = boardElement.getBoundingClientRect();
    const panelRect = panelElement.getBoundingClientRect();
    const opponentBenchRect = opponentBenchElement.getBoundingClientRect();
    const currentResourcesRect = currentResourcesElement.getBoundingClientRect();
    const basePanelHeight = this.statusPanelBaseHeight ?? panelRect.height;
    this.statusPanelBaseHeight = basePanelHeight;

    const gapTop = opponentBenchRect.bottom;
    const gapBottom = currentResourcesRect.top;
    const gapHeight = gapBottom - gapTop;
    const rawTop = gapTop - boardRect.top + (gapHeight - basePanelHeight) / 2;
    const rawLeft = Math.min(opponentBenchRect.left, currentResourcesRect.left) - boardRect.left;

    const maxTop = Math.max(0, boardRect.height - basePanelHeight);
    const maxLeft = Math.max(0, boardRect.width - panelRect.width);
    this.statusPanelPosition.set({
      top: clamp(rawTop, 0, maxTop),
      left: clamp(rawLeft, 0, maxLeft)
    });
  }

  // Mobile-only Mulligan modal: an AUTOMATIC, per-attempt presentation of the reveal currently playing.
  // It rides the existing animation pipeline — `revealedHand()` is set for each MULLIGAN_REVEAL_HAND command
  // (with that attempt's cards) and cleared when it ends (~2s), so the modal opens and auto-closes on its own.
  // The ¡MULLIGAN! #N banner and the shuffle keep running from the queue right after. Desktop never shows it.
  readonly mulliganModalVisible = computed(
    () => this.isMobileViewport() && this.mulliganVisual.revealedHand() !== null
  );
  // Each side reflects ONLY the reveal of the CURRENT attempt (the transient revealedHand, one player at a
  // time), never the history. So when one player already resolved and the other still mulligans, the modal
  // shows only the player revealing right now — not the resolved player's previous hand. The modal block of
  // a side with no current reveal is omitted. Only the publicly-revealed invalid hand is shown.
  readonly modalOwnRevealedCards = computed<MulliganRevealedCard[]>(() => {
    const revealed = this.mulliganVisual.revealedHand();
    return revealed && revealed.playerId === this.localPlayer().id ? revealed.cards : [];
  });
  readonly modalRivalRevealedCards = computed<MulliganRevealedCard[]>(() => {
    const revealed = this.mulliganVisual.revealedHand();
    return revealed && revealed.playerId === this.rivalPlayer().id ? revealed.cards : [];
  });

  /** Mobile "Iniciar mulligan" / watcher confirm: both just acknowledge the barrier to start the flow. */
  onMobileMulliganStart(): void {
    this.mulliganNoticeAcknowledgedRequested.emit();
  }
  readonly leaveTable = output<void>();
  readonly endTurnRequested = output<void>();
  readonly setupSubmitted = output<void>();
  readonly setupCancelled = output<void>();
  readonly mulliganNoticeAcknowledgedRequested = output<void>();
  readonly playBasicPokemon = output<string>();
  readonly setupActiveCardDropped = output<string>();
  readonly setupBenchCardDropped = output<string>();
  readonly setupBenchCardReturned = output<string>();
  readonly benchPokemonPromoted = output<string>();
  readonly attackChoiceResolved = output<ResolveAttackChoicePayload>();
  readonly activePokemonRetreated = output<string>();
  readonly drawCardRequested = output<void>();
  readonly energyDropped = output<{
    energyCardInstanceId: string;
    targetPokemonInPlayId: string;
  }>();
  readonly evolutionDropped = output<{
    evolutionCardInstanceId: string;
    targetPokemonInPlayId: string;
  }>();
  readonly handCardSelected = output<string>();
  readonly pokemonSelected = output<string>();
  readonly attackDeclared = output<{
    attackId: string;
    useBonusDamage?: boolean;
    selfTargetPokemonInPlayId?: string;
    switchTargetPokemonInPlayId?: string;
  }>();
  readonly abilityUsed = output<UseAbilityPayload>();
  readonly trainerPlayed = output<{
    cardId?: string;
    cardInstanceId?: string;
    targetPokemonInPlayId?: string;
    targetCardInstanceId?: string;
    targetEnergyCardInstanceId?: string;
    selectedEvolutionExternalId?: string;
    selectedCardInstanceId?: string;
    selectedCardIds?: string[];
  }>();
  readonly chatMessageSubmitted = output<string>();
  readonly cardHoverChanged = output<CardHoverVisualTarget>();
  readonly draggedInitialActiveCardId = signal<string | null>(null);
  readonly draggedEnergyCardId = signal<string | null>(null);
  readonly draggedEvolutionCardId = signal<string | null>(null);
  readonly draggedTrainerCardId = signal<string | null>(null);
  readonly draggedBenchPokemonInPlayId = signal<string | null>(null);
  readonly isDraggingTrainer = signal(false);
  readonly trainerDropAreaActive = signal(false);
  readonly pendingTrainerCard = signal<BoardCardViewModel | null>(null);
  readonly evosodaEvolutionOptions = signal<EvosodaEvolutionOption[]>([]);
  readonly evosodaPreviewLoading = signal(false);
  readonly evosodaPreviewError = signal<string | null>(null);
  readonly greatBallDeckPokemonOptions = signal<GreatBallPokemonOption[]>([]);
  readonly greatBallCardsLookedAt = signal(0);
  readonly greatBallPreviewLoading = signal(false);
  readonly greatBallPreviewReady = signal(false);
  readonly greatBallPreviewError = signal<string | null>(null);
  readonly professorLetterEnergyOptions = signal<ProfessorLetterEnergyOption[]>([]);
  readonly professorLetterMaxSelectable = signal(2);
  readonly professorLetterPreviewLoading = signal(false);
  readonly professorLetterPreviewError = signal<string | null>(null);
  readonly selectedCardForModal = signal<BoardCardViewModel | null>(null);
  readonly selectedCardContext = signal<CardContext | null>(null);
  readonly discardModalCards = signal<BoardCardViewModel[]>([]);
  readonly discardModalLabel = signal<string | null>(null);
  readonly selectedCardDetailForModal = signal<CardSummary | null>(null);
  readonly isCardDetailLoading = signal(false);
  readonly cardDetailError = signal<string | null>(null);
  readonly revealedAttackChoiceCard = signal<CardSummary | null>(null);
  readonly isRevealedAttackChoiceCardLoading = signal(false);
  readonly revealedAttackChoiceCardError = signal<string | null>(null);
  readonly pendingFairyTransferAbility = signal<{
    sourcePokemonId: string;
    ability: BoardPokemonAbilityViewModel;
  } | null>(null);
  private readonly revealedAttackChoiceCardCache = new Map<string, CardSummary>();
  private loadedRevealedAttackChoiceCardId: string | null = null;
  private loadedEvosodaTrainerCardId: string | null = null;
  private loadedGreatBallTrainerCardId: string | null = null;
  private loadedProfessorLetterTrainerCardId: string | null = null;
  private lastResolvedTurnKey: string | null = null;
  private lastResolvedPendingKey: string | null = null;

  constructor() {
    this.mobileViewportMediaQuery.addEventListener('change', this.mobileViewportListener);
    effect(() => {
      const revealedCardId = this.board().resolution?.revealedCardId ?? null;
      this.loadRevealedAttackChoiceCard(revealedCardId);
    });

    effect(() => {
      const board = this.board();
      this.scheduleStatusPanelPosition();
      const turnKey = `${board.turnContext.turnNumber}:${board.turnContext.activePlayerId ?? 'none'}`;
      const pendingKey = `${board.resolution?.resolutionType ?? 'none'}:${board.resolution?.pendingChoicePlayerId ?? 'none'}:${board.resolution?.pendingChoiceType ?? 'none'}:${board.resolution?.playerToPromoteId ?? 'none'}`;

      if (this.lastResolvedTurnKey !== null && this.lastResolvedTurnKey !== turnKey) {
        this.closeTransientModals();
      } else if (this.lastResolvedPendingKey !== null && this.lastResolvedPendingKey !== pendingKey) {
        this.closeTransientModals();
      }

      this.lastResolvedTurnKey = turnKey;
      this.lastResolvedPendingKey = pendingKey;
    });

    effect(() => {
      const pendingCard = this.pendingTrainerCard();
      if (pendingCard?.externalId === 'xy1-116') {
        this.clearGreatBallPreview();
        this.clearProfessorLetterPreview();
        void this.loadEvosodaPreview(pendingCard);
      } else if (pendingCard?.externalId === 'xy1-118') {
        this.clearEvosodaPreview();
        this.clearProfessorLetterPreview();
        void this.loadGreatBallPreview(pendingCard);
      } else if (pendingCard?.externalId === 'xy1-123') {
        this.clearEvosodaPreview();
        this.clearGreatBallPreview();
        void this.loadProfessorLetterPreview(pendingCard);
      } else {
        this.clearEvosodaPreview();
        this.clearGreatBallPreview();
        this.clearProfessorLetterPreview();
      }
    });
  }

  readonly rivalPlayer = computed(() => this.board().rivalPlayer);
  readonly localPlayer = computed(() =>
    this.projectLocalEnergyAttachment(this.projectLocalSetupState(this.board().localPlayer))
  );
  // Hand staging precedence (per side):
  //  1. A Mulligan reveal shows the invalid hand face-up (own zone for the owner, rival zone otherwise).
  //  2. Hidden (empty) while the opening deal gate OR the Mulligan per-player gate hides this side.
  //  3. Otherwise the snapshot hand, with any not-yet-animated extra cards sliced off (shown only when
  //     their extra-cards animation lands).
  readonly opponentHandCards = computed<BoardCardViewModel[]>(() => {
    const rivalId = this.rivalPlayer().id;
    const revealed = this.mulliganVisual.revealedHand();
    if (revealed && revealed.playerId === rivalId) {
      return revealed.cards.map((card, index) => this.revealedMulliganCard(card, index));
    }

    // After the rival's draw, show their new hand as face-down cards (no identity) for this attempt.
    const settled = this.mulliganVisual.settledHandByPlayer().get(rivalId);
    if (settled) {
      return this.settledHandCards(settled, rivalId);
    }

    if (this.opponentHandHidden() || this.mulliganVisual.gatedHandPlayerIds().has(rivalId)) {
      return [];
    }

    // Same extra-card slice as the local side: when only ONE player mulligans, the OTHER still receives
    // extra cards at the very end but is never gated/settled, so without this slice its snapshot hand would
    // show 7+N cards before the extra-cards animation lands. Hidden until that animation clears pendingExtra.
    const cards = this.rivalPlayer().handCards;
    const pendingExtra = this.mulliganVisual.pendingExtraCardsByPlayer().get(rivalId) ?? 0;
    return pendingExtra > 0 ? cards.slice(0, Math.max(0, cards.length - pendingExtra)) : cards;
  });
  // During a Mulligan the local player must also SEE their own invalid hand (face-up) while the
  // ¡MULLIGAN! reveal plays, before it returns to the deck. The reveal takes precedence over the gate.
  readonly localHandCards = computed<BoardCardViewModel[]>(() => {
    const localId = this.localPlayer().id;
    const revealed = this.mulliganVisual.revealedHand();
    if (revealed && revealed.playerId === localId) {
      return revealed.cards.map((card, index) => this.revealedMulliganCard(card, index));
    }

    // After the local draw, show the real hand just drawn (this attempt) — even if still invalid.
    const settled = this.mulliganVisual.settledHandByPlayer().get(localId);
    if (settled) {
      return this.settledHandCards(settled, localId);
    }

    if (this.localHandHidden() || this.mulliganVisual.gatedHandPlayerIds().has(localId)) {
      return [];
    }

    const cards = this.localPlayer().handCards;
    const pendingExtra = this.mulliganVisual.pendingExtraCardsByPlayer().get(localId) ?? 0;
    return pendingExtra > 0 ? cards.slice(0, Math.max(0, cards.length - pendingExtra)) : cards;
  });
  readonly stadium = computed(() => this.board().stadium);
  readonly stadiumCard = computed(() => this.board().stadium?.card ?? null);
  readonly ownPokemonOptions = computed(() => this.pokemonOptionsFor(this.localPlayer()));
  readonly opponentPokemonOptions = computed(() => this.pokemonOptionsFor(this.rivalPlayer()));
  readonly canDeclareAttackFromModal = computed(() => {
    const context = this.selectedCardContext();
    const declareAttackAction = this.board().actions.find((action) => action.actionType === GameActionType.DeclareAttack);
    return Boolean(
      context?.zone === 'active' &&
      context.owner === 'local' &&
      declareAttackAction?.enabled &&
      !this.mandatoryDrawPending() &&
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot
    );
  });
  readonly canUseAbilityFromModal = computed(() => {
    const context = this.selectedCardContext();
    const useAbilityAction = this.board().actions.find((action) => action.actionType === GameActionType.UseAbility);
    return Boolean(
      context?.owner === 'local' &&
      context.pokemonInPlayId &&
      useAbilityAction?.enabled &&
      !this.mandatoryDrawPending() &&
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot
    );
  });
  readonly canPlayTrainerAction = computed(() => {
    const playTrainerAction = this.board().actions.find(
      (action) => action.actionType === GameActionType.PlayTrainer
    );
    return Boolean(
      playTrainerAction?.enabled &&
      !this.mandatoryDrawPending() &&
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot
    );
  });
  readonly trainerToolTargetOptions = computed(() => {
    const card = this.pendingTrainerCard() ?? this.selectedCardForModal();
    if (card?.category !== 'POKEMON_TOOL_TRAINER') {
      return [];
    }

    const targetIds = this.board().actionHints.trainerToolTargetPokemonInPlayIds ?? [];
    if (!targetIds.length) {
      return [];
    }

    const allOptions = this.pokemonOptionsFor(this.localPlayer());
    return allOptions.filter((option) => targetIds.includes(option.pokemonInPlayId));
  });
  readonly trainerPokemonPickerOptions = computed(() => {
    const card = this.pendingTrainerCard() ?? this.selectedCardForModal();
    if (!card) {
      return [];
    }
    if (card.externalId === 'xy1-115') {
      const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
      const options: { pokemonInPlayId: string; label: string }[] = [];
      for (const slot of this.localPlayer().benchSlots) {
        if (slot.pokemon && UUID_PATTERN.test(slot.pokemon.id)) {
          options.push({ pokemonInPlayId: slot.pokemon.id, label: slot.pokemon.activeCard.label });
        }
      }
      return options;
    }
    if (card.externalId === 'xy1-116') {
      const options: { pokemonInPlayId: string; label: string }[] = [];
      const active = this.localPlayer().activePokemon.pokemon;
      if (active) {
        options.push({
          pokemonInPlayId: active.id,
          label: this.t('GAME.TRAINER.ACTIVE_EVOLUTION_TARGET', { name: active.activeCard.label })
        });
      }
      this.localPlayer().benchSlots.forEach((slot, index) => {
        if (slot.pokemon) {
          options.push({
            pokemonInPlayId: slot.pokemon.id,
            label: this.t('GAME.TRAINER.BENCH_EVOLUTION_TARGET', {
              name: slot.pokemon.activeCard.label,
              position: index + 1
            })
          });
        }
      });
      return options;
    }
    if (card.externalId === 'xy1-128') {
      return this.pokemonOptionsFor(this.localPlayer());
    }
    return [];
  });
  readonly maxReviveDiscardPokemonOptions = computed(() => {
    const card = this.pendingTrainerCard() ?? this.selectedCardForModal();
    if (card?.externalId !== 'xy1-120') {
      return [];
    }
    return this.localPlayer().discardCards
      .filter((c) => c.visibility === 'visible' && c.supertype === 'POKEMON' && c.cardInstanceId)
      .map((c) => ({
        cardInstanceId: c.cardInstanceId!,
        label: c.label,
        imageSmallUrl: c.imageSmallUrl,
        imageLargeUrl: c.imageLargeUrl
      }));
  });
  readonly maxReviveDiscardEnergyOptions = computed(() => {
    const card = this.selectedCardForModal();
    if (card?.externalId !== 'xy1-120') {
      return [];
    }
    return this.localPlayer().discardCards
      .filter((c) => c.visibility === 'visible' && c.category === 'BASIC_ENERGY' && c.cardInstanceId)
      .map((c) => ({ cardInstanceId: c.cardInstanceId!, label: c.label }));
  });
  readonly attackAvailabilityByIndex = computed(() => {
    const context = this.selectedCardContext();
    const declareAttackAction = this.board().actions.find((action) => action.actionType === GameActionType.DeclareAttack);
    const attacks = context?.attacks ?? [];
    return Object.fromEntries(attacks.map((attack, index) => [
      index,
      {
        available: Boolean(
          this.canDeclareAttackFromModal() &&
          attack.enabled &&
          declareAttackAction?.enabled
        ),
        reason: this.languageService.gameDisabledReason(
          attack.disabledReason ?? declareAttackAction?.disabledReason,
          'GAME.DISABLED.ATTACK_UNAVAILABLE'
        )
      }
    ]));
  });
  readonly abilitySelectionByIndex = computed<Record<number, AbilitySelectionViewModel | null>>(() => {
    const context = this.selectedCardContext();
    const sourcePokemonId = context?.pokemonInPlayId;
    const abilities = context?.abilities ?? [];

    if (!sourcePokemonId) {
      return {};
    }

    return Object.fromEntries(abilities.map((ability, index) => [
      index,
      buildAbilitySelectionViewModel({
        ability,
        sourcePokemonId,
        localPlayer: this.localPlayer(),
        rivalPlayer: this.rivalPlayer()
      })
    ]));
  });
  readonly abilityAvailabilityByIndex = computed(() => {
    const context = this.selectedCardContext();
    const useAbilityAction = this.board().actions.find((action) => action.actionType === GameActionType.UseAbility);
    const abilities = context?.abilities ?? [];
    return Object.fromEntries(abilities.map((ability, index) => [
      index,
      {
        available: Boolean(
          this.canUseAbilityFromModal() &&
          !this.isPassiveAbility(ability) &&
          ability.enabled &&
          useAbilityAction?.enabled &&
          !this.abilitySelectionByIndex()[index]?.unsupportedReason
        ),
        reason: this.isPassiveAbility(ability)
          ? this.t('GAME.ABILITY.PASSIVE_AUTOMATIC')
          : this.abilitySelectionByIndex()[index]?.unsupportedReason ?? this.languageService.gameDisabledReason(
              ability.disabledReason ?? useAbilityAction?.disabledReason,
              'GAME.BACKEND_PENDING'
            )
      }
    ]));
  });

  readonly selectedCardInstanceId = computed(() => this.interaction()?.selectedCardInstanceId ?? null);
  readonly setupActiveCardInstanceId = computed(() => this.interaction()?.setupActiveCardInstanceId ?? null);
  readonly setupBenchCardInstanceIds = computed(() => this.interaction()?.setupBenchCardInstanceIds ?? []);
  readonly selectableCardInstanceIds = computed(() => this.interaction()?.selectableCardInstanceIds ?? []);
  readonly selectedPokemonInPlayId = computed(() => this.interaction()?.selectedPokemonInPlayId ?? null);
  readonly selectablePokemonInPlayIds = computed(() => this.interaction()?.selectablePokemonInPlayIds ?? []);
  readonly setupMode = computed(() =>
    this.interaction()?.pendingAction === GameActionType.ChooseInitialPokemon || this.board().mulliganFlowActive
  );
  readonly setupPhase = computed(() => this.board().status === GameStatus.Setup);
  readonly setupAlreadySubmitted = computed(() => this.board().localPlayer.setupSelectionSubmitted);
  readonly mulliganAckPending = computed(() =>
    this.actionPending() && this.board().mulliganNoticePending
  );
  // Staged Mulligan counters: derived from the reveal events whose animation HAS ALREADY STARTED
  // (releasedEventIds), not from the final snapshot count. So each counter climbs 1-by-1 exactly when
  // that attempt lands visually (the ¡MULLIGAN! reveal); once every reveal is released it equals the
  // authoritative snapshot mulliganCount. The real count stays in the view-model; this is presentation
  // only (no snapshot mutation, no DOM).
  readonly stagedMulliganCounts = computed(() => {
    const released = this.mulliganVisual.releasedEventIds();
    const localId = this.localPlayer().id;
    const rivalId = this.rivalPlayer().id;
    let own = 0;
    let opponent = 0;
    for (const event of this.board().mulliganSetupEvents) {
      if (event.kind !== 'revealed-hand' || !released.has(event.id)) {
        continue;
      }
      if (event.revealingPlayerId === localId) {
        own = Math.max(own, event.mulliganCount);
      } else if (event.revealingPlayerId === rivalId) {
        opponent = Math.max(opponent, event.mulliganCount);
      }
    }
    return { own, opponent };
  });
  // Staged deck/prize counts. During the Mulligan replay the snapshot is ALREADY final (deck 47, prizes 6),
  // so we synthesize the in-flight counts from card conservation during setup (deck + hand + prizes is
  // constant): deck = deckCount + prizeCount + (snapshotHand - visualHand); prizes = 0 until the flow ends.
  // The deck thus cycles 60<->53 with the hand staging, and prizes only appear (6) when the flow completes.
  // Outside the replay window (setupCountsStaged=false) the real snapshot counts show unchanged.
  readonly rivalDeckCountStaged = computed(() =>
    this.stagedDeckCount(this.rivalPlayer(), this.opponentHandCards().length));
  readonly localDeckCountStaged = computed(() =>
    this.stagedDeckCount(this.localPlayer(), this.localHandCards().length));
  readonly rivalPrizeCountStaged = computed(() => this.stagedPrizeCount(this.rivalPlayer()));
  readonly localPrizeCountStaged = computed(() => this.stagedPrizeCount(this.localPlayer()));
  readonly canSubmitSetup = computed(() =>
    Boolean(
      this.interaction()?.canConfirm &&
      !this.board().mulliganNoticePending &&
      !this.board().mulliganFlowActive &&
      this.board().mulliganReadyForInitialSelection &&
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot
    )
  );
  readonly setupBusy = computed(() => this.actionPending() || Boolean(this.interaction()?.awaitingSnapshot));
  readonly setupDisabledReason = computed(() => {
    if (this.setupAlreadySubmitted()) {
      return this.t('GAME.WAITING_FOR_OPPONENT');
    }
    if (!this.setupActiveCardInstanceId()) {
      return this.t('GAME.SETUP_ACTIVE_REQUIRED');
    }
    if (this.setupBusy()) {
      return this.t('GAME.DISABLED.ACTION_PENDING');
    }
    return null;
  });
  readonly endTurnAction = computed(() =>
    this.board().actions.find((action) => action.actionType === GameActionType.EndTurn) ?? null
  );
  readonly drawCardAction = computed(() =>
    this.board().actions.find((action) => action.actionType === GameActionType.DrawCard) ?? null
  );
  readonly isLocalPlayersTurn = computed(() => this.board().activePlayerId === this.localPlayer().id);
  readonly turnStartedAt = computed(() => this.board().turnContext.turnStartedAt);
  readonly turnRemainingSeconds = computed(() => {
    const startedAt = this.turnStartedAt();
    if (!startedAt) {
      return null;
    }

    const elapsedSeconds = Math.floor((this.nowMillis() - new Date(startedAt).getTime()) / 1000);
    return Math.max(0, TURN_TIMEOUT_SECONDS - elapsedSeconds);
  });
  readonly turnRemainingLabel = computed(() => {
    const remaining = this.turnRemainingSeconds();
    if (remaining === null) {
      return null;
    }

    const minutes = Math.floor(remaining / 60);
    const seconds = remaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  });
  readonly turnTimerUrgent = computed(() => {
    const remaining = this.turnRemainingSeconds();
    return remaining !== null && remaining <= URGENT_THRESHOLD_SECONDS;
  });
  readonly canDrawCard = computed(() =>
    Boolean(
      this.drawCardAction()?.enabled &&
      this.board().status === GameStatus.Active &&
      this.isLocalPlayersTurn() &&
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot
    )
  );
  readonly mandatoryDrawPending = computed(() => this.canDrawCard());
  readonly canEndTurn = computed(() =>
    Boolean(
      this.endTurnAction()?.enabled &&
      this.isLocalPlayersTurn() &&
      !this.mandatoryDrawPending() &&
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot
    )
  );
  readonly endTurnDisabledReason = computed(() => {
    if (this.mandatoryDrawPending()) {
      return this.t('GAME.DISABLED.DRAW_REQUIRED');
    }

    if (!this.isLocalPlayersTurn()) {
      return this.t('GAME.DISABLED.NOT_YOUR_TURN');
    }

    if (this.actionPending()) {
      return this.t('GAME.DISABLED.ACTION_PENDING');
    }

    if (this.interaction()?.awaitingSnapshot) {
      return this.t('GAME.DISABLED.WAITING_SYNC');
    }

    return this.languageService.gameDisabledReason(
      this.endTurnAction()?.disabledReason,
      'GAME.END_TURN_UNAVAILABLE'
    );
  });
  readonly drawCardDisabledReason = computed(() => {
    if (!this.isLocalPlayersTurn()) {
      return this.t('GAME.DISABLED.NOT_YOUR_TURN');
    }

    if (this.actionPending()) {
      return this.t('GAME.DISABLED.ACTION_PENDING');
    }

    if (this.interaction()?.awaitingSnapshot) {
      return this.t('GAME.DISABLED.WAITING_SYNC');
    }

    return this.languageService.gameDisabledReason(
      this.drawCardAction()?.disabledReason,
      'GAME.DISABLED.DRAW_UNAVAILABLE'
    );
  });
  readonly canDragHandCards = computed(() =>
    (this.setupMode() || this.isLocalPlayersTurn()) &&
    !this.board().mulliganFlowActive &&
    !this.mandatoryDrawPending() &&
    !this.actionPending() &&
    !this.interaction()?.awaitingSnapshot
  );
  readonly handPlayableCardIds = computed(() => {
    if (!this.canDragHandCards()) {
      return [];
    }

    const cards = this.localPlayer().handCards;
    const hasBenchSpace = this.localPlayer().benchSlots.some((slot) => !slot.occupied);

    return cards
      .filter((card) => {
        if (this.canDragEnergy(card)) return true;
        if (this.canDragToActive(card)) return true;
        if (this.canDragEvolution(card)) return true;
        if (this.canDragTrainer(card)) return true;
        if (isBasicStagePokemon(card) && (hasBenchSpace || this.setupMode())) return true;
        if (this.canDropCardAsBasicPokemon(card)) return true;
        return false;
      })
      .map((card) => cardReferenceId(card));
  });
  readonly initialActiveDropCardIds = computed(() =>
    this.localPlayer().handCards.filter((card) => this.canDragToActive(card)).map((card) => cardReferenceId(card))
  );
  readonly energyDropCardIds = computed(() =>
    this.localPlayer().handCards.filter((card) => this.canDragEnergy(card)).map((card) => cardReferenceId(card))
  );
  readonly evolutionDropCardIds = computed(() =>
    this.localPlayer().handCards.filter((card) => this.canDragEvolution(card)).map((card) => cardReferenceId(card))
  );
  readonly evolutionActiveDropCardIds = computed(() => {
    const activeId = this.localPlayer().activePokemon.pokemon?.id;
    if (!activeId) {
      return [];
    }

    return this.localPlayer().handCards
      .filter((card) => {
        if (!this.canDragEvolution(card)) {
          return false;
        }

        return (card.validTargetPokemonInPlayIds ?? []).includes(activeId);
      })
      .map((card) => cardReferenceId(card));
  });
  readonly draggedTrainerCard = computed(() => {
    const draggedTrainerCardId = this.draggedTrainerCardId();
    if (!draggedTrainerCardId) {
      return null;
    }

    return this.localPlayer().handCards.find((card) => cardReferenceId(card) === draggedTrainerCardId || card.id === draggedTrainerCardId) ?? null;
  });
  readonly trainerDragMode = computed<TrainerDragMode | null>(() => {
    const card = this.draggedTrainerCard();
    if (!card || !this.canDragTrainer(card)) {
      return null;
    }

    if (this.isPokemonToolTrainer(card)) {
      return 'POKEMON_TOOL_TARGET';
    }

    if (this.isStadiumTrainer(card)) {
      return 'STADIUM_AREA';
    }

    if (this.isItemTrainer(card) || this.isSupporterTrainer(card)) {
      return 'TRAINER_PLAY_AREA';
    }

    return null;
  });
  readonly generalTrainerDropZoneEnabled = computed(() => this.trainerDragMode() === 'TRAINER_PLAY_AREA');
  readonly stadiumDropZoneVisible = computed(() => this.trainerDragMode() === 'STADIUM_AREA');
  readonly trainerActiveDropCardIds = computed(() => {
    const activeId = this.localPlayer().activePokemon.pokemon?.id;
    if (!activeId) {
      return [];
    }

    return this.localPlayer().handCards
      .filter((card) => this.canDropTrainerOnActive(card, activeId))
      .map((card) => cardReferenceId(card));
  });
  readonly activePokemonToolDropEnabled = computed(() => {
    const card = this.draggedTrainerCard();
    const activeId = this.localPlayer().activePokemon.pokemon?.id;
    return Boolean(card && this.isPokemonToolTrainer(card) && activeId && this.canDropTrainerOnActive(card, activeId));
  });
  readonly activeDropCardIds = computed(() => [
    ...this.initialActiveDropCardIds(),
    ...this.energyDropCardIds(),
    ...this.evolutionActiveDropCardIds(),
    ...this.trainerActiveDropCardIds(),
    ...this.benchToActivePokemonIds()
  ]);
  readonly activeSlotDropEnabled = computed(() => {
    const draggedCardId = this.draggedInitialActiveCardId();
    const draggedEnergyCardId = this.draggedEnergyCardId();
    const draggedEvolutionCardId = this.draggedEvolutionCardId();
    const draggedTrainerCardId = this.draggedTrainerCardId();
    const draggedBenchPokemonId = this.draggedBenchPokemonInPlayId();
    return (
      (this.canDragHandCards() &&
        draggedCardId !== null &&
        this.initialActiveDropCardIds().includes(draggedCardId)) ||
      (this.canDragHandCards() &&
        draggedEnergyCardId !== null &&
        this.canDropEnergyOnSlot(this.localPlayer().activePokemon)) ||
      (this.canDragHandCards() &&
        draggedEvolutionCardId !== null &&
        this.evolutionActiveDropCardIds().includes(draggedEvolutionCardId)) ||
      (this.canDragHandCards() &&
        draggedTrainerCardId !== null &&
        this.trainerActiveDropCardIds().includes(draggedTrainerCardId)) ||
      (draggedBenchPokemonId !== null && this.canMoveBenchPokemonToActive(draggedBenchPokemonId))
    );
  });
  readonly retreatTargetPokemonInPlayIds = computed(() => this.resolveRetreatTargetPokemonInPlayIds());
  /**
   * Single source of truth for "the local player must promote a bench Pokemon to active".
   * Deliberately derived ONLY from the authoritative resolution state + bench occupancy,
   * not from the `board().actions` enabled-flag chain. That chain has multiple hops
   * (backend availableActions -> isActionEnabledForPlayer -> action.enabled -> hinted ids)
   * and any desync there was silently disabling bench drag during a KO promotion.
   */
  readonly canPromoteFromBench = computed<boolean>(() => {
    const board = this.board();
    return (
      board.status === GameStatus.Active &&
      board.resolution?.playerToPromoteId === this.localPlayer().id &&
      this.localPlayer().benchSlots.some((slot) => Boolean(slot.pokemon))
    );
  });
  readonly promoteTargetPokemonInPlayIds = computed(() => this.resolvePromoteTargetPokemonInPlayIds());
  readonly promotionBannerVisible = computed(() => this.canPromoteFromBench());
  readonly opponentPromotionBannerVisible = computed(
    () => this.board().resolution?.playerToPromoteId === this.rivalPlayer().id
  );
  readonly attackChoiceModalVisible = computed(
    () => this.board().resolution?.pendingChoicePlayerId === this.localPlayer().id
  );
  readonly opponentAttackChoiceBannerVisible = computed(
    () => this.board().resolution?.pendingChoicePlayerId === this.rivalPlayer().id
  );
  readonly opponentAttackChoiceTitle = computed(() =>
    this.board().resolution?.pendingChoiceType === SELECT_HAND_CARDS_TO_DISCARD
      ? this.t('GAME.ATTACK_CHOICE.MENTAL_TRASH_WAIT_TITLE')
      : this.t('GAME.ATTACK_CHOICE.WAIT_TITLE')
  );
  readonly opponentAttackChoiceMessage = computed(() =>
    this.board().resolution?.pendingChoiceType === SELECT_HAND_CARDS_TO_DISCARD
      ? this.t('GAME.ATTACK_CHOICE.MENTAL_TRASH_WAIT_MESSAGE')
      : this.t('GAME.ATTACK_CHOICE.WAIT_MESSAGE')
  );
  readonly benchToActivePokemonIds = computed(() => [
    ...new Set([
      ...this.retreatTargetPokemonInPlayIds(),
      ...this.promoteTargetPokemonInPlayIds()
    ])
  ]);
  readonly energyBenchDropSlotIds = computed(() => {
    if (!this.draggedEnergyCardId()) {
      return [];
    }

    return this.localPlayer().benchSlots
      .filter((slot) => this.canDropEnergyOnSlot(slot))
      .map((slot) => slot.id);
  });
  readonly evolutionBenchDropSlotIds = computed(() => {
    const draggedEvolutionCardId = this.draggedEvolutionCardId();
    if (!draggedEvolutionCardId) {
      return [];
    }

    const draggedCard = this.localPlayer().handCards.find(
      (card) => cardReferenceId(card) === draggedEvolutionCardId || card.id === draggedEvolutionCardId
    );
    if (!draggedCard) {
      return [];
    }

    const targetIds = draggedCard.validTargetPokemonInPlayIds ?? [];
    if (targetIds.length === 0) {
      return [];
    }

    return this.localPlayer().benchSlots
      .filter((slot) => slot.pokemon && targetIds.includes(slot.pokemon.id))
      .map((slot) => slot.id);
  });
  readonly pokemonToolBenchDropSlotIds = computed(() => {
    const card = this.draggedTrainerCard();
    if (!card || !this.isPokemonToolTrainer(card)) {
      return [];
    }

    return this.localPlayer().benchSlots
      .filter((slot) => slot.pokemon && this.canDropTrainerOnPokemon(card, slot.pokemon.id))
      .map((slot) => slot.id);
  });
  readonly setupBenchDropSlotIds = computed(() => {
    if (!this.draggedInitialActiveCardId()) {
      return [];
    }

    return this.localPlayer().benchSlots.filter((slot) => !slot.occupied).map((slot) => slot.id);
  });
  readonly benchDropSlotIds = computed(() => [
    ...this.setupBenchDropSlotIds(),
    ...this.energyBenchDropSlotIds(),
    ...this.evolutionBenchDropSlotIds(),
    ...this.pokemonToolBenchDropSlotIds()
  ]);
  readonly benchDropEnabled = computed(() =>
    this.setupMode()
      ? Boolean(this.draggedInitialActiveCardId() && !this.actionPending())
      : this.canDragHandCards()
  );

  moveHandCardToBench(drop: BenchCardDrop): void {
    if (this.actionPending() || drop.source !== 'current-hand') {
      return;
    }

    const currentPlayer = this.localPlayer();
    const targetSlot = currentPlayer.benchSlots.find((slot) => slot.id === drop.slotId);
    const draggedCard = currentPlayer.handCards.find((card) => cardReferenceId(card) === drop.cardId || card.id === drop.cardId);

    if (!targetSlot || !draggedCard || draggedCard.visibility !== 'visible') {
      return;
    }

    const trainerTargetPokemonInPlayId = targetSlot.pokemon?.id;
    if (trainerTargetPokemonInPlayId && this.canDropTrainerOnPokemon(draggedCard, trainerTargetPokemonInPlayId)) {
      this.playTrainerOnTarget(draggedCard, trainerTargetPokemonInPlayId);
      this.finishHandCardDrag();
      return;
    }

    if (this.canDragEvolution(draggedCard)) {
      const targetPokemonInPlayId = targetSlot.pokemon?.id;
      if (
        targetPokemonInPlayId &&
        (draggedCard.validTargetPokemonInPlayIds ?? []).includes(targetPokemonInPlayId)
      ) {
        this.evolutionDropped.emit({
          evolutionCardInstanceId: cardReferenceId(draggedCard),
          targetPokemonInPlayId
        });
        this.finishHandCardDrag();
      }
      return;
    }

    if (this.setupMode() && this.canDragToActive(draggedCard) && !targetSlot.occupied) {
      this.setupBenchCardDropped.emit(cardReferenceId(draggedCard));
      this.finishHandCardDrag();
      return;
    }

    if (this.canDropEnergyOnSlot(targetSlot, draggedCard)) {
      const targetPokemonInPlayId = targetSlot.pokemon?.id;
      if (targetPokemonInPlayId) {
        this.energyDropped.emit({
          energyCardInstanceId: cardReferenceId(draggedCard),
          targetPokemonInPlayId
        });
        this.finishHandCardDrag();
      }
      return;
    }

    if (targetSlot.occupied || !this.canDropCardAsBasicPokemon(draggedCard)) {
      return;
    }

    this.playBasicPokemon.emit(draggedCard.cardId ?? draggedCard.cardInstanceId ?? draggedCard.id);
  }

  handlePointerCardDrop(drop: HandCardPointerDrop): void {
    if (drop.target === 'active') {
      this.handleDropOnActive(drop);
      return;
    }

    if (drop.target === 'pokemon-tool-target') {
      if (!drop.pokemonInPlayId) {
        return;
      }

      this.handleDropOnPokemonToolTarget(drop.cardId, drop.pokemonInPlayId);
      return;
    }

    if (drop.target === 'trainer-play-area') {
      this.playTrainerFromDrop(drop.cardId);
      return;
    }

    if (drop.target === 'stadium-area') {
      this.playStadiumFromDrop(drop.cardId);
      return;
    }

    if (drop.slotId) {
      this.moveHandCardToBench({ ...drop, slotId: drop.slotId });
    }
  }

  confirmTrainerDrop(result: {
    targetPokemonInPlayId?: string;
    targetCardInstanceId?: string;
    selectedEvolutionExternalId?: string;
    selectedCardInstanceId?: string;
    selectedCardIds?: string[];
  }): void {
    const card = this.pendingTrainerCard();
    if (!card) {
      return;
    }
    this.pendingTrainerCard.set(null);
    this.trainerPlayed.emit({
      cardInstanceId: this.trainerPayloadCardInstanceId(card),
      targetPokemonInPlayId: result.targetPokemonInPlayId,
      targetCardInstanceId: result.targetCardInstanceId,
      selectedEvolutionExternalId: result.selectedEvolutionExternalId,
      selectedCardInstanceId: result.selectedCardInstanceId,
      selectedCardIds: result.selectedCardIds
    });
  }

  cancelTrainerDrop(): void {
    this.pendingTrainerCard.set(null);
  }

  private async loadEvosodaPreview(card: BoardCardViewModel): Promise<void> {
    const trainerCardInstanceId = card.cardInstanceId;
    if (!trainerCardInstanceId) {
      this.evosodaPreviewError.set(this.t('GAME.TRAINER.EVOLUTION_PREVIEW_ERROR'));
      return;
    }
    if (this.loadedEvosodaTrainerCardId === trainerCardInstanceId) {
      return;
    }

    this.loadedEvosodaTrainerCardId = trainerCardInstanceId;
    this.evosodaPreviewLoading.set(true);
    this.evosodaPreviewError.set(null);
    this.evosodaEvolutionOptions.set([]);

    try {
      const preview = await this.trainerPreviewService.previewEvosoda(
        this.board().gameId,
        trainerCardInstanceId
      );
      if (this.pendingTrainerCard() !== card) {
        return;
      }
      this.evosodaEvolutionOptions.set(preview.options);
    } catch {
      if (this.pendingTrainerCard() === card) {
        this.evosodaPreviewError.set(this.t('GAME.TRAINER.EVOLUTION_PREVIEW_ERROR'));
      }
    } finally {
      if (this.pendingTrainerCard() === card) {
        this.evosodaPreviewLoading.set(false);
      }
    }
  }

  private clearEvosodaPreview(): void {
    this.loadedEvosodaTrainerCardId = null;
    this.evosodaEvolutionOptions.set([]);
    this.evosodaPreviewLoading.set(false);
    this.evosodaPreviewError.set(null);
  }

  private async loadGreatBallPreview(card: BoardCardViewModel): Promise<void> {
    const trainerCardInstanceId = card.cardInstanceId;
    if (!trainerCardInstanceId) {
      this.greatBallPreviewError.set(this.t('GAME.TRAINER.DECK_POKEMON_PREVIEW_ERROR'));
      return;
    }
    if (this.loadedGreatBallTrainerCardId === trainerCardInstanceId) {
      return;
    }

    this.loadedGreatBallTrainerCardId = trainerCardInstanceId;
    this.greatBallPreviewLoading.set(true);
    this.greatBallPreviewReady.set(false);
    this.greatBallPreviewError.set(null);
    this.greatBallDeckPokemonOptions.set([]);
    this.greatBallCardsLookedAt.set(0);

    try {
      const preview = await this.trainerPreviewService.previewGreatBall(
        this.board().gameId,
        trainerCardInstanceId
      );
      if (this.pendingTrainerCard() !== card) {
        return;
      }
      this.greatBallDeckPokemonOptions.set(preview.pokemonOptions);
      this.greatBallCardsLookedAt.set(preview.cardsLookedAt);
      this.greatBallPreviewReady.set(true);
    } catch {
      if (this.pendingTrainerCard() === card) {
        this.greatBallPreviewError.set(this.t('GAME.TRAINER.DECK_POKEMON_PREVIEW_ERROR'));
      }
    } finally {
      if (this.pendingTrainerCard() === card) {
        this.greatBallPreviewLoading.set(false);
      }
    }
  }

  private clearGreatBallPreview(): void {
    this.loadedGreatBallTrainerCardId = null;
    this.greatBallDeckPokemonOptions.set([]);
    this.greatBallCardsLookedAt.set(0);
    this.greatBallPreviewLoading.set(false);
    this.greatBallPreviewReady.set(false);
    this.greatBallPreviewError.set(null);
  }

  private async loadProfessorLetterPreview(card: BoardCardViewModel): Promise<void> {
    const trainerCardInstanceId = this.trainerPayloadCardInstanceId(card);
    if (!trainerCardInstanceId || this.loadedProfessorLetterTrainerCardId === trainerCardInstanceId) {
      return;
    }

    this.loadedProfessorLetterTrainerCardId = trainerCardInstanceId;
    this.professorLetterPreviewLoading.set(true);
    this.professorLetterPreviewError.set(null);
    this.professorLetterEnergyOptions.set([]);
    this.professorLetterMaxSelectable.set(2);

    try {
      const preview = await this.trainerPreviewService.previewProfessorLetter(
        this.board().gameId,
        trainerCardInstanceId
      );
      if (this.pendingTrainerCard() !== card) {
        return;
      }
      this.professorLetterEnergyOptions.set(preview.options);
      this.professorLetterMaxSelectable.set(preview.maxSelectable);
    } catch {
      if (this.pendingTrainerCard() === card) {
        this.professorLetterPreviewError.set(this.t('GAME.TRAINER.BASIC_ENERGY_PREVIEW_ERROR'));
      }
    } finally {
      if (this.pendingTrainerCard() === card) {
        this.professorLetterPreviewLoading.set(false);
      }
    }
  }

  private clearProfessorLetterPreview(): void {
    this.loadedProfessorLetterTrainerCardId = null;
    this.professorLetterEnergyOptions.set([]);
    this.professorLetterMaxSelectable.set(2);
    this.professorLetterPreviewLoading.set(false);
    this.professorLetterPreviewError.set(null);
  }

  handleTrainerPlayAreaDragOver(event: DragEvent): void {
    if (!this.generalTrainerDropZoneEnabled()) {
      return;
    }

    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  handleTrainerPlayAreaDragLeave(event: DragEvent): void {
    const nextTarget = event.relatedTarget as Node | null;
    if (nextTarget && event.currentTarget instanceof HTMLElement && event.currentTarget.contains(nextTarget)) {
      return;
    }

    this.trainerDropAreaActive.set(false);
  }

  handleTrainerPlayAreaDrop(event: DragEvent): void {
    if (!this.generalTrainerDropZoneEnabled()) {
      this.trainerDropAreaActive.set(false);
      return;
    }

    event.preventDefault();
    this.trainerDropAreaActive.set(false);
    const cardId = event.dataTransfer?.getData('application/x-pokemon-card-id');
    if (!cardId) {
      return;
    }

    this.playTrainerFromDrop(cardId);
  }

  private playTrainerFromDrop(cardId: string): void {
    this.isDraggingTrainer.set(false);
    this.trainerDropAreaActive.set(false);
    if (!this.canPlayTrainerAction()) {
      return;
    }

    const card = this.localPlayer().handCards.find((c) => cardReferenceId(c) === cardId || c.id === cardId);
    if (!card || this.trainerDragModeForCard(card) !== 'TRAINER_PLAY_AREA') {
      return;
    }

    if (this.trainerNeedsSelection(card)) {
      this.pendingTrainerCard.set(card);
    } else {
      this.trainerPlayed.emit({ cardInstanceId: this.trainerPayloadCardInstanceId(card) });
    }
    this.finishHandCardDrag();
  }

  handleStadiumAreaDragOver(event: DragEvent): void {
    if (!this.stadiumDropZoneVisible()) {
      return;
    }

    event.preventDefault();
    this.trainerDropAreaActive.set(true);
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  handleStadiumAreaDragLeave(): void {
    this.trainerDropAreaActive.set(false);
  }

  handleStadiumAreaDrop(event: DragEvent): void {
    if (!this.stadiumDropZoneVisible()) {
      this.trainerDropAreaActive.set(false);
      return;
    }

    event.preventDefault();
    this.trainerDropAreaActive.set(false);
    const cardId = event.dataTransfer?.getData('application/x-pokemon-card-id');
    if (!cardId) {
      return;
    }

    this.playStadiumFromDrop(cardId);
  }

  private playStadiumFromDrop(cardId: string): void {
    this.isDraggingTrainer.set(false);
    this.trainerDropAreaActive.set(false);
    if (!this.canPlayTrainerAction()) {
      return;
    }

    const card = this.localPlayer().handCards.find((c) => cardReferenceId(c) === cardId || c.id === cardId);
    if (!card || this.trainerDragModeForCard(card) !== 'STADIUM_AREA') {
      return;
    }

    this.trainerPlayed.emit({ cardInstanceId: this.trainerPayloadCardInstanceId(card) });
    this.finishHandCardDrag();
  }

  private handleDropOnPokemonToolTarget(cardId: string, pokemonInPlayId: string): void {
    const card = this.localPlayer().handCards.find((c) => cardReferenceId(c) === cardId || c.id === cardId);
    if (!card || !this.canDropTrainerOnPokemon(card, pokemonInPlayId)) {
      return;
    }

    this.playTrainerOnTarget(card, pokemonInPlayId);
    this.finishHandCardDrag();
  }

  private trainerNeedsSelection(card: BoardCardViewModel): boolean {
    if (this.isPokemonToolTrainer(card)) {
      return true;
    }
    return ['xy1-115', 'xy1-116', 'xy1-118', 'xy1-120', 'xy1-123', 'xy1-128'].includes(card.externalId ?? '');
  }

  selectHandCard(cardInstanceId: string): void {
    this.handCardSelected.emit(cardInstanceId);
  }

  openCardModal(card: BoardCardViewModel, context: CardContext): void {
    this.selectedCardForModal.set(card);
    this.selectedCardContext.set(context);
    this.selectedCardDetailForModal.set(null);
    this.cardDetailError.set(null);
    this.loadCardDetail(card);
  }

  closeCardModal(): void {
    this.selectedCardForModal.set(null);
    this.selectedCardContext.set(null);
    this.selectedCardDetailForModal.set(null);
    this.isCardDetailLoading.set(false);
    this.cardDetailError.set(null);
  }

  closeDiscardModal(): void {
    this.discardModalCards.set([]);
    this.discardModalLabel.set(null);
  }

  private closeTransientModals(): void {
    this.closeCardModal();
    this.closeDiscardModal();
    this.closeMobileLogs();
    this.pendingTrainerCard.set(null);
  }

  openHandCardModal(card: BoardCardViewModel, owner: CardContext['owner']): void {
    this.openCardModal(card, {
      zone: 'hand',
      owner,
      label: owner === 'local' ? this.t('GAME.YOUR_HAND') : this.t('GAME.OPPONENT_HAND')
    });
  }

  openActiveCardModal(card: BoardCardViewModel, owner: CardContext['owner']): void {
    const slot = owner === 'local' ? this.localPlayer().activePokemon : this.rivalPlayer().activePokemon;
    this.openCardModal(card, {
      zone: 'active',
      owner,
      label: owner === 'local' ? this.t('GAME.ACTIVE_POKEMON') : this.t('GAME.ACTIVE_POKEMON'),
      slotId: slot.id,
      pokemonInPlayId: slot.pokemon?.id,
      damageCounters: slot.pokemon?.damageCounters,
      attacks: slot.pokemon?.attacks ?? [],
      abilities: this.realPokemonAbilities(slot.pokemon?.abilities)
    });
  }

  openBenchCardModal(
    inspected: { card: BoardCardViewModel; slot: BoardPlayerViewModel['activePokemon'] },
    owner: CardContext['owner']
  ): void {
    this.openCardModal(inspected.card, {
      zone: 'bench',
      owner,
      label: owner === 'local' ? this.t('GAME.YOUR_BENCH') : this.t('GAME.OPPONENT_BENCH'),
      slotId: inspected.slot.id,
      pokemonInPlayId: inspected.slot.pokemon?.id,
      damageCounters: inspected.slot.pokemon?.damageCounters,
      attacks: inspected.slot.pokemon?.attacks ?? [],
      abilities: this.realPokemonAbilities(inspected.slot.pokemon?.abilities)
    });
  }

  openDiscardCardModal(card: BoardCardViewModel, owner: CardContext['owner']): void {
    void card;
    this.discardModalCards.set(owner === 'local' ? this.localPlayer().discardCards : this.rivalPlayer().discardCards);
    this.discardModalLabel.set(this.t('GAME.DISCARD'));
  }

  openStadiumCardModal(card: BoardCardViewModel): void {
    this.openCardModal(card, {
      zone: 'stadium',
      owner: 'neutral',
      label: this.t('GAME.STADIUM')
    });
  }

  declareAttackFromModal(event: {
    attackId?: string;
    attackIndex: number;
    attackName: string;
    useBonusDamage?: boolean;
    selfTargetPokemonInPlayId?: string;
    switchTargetPokemonInPlayId?: string;
  }): void {
    if (!event.attackId || !this.canDeclareAttackFromModal()) {
      return;
    }

    this.attackDeclared.emit({
      attackId: event.attackId,
      useBonusDamage: event.useBonusDamage,
      selfTargetPokemonInPlayId: event.selfTargetPokemonInPlayId,
      switchTargetPokemonInPlayId: event.switchTargetPokemonInPlayId
    });
    this.closeCardModal();
  }

  declareAbilityFromCardModal(event: {
    sourcePokemonId: string;
    ability: BoardPokemonAbilityViewModel;
    payload: UseAbilityPayload;
  }): void {
    if (!this.canUseAbilityFromModal()) {
      return;
    }

    if (event.ability.id === FAIRY_TRANSFER_ABILITY_ID) {
      this.pendingFairyTransferAbility.set(event);
      this.closeCardModal();
      return;
    }

    this.abilityUsed.emit(event.payload);
    this.closeCardModal();
  }

  useAbilityFromModal(payload: UseAbilityPayload): void {
    this.abilityUsed.emit(payload);
    this.pendingFairyTransferAbility.set(null);
    this.closeCardModal();
  }

  closeFairyTransferModal(): void {
    this.pendingFairyTransferAbility.set(null);
  }

  resolveAttackChoiceFromModal(payload: ResolveAttackChoicePayload): void {
    this.attackChoiceResolved.emit(payload);
  }

  private loadRevealedAttackChoiceCard(revealedCardId: string | null): void {
    if (!revealedCardId) {
      this.loadedRevealedAttackChoiceCardId = null;
      this.revealedAttackChoiceCard.set(null);
      this.revealedAttackChoiceCardError.set(null);
      this.isRevealedAttackChoiceCardLoading.set(false);
      return;
    }

    if (this.loadedRevealedAttackChoiceCardId === revealedCardId) {
      return;
    }

    this.loadedRevealedAttackChoiceCardId = revealedCardId;
    this.revealedAttackChoiceCardError.set(null);

    const cachedDetail = this.revealedAttackChoiceCardCache.get(revealedCardId);
    if (cachedDetail) {
      this.revealedAttackChoiceCard.set(cachedDetail);
      return;
    }

    this.isRevealedAttackChoiceCardLoading.set(true);
    this.cardsApiService.getCardById(revealedCardId).subscribe({
      next: (detail) => {
        if (this.loadedRevealedAttackChoiceCardId !== revealedCardId) {
          return;
        }

        this.revealedAttackChoiceCardCache.set(revealedCardId, detail);
        this.revealedAttackChoiceCard.set(detail);
        this.isRevealedAttackChoiceCardLoading.set(false);
      },
      error: () => {
        if (this.loadedRevealedAttackChoiceCardId !== revealedCardId) {
          return;
        }

        this.revealedAttackChoiceCard.set(null);
        this.isRevealedAttackChoiceCardLoading.set(false);
        this.revealedAttackChoiceCardError.set(this.t('POKEDEX.LOAD_ERROR'));
      }
    });
  }

  private loadCardDetail(card: BoardCardViewModel): void {
    const requestedCardId = card.cardId;
    this.isCardDetailLoading.set(false);

    if (!requestedCardId) {
      this.cardDetailError.set(this.t('COMMON.NO_DATA'));
      return;
    }

    const cachedDetail = this.cardDetailCache.get(requestedCardId);
    if (cachedDetail) {
      this.selectedCardDetailForModal.set(cachedDetail);
      return;
    }

    this.isCardDetailLoading.set(true);
    this.cardsApiService.getCardById(requestedCardId).subscribe({
      next: (detail) => {
        if (this.selectedCardForModal()?.cardId !== requestedCardId) {
          return;
        }

        this.cardDetailCache.set(requestedCardId, detail);
        this.selectedCardDetailForModal.set(detail);
        this.isCardDetailLoading.set(false);
      },
      error: () => {
        if (this.selectedCardForModal()?.cardId !== requestedCardId) {
          return;
        }

        this.selectedCardDetailForModal.set(null);
        this.isCardDetailLoading.set(false);
        this.cardDetailError.set(this.t('POKEDEX.LOAD_ERROR'));
      }
    });
  }

  private realPokemonAbilities(
    abilities: BoardPokemonAbilityViewModel[] | null | undefined
  ): BoardPokemonAbilityViewModel[] {
    return (abilities ?? []).filter((ability) => ability.id !== GameActionType.UseAbility);
  }

  private isPassiveAbility(ability: BoardPokemonAbilityViewModel): boolean {
    return ability.activationType?.toUpperCase() === PASSIVE_ABILITY_ACTIVATION_TYPE;
  }

  selectPokemon(pokemonInPlayId: string): void {
    const setupBenchCardId = setupBenchCardIdFromPokemonId(pokemonInPlayId)
      ?? this.board().localPlayer.benchSlots
        .find((slot) => slot.pokemon?.id === pokemonInPlayId)
        ?.card;
    if (this.setupMode() && setupBenchCardId) {
      this.setupBenchCardReturned.emit(
        typeof setupBenchCardId === 'string' ? setupBenchCardId : cardReferenceId(setupBenchCardId)
      );
      return;
    }

    this.pokemonSelected.emit(pokemonInPlayId);
  }

  startHandCardDrag(card: BoardCardViewModel): void {
    this.draggedBenchPokemonInPlayId.set(null);
    this.trainerDropAreaActive.set(false);
    const cardId = cardReferenceId(card);
    this.draggedInitialActiveCardId.set(this.canDragToActive(card) ? cardId : null);
    this.draggedEnergyCardId.set(this.canDragEnergy(card) ? cardId : null);
    this.draggedEvolutionCardId.set(this.canDragEvolution(card) ? cardId : null);
    this.draggedTrainerCardId.set(this.canDragTrainer(card) ? cardId : null);
    this.isDraggingTrainer.set(this.isTrainerCategory(card) && this.canPlayTrainerAction());
  }

  finishHandCardDrag(): void {
    this.draggedInitialActiveCardId.set(null);
    this.draggedEnergyCardId.set(null);
    this.draggedEvolutionCardId.set(null);
    this.draggedTrainerCardId.set(null);
    this.isDraggingTrainer.set(false);
    this.trainerDropAreaActive.set(false);
  }

  startBenchPokemonDrag(pokemonInPlayId: string): void {
    this.draggedInitialActiveCardId.set(null);
    this.draggedEnergyCardId.set(null);
    this.draggedEvolutionCardId.set(null);
    this.draggedTrainerCardId.set(null);
    this.isDraggingTrainer.set(false);
    this.trainerDropAreaActive.set(false);
    this.draggedBenchPokemonInPlayId.set(
      this.canMoveBenchPokemonToActive(pokemonInPlayId) ? pokemonInPlayId : null
    );
  }

  finishBenchPokemonDrag(): void {
    this.draggedBenchPokemonInPlayId.set(null);
  }

  clearSetupBench(): void {
    if (!this.setupMode() || this.setupBusy()) {
      return;
    }

    for (const cardInstanceId of this.setupBenchCardInstanceIds()) {
      this.setupBenchCardReturned.emit(cardInstanceId);
    }
  }

  handleDropOnActive(drop: ActivePokemonCardDrop): void {
    if (this.actionPending()) {
      return;
    }

    if (drop.source === 'local-bench') {
      this.moveBenchPokemonToActive(drop.cardId);
      this.finishBenchPokemonDrag();
      return;
    }

    if (drop.source !== 'current-hand') {
      return;
    }

    const draggedCard = this.localPlayer().handCards.find((card) => cardReferenceId(card) === drop.cardId || card.id === drop.cardId);
    if (!draggedCard) {
      return;
    }

    const activePokemonInPlayId = this.localPlayer().activePokemon.pokemon?.id;
    if (activePokemonInPlayId && this.canDropTrainerOnActive(draggedCard, activePokemonInPlayId)) {
      this.playTrainerOnTarget(draggedCard, activePokemonInPlayId);
      this.finishHandCardDrag();
      return;
    }

    if (this.canDragEvolution(draggedCard)) {
      const targetPokemonInPlayId = this.localPlayer().activePokemon.pokemon?.id;
      if (
        targetPokemonInPlayId &&
        (draggedCard.validTargetPokemonInPlayIds ?? []).includes(targetPokemonInPlayId)
      ) {
        this.evolutionDropped.emit({
          evolutionCardInstanceId: cardReferenceId(draggedCard),
          targetPokemonInPlayId
        });
        this.finishHandCardDrag();
      }
      return;
    }

    if (!this.canDropOnActive(draggedCard)) {
      return;
    }

    const targetPokemonInPlayId = this.localPlayer().activePokemon.pokemon?.id;
    if (targetPokemonInPlayId && this.canDropEnergyOnSlot(this.localPlayer().activePokemon, draggedCard)) {
      this.energyDropped.emit({
        energyCardInstanceId: cardReferenceId(draggedCard),
        targetPokemonInPlayId
      });
    } else {
      this.setupActiveCardDropped.emit(cardReferenceId(draggedCard));
    }
    this.finishHandCardDrag();
  }

  private moveBenchPokemonToActive(pokemonInPlayId: string): void {
    if (!this.canMoveBenchPokemonToActive(pokemonInPlayId)) {
      return;
    }

    if (this.canPromoteBenchPokemonToActive(pokemonInPlayId)) {
      this.benchPokemonPromoted.emit(pokemonInPlayId);
      return;
    }

    if (this.canRetreatActiveToBenchPokemon(pokemonInPlayId)) {
      this.activePokemonRetreated.emit(pokemonInPlayId);
    }
  }

  private canDragToActive(card: BoardCardViewModel): boolean {
    const interaction = this.interaction();
    return (
      interaction?.pendingAction === GameActionType.ChooseInitialPokemon &&
      !this.actionPending() &&
      card.visibility === 'visible' &&
      isBasicStagePokemon(card) &&
      interaction.selectableCardInstanceIds.includes(cardReferenceId(card))
    );
  }

  private canDropOnActive(card: BoardCardViewModel): boolean {
    return this.canDragToActive(card) || this.canDropEnergyOnSlot(this.localPlayer().activePokemon, card);
  }

  private canMoveBenchPokemonToActive(pokemonInPlayId: string): boolean {
    return (
      !this.actionPending() &&
      !this.interaction()?.awaitingSnapshot &&
      this.isLocalBenchPokemon(pokemonInPlayId) &&
      (this.canPromoteBenchPokemonToActive(pokemonInPlayId) || this.canRetreatActiveToBenchPokemon(pokemonInPlayId))
    );
  }

  private canPromoteBenchPokemonToActive(pokemonInPlayId: string): boolean {
    return this.canPromoteFromBench() && this.isLocalBenchPokemon(pokemonInPlayId);
  }

  /** Maps a revealed Mulligan card (the rival's invalid hand) to a face-up board card view model. */
  private revealedMulliganCard(card: MulliganRevealedCard, index: number): BoardCardViewModel {
    const label = card.name ?? this.t('GAME.CARD_FALLBACK');
    return {
      id: `mulligan-revealed-${card.cardId}-${index}`,
      cardId: card.cardId,
      cardInstanceId: card.cardId,
      label,
      visibility: 'visible',
      faceDown: false,
      rotation: 0,
      imageSmallUrl: card.imageSmallUrl,
      imageLargeUrl: card.imageLargeUrl,
      hp: card.hp,
      altText: label,
      externalId: card.externalId ?? undefined,
      setCode: card.setCode ?? undefined,
      number: card.number ?? undefined,
      supertype: card.supertype ?? undefined,
      category: card.category ?? undefined
    };
  }

  /** The deck count to display: the real snapshot value, or the staged in-flight value during a Mulligan. */
  private stagedDeckCount(player: BoardPlayerViewModel, visualHandCount: number): number {
    if (!this.mulliganVisual.setupCountsStaged()) {
      return player.deckCount;
    }
    // Cards momentarily back in the deck (hand emptied during return), plus the prizes not yet separated.
    const cardsReturnedToDeck = Math.max(0, player.handCards.length - visualHandCount);
    return player.deckCount + player.prizeCount + cardsReturnedToDeck;
  }

  /** The prize count to display: real snapshot value, or 0 while the Mulligan flow has not finished. */
  private stagedPrizeCount(player: BoardPlayerViewModel): number {
    return this.mulliganVisual.setupCountsStaged() ? 0 : player.prizeCount;
  }

  /** The hand shown right after a draw: the owner's real cards, or a count of face-down cards for the rival. */
  private settledHandCards(settled: MulliganSettledHand, playerId: string): BoardCardViewModel[] {
    if (settled.cards && settled.cards.length > 0) {
      return settled.cards.map((card, index) => this.revealedMulliganCard(card, index));
    }

    const count = settled.faceDownCount ?? 0;
    return Array.from({ length: count }, (_, index) => ({
      id: `mulligan-settled-back-${playerId}-${index}`,
      cardId: null,
      cardInstanceId: null,
      label: this.t('GAME.OPPONENT_HAND'),
      visibility: 'hidden' as const,
      faceDown: true,
      rotation: 0
    }));
  }

  private pokemonOptionsFor(player: BoardPlayerViewModel): { pokemonInPlayId: string; label: string }[] {
    const options: { pokemonInPlayId: string; label: string }[] = [];
    const active = player.activePokemon.pokemon;
    if (active) {
      options.push({ pokemonInPlayId: active.id, label: active.activeCard.label });
    }
    for (const slot of player.benchSlots) {
      if (slot.pokemon) {
        options.push({ pokemonInPlayId: slot.pokemon.id, label: slot.pokemon.activeCard.label });
      }
    }
    return options;
  }

  private canRetreatActiveToBenchPokemon(pokemonInPlayId: string): boolean {
    return (
      this.board().activePlayerId === this.localPlayer().id &&
      this.localPlayer().activePokemon.occupied &&
      !this.mandatoryDrawPending() &&
      this.retreatTargetPokemonInPlayIds().includes(pokemonInPlayId)
    );
  }

  private resolveRetreatTargetPokemonInPlayIds(): string[] {
    const retreatAction = this.board().actions.find((action) => action.actionType === GameActionType.Retreat);
    if (!retreatAction?.enabled || !this.localPlayer().activePokemon.pokemon) {
      return [];
    }

    const hintedIds = this.board().actionHints.retreatTargetPokemonInPlayIds;
    return hintedIds.length > 0
      ? hintedIds.filter((pokemonInPlayId) => this.isLocalBenchPokemon(pokemonInPlayId))
      : this.localPlayer().benchSlots
          .map((slot) => slot.pokemon)
          .filter((pokemon): pokemon is NonNullable<typeof pokemon> => Boolean(pokemon?.canRetreatTo))
          .map((pokemon) => pokemon.id);
  }

  private resolvePromoteTargetPokemonInPlayIds(): string[] {
    if (!this.canPromoteFromBench()) {
      return [];
    }

    const hintedIds = this.board().actionHints.promoteTargetPokemonInPlayIds;
    return hintedIds.length > 0
      ? hintedIds.filter((pokemonInPlayId) => this.isLocalBenchPokemon(pokemonInPlayId))
      : this.localPlayer().benchSlots
          .map((slot) => slot.pokemon)
          .filter((pokemon): pokemon is NonNullable<typeof pokemon> => Boolean(pokemon))
          .map((pokemon) => pokemon.id);
  }

  private isLocalBenchPokemon(pokemonInPlayId: string): boolean {
    return this.localPlayer().benchSlots.some((slot) => slot.pokemon?.id === pokemonInPlayId);
  }

  private canDragEnergy(card: BoardCardViewModel): boolean {
    const interaction = this.interaction();
    const attachEnergyAction = this.board().actions.find((action) => action.actionType === GameActionType.AttachEnergy);

    if (
      !attachEnergyAction?.enabled ||
      this.actionPending() ||
      this.mandatoryDrawPending() ||
      interaction?.awaitingSnapshot
    ) {
      return false;
    }

    if (interaction?.pendingAction && interaction.pendingAction !== GameActionType.AttachEnergy) {
      return false;
    }

    return card.visibility === 'visible' && (
      cardCanBeSelectedForAction(card, GameActionType.AttachEnergy) ||
      cardSupportsAction(card, GameActionType.AttachEnergy)
    );
  }

  private canDragEvolution(card: BoardCardViewModel): boolean {
    const interaction = this.interaction();
    const evolveAction = this.board().actions.find((action) => action.actionType === GameActionType.EvolvePokemon);

    if (
      !evolveAction?.enabled ||
      this.actionPending() ||
      this.mandatoryDrawPending() ||
      interaction?.awaitingSnapshot ||
      this.setupMode()
    ) {
      return false;
    }

    if (interaction?.pendingAction && interaction.pendingAction !== GameActionType.EvolvePokemon) {
      return false;
    }

    if (!cardSupportsAction(card, GameActionType.EvolvePokemon)) {
      return false;
    }

    if (card.visibility !== 'visible') {
      return false;
    }

    const validTargetIds = card.validTargetPokemonInPlayIds ?? [];
    if (validTargetIds.length === 0) {
      return false;
    }

    const localPokemonIds = [
      this.localPlayer().activePokemon.pokemon?.id ?? null,
      ...this.localPlayer().benchSlots.map((slot) => slot.pokemon?.id ?? null)
    ].filter((pokemonId): pokemonId is string => typeof pokemonId === 'string');

    return validTargetIds.some((pokemonId) => localPokemonIds.includes(pokemonId));
  }

  private canDragTrainer(card: BoardCardViewModel): boolean {
    const interaction = this.interaction();

    if (
      !this.canPlayTrainerAction() ||
      this.setupMode() ||
      (interaction?.pendingAction && interaction.pendingAction !== GameActionType.PlayTrainer)
    ) {
      return false;
    }

    return card.visibility === 'visible' && (
      cardCanBeSelectedForAction(card, GameActionType.PlayTrainer) ||
      cardSupportsAction(card, GameActionType.PlayTrainer)
    );
  }

  private canDropTrainerOnActive(card: BoardCardViewModel, activePokemonInPlayId: string): boolean {
    return this.canDropTrainerOnPokemon(card, activePokemonInPlayId);
  }

  private canDropTrainerOnPokemon(card: BoardCardViewModel, pokemonInPlayId: string): boolean {
    return (
      this.isPokemonToolTrainer(card) &&
      this.canDragTrainer(card) &&
      this.trainerTargetPokemonIds(card).includes(pokemonInPlayId)
    );
  }

  private trainerTargetPokemonIds(card: BoardCardViewModel): string[] {
    const ownPokemonIds = [
      this.localPlayer().activePokemon.pokemon?.id ?? null,
      ...this.localPlayer().benchSlots.map((slot) => slot.pokemon?.id ?? null)
    ].filter((pokemonId): pokemonId is string => typeof pokemonId === 'string');
    const activePokemon = this.localPlayer().activePokemon.pokemon;
    const cardTargetIds = card.validTargetPokemonInPlayIds ?? [];
    const hintedTargetIds = this.isPokemonToolTrainer(card)
      ? this.board().actionHints.trainerToolTargetPokemonInPlayIds
      : this.board().actionHints.trainerTargetPokemonInPlayIds;
    const fallbackTargetIds = [
      activePokemon?.canReceiveTrainer ? activePokemon.id : null,
      ...this.localPlayer().benchSlots.map((slot) => slot.pokemon?.canReceiveTrainer ? slot.pokemon.id : null)
    ].filter((pokemonId): pokemonId is string => typeof pokemonId === 'string');
    const targetIds =
      cardTargetIds.length > 0 ? cardTargetIds :
      hintedTargetIds.length > 0 ? hintedTargetIds :
      fallbackTargetIds;

    return targetIds.filter((pokemonId) => ownPokemonIds.includes(pokemonId));
  }

  private playTrainerOnTarget(card: BoardCardViewModel, targetPokemonInPlayId: string): void {
    this.pendingTrainerCard.set(null);
    this.trainerPlayed.emit({
      cardInstanceId: this.trainerPayloadCardInstanceId(card),
      targetPokemonInPlayId
    });
  }

  private trainerPayloadCardInstanceId(card: BoardCardViewModel): string | undefined {
    return card.cardInstanceId ?? cardReferenceId(card);
  }

  private trainerDragModeForCard(card: BoardCardViewModel): TrainerDragMode | null {
    if (!this.canDragTrainer(card)) {
      return null;
    }

    if (this.isPokemonToolTrainer(card)) {
      return 'POKEMON_TOOL_TARGET';
    }

    if (this.isStadiumTrainer(card)) {
      return 'STADIUM_AREA';
    }

    if (this.isItemTrainer(card) || this.isSupporterTrainer(card)) {
      return 'TRAINER_PLAY_AREA';
    }

    return null;
  }

  private isPokemonToolTrainer(card: BoardCardViewModel): boolean {
    const category = card.category?.toUpperCase() ?? '';
    const subtype = card.subtype?.toUpperCase().replace(/[\s-]+/g, '_') ?? '';
    return (
      category === POKEMON_TOOL_TRAINER_CATEGORY ||
      (this.isTrainerCategory(card) && (subtype === POKEMON_TOOL_SUBTYPE || subtype.includes('TOOL')))
    );
  }

  private isTrainerCategory(card: BoardCardViewModel): boolean {
    return Boolean(card.category && Object.prototype.hasOwnProperty.call(TRAINER_CATEGORY_MAP_BOARD, card.category));
  }

  private isItemTrainer(card: BoardCardViewModel): boolean {
    const category = card.category?.toUpperCase() ?? '';
    return category === ITEM_TRAINER_CATEGORY || category === ACE_SPEC_TRAINER_CATEGORY;
  }

  private isSupporterTrainer(card: BoardCardViewModel): boolean {
    return card.category?.toUpperCase() === SUPPORTER_TRAINER_CATEGORY;
  }

  private isStadiumTrainer(card: BoardCardViewModel): boolean {
    return card.category?.toUpperCase() === STADIUM_TRAINER_CATEGORY;
  }

  private canDropEnergyOnSlot(slot: BoardPlayerViewModel['activePokemon'], draggedCard = this.draggedEnergyCard()): boolean {
    const pokemonId = slot.pokemon?.id;
    return Boolean(
      draggedCard &&
      slot.occupied &&
      pokemonId &&
      this.energyTargetPokemonIds(draggedCard).includes(pokemonId)
    );
  }

  private draggedEnergyCard(): BoardCardViewModel | null {
    const draggedEnergyCardId = this.draggedEnergyCardId();
    if (!draggedEnergyCardId) {
      return null;
    }

    return this.localPlayer().handCards.find((card) => cardReferenceId(card) === draggedEnergyCardId || card.id === draggedEnergyCardId) ?? null;
  }

  private energyTargetPokemonIds(card: BoardCardViewModel): string[] {
    const ownPokemonIds = [
      this.localPlayer().activePokemon.pokemon?.id ?? null,
      ...this.localPlayer().benchSlots.map((slot) => slot.pokemon?.id ?? null)
    ].filter((pokemonId): pokemonId is string => typeof pokemonId === 'string');
    const activePokemon = this.localPlayer().activePokemon.pokemon;
    const cardTargetIds = card.validTargetPokemonInPlayIds ?? [];
    const hintedTargetIds = this.board().actionHints.attachEnergyTargetPokemonInPlayIds;
    const fallbackTargetIds = [
      activePokemon?.canReceiveEnergy ? activePokemon.id : null,
      ...this.localPlayer().benchSlots.map((slot) => slot.pokemon?.canReceiveEnergy ? slot.pokemon.id : null)
    ].filter((pokemonId): pokemonId is string => typeof pokemonId === 'string');
    const targetIds =
      cardTargetIds.length > 0 ? cardTargetIds :
      hintedTargetIds.length > 0 ? hintedTargetIds :
      fallbackTargetIds;

    return targetIds.filter((pokemonId) => ownPokemonIds.includes(pokemonId));
  }

  private canDropCardAsBasicPokemon(card: BoardCardViewModel): boolean {
    const interaction = this.interaction();
    const playBasicAction = this.board().actions.find((action) => action.actionType === GameActionType.PlayBasicPokemon);

    if (
      !playBasicAction?.enabled ||
      this.mandatoryDrawPending() ||
      this.actionPending() ||
      interaction?.awaitingSnapshot
    ) {
      return false;
    }

    if (interaction?.pendingAction && interaction.pendingAction !== GameActionType.PlayBasicPokemon) {
      return false;
    }

    if (interaction?.pendingAction === GameActionType.PlayBasicPokemon) {
      return interaction.selectableCardInstanceIds.includes(cardReferenceId(card));
    }

    return cardCanBeSelectedForAction(card, GameActionType.PlayBasicPokemon)
      || cardSupportsAction(card, GameActionType.PlayBasicPokemon);
  }

  private projectLocalSetupState(player: BoardPlayerViewModel): BoardPlayerViewModel {
    const interaction = this.interaction();
    const activeCardId = interaction?.setupActiveCardInstanceId;
    const benchCardIds = interaction?.setupBenchCardInstanceIds ?? [];
    if (this.board().status !== GameStatus.Setup || (!activeCardId && benchCardIds.length === 0)) {
      return player;
    }

    const setupCardIds = new Set([activeCardId, ...benchCardIds].filter((cardId): cardId is string => typeof cardId === 'string'));
    const setupCards = new Map<string, BoardCardViewModel>();
    for (const card of player.handCards) {
      if (setupCardIds.has(cardReferenceId(card)) || setupCardIds.has(card.id)) {
        setupCards.set(cardReferenceId(card), card);
        setupCards.set(card.id, card);
      }
    }
    const handCards = player.handCards.filter((card) => !setupCardIds.has(cardReferenceId(card)) && !setupCardIds.has(card.id));
    const activeCard = activeCardId ? setupCards.get(activeCardId) ?? null : null;
    const activePokemon = activeCard
      ? this.createSetupPokemonSlot(player.activePokemon, activeCard, `${activeCardId}-setup-active`)
      : player.activePokemon;
    const benchCards = benchCardIds
      .map((cardId) => setupCards.get(cardId) ?? null)
      .filter((card): card is BoardCardViewModel => card !== null);
    const benchSlots = this.projectSetupBenchSlots(player.benchSlots, benchCards);

    return {
      ...player,
      handCards,
      handZone: player.handZone ? { ...player.handZone, count: handCards.length, cards: handCards } : player.handZone,
      activePokemon,
      benchSlots
    };
  }

  private projectSetupBenchSlots(
    slots: BoardPlayerViewModel['benchSlots'],
    benchCards: BoardCardViewModel[]
  ): BoardPlayerViewModel['benchSlots'] {
    let nextBenchCardIndex = 0;
    return slots.map((slot) => {
      if (slot.occupied) {
        return slot;
      }

      const setupCard = benchCards[nextBenchCardIndex];
      nextBenchCardIndex += 1;
      return setupCard
        ? this.createSetupPokemonSlot(slot, setupCard, setupBenchPokemonId(cardReferenceId(setupCard)))
        : slot;
    });
  }

  private createSetupPokemonSlot(
    slot: BoardPlayerViewModel['activePokemon'],
    card: BoardCardViewModel,
    pokemonId: string
  ): BoardPlayerViewModel['activePokemon'] {
    return {
      ...slot,
      occupied: true,
      card,
      pokemon: {
        id: pokemonId,
        activeCard: card,
        evolutionStack: [card],
        attachedEnergyCards: [],
        attachedTrainerCards: [],
        damageCounters: null,
        specialConditions: [],
        attacks: [],
        abilities: [],
        actions: [],
        canReceiveEnergy: false,
        canReceiveTrainer: false,
        canRetreatTo: false,
        canPromote: false,
        visualEffects: []
      }
    };
  }

  private projectLocalEnergyAttachment(player: BoardPlayerViewModel): BoardPlayerViewModel {
    const optimisticEnergyAttachment = this.interaction()?.optimisticEnergyAttachment;
    if (!optimisticEnergyAttachment) {
      return player;
    }

    const energyCard = player.handCards.find((card) =>
      cardReferenceId(card) === optimisticEnergyAttachment.energyCardInstanceId ||
      card.id === optimisticEnergyAttachment.energyCardInstanceId
    );
    if (!energyCard) {
      return player;
    }

    const handCards = player.handCards.filter((card) =>
      cardReferenceId(card) !== optimisticEnergyAttachment.energyCardInstanceId &&
      card.id !== optimisticEnergyAttachment.energyCardInstanceId
    );

    return {
      ...player,
      handCards,
      handZone: player.handZone ? { ...player.handZone, count: handCards.length, cards: handCards } : player.handZone,
      activePokemon: this.attachEnergyToSlot(player.activePokemon, optimisticEnergyAttachment.targetPokemonInPlayId, energyCard),
      benchSlots: player.benchSlots.map((slot) =>
        this.attachEnergyToSlot(slot, optimisticEnergyAttachment.targetPokemonInPlayId, energyCard)
      )
    };
  }

  private attachEnergyToSlot(
    slot: BoardPlayerViewModel['activePokemon'],
    targetPokemonInPlayId: string,
    energyCard: BoardCardViewModel
  ): BoardPlayerViewModel['activePokemon'] {
    if (slot.pokemon?.id !== targetPokemonInPlayId) {
      return slot;
    }

    const pokemon = {
      ...slot.pokemon,
      attachedEnergyCards: [...slot.pokemon.attachedEnergyCards, energyCard]
    };

    return {
      ...slot,
      pokemon
    };
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.handleWindowResize);
    this.statusPanelResizeObserver?.disconnect();
    if (this.statusPanelPositionFrameId !== null) {
      window.cancelAnimationFrame(this.statusPanelPositionFrameId);
    }
    window.clearInterval(this.intervalId);
    this.mobileViewportMediaQuery.removeEventListener('change', this.mobileViewportListener);
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function setupBenchPokemonId(cardInstanceId: string): string {
  return `${cardInstanceId}-setup-bench`;
}

function setupBenchCardIdFromPokemonId(pokemonInPlayId: string): string | null {
  const suffix = '-setup-bench';
  return pokemonInPlayId.endsWith(suffix) ? pokemonInPlayId.slice(0, -suffix.length) : null;
}
