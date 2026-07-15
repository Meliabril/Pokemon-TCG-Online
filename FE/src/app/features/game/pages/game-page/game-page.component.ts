import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostListener, OnDestroy, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameStatus } from '../../../../core/models/enums/game/game-status.enum';
import { GameEventType } from '../../../../core/models/enums/game/game-event-type.enum';
import {
  readMulliganExtraCardsGrantedPayload,
  readMulliganHandRevealedPayload
} from '../../../../core/models/interfaces/game/mulligan-event.interface';
import { CardSummary } from '../../../../core/models/interfaces/card/card-summary.interface';
import { ResolveAttackChoicePayload, UseAbilityPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { GameEvent } from '../../../../core/models/interfaces/game/game-event.interface';
import { LanguageService } from '../../../../core/services/language.service';
import { CardsApiService } from '../../../../infrastructure/api/card/cards-api.service';
import { BoardAnimationCommand } from '../../domain/animations/board-animation.types';
import { BoardCardViewModel, BoardGameViewModel } from '../../domain/board/board-game-view-model.interface';
import { EMPTY_GAME_COMMAND_STATE, GameCommandState } from '../../domain/actions/game-command-state.interface';
import { buildBlockingOverlay, buildGameInteractionViewModel, cardSupportsAction, isBasicStagePokemon } from '../../domain/interaction/game-interaction.helpers';
import { mapEventToBoardAnimations } from '../../domain/animations/opponent-animation.mapper';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { GameShellComponent } from '../../components/game-shell/game-shell.component';
import { GameToastComponent } from '../../components/game-toast/game-toast.component';
import { MulliganBannerComponent } from '../../components/mulligan-banner/mulligan-banner.component';
import { BoardAnimationService } from '../../services/board-animation.service';
import { MulliganVisualService } from '../../services/mulligan-visual.service';
import { GameToastService } from '../../services/game-toast.service';
import { GameActionExecutionResult, GameFacadeService } from '../../services/game-facade.service';
import { CardHoverVisualTarget } from '../../domain/cards/card-hover-visual-target.interface';
import { BoardSlotViewModel } from '../../domain/board/board-game-view-model.interface';

const SNAPSHOT_WAIT_FALLBACK_MS = 4_000;
const OPENING_DEAL_FRESH_WINDOW_MS = 45_000;
const OPENING_DEAL_RENDER_DELAY_MS = 150;
const HAND_GATE_FAILSAFE_MS = 60_000;
const ATTACK_LUNGE_POST_COIN_DELAY_MS = 400;
const ATTACK_LUNGE_POST_RETURN_SELF_DAMAGE_DELAY_MS = 140;
const RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE_EFFECT = 'RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE';
const GREAT_BALL_EXTERNAL_ID = 'xy1-118';

interface OptimisticEnergyAttachment {
  energyCardInstanceId: string;
  targetPokemonInPlayId: string;
}

@Component({
  selector: 'app-game-page',
  templateUrl: './game-page.component.html',
  styleUrl: './game-page.component.css',
  imports: [GameShellComponent, GameToastComponent, MulliganBannerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GamePageComponent implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly boardAnimationService = inject(BoardAnimationService);
  private readonly mulliganVisualService = inject(MulliganVisualService);
  private readonly cardsApiService = inject(CardsApiService);
  private readonly gameToastService = inject(GameToastService);
  private readonly languageService = inject(LanguageService);
  private readonly previousBodyOverflow = this.document.body.style.overflow;
  private readonly commandStateSignal = signal<GameCommandState>(EMPTY_GAME_COMMAND_STATE);
  private readonly optimisticEnergyAttachmentSignal = signal<OptimisticEnergyAttachment | null>(null);
  private readonly seenAnimatedEventIds = new Set<string>();
  private eventAnimationBaselineReady = false;
  private openingDealEvaluated = false;
  private readonly hiddenHandPlayerIdsSignal = signal<ReadonlySet<string>>(new Set());
  private previousAnimatingPlayerIds: ReadonlySet<string> = new Set();
  private readonly handGateFailsafeByPlayer = new Map<string, number>();
  private snapshotFallbackTimeoutId: number | null = null;
  private previousAnimatedBoard: BoardGameViewModel | null = null;
  private pendingAttackLunge: BoardAnimationCommand[] | null = null;
  private pendingEvosodaTargetId: string | null = null;
  private readonly animatedGreatBallDiscardIds = new Set<string>();
  private readonly seenRevealShuffleAnimationKeys = new Set<string>();
  private readonly revealedCardCache = new Map<string, CardSummary>();

  readonly gameFacade = inject(GameFacadeService);
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly isAwaitingSnapshot = this.gameFacade.isAwaitingSnapshot;
  /** When true, the local hand is hidden (empty) while a deal/Mulligan animation for it plays. */
  readonly localHandHidden = computed(() => {
    const board = this.gameFacade.board();
    return board ? this.hiddenHandPlayerIdsSignal().has(board.localPlayer.id) : false;
  });
  /** When true, the rival hand zone is hidden while a deal/Mulligan animation for it plays. */
  readonly opponentHandHidden = computed(() => {
    const board = this.gameFacade.board();
    return board ? this.hiddenHandPlayerIdsSignal().has(board.rivalPlayer.id) : false;
  });
  readonly finalStatusLabel = computed(() => {
    switch (this.gameFacade.finalOutcome()) {
      case 'victory':
        return this.t('GAME.STATUS.VICTORY');
      case 'defeat':
        return this.t('GAME.STATUS.DEFEAT');
      default:
        return this.t('GAME.FINISHED');
    }
  });
  readonly finalBannerClasses = computed(() =>
    this.gameFacade.finalOutcome() === 'defeat'
      ? 'fixed left-1/2 top-3 z-50 -translate-x-1/2 rounded-full border border-[#dc2626]/45 bg-[#450a0a]/90 px-4 py-2 text-xs text-[#fecaca]'
      : 'fixed left-1/2 top-3 z-50 -translate-x-1/2 rounded-full border border-[#16a34a]/35 bg-[#052e16]/90 px-4 py-2 text-xs text-[#bbf7d0]'
  );
  readonly interaction = computed(() => {
    const board = this.gameFacade.board();
    this.languageService.translations();
    if (!board) {
      return null;
    }

      return buildGameInteractionViewModel({
        board,
        screenState: this.gameFacade.screenState(),
        commandState: {
          ...this.commandStateSignal(),
          optimisticEnergyAttachment: this.optimisticEnergyAttachmentSignal()
        },
        actionPending: this.gameFacade.isActionPending(),
        awaitingSnapshot: this.isAwaitingSnapshot(),
        t: this.languageService.t.bind(this.languageService)
      });
  });
  readonly blockingOverlay = computed(() => {
    const board = this.gameFacade.board();
    this.languageService.translations();
    if (!board) {
      return null;
    }

    return buildBlockingOverlay(
      board,
      this.gameFacade.screenState(),
      this.isAwaitingSnapshot(),
      this.gameFacade.isActionPending(),
      this.languageService.t.bind(this.languageService),
      this.gameFacade.finalOutcome()
    );
  });

  constructor() {
    this.document.body.style.overflow = 'hidden';
    const gameId = this.route.snapshot.paramMap.get('gameId');

    if (!gameId) {
      this.gameFacade.disconnect();
      void this.router.navigateByUrl(`/${APP_ROUTES.home}`);
      return;
    }

    void this.gameFacade.enterGame(gameId);

    let wasAwaiting = false;
    effect(() => {
      const isAwaiting = this.gameFacade.isAwaitingSnapshot();

      if (wasAwaiting && !isAwaiting) {
        this.optimisticEnergyAttachmentSignal.set(null);
        untracked(() => {
          const currentState = this.commandStateSignal();
          const screenState = this.gameFacade.screenState();
          const hasConfirmedSetupSelection =
            screenState === 'setup-waiting-opponent' &&
            (currentState.setupActiveCardInstanceId !== null || currentState.setupBenchCardInstanceIds.length > 0);

          if (hasConfirmedSetupSelection) {
            this.commandStateSignal.update((state) => ({
              ...state,
              pendingAction: null,
              selectedCardInstanceId: null,
              selectedPokemonInPlayId: null,
              selectedAttackId: null
            }));
            return;
          }

          this.resetCommandState();
        });
      }

      wasAwaiting = isAwaiting;
    });

    effect(() => {
      const screenState = this.gameFacade.screenState();
      if (
        screenState !== 'setup-selecting' &&
        screenState !== 'setup-waiting-opponent' &&
        this.commandStateSignal().pendingAction === GameActionType.ChooseInitialPokemon
      ) {
        this.resetCommandState();
      }
    });

    effect(() => {
      const isAwaiting = this.gameFacade.isAwaitingSnapshot();

      this.clearSnapshotFallbackTimeout();
      if (isAwaiting) {
        this.snapshotFallbackTimeoutId = window.setTimeout(() => {
          void this.handleSnapshotWaitTimeout();
        }, SNAPSHOT_WAIT_FALLBACK_MS);
      }
    });

    effect(() => {
      const board = this.gameFacade.board();
      const screenState = this.gameFacade.screenState();
      const currentState = this.commandStateSignal();

      if (!board) {
        if (currentState !== EMPTY_GAME_COMMAND_STATE) {
          this.resetCommandState();
        }
        return;
      }

      if (
        screenState === 'setup-selecting'
        && currentState.setupActiveCardInstanceId === null
        && currentState.setupBenchCardInstanceIds.length === 0
      ) {
        const participantId = this.gameFacade.currentParticipantId();
        const persistedSetup = participantId ? this.gameFacade.snapshot()?.players[participantId] : null;
        if (
          persistedSetup?.initialActiveCardInstanceId
          || persistedSetup?.initialBenchCardInstanceIds.length
        ) {
          const restoredState = sanitizeCommandState(board, screenState, {
            ...currentState,
            setupActiveCardInstanceId: persistedSetup.initialActiveCardInstanceId,
            setupBenchCardInstanceIds: persistedSetup.initialBenchCardInstanceIds
          });
          this.commandStateSignal.set(restoredState);
          this.gameFacade.hydrateLocalSetupPreview(readLocalSetupPreviewFromState(restoredState));
          return;
        }
      }

      const nextState = sanitizeCommandState(board, screenState, currentState);
      if (!gameCommandStatesEqual(currentState, nextState)) {
        this.commandStateSignal.set(nextState);
      }
    });

    effect(() => {
      const board = this.gameFacade.board();
      const events = this.gameFacade.eventFeed();
      const previousBoard = this.previousAnimatedBoard;
      if (!board) {
        this.previousAnimatedBoard = null;
        return;
      }

      // Opening shuffle/deal is anchored to its event but guarded so it animates once when the
      // game actually starts, never on refresh/re-entry (the baseline would otherwise swallow it).
      this.maybeAnimateOpeningHandsDeal(events, board);

      if (!this.eventAnimationBaselineReady) {
        const mulliganShuffleAnchors = new Set<string>();
        const baselineMulliganEventIds: string[] = [];
        for (const event of events) {
          this.seenAnimatedEventIds.add(event.eventId);
          // Mulligan events already present at baseline (refresh/reconnect) won't animate, so release
          // them to the panel immediately; live events are released later when their animation starts.
          if (
            event.eventType === GameEventType.MulliganHandRevealed ||
            event.eventType === GameEventType.MulliganSequenceCompleted
          ) {
            baselineMulliganEventIds.push(event.eventId);
          }
          if (event.eventType === GameEventType.MulliganHandRevealed) {
            const commands = mapEventToBoardAnimations(event, board, this.languageService.t.bind(this.languageService));
            for (const command of commands) {
              if (command.fromAnchor) {
                mulliganShuffleAnchors.add(command.fromAnchor);
              }
            }
          }
        }
        this.eventAnimationBaselineReady = true;
        this.previousAnimatedBoard = board;
        if (baselineMulliganEventIds.length > 0) {
          this.mulliganVisualService.releaseEvents(baselineMulliganEventIds);
        }

        if (mulliganShuffleAnchors.size > 0) {
          const commands: BoardAnimationCommand[] = Array.from(mulliganShuffleAnchors).flatMap((fromAnchor, index) => [
            ...(index > 0 ? [{ type: 'WAIT' as const, durationMs: 120 }] : []),
            { type: 'SHUFFLE_DECK' as const, fromAnchor }
          ]);
          queueMicrotask(() => this.boardAnimationService.enqueue(commands));
        }

        return;
      }

      // Mulligan events carry a backend-decided `sequenceIndex` (the interleaved resolution order). Sort
      // by it first so BOTH clients animate the same order regardless of WS/HTTP delivery; fall back to
      // `occurredAt` (stable) for events without a sequence and to preserve within-attempt step order.
      const newEvents = events
        .filter((event) => !this.seenAnimatedEventIds.has(event.eventId))
        .sort((left, right) => {
          const leftSeq = mulliganSequenceIndex(left);
          const rightSeq = mulliganSequenceIndex(right);
          if (leftSeq !== null && rightSeq !== null && leftSeq !== rightSeq) {
            return leftSeq - rightSeq;
          }
          return left.occurredAt.localeCompare(right.occurredAt);
        });

      void this.enqueueNewEventAnimations(newEvents, board, previousBoard);
      this.previousAnimatedBoard = board;
    });

    // Reveal each player's hand when THEIR OWN animations drain (per-player leave edge): a player's
    // commands for one Mulligan attempt are contiguous (reveal → return → shuffle → draw, same
    // sequenceIndex) and tagged with their affectedPlayerId, so when that run ends and the other player's
    // run takes over, this player leaves `animatingPlayerIds` and its just-drawn hand reappears — without
    // waiting for the whole interleaved flow to finish. Extra cards re-hide the receiver briefly at the end.
    effect(() => {
      const animating = this.boardAnimationService.animatingPlayerIds();
      for (const playerId of this.previousAnimatingPlayerIds) {
        if (!animating.has(playerId)) {
          this.revealHand(playerId);
        }
      }
      this.previousAnimatingPlayerIds = animating;
    });

    effect(() => {
      const screenState = this.gameFacade.screenState();

      if (screenState !== 'setup-selecting' && screenState !== 'setup-waiting-opponent') {
        this.gameFacade.clearLocalSetupPreview();
      }
    });
  }

  private async enqueueNewEventAnimations(
    newEvents: GameEvent[],
    board: BoardGameViewModel,
    previousBoard: BoardGameViewModel | null
  ): Promise<void> {
    const combinedCommands: BoardAnimationCommand[] = [];
    const privateRevealShuffleKeys = new Set(
      newEvents
        .filter((event) => event.privateEvent && this.isRevealShuffleEvent(event))
        .map((event) => this.revealShuffleDedupeKey(event))
        .filter((key): key is string => key !== null)
    );

    for (const event of newEvents) {
      this.seenAnimatedEventIds.add(event.eventId);
      if (this.shouldSkipDuplicateGreatBallDiscard(event)) {
        continue;
      }
      if (this.shouldSkipRevealShuffleAnimation(event, privateRevealShuffleKeys)) {
        continue;
      }

      if (
        event.eventType === GameEventType.AttackEffectResolved &&
        event.payload?.['effectType'] === 'ATTACK_LOCKED'
      ) {
        this.gameToastService.show(this.t('GAME.TOAST.ATTACK_LOCKED'), 'warning');
      }

      if (
        event.eventType === GameEventType.AttackEffectResolved &&
        event.payload?.['effectType'] === 'PICKUP_NO_VALID_CARDS'
      ) {
        this.gameToastService.show(this.t('GAME.TOAST.PICKUP_NO_VALID_CARDS'), 'warning');
      }

      const isPendingEvosodaEvolution =
        event.eventType === GameEventType.PokemonEvolved &&
        readStringPayload(event, 'pokemonInPlayId') === this.pendingEvosodaTargetId;
      const animationEvent: GameEvent = isPendingEvosodaEvolution
        ? {
            ...event,
            payload: { ...event.payload, effectType: 'SEARCH_EVOLUTION_FROM_DECK' }
          }
        : event;
      if (isPendingEvosodaEvolution) {
        this.pendingEvosodaTargetId = null;
      }

      const baseCommands = mapEventToBoardAnimations(
        animationEvent,
        board,
        this.languageService.t.bind(this.languageService),
        previousBoard
      );
      const commands = this.needsRevealedCardVisual(event)
        ? await this.withRevealedCardVisual(baseCommands, event)
        : baseCommands;

      if (event.eventType === GameEventType.AttackDeclared) {
        this.flushPendingAttackLunge();
        if (commands.length > 0) {
          this.pendingAttackLunge = commands;
          this.holdDamageForCommands(commands);
        }
        continue;
      }

      if (event.eventType === GameEventType.AttackChoiceRequired && this.pendingAttackLunge) {
        continue;
      }

      if (event.eventType === GameEventType.AttackChoiceResolved && this.pendingAttackLunge) {
        const lunge = this.pendingAttackLunge;
        this.pendingAttackLunge = null;
        combinedCommands.push(...lunge);
        continue;
      }

      if (this.pendingAttackLunge && commands.some((command) => command.type === 'SELF_DAMAGE')) {
        const lunge = this.pendingAttackLunge;
        this.pendingAttackLunge = null;
        combinedCommands.push(
          ...lunge,
          { type: 'WAIT', durationMs: ATTACK_LUNGE_POST_RETURN_SELF_DAMAGE_DELAY_MS },
          ...commands
        );
        continue;
      }

      if (event.eventType === GameEventType.AttackEffectResolved && this.pendingAttackLunge) {
        const lunge = this.pendingAttackLunge;
        this.pendingAttackLunge = null;
        const coinFlipCommands = commands.filter((command) => command.type === 'COIN_FLIP');
        const restCommands = commands.filter((command) => command.type !== 'COIN_FLIP');
        const postCoinDelay: BoardAnimationCommand[] =
          coinFlipCommands.length > 0 ? [{ type: 'WAIT', durationMs: ATTACK_LUNGE_POST_COIN_DELAY_MS }] : [];
        combinedCommands.push(...coinFlipCommands, ...postCoinDelay, ...lunge, ...restCommands);
        continue;
      }

      this.flushPendingAttackLunge();
      if (commands.length > 0) {
        // Mulligan uses its own deterministic, per-player gate (MulliganVisualService): each player is
        // hidden up-front and released when their VALIDATED-with-Basic animation completes. Drive that
        // staging here; the rest of the pipeline (attacks/draws) is unchanged.
        if (this.isMulliganAnimationEvent(event.eventType)) {
          this.applyMulliganStaging(event);
        }
        this.holdDamageForCommands(commands);
        combinedCommands.push(...commands);
      }
    }

    if (combinedCommands.length > 0) {
      queueMicrotask(() => this.boardAnimationService.enqueue(combinedCommands));
    }
    if (!board.resolution?.pendingChoiceType) {
      this.flushPendingAttackLunge();
    }
  }

  private async withRevealedCardVisual(
    commands: BoardAnimationCommand[],
    event: GameEvent
  ): Promise<BoardAnimationCommand[]> {
    const revealedCardId =
      readStringPayload(event, 'revealedCardId') ??
      readStringPayload(event, 'takenCardId') ??
      readStringArrayPayload(event, 'cardIds')[0] ??
      null;
    if (!revealedCardId) {
      return commands;
    }

    const revealedCard = await this.loadRevealedCard(revealedCardId);
    if (!revealedCard) {
      return commands;
    }

    return commands.map((command) =>
      command.type === 'OPPONENT_HAND_REVEAL_SHUFFLE' ||
      command.type === 'OPPONENT_REVEAL_CARD' ||
      command.type === 'PLAYER_DRAW_CARD'
        ? {
            ...command,
            cardImageUrl: revealedCard.imageSmallUrl ?? revealedCard.imageLargeUrl ?? undefined,
            cardLabel: revealedCard.displayName ?? revealedCard.name
          }
        : command
    );
  }

  private async loadRevealedCard(cardId: string): Promise<CardSummary | null> {
    const cachedCard = this.revealedCardCache.get(cardId);
    if (cachedCard) {
      return cachedCard;
    }

    try {
      const card = await firstValueFrom(this.cardsApiService.getCardById(cardId));
      this.revealedCardCache.set(cardId, card);
      return card;
    } catch {
      return null;
    }
  }

  private isRevealShuffleEvent(event: GameEvent): boolean {
    return (
      event.eventType === GameEventType.AttackEffectResolved &&
      event.payload?.['effectType'] === RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE_EFFECT
    );
  }

  private shouldSkipRevealShuffleAnimation(
    event: GameEvent,
    privateRevealShuffleKeys: Set<string>
  ): boolean {
    if (!this.isRevealShuffleEvent(event)) {
      return false;
    }

    const revealShuffleKey = this.revealShuffleDedupeKey(event);
    if (!revealShuffleKey) {
      return false;
    }

    if (!event.privateEvent && privateRevealShuffleKeys.has(revealShuffleKey)) {
      return true;
    }

    if (this.seenRevealShuffleAnimationKeys.has(revealShuffleKey)) {
      return true;
    }

    this.seenRevealShuffleAnimationKeys.add(revealShuffleKey);
    return false;
  }

  private revealShuffleDedupeKey(event: GameEvent): string | null {
    const opponentPlayerId = readStringPayload(event, 'opponentPlayerId');
    const revealedCardId = readStringPayload(event, 'revealedCardId');
    return opponentPlayerId ? `${event.stateVersion}:${opponentPlayerId}:${revealedCardId ?? 'unknown'}` : null;
  }

  private flushPendingAttackLunge(): void {
    if (!this.pendingAttackLunge) {
      return;
    }

    const commands = this.pendingAttackLunge;
    this.pendingAttackLunge = null;
    if (commands.length > 0) {
      queueMicrotask(() => this.boardAnimationService.enqueue(commands));
    }
  }

  private holdDamageForCommands(commands: BoardAnimationCommand[]): void {
    for (const command of commands) {
      if (
        (command.type === 'ATTACK_LUNGE' || command.type === 'SELF_DAMAGE') &&
        command.targetPokemonInPlayId
      ) {
        this.boardAnimationService.holdDamage(command.targetPokemonInPlayId);
      }
    }
  }

  async leaveTable(): Promise<void> {
    this.gameFacade.disconnect();
    await this.router.navigateByUrl(`/${APP_ROUTES.home}`);
  }

  @HostListener('window:blur')
  handleWindowBlur(): void {
    this.gameFacade.clearLocalCardHover();
  }

  handleCardHoverChanged(target: CardHoverVisualTarget): void {
    this.gameFacade.publishCardHoverChanged(target);
  }

  sendChatMessage(content: string): void {
    try {
      this.gameFacade.sendChatMessage(content);
    } catch {
      this.gameToastService.show(this.t('GAME.ERRORS.NO_CHAT_GAME'), 'warning');
    }
  }

  async playBasicPokemon(cardId: string): Promise<void> {
    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    await this.submitAndAwaitSnapshot(board.stateVersion, this.gameFacade.playBasicPokemon(cardId));
  }

  selectAction(actionType: GameActionType): void {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    const matchingAction = board.actions.find((action) => action.actionType === actionType);
    if (!matchingAction?.enabled) {
      return;
    }

    this.commandStateSignal.set({
      ...EMPTY_GAME_COMMAND_STATE,
      pendingAction: actionType
    });
  }

  cancelAction(): void {
    if (this.isAwaitingSnapshot()) {
      return;
    }

    if (hasPendingSetupPreviewState(this.commandStateSignal(), this.gameFacade.screenState())) {
      this.gameFacade.clearLocalCardHover();
      this.gameFacade.clearLocalSetupPreview();
    }

    this.resetCommandState();
  }

  selectHandCard(cardInstanceId: string): void {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    const interaction = this.interaction();
    if (!board || !interaction) {
      return;
    }

    const selectedCard = findHandCard(board.localPlayer.handCards, cardInstanceId);
    if (!selectedCard) {
      return;
    }

    if (!interaction.selectableCardInstanceIds.includes(cardInstanceId)) {
      const autoAction = resolveAutoActionFromCard(board, selectedCard);
      if (autoAction) {
        this.commandStateSignal.set({
          ...EMPTY_GAME_COMMAND_STATE,
          pendingAction: autoAction,
          selectedCardInstanceId: cardInstanceId
        });
      }
      return;
    }

    this.commandStateSignal.update((currentState) => ({
      ...currentState,
      selectedCardInstanceId: currentState.selectedCardInstanceId === cardInstanceId ? null : cardInstanceId,
      selectedPokemonInPlayId: null,
      selectedAttackId: null,
      selectedAttackUseBonusDamage: false
    }));
  }

  selectPokemon(pokemonInPlayId: string): void {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const interaction = this.interaction();
    if (!interaction || !interaction.selectablePokemonInPlayIds.includes(pokemonInPlayId)) {
      return;
    }

    this.commandStateSignal.update((currentState) => ({
      ...currentState,
      selectedPokemonInPlayId:
        currentState.selectedPokemonInPlayId === pokemonInPlayId ? null : pokemonInPlayId
    }));
  }

  selectAttack(attackId: string): void {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    this.commandStateSignal.update((currentState) => ({
      ...currentState,
      pendingAction: GameActionType.DeclareAttack,
      selectedAttackId: currentState.selectedAttackId === attackId ? null : attackId,
      selectedAttackUseBonusDamage: false,
      selectedPokemonInPlayId: null
    }));
  }

  async declareAttackFromModal(event: {
    attackId: string;
    useBonusDamage?: boolean;
    selfTargetPokemonInPlayId?: string;
    switchTargetPokemonInPlayId?: string;
  }): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    this.commandStateSignal.update((currentState) => ({
      ...currentState,
      pendingAction: GameActionType.DeclareAttack,
      selectedAttackId: event.attackId,
      selectedAttackUseBonusDamage: event.useBonusDamage ?? false,
      selectedPokemonInPlayId: null
    }));

    await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.declareAttack(
        event.attackId,
        event.switchTargetPokemonInPlayId,
        event.useBonusDamage,
        event.selfTargetPokemonInPlayId
      )
    );
  }

  async playTrainerFromModal(event: {
    cardId?: string;
    cardInstanceId?: string;
    targetPokemonInPlayId?: string;
    targetCardInstanceId?: string;
    targetEnergyCardInstanceId?: string;
    selectedEvolutionExternalId?: string;
    selectedCardInstanceId?: string;
    selectedCardIds?: string[];
  }): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    if (event.selectedEvolutionExternalId && event.targetPokemonInPlayId) {
      this.pendingEvosodaTargetId = event.targetPokemonInPlayId;
    }

    const accepted = await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.playTrainer(
        event.cardId,
        undefined,
        event.targetPokemonInPlayId,
        event.targetCardInstanceId,
        event.targetEnergyCardInstanceId,
        event.cardInstanceId,
        event.selectedEvolutionExternalId,
        event.selectedCardInstanceId,
        event.selectedCardIds
      )
    );
    if (!accepted) {
      this.pendingEvosodaTargetId = null;
    }
  }

  private needsRevealedCardVisual(event: GameEvent): boolean {
    return (
      this.isRevealShuffleEvent(event) ||
      (
        event.eventType === GameEventType.CardDrawn &&
        event.payload?.['source'] === 'TRAINER' &&
        (
          typeof event.payload?.['takenCardId'] === 'string' ||
          readStringArrayPayload(event, 'cardIds').length > 0
        )
      )
    );
  }

  private shouldSkipDuplicateGreatBallDiscard(event: GameEvent): boolean {
    if (
      event.eventType !== GameEventType.TrainerPlayed ||
      readStringPayload(event, 'externalId') !== GREAT_BALL_EXTERNAL_ID
    ) {
      return false;
    }

    const trainerCardInstanceId = readStringPayload(event, 'cardInstanceId');
    if (!trainerCardInstanceId) {
      return false;
    }

    if (this.animatedGreatBallDiscardIds.has(trainerCardInstanceId)) {
      return true;
    }

    this.animatedGreatBallDiscardIds.add(trainerCardInstanceId);
    return false;
  }

  async useAbilityFromModal(payload: UseAbilityPayload): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    await this.submitAndAwaitSnapshot(board.stateVersion, this.gameFacade.useAbility(payload));
  }

  async drawCardFromDeck(): Promise<void> {
    const board = this.gameFacade.board();
    if (!board || this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const drawAction = board.actions.find((action) => action.actionType === GameActionType.DrawCard);
    if (!drawAction?.enabled || board.activePlayerId !== board.localPlayer.id) {
      return;
    }

    await this.submitAndAwaitSnapshot(board.stateVersion, this.gameFacade.drawCard());
  }

  async retreatActivePokemonFromBoard(targetPokemonInPlayId: string): Promise<void> {
    const board = this.gameFacade.board();
    if (!board || this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const retreatAction = board.actions.find((action) => action.actionType === GameActionType.Retreat);
    const isValidBenchTarget = board.localPlayer.benchSlots.some((slot) => slot.pokemon?.id === targetPokemonInPlayId);
    if (!retreatAction?.enabled || board.activePlayerId !== board.localPlayer.id || !isValidBenchTarget) {
      return;
    }

    await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.retreat(targetPokemonInPlayId)
    );
  }

  async promoteBenchPokemonFromBoard(pokemonInPlayId: string): Promise<void> {
    const board = this.gameFacade.board();
    if (!board || this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    // Mirrors game-board.component.ts's canPromoteFromBench: derive eligibility directly
    // from the authoritative resolution state rather than the actions[].enabled chain,
    // which can desync from the real pending-promotion state.
    const isValidBenchTarget = board.localPlayer.benchSlots.some((slot) => slot.pokemon?.id === pokemonInPlayId);
    const canPromote =
      board.status === GameStatus.Active &&
      board.resolution?.playerToPromoteId === board.localPlayer.id &&
      board.localPlayer.benchSlots.some((slot) => Boolean(slot.pokemon));
    if (!canPromote || !isValidBenchTarget) {
      return;
    }

    await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.promoteBenchPokemon(pokemonInPlayId)
    );
  }

  async resolveAttackChoiceFromBoard(payload: ResolveAttackChoicePayload): Promise<void> {
    const board = this.gameFacade.board();
    if (!board || this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const resolveAttackChoiceAction = board.actions.find(
      (action) => action.actionType === GameActionType.ResolveAttackChoice
    );
    if (
      !resolveAttackChoiceAction?.enabled ||
      board.resolution?.pendingChoicePlayerId !== board.localPlayer.id
    ) {
      return;
    }

    await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.resolveAttackChoice(payload)
    );
  }

  selectInitialActivePokemon(cardInstanceId: string): void {
    const interaction = this.interaction();
    const board = this.gameFacade.board();
    if (!board || !interaction?.selectableCardInstanceIds.includes(cardInstanceId)) {
      return;
    }

    this.updateSetupCommandState(board, (currentState) => ({
      ...currentState,
      setupActiveCardInstanceId: cardInstanceId,
      setupBenchCardInstanceIds: currentState.setupBenchCardInstanceIds.filter((cardId) => cardId !== cardInstanceId)
    }));
  }

  selectInitialBenchPokemon(cardInstanceId: string): void {
    const interaction = this.interaction();
    const board = this.gameFacade.board();
    if (!board || !interaction?.selectableCardInstanceIds.includes(cardInstanceId)) {
      return;
    }

    this.updateSetupCommandState(board, (currentState) => ({
      ...currentState,
      setupActiveCardInstanceId:
        currentState.setupActiveCardInstanceId === cardInstanceId ? null : currentState.setupActiveCardInstanceId,
      setupBenchCardInstanceIds: currentState.setupBenchCardInstanceIds.includes(cardInstanceId)
        ? currentState.setupBenchCardInstanceIds
        : [...currentState.setupBenchCardInstanceIds, cardInstanceId]
    }));
  }

  removeInitialBenchPokemon(cardInstanceId: string): void {
    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    this.updateSetupCommandState(board, (currentState) => ({
      ...currentState,
      setupBenchCardInstanceIds: currentState.setupBenchCardInstanceIds.filter((cardId) => cardId !== cardInstanceId)
    }));
  }

  async attachEnergyToPokemon(
    energyCardInstanceId: string,
    targetPokemonInPlayId: string
  ): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    const selectedCard = findHandCard(board.localPlayer.handCards, energyCardInstanceId);
    if (!selectedCard?.cardId) {
      return;
    }

    this.optimisticEnergyAttachmentSignal.set({ energyCardInstanceId, targetPokemonInPlayId });
    const accepted = await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.attachEnergy(selectedCard.cardId, energyCardInstanceId, targetPokemonInPlayId)
    );

    if (!accepted) {
      this.optimisticEnergyAttachmentSignal.set(null);
    }
  }

  async evolvePokemonFromBoard(
    evolutionCardInstanceId: string,
    targetPokemonInPlayId: string
  ): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    const selectedCard = findHandCard(board.localPlayer.handCards, evolutionCardInstanceId);
    if (!selectedCard?.cardId) {
      return;
    }

    const targetIds = selectedCard.validTargetPokemonInPlayIds ?? [];
    if (!targetIds.includes(targetPokemonInPlayId)) {
      return;
    }

    await this.submitAndAwaitSnapshot(
      board.stateVersion,
      this.gameFacade.evolvePokemon(selectedCard.cardId, targetPokemonInPlayId)
    );
  }

  async endTurn(): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    const matchingAction = board?.actions.find((action) => action.actionType === GameActionType.EndTurn);
    if (!board || !matchingAction?.enabled || board.activePlayerId !== board.localPlayer.id) {
      return;
    }

    this.gameFacade.clearLocalCardHover();
    await this.submitAndAwaitSnapshot(board.stateVersion, this.gameFacade.endTurn());
  }

  toggleSelectedCardAsSetupBench(): void {
    const selectedCardInstanceId = this.commandStateSignal().selectedCardInstanceId;
    const board = this.gameFacade.board();
    if (!board || !selectedCardInstanceId || this.commandStateSignal().setupActiveCardInstanceId === selectedCardInstanceId) {
      return;
    }

    this.updateSetupCommandState(board, (currentState) => ({
      ...currentState,
      setupBenchCardInstanceIds: currentState.setupBenchCardInstanceIds.includes(selectedCardInstanceId)
        ? currentState.setupBenchCardInstanceIds.filter((cardId) => cardId !== selectedCardInstanceId)
        : [...currentState.setupBenchCardInstanceIds, selectedCardInstanceId]
    }));
  }

  async submitSetup(): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    const state = this.commandStateSignal();
    if (
      !board ||
      !state.setupActiveCardInstanceId ||
      board.mulliganNoticePending ||
      board.mulliganFlowActive ||
      !board.mulliganReadyForInitialSelection
    ) {
      return;
    }

    const result = await this.gameFacade.chooseInitialPokemon(
      state.setupActiveCardInstanceId,
      state.setupBenchCardInstanceIds
    );

    if (!result.accepted) {
      this.resetCommandState();
      return;
    }

    this.commandStateSignal.update((currentState) => ({
      ...currentState,
      pendingAction: null,
      selectedCardInstanceId: null,
      selectedPokemonInPlayId: null,
      selectedAttackId: null,
      selectedAttackUseBonusDamage: false
    }));
  }

  async acknowledgeMulliganNotice(): Promise<void> {
    if (this.gameFacade.isActionPending() || this.isAwaitingSnapshot()) {
      return;
    }

    const board = this.gameFacade.board();
    if (!board?.mulliganNoticePending) {
      return;
    }

    await this.submitAndAwaitSnapshot(board.stateVersion, this.gameFacade.ackMulliganNotice());
  }

  async confirmAction(): Promise<void> {
    const board = this.gameFacade.board();
    if (!board) {
      return;
    }

    const state = this.commandStateSignal();
    const screenState = this.gameFacade.screenState();

    if (screenState === 'setup-selecting' && state.setupActiveCardInstanceId) {
      await this.submitSetup();
      return;
    }

    const selectedCard = state.selectedCardInstanceId
      ? findHandCard(board.localPlayer.handCards, state.selectedCardInstanceId)
      : null;

    switch (state.pendingAction) {
      case GameActionType.PlayBasicPokemon:
        if (selectedCard?.cardId) {
          await this.submitAndAwaitSnapshot(board.stateVersion, this.gameFacade.playBasicPokemon(selectedCard.cardId));
        }
        return;
      case GameActionType.AttachEnergy:
        if (state.selectedCardInstanceId && state.selectedPokemonInPlayId) {
          await this.attachEnergyToPokemon(state.selectedCardInstanceId, state.selectedPokemonInPlayId);
        }
        return;
      case GameActionType.EvolvePokemon:
        if (selectedCard?.cardId && state.selectedPokemonInPlayId) {
          await this.submitAndAwaitSnapshot(
            board.stateVersion,
            this.gameFacade.evolvePokemon(selectedCard.cardId, state.selectedPokemonInPlayId)
          );
        }
        return;
      case GameActionType.PlayTrainer:
        if (selectedCard?.cardInstanceId || selectedCard?.cardId) {
          await this.submitAndAwaitSnapshot(
            board.stateVersion,
            this.gameFacade.playTrainer(
              selectedCard.cardId ?? undefined,
              undefined,
              state.selectedPokemonInPlayId ?? undefined,
              undefined,
              undefined,
              selectedCard.cardInstanceId ?? undefined
            )
          );
        }
        return;
      case GameActionType.Retreat:
        if (state.selectedPokemonInPlayId) {
          await this.submitAndAwaitSnapshot(
            board.stateVersion,
            this.gameFacade.retreat(state.selectedPokemonInPlayId)
          );
        }
        return;
      case GameActionType.PromoteBenchPokemon:
        if (state.selectedPokemonInPlayId) {
          await this.submitAndAwaitSnapshot(
            board.stateVersion,
            this.gameFacade.promoteBenchPokemon(state.selectedPokemonInPlayId)
          );
        }
        return;
      case GameActionType.DeclareAttack:
        if (state.selectedAttackId) {
          const selectedAttack = board.localPlayer.activePokemon.pokemon?.attacks.find(
            (attack) => attack.id === state.selectedAttackId
          );
          if (selectedAttack?.requiresTarget && !state.selectedPokemonInPlayId) {
            return;
          }

          const targetsOwnPokemon = selectedAttack?.targetsOwnPokemon ?? false;
          await this.submitAndAwaitSnapshot(
            board.stateVersion,
            this.gameFacade.declareAttack(
              state.selectedAttackId,
              targetsOwnPokemon ? undefined : state.selectedPokemonInPlayId ?? undefined,
              state.selectedAttackUseBonusDamage,
              targetsOwnPokemon ? state.selectedPokemonInPlayId ?? undefined : undefined
            )
          );
        }
        return;
      case GameActionType.DrawCard:
        await this.drawCardFromDeck();
        return;
      case GameActionType.EndTurn:
        await this.endTurn();
        return;
      default:
        return;
    }
  }

  ngOnDestroy(): void {
    this.gameFacade.disconnect();
    this.boardAnimationService.clear();
    this.document.body.style.overflow = this.previousBodyOverflow;
    this.clearSnapshotFallbackTimeout();
    this.animatedGreatBallDiscardIds.clear();
    for (const failsafe of this.handGateFailsafeByPlayer.values()) {
      window.clearTimeout(failsafe);
    }
    this.handGateFailsafeByPlayer.clear();
  }

  private maybeAnimateOpeningHandsDeal(events: GameEvent[], board: BoardGameViewModel): void {
    if (this.openingDealEvaluated) {
      return;
    }

    const openingDealEvent = events.find((event) => event.eventType === GameEventType.OpeningHandsDealt);
    if (!openingDealEvent) {
      return;
    }

    // Evaluate the opening deal only once, and keep the generic loop from re-animating it.
    this.openingDealEvaluated = true;
    this.seenAnimatedEventIds.add(openingDealEvent.eventId);

    const storage = this.document.defaultView?.sessionStorage ?? null;
    const storageKey = `pkmn:opening-deal:${board.gameId}`;
    if (storage?.getItem(storageKey)) {
      return;
    }

    const occurredAtMs = Date.parse(openingDealEvent.occurredAt);
    const isFresh = Number.isFinite(occurredAtMs) && Date.now() - occurredAtMs <= OPENING_DEAL_FRESH_WINDOW_MS;
    if (!isFresh) {
      return;
    }

    storage?.setItem(storageKey, '1');
    const commands = mapEventToBoardAnimations(
      openingDealEvent,
      board,
      this.languageService.t.bind(this.languageService)
    );
    if (commands.length > 0) {
      this.hideHandsForCommands(commands);
      window.setTimeout(() => this.boardAnimationService.enqueue(commands), OPENING_DEAL_RENDER_DELAY_MS);
    }
  }

  private isMulliganAnimationEvent(eventType: GameEventType): boolean {
    return eventType === GameEventType.MulliganHandRevealed
      || eventType === GameEventType.MulliganHandReturned
      || eventType === GameEventType.MulliganDeckShuffled
      || eventType === GameEventType.MulliganNewHandDrawn
      || eventType === GameEventType.MulliganHandValidated
      || eventType === GameEventType.MulliganExtraCardsGranted
      || eventType === GameEventType.MulliganSequenceCompleted;
  }

  /**
   * Drives the deterministic Mulligan hand gate: hide a player up-front when their hand is first
   * revealed (their sequence is starting), and remember pending extra cards so the board can hide the
   * trailing extras (already present in the final snapshot) until the extra-cards animation deals them.
   */
  private applyMulliganStaging(event: GameEvent): void {
    if (event.eventType === GameEventType.MulliganHandRevealed) {
      const payload = readMulliganHandRevealedPayload(event.payload);
      if (payload) {
        this.mulliganVisualService.gateHide(payload.revealingPlayerId);
      }
      return;
    }

    if (event.eventType === GameEventType.MulliganExtraCardsGranted) {
      const payload = readMulliganExtraCardsGrantedPayload(event.payload);
      if (payload) {
        this.mulliganVisualService.setPendingExtra(payload.playerId, payload.cardsGranted);
      }
    }
  }

  /** Hide the hand of every player targeted by these animations, until their sequence drains. */
  private hideHandsForCommands(commands: BoardAnimationCommand[]): void {
    const playerIds = new Set<string>();
    for (const command of commands) {
      if (command.affectedPlayerId) {
        playerIds.add(command.affectedPlayerId);
      }
    }
    if (playerIds.size === 0) {
      return;
    }

    const next = new Set(untracked(() => this.hiddenHandPlayerIdsSignal()));
    for (const playerId of playerIds) {
      next.add(playerId);
    }
    this.hiddenHandPlayerIdsSignal.set(next);

    for (const playerId of playerIds) {
      const existing = this.handGateFailsafeByPlayer.get(playerId);
      if (existing !== undefined) {
        window.clearTimeout(existing);
      }
      this.handGateFailsafeByPlayer.set(
        playerId,
        window.setTimeout(() => this.revealHand(playerId), HAND_GATE_FAILSAFE_MS)
      );
    }
  }

  private revealHand(playerId: string): void {
    const failsafe = this.handGateFailsafeByPlayer.get(playerId);
    if (failsafe !== undefined) {
      window.clearTimeout(failsafe);
      this.handGateFailsafeByPlayer.delete(playerId);
    }
    const current = untracked(() => this.hiddenHandPlayerIdsSignal());
    if (!current.has(playerId)) {
      return;
    }
    const next = new Set(current);
    next.delete(playerId);
    this.hiddenHandPlayerIdsSignal.set(next);
  }

  private resetCommandState(): void {
    this.commandStateSignal.set(EMPTY_GAME_COMMAND_STATE);
  }

  private updateSetupCommandState(
    board: BoardGameViewModel,
    updater: (currentState: GameCommandState) => GameCommandState
  ): void {
    const nextState = updater(this.commandStateSignal());
    this.commandStateSignal.set(nextState);
    this.gameFacade.publishSetupPreview(readLocalSetupPreviewFromState(nextState));
  }

  private async submitAndAwaitSnapshot(
    _stateVersion: number,
    commandPromise: Promise<GameActionExecutionResult>
  ): Promise<boolean> {
    const result = await commandPromise;
    this.resetCommandState();
    return result.accepted;
  }

  private async handleSnapshotWaitTimeout(): Promise<void> {
    if (!this.gameFacade.isAwaitingSnapshot()) {
      return;
    }

    await this.gameFacade.reload();

    if (this.gameFacade.isAwaitingSnapshot()) {
      this.gameFacade.clearAwaitingSnapshotFallback();
    }
  }

  private clearSnapshotFallbackTimeout(): void {
    if (this.snapshotFallbackTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.snapshotFallbackTimeoutId);
    this.snapshotFallbackTimeoutId = null;
  }
}

function findHandCard(cards: BoardCardViewModel[], cardInstanceId: string): BoardCardViewModel | null {
  return cards.find((card) => card.cardInstanceId === cardInstanceId || card.id === cardInstanceId) ?? null;
}

function readStringPayload(event: GameEvent, key: string): string | null {
  const value = event.payload[key];
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function readStringArrayPayload(event: GameEvent, key: string): string[] {
  const value = event.payload[key];
  return Array.isArray(value)
    ? value.filter((entry): entry is string => typeof entry === 'string' && entry.trim().length > 0)
    : [];
}

function resolveAutoActionFromCard(
  board: BoardGameViewModel,
  card: BoardCardViewModel
): GameActionType | null {
  const enabledActions = board.actions
    .filter((candidate) => candidate.enabled)
    .filter((candidate) => candidate.actionType !== GameActionType.AttachEnergy)
    .map((candidate) => candidate.actionType);

  return enabledActions.find((actionType) => cardSupportsAction(card, actionType)) ?? null;
}

function sanitizeCommandState(
  board: BoardGameViewModel,
  screenState: string,
  state: GameCommandState
): GameCommandState {
  if (screenState === 'setup-selecting') {
    const selectableBasicCardIds = board.localPlayer.handCards
      .filter((card) => card.visibility === 'visible' && isBasicStagePokemon(card))
      .map((card) => card.cardInstanceId ?? card.id);
    const persistedSetupCardIds = [
      board.localPlayer.activePokemon.card,
      ...board.localPlayer.benchSlots.map((slot) => slot.card)
    ]
      .filter((card): card is BoardCardViewModel => card !== null)
      .map((card) => card.cardInstanceId ?? card.id);
    const validSetupCardIds = new Set([...selectableBasicCardIds, ...persistedSetupCardIds]);

    const selectedCardInstanceId =
      state.selectedCardInstanceId && selectableBasicCardIds.includes(state.selectedCardInstanceId)
        ? state.selectedCardInstanceId
        : null;
    const setupActiveCardInstanceId =
      state.setupActiveCardInstanceId && validSetupCardIds.has(state.setupActiveCardInstanceId)
        ? state.setupActiveCardInstanceId
        : null;
    const setupBenchCardInstanceIds = state.setupBenchCardInstanceIds.filter(
      (cardInstanceId) => validSetupCardIds.has(cardInstanceId) && cardInstanceId !== setupActiveCardInstanceId
    );

    return {
      ...state,
      pendingAction: state.pendingAction === GameActionType.ChooseInitialPokemon ? state.pendingAction : null,
      selectedCardInstanceId,
      selectedPokemonInPlayId: null,
      selectedAttackId: null,
      setupActiveCardInstanceId,
      setupBenchCardInstanceIds
    };
  }

  if (screenState === 'setup-waiting-opponent') {
    const setupCardIds = new Set(board.localPlayer.handCards.map((card) => card.cardInstanceId ?? card.id));
    const setupActiveCardInstanceId =
      state.setupActiveCardInstanceId && setupCardIds.has(state.setupActiveCardInstanceId)
        ? state.setupActiveCardInstanceId
        : null;
    const setupBenchCardInstanceIds = state.setupBenchCardInstanceIds.filter(
      (cardInstanceId) => setupCardIds.has(cardInstanceId) && cardInstanceId !== setupActiveCardInstanceId
    );

    return {
      ...state,
      pendingAction: null,
      selectedCardInstanceId: null,
      selectedPokemonInPlayId: null,
      selectedAttackId: null,
      setupActiveCardInstanceId,
      setupBenchCardInstanceIds
    };
  }

  const availableActionTypes = new Set(
    board.actions.filter((action) => action.enabled).map((action) => action.actionType)
  );
  const pendingAction = state.pendingAction && availableActionTypes.has(state.pendingAction) ? state.pendingAction : null;

  if (pendingAction === null) {
    return {
      ...state,
      pendingAction: null,
      selectedCardInstanceId: null,
      selectedPokemonInPlayId: null,
      selectedAttackId: null,
      selectedAttackUseBonusDamage: false,
      setupActiveCardInstanceId: null,
      setupBenchCardInstanceIds: []
    };
  }

  const selectedCard = state.selectedCardInstanceId
    ? findHandCard(board.localPlayer.handCards, state.selectedCardInstanceId)
    : null;
  const selectedCardInstanceId = selectedCard ? state.selectedCardInstanceId : null;
  const selectedAttack =
    pendingAction === GameActionType.DeclareAttack && state.selectedAttackId
      ? board.localPlayer.activePokemon.pokemon?.attacks.find((attack) => attack.id === state.selectedAttackId) ?? null
      : null;
  const selectedAttackId = selectedAttack ? state.selectedAttackId : null;

  let selectedPokemonInPlayId = state.selectedPokemonInPlayId;

  if (pendingAction === GameActionType.AttachEnergy) {
    const validTargetIds = selectedCard?.validTargetPokemonInPlayIds ?? [];
    if (selectedPokemonInPlayId && validTargetIds.length > 0 && !validTargetIds.includes(selectedPokemonInPlayId)) {
      selectedPokemonInPlayId = null;
    }
  }

  if (pendingAction === GameActionType.PlayTrainer) {
    const validTargetIds = selectedCard?.validTargetPokemonInPlayIds ?? [];
    if (selectedPokemonInPlayId && validTargetIds.length > 0 && !validTargetIds.includes(selectedPokemonInPlayId)) {
      selectedPokemonInPlayId = null;
    }
  }

  if (pendingAction === GameActionType.DeclareAttack) {
    const validTargetIds = selectedAttack?.validTargetPokemonInPlayIds ?? [];
    if (selectedPokemonInPlayId && validTargetIds.length > 0 && !validTargetIds.includes(selectedPokemonInPlayId)) {
      selectedPokemonInPlayId = null;
    }
  }

  if (
    pendingAction !== GameActionType.AttachEnergy &&
    pendingAction !== GameActionType.EvolvePokemon &&
    pendingAction !== GameActionType.PlayTrainer &&
    pendingAction !== GameActionType.Retreat &&
    pendingAction !== GameActionType.PromoteBenchPokemon &&
    pendingAction !== GameActionType.DeclareAttack
  ) {
    selectedPokemonInPlayId = null;
  }

  return {
    ...state,
    pendingAction,
    selectedCardInstanceId,
    selectedPokemonInPlayId,
    selectedAttackId,
    selectedAttackUseBonusDamage: pendingAction === GameActionType.DeclareAttack ? state.selectedAttackUseBonusDamage : false,
    setupActiveCardInstanceId: null,
    setupBenchCardInstanceIds: []
  };
}

function gameCommandStatesEqual(left: GameCommandState, right: GameCommandState): boolean {
  return (
    left.pendingAction === right.pendingAction &&
    left.selectedCardInstanceId === right.selectedCardInstanceId &&
    left.selectedPokemonInPlayId === right.selectedPokemonInPlayId &&
    left.selectedAttackId === right.selectedAttackId &&
    left.selectedAttackUseBonusDamage === right.selectedAttackUseBonusDamage &&
    left.setupActiveCardInstanceId === right.setupActiveCardInstanceId &&
    left.setupBenchCardInstanceIds.length === right.setupBenchCardInstanceIds.length &&
    left.setupBenchCardInstanceIds.every((cardId, index) => cardId === right.setupBenchCardInstanceIds[index])
  );
}

function readLocalSetupPreviewFromState(state: GameCommandState): {
  activeOccupied: boolean;
  activeCardInstanceId: string | null;
  benchOccupiedIndexes: number[];
  benchCardInstanceIdsByIndex: Record<number, string>;
} {
  const benchOccupiedIndexes = state.setupBenchCardInstanceIds.map((_, index) => index);

  return {
    activeOccupied: state.setupActiveCardInstanceId !== null,
    activeCardInstanceId: state.setupActiveCardInstanceId,
    benchOccupiedIndexes,
    benchCardInstanceIdsByIndex: Object.fromEntries(
      state.setupBenchCardInstanceIds.map((cardInstanceId, index) => [index, cardInstanceId])
    )
  };
}

function hasPendingSetupPreviewState(state: GameCommandState, screenState: string): boolean {
  return (
    screenState === 'setup-selecting' ||
    screenState === 'setup-waiting-opponent' ||
    state.setupActiveCardInstanceId !== null ||
    state.setupBenchCardInstanceIds.length > 0
  );
}

/** Backend-decided Mulligan ordering index (interleaved resolution); null for non-Mulligan events. */
function mulliganSequenceIndex(event: GameEvent): number | null {
  const value = event.payload?.['sequenceIndex'];
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}
