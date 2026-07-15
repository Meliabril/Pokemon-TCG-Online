import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription, firstValueFrom, forkJoin } from 'rxjs';
import { GameActionType } from '../../../core/models/enums/game/game-action-type.enum';
import { GameChatContext } from '../../../core/models/enums/game/game-chat-context.enum';
import { GameEventType } from '../../../core/models/enums/game/game-event-type.enum';
import { GameStatus } from '../../../core/models/enums/game/game-status.enum';
import { ApiError } from '../../../core/models/interfaces/common/api-error.interface';
import {
  AttachEnergyPayload,
  ChooseInitialPokemonPayload,
  DeclareAttackPayload,
  EmptyActionPayload,
  EvolvePokemonPayload,
  GameActionPayload,
  PlayBasicPokemonPayload,
  PlayTrainerPayload,
  PromoteBenchPokemonPayload,
  ResolveAttackChoicePayload,
  RetreatPayload,
  UseAbilityPayload
} from '../../../core/models/interfaces/game/game-action-payloads.interface';
import {
  GameActionLogEntry,
  GameActionResponse,
  GameActionRequest
} from '../../../core/models/interfaces/game/game-action.interface';
import {
  GameChatMessage,
  GameChatMessageRequest
} from '../../../core/models/interfaces/game/game-chat-message.interface';
import { GameEvent } from '../../../core/models/interfaces/game/game-event.interface';
import { GameSnapshot } from '../../../core/models/interfaces/game/game-snapshot.interface';
import { GameDetail, GameStateSync } from '../../../core/models/interfaces/game/game-state.interface';
import {
  BoardVisualOwner,
  BoardZone,
  CardHoverChangedEvent,
  GameVisualEvent,
  SetupSlotChangedEvent
} from '../../../core/models/interfaces/game/game-visual-event.interface';
import { GameRealtimeEnvelope } from '../../../core/models/interfaces/matchmaking/matchmaking-realtime.interface';
import { StorageService } from '../../../core/storage/storage.service';
import { LanguageService } from '../../../core/services/language.service';
import { AuthApiService } from '../../../infrastructure/api/auth/auth-api.service';
import { GameApiService } from '../../../infrastructure/api/game/game-api.service';
import { avatarImageUrlFor } from '../../../shared/ui/profile/avatar-picker/avatar-picker.component';
import { BoardGameViewModel } from '../domain/board/board-game-view-model.interface';
import { BoardEphemeralUiState } from '../domain/ui-state/board-ephemeral-ui-state.interface';
import { mapGameSnapshotToBoardGameViewModel } from '../domain/board/game-board.mapper';
import { GameFinalOutcome } from '../domain/result/game-final-outcome.type';
import { GameScreenState } from '../domain/ui-state/game-screen-state.type';
import {
  attackChoiceScreenState,
  canExecuteGameAction,
  isPromotionRequired,
  promotionScreenState
} from '../domain/board/game-state.helpers';
import { GameUiError, GameUiErrorKind } from '../domain/ui-state/game-ui-error.interface';
import { GameRealtimeService } from './game-realtime.service';
import { BoardAnimationService } from './board-animation.service';

type RealtimeConnectionStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting';

export interface GameActionExecutionResult {
  accepted: boolean;
  newStateVersion: number | null;
}

const EMPTY_PAYLOAD: EmptyActionPayload = {};
const SNAPSHOT_READY_MAX_ATTEMPTS = 10;
const SNAPSHOT_READY_RETRY_MS = 300;
const REMOTE_HOVER_CLEAR_MS = 2_500;

const EMPTY_UI_STATE: BoardEphemeralUiState = {
  selectedCardInstanceId: null,
  selectedPokemonInPlayId: null,
  targetPokemonInPlayId: null,
  activePanel: 'none',
  pendingAction: null
};

class InvalidGameSnapshotError extends Error {
  constructor() {
    super('GAME.ERRORS.INVALID_SNAPSHOT');
    this.name = 'InvalidGameSnapshotError';
  }
}

class MissingCurrentUserError extends Error {
  constructor() {
    super('GAME.ERRORS.MISSING_USER');
    this.name = 'MissingCurrentUserError';
  }
}

class MissingLocalParticipantError extends Error {
  constructor() {
    super('GAME.ERRORS.MISSING_PARTICIPANT');
    this.name = 'MissingLocalParticipantError';
  }
}

class SnapshotNotReadyError extends Error {
  constructor() {
    super('GAME.ERRORS.SNAPSHOT_NOT_READY');
    this.name = 'SnapshotNotReadyError';
  }
}

export interface CardHoverVisualTarget {
  zone: BoardZone;
  owner: BoardVisualOwner;
  visualIndex?: number;
  cardInstanceId?: string | null;
  hovered: boolean;
}

interface SetupPreviewVisualTarget {
  zone: Extract<BoardZone, 'ACTIVE' | 'BENCH'>;
  owner: BoardVisualOwner;
  visualIndex: number;
  occupied: boolean;
}

interface SetupPreviewState {
  activeOccupied: boolean;
  activeCardInstanceId: string | null;
  benchOccupiedIndexes: number[];
  benchCardInstanceIdsByIndex: Record<number, string>;
}

const EMPTY_SETUP_PREVIEW_STATE: SetupPreviewState = {
  activeOccupied: false,
  activeCardInstanceId: null,
  benchOccupiedIndexes: [],
  benchCardInstanceIdsByIndex: {}
};

function isGameStateSync(envelope: GameRealtimeEnvelope): envelope is GameStateSync {
  return envelope.eventType === GameEventType.StateSync;
}

function isGameEvent(envelope: GameRealtimeEnvelope): envelope is GameEvent {
  return 'eventId' in envelope;
}

function isChatEvent(envelope: GameRealtimeEnvelope): envelope is GameEvent {
  return isGameEvent(envelope) && envelope.eventType === GameEventType.ChatMessage;
}

function isPresenceEvent(envelope: GameRealtimeEnvelope): envelope is GameEvent {
  return isGameEvent(envelope) && envelope.eventType === GameEventType.ParticipantPresenceChanged;
}

function mapChatEvent(event: GameEvent): GameChatMessage {
  return {
    messageId: String(event.payload['messageId']),
    gameId: event.gameId,
    senderUserId: String(event.payload['senderUserId']),
    senderUsername: String(event.payload['senderUsername']),
    context: String(event.payload['context']) as GameChatContext,
    content: String(event.payload['content']),
    sentAt: normalizeDateLike(event.payload['sentAt'])
  };
}

function appendUniqueChat(messages: GameChatMessage[], nextMessage: GameChatMessage): GameChatMessage[] {
  if (messages.some((message) => message.messageId === nextMessage.messageId)) {
    return messages;
  }

  return [...messages, nextMessage].sort((left, right) => left.sentAt.localeCompare(right.sentAt));
}

function appendUniqueEvent(events: GameEvent[], nextEvent: GameEvent): GameEvent[] {
  if (events.some((event) => event.eventId === nextEvent.eventId)) {
    return events;
  }

  return [...events, nextEvent].sort((left, right) => left.occurredAt.localeCompare(right.occurredAt));
}

function dedupeEvents(events: GameEvent[]): GameEvent[] {
  return events.reduce<GameEvent[]>((accumulator, event) => appendUniqueEvent(accumulator, event), []);
}

function normalizeDateLike(value: unknown): string {
  if (typeof value === 'number') {
    return new Date(value * 1000).toISOString();
  }

  const text = String(value);
  const numeric = Number(text);
  if (!Number.isNaN(numeric) && text.trim() !== '') {
    return new Date(numeric * 1000).toISOString();
  }

  return text;
}

function participantLabels(
  detail: GameDetail,
  currentUserId: string | null,
  currentUsername: string | null,
  t: (key: string, params?: Record<string, string | number>) => string
): Record<string, string> {
  return Object.fromEntries(
    detail.participants.flatMap((participant) => {
      const label =
        participant.userId === currentUserId
          ? currentUsername || participant.username?.trim() || t('GAME.SELF')
          : participant.username?.trim() || t('GAME.RIVAL_NUMBER', { number: participant.playerOrder });

      return [
        [participant.id, label],
        [participant.userId, label]
      ];
    })
  );
}

function currentPlayerId(
  detail: GameDetail | null,
  snapshot: GameSnapshot | null,
  currentUserId: string | null
): string | null {
  if (!detail || !currentUserId) {
    return null;
  }

  const participant = detail.participants.find((nextParticipant) => nextParticipant.userId === currentUserId);
  if (!participant) {
    return null;
  }

  const boardView = snapshot?.board?.view ?? null;
  const visibleLocalPlayerId =
    boardView?.players.find((player) => player.local)?.playerId ??
    boardView?.localPlayerId ??
    null;
  if (
    visibleLocalPlayerId &&
    (visibleLocalPlayerId === participant.id || visibleLocalPlayerId === participant.userId)
  ) {
    return visibleLocalPlayerId;
  }

  if (snapshot?.playerIds.includes(participant.id)) {
    return participant.id;
  }

  if (snapshot?.playerIds.includes(participant.userId)) {
    return participant.userId;
  }

  return participant.id;
}

function localPlayerAliases(
  detail: GameDetail | null,
  currentUserId: string | null,
  currentParticipantId: string | null
): Set<string> {
  const aliases = new Set<string>();
  if (currentUserId) {
    aliases.add(currentUserId);
  }
  if (currentParticipantId) {
    aliases.add(currentParticipantId);
  }

  const participant = detail?.participants.find(
    (candidate) =>
      candidate.userId === currentUserId ||
      candidate.id === currentParticipantId ||
      candidate.userId === currentParticipantId
  );
  if (participant) {
    aliases.add(participant.id);
    aliases.add(participant.userId);
  }

  return aliases;
}

function participantAvatarUrls(
  detail: GameDetail,
  currentUserId: string | null,
  currentAvatar: string | null | undefined
): Record<string, string> {
  return Object.fromEntries(
    detail.participants.flatMap((participant) => {
      const avatarId =
        participant.userId === currentUserId ? currentAvatar ?? participant.avatar : participant.avatar;
      const avatarUrl = avatarImageUrlFor(avatarId);

      return [
        [participant.id, avatarUrl],
        [participant.userId, avatarUrl]
      ];
    })
  );
}

function isApiError(payload: unknown): payload is ApiError {
  if (!payload || typeof payload !== 'object') {
    return false;
  }

  return 'message' in payload && typeof payload.message === 'string';
}

function readErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const payload = error.error;
    if (isApiError(payload) && payload.message.trim()) {
      return payload.message;
    }

    if (error.status === 0) {
      return fallback;
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return fallback;
}

function readErrorCode(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && isApiError(error.error)) {
    return error.error.error;
  }

  return null;
}

function createUiError(
  kind: GameUiErrorKind,
  message: string,
  recoverable: boolean,
  error?: unknown,
  title?: string,
  detail?: string | null
): GameUiError {
  return {
    kind,
    title,
    message,
    detail: detail ?? null,
    status: error instanceof HttpErrorResponse ? error.status : null,
    code: readErrorCode(error),
    recoverable
  };
}

function isBackendSupportGapMessage(message: string): boolean {
  const normalizedMessage = message.trim();

  return (
    /No Trainer effect is registered/i.test(normalizedMessage)
    || /not supported/i.test(normalizedMessage)
    || /not implemented/i.test(normalizedMessage)
    || /todavia no esta soportad/i.test(normalizedMessage)
    || /todavia no esta implementad/i.test(normalizedMessage)
  );
}

function createUnsupportedActionUiError(
  message: string,
  t: (key: string) => string,
  error?: unknown
): GameUiError {
  return createUiError(
    'action-not-supported',
    t('GAME.ERRORS.BACKEND_UNSUPPORTED_MESSAGE'),
    true,
    error,
    t('GAME.ERRORS.BACKEND_UNSUPPORTED_TITLE'),
    null
  );
}

function createHttpUiError(
  error: unknown,
  fallbackMessage: string,
  fallbackKind: GameUiErrorKind,
  t: (key: string) => string
): GameUiError {
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return createUiError(
      'connection-lost',
      t('GAME.CONNECTION_LOST'),
      true,
      error
    );
  }

  if (error instanceof HttpErrorResponse && (error.status === 403 || error.status === 422)) {
    const message = readErrorMessage(error, fallbackMessage);
    if (isBackendSupportGapMessage(message)) {
      return createUnsupportedActionUiError(message, t, error);
    }

    return createUiError(
      'action-rejected',
      message,
      true,
      error,
      t('GAME.ERRORS.ACTION_REJECTED')
    );
  }

  return createUiError(fallbackKind, fallbackMessage, true, error);
}

function randomHex(length: number): string {
  return Array.from({ length }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}

function createClientActionId(): string {
  const cryptoApi = globalThis.crypto;
  if (cryptoApi?.randomUUID) {
    return cryptoApi.randomUUID();
  }

  return `${randomHex(8)}-${randomHex(4)}-4${randomHex(3)}-${(8 + Math.floor(Math.random() * 4)).toString(16)}${randomHex(3)}-${randomHex(12)}`;
}

function readNewStateVersion(
  response: GameActionResponse<unknown>,
  currentStateVersion: number
): number {
  return typeof response.newStateVersion === 'number' && response.newStateVersion > currentStateVersion
    ? response.newStateVersion
    : currentStateVersion + 1;
}

function visualCardKey(target: {
  owner: BoardVisualOwner;
  zone: BoardZone;
  visualIndex?: number;
  cardInstanceId?: string | null;
}): string | null {
  const cardInstanceId = target.cardInstanceId?.trim();
  if (cardInstanceId) {
    return `${target.owner}:${target.zone}:${cardInstanceId}`;
  }

  if (typeof target.visualIndex === 'number') {
    return `${target.owner}:${target.zone}:${target.visualIndex}`;
  }

  return null;
}

function remoteOwnerFor(owner: BoardVisualOwner): BoardVisualOwner {
  return owner === 'SELF' ? 'OPPONENT' : 'SELF';
}

function setupPreviewStatesEqual(left: SetupPreviewState, right: SetupPreviewState): boolean {
  return (
    left.activeOccupied === right.activeOccupied &&
    left.activeCardInstanceId === right.activeCardInstanceId &&
    left.benchOccupiedIndexes.length === right.benchOccupiedIndexes.length &&
    left.benchOccupiedIndexes.every((index, position) =>
      index === right.benchOccupiedIndexes[position]
      && left.benchCardInstanceIdsByIndex[index] === right.benchCardInstanceIdsByIndex[index]
    )
  );
}

function normalizeSetupPreview(preview: SetupPreviewState): SetupPreviewState {
  return {
    activeOccupied: preview.activeOccupied,
    activeCardInstanceId: preview.activeCardInstanceId,
    benchOccupiedIndexes: [...new Set(preview.benchOccupiedIndexes)]
      .filter((index) => Number.isInteger(index) && index >= 0)
      .sort((left, right) => left - right),
    benchCardInstanceIdsByIndex: Object.fromEntries(
      Object.entries(preview.benchCardInstanceIdsByIndex)
        .filter(([index, cardInstanceId]) =>
          Number.isInteger(Number(index)) && Number(index) >= 0 && typeof cardInstanceId === 'string'
        )
    )
  };
}

function assertValidSnapshot(snapshot: unknown): asserts snapshot is GameSnapshot {
  if (
    !snapshot ||
    typeof snapshot !== 'object' ||
    !('gameId' in snapshot) ||
    typeof snapshot.gameId !== 'string' ||
    !('playerIds' in snapshot) ||
    !Array.isArray(snapshot.playerIds) ||
    snapshot.playerIds.length === 0 ||
    !('players' in snapshot) ||
    !snapshot.players ||
    typeof snapshot.players !== 'object' ||
    !('turn' in snapshot) ||
    !snapshot.turn ||
    typeof snapshot.turn !== 'object' ||
    !('board' in snapshot) ||
    !snapshot.board ||
    typeof snapshot.board !== 'object' ||
    !('actions' in snapshot) ||
    !snapshot.actions ||
    typeof snapshot.actions !== 'object'
  ) {
    throw new InvalidGameSnapshotError();
  }
}

function logGameFacadeGroup(title: string, entries: Record<string, unknown>): void {
  void title;
  void entries;
}

@Injectable({ providedIn: 'root' })
export class GameFacadeService {
  private readonly authApi = inject(AuthApiService);
  private readonly gameApi = inject(GameApiService);
  private readonly gameRealtime = inject(GameRealtimeService);
  private readonly storageService = inject(StorageService);
  private readonly languageService = inject(LanguageService);
  private readonly boardAnimationService = inject(BoardAnimationService);

  private readonly gameIdSignal = signal<string | null>(null);
  private readonly detailSignal = signal<GameDetail | null>(null);
  private readonly snapshotSignal = signal<GameSnapshot | null>(null);
  private readonly eventFeedSignal = signal<GameEvent[]>([]);
  private readonly historySignal = signal<GameActionLogEntry[]>([]);
  private readonly chatMessagesSignal = signal<GameChatMessage[]>([]);
  private readonly uiStateSignal = signal<BoardEphemeralUiState>(EMPTY_UI_STATE);
  private readonly remoteHoveredCardKeySignal = signal<string | null>(null);
  private readonly remoteSetupPreviewSignal = signal<SetupPreviewState>(EMPTY_SETUP_PREVIEW_STATE);
  private readonly isLoadingSignal = signal(false);
  private readonly isActionPendingSignal = signal(false);
  private readonly awaitingSnapshotFromVersionSignal = signal<number | null>(null);
  private readonly uiErrorSignal = signal<GameUiError | null>(null);
  private readonly connectionStatusSignal = signal<RealtimeConnectionStatus>('idle');
  private readonly pendingStateSyncRequests = new Set<string>();
  private realtimeSubscription: Subscription | null = null;
  private visualRealtimeSubscription: Subscription | null = null;
  private activeRealtimeGameId: string | null = null;
  private lastPublishedHoverKey: string | null = null;
  private lastPublishedSetupPreview = EMPTY_SETUP_PREVIEW_STATE;
  private remoteHoverTimeoutId: number | null = null;

  readonly gameId = this.gameIdSignal.asReadonly();
  readonly detail = this.detailSignal.asReadonly();
  readonly snapshot = this.snapshotSignal.asReadonly();
  readonly eventFeed = this.eventFeedSignal.asReadonly();
  readonly history = this.historySignal.asReadonly();
  readonly chatMessages = this.chatMessagesSignal.asReadonly();
  readonly uiState = this.uiStateSignal.asReadonly();
  readonly remoteHoveredCardKey = this.remoteHoveredCardKeySignal.asReadonly();
  readonly isLoading = this.isLoadingSignal.asReadonly();
  readonly isActionPending = this.isActionPendingSignal.asReadonly();
  readonly isAwaitingSnapshot = computed(
    () => this.awaitingSnapshotFromVersionSignal() !== null
  );
  readonly uiError = this.uiErrorSignal.asReadonly();
  readonly errorMessage = computed(() => this.uiErrorSignal()?.message ?? '');
  readonly connectionStatus = this.connectionStatusSignal.asReadonly();
  readonly isReconnecting = computed(() => this.connectionStatusSignal() === 'reconnecting');
  readonly currentUserId = computed(() => this.storageService.currentUser()?.id ?? null);
  readonly currentParticipantId = computed(() =>
    currentPlayerId(this.detailSignal(), this.snapshotSignal(), this.currentUserId())
  );
  readonly finalOutcome = computed<GameFinalOutcome>(() => {
    const snapshot = this.snapshotSignal();
    if (snapshot?.status !== GameStatus.Finished) {
      return null;
    }

    const detail = this.detailSignal();
    const winnerPlayerId = detail?.winnerPlayerId ?? null;
    if (!winnerPlayerId) {
      return 'pending';
    }

    return localPlayerAliases(detail, this.currentUserId(), this.currentParticipantId()).has(winnerPlayerId)
      ? 'victory'
      : 'defeat';
  });
  readonly screenState = computed<GameScreenState>(() => {
    if (this.isLoadingSignal()) {
      return 'loading';
    }

    if (this.connectionStatusSignal() === 'reconnecting') {
      return 'reconnecting';
    }

    const snapshot = this.snapshotSignal();
    if (!snapshot) {
      return this.uiErrorSignal() ? 'load-error' : 'loading';
    }

    if (!this.isSnapshotReadyForBoardRender(snapshot)) {
      return this.uiErrorSignal() ? 'load-error' : 'loading';
    }

    if (!this.storageService.currentUser() || !this.currentParticipantId()) {
      return this.uiErrorSignal() ? 'load-error' : 'loading';
    }

    if (snapshot.status === GameStatus.Paused) {
      return 'paused';
    }

    if (snapshot.status === GameStatus.Finished) {
      return 'finished';
    }

    if (snapshot.status === GameStatus.Cancelled) {
      return 'cancelled';
    }

    if (snapshot.resolution?.resolutionType) {
      const participantId = this.currentParticipantId();
      return (
        promotionScreenState(snapshot, participantId) ??
        attackChoiceScreenState(snapshot, participantId) ??
        'resolution-pending'
      );
    }

    if (snapshot.status === GameStatus.Waiting) {
      return 'waiting-opponent';
    }

    if (snapshot.status === GameStatus.Setup) {
      const participantId = this.currentParticipantId();
      const localPlayerState = participantId ? snapshot.players[participantId] : null;
      return localPlayerState?.initialPokemonSelectionSubmitted
        ? 'setup-waiting-opponent'
        : 'setup-selecting';
    }

    return 'playing';
  });
  readonly board = computed<BoardGameViewModel | null>(() => {
    const detail = this.detailSignal();
    const snapshot = this.snapshotSignal();
    const currentUser = this.storageService.currentUser();
    const localPlayerId = this.currentParticipantId();
    this.languageService.translations();

    if (!detail || !snapshot || !currentUser || !localPlayerId) {
      return null;
    }

    if (!this.isSnapshotReadyForBoardRender(snapshot)) {
      logGameFacadeGroup('board mapping skipped', {
        reason: 'snapshot not ready',
        readiness: this.snapshotReadiness(snapshot)
      });
      return null;
    }

    const labels = participantLabels(
      detail,
      currentUser.id,
      currentUser.username,
      this.languageService.t.bind(this.languageService)
    );
    const avatarUrlByPlayerId = participantAvatarUrls(detail, currentUser.id, currentUser.avatar);
    const connectedByPlayerId = Object.fromEntries(
      detail.participants.flatMap((participant) => [
        [participant.id, participant.connected === true],
        [participant.userId, participant.connected === true]
      ])
    );

    const mappedBoard = mapGameSnapshotToBoardGameViewModel(snapshot, {
      localPlayerId,
      playerLabels: labels,
      connectedByPlayerId,
      avatarUrlByPlayerId,
      eventFeed: this.eventFeedSignal(),
      uiState: this.uiStateSignal(),
      t: this.languageService.t.bind(this.languageService)
    });

    return this.projectRemoteSetupPreview(mappedBoard, snapshot.status);
  });

  async enterGame(gameId: string): Promise<void> {
    logGameFacadeGroup('enterGame', {
      gameId,
      step: 'inicio de carga'
    });
    this.gameIdSignal.set(gameId);
    await this.hydrate(gameId, true);
    logGameFacadeGroup('enterGame realtime', {
      gameId,
      step: 'inicio de conexion realtime'
    });
    this.connectRealtime(gameId);
  }

  async load(gameId: string): Promise<void> {
    await this.enterGame(gameId);
  }

  async reload(): Promise<void> {
    const gameId = this.gameIdSignal();
    if (!gameId) {
      return;
    }

    await this.hydrate(gameId, true);
  }

  resetUiState(): void {
    this.uiStateSignal.set(EMPTY_UI_STATE);
  }

  disconnect(): void {
    this.clearLocalCardHover();
    this.clearLocalSetupPreview();
    this.clearRemoteCardHover();
    this.clearRemoteSetupPreview();
    if (this.activeRealtimeGameId) {
      this.gameRealtime.leave(this.activeRealtimeGameId);
      this.activeRealtimeGameId = null;
    }

    this.realtimeSubscription?.unsubscribe();
    this.visualRealtimeSubscription?.unsubscribe();
    this.realtimeSubscription = null;
    this.visualRealtimeSubscription = null;
    this.pendingStateSyncRequests.clear();
    this.connectionStatusSignal.set('idle');
    this.resetUiState();
  }

  async executeAction(
    actionType: GameActionType,
    payload: GameActionPayload = EMPTY_PAYLOAD
  ): Promise<GameActionExecutionResult> {
    const gameId = this.gameIdSignal();
    const snapshot = this.snapshotSignal();
    if (!gameId || !snapshot) {
      this.uiErrorSignal.set(
        createUiError(
          'action-rejected',
          this.languageService.t('GAME.ERRORS.NO_ACTIVE_GAME'),
          true
        )
      );
      return { accepted: false, newStateVersion: null };
    }

    if (!canExecuteGameAction(snapshot, this.currentParticipantId(), actionType)) {
      this.uiErrorSignal.set(
        createUiError(
          'action-rejected',
          this.languageService.t(
            isPromotionRequired(snapshot)
              ? 'GAME.ERRORS.PROMOTION_REQUIRED'
              : 'GAME.ERRORS.GAME_BLOCKED'
          ),
          true
        )
      );
      return { accepted: false, newStateVersion: null };
    }

    this.isActionPendingSignal.set(true);
    if (this.uiErrorSignal()?.kind !== 'connection-lost') {
      this.uiErrorSignal.set(null);
    }

    const request = {
      gameId,
      clientActionId: createClientActionId(),
      actionType,
      expectedStateVersion: snapshot.stateVersion,
      payload
    } as GameActionRequest;

    const optimisticNewStateVersion = snapshot.stateVersion + 1;
    this.awaitingSnapshotFromVersionSignal.set(optimisticNewStateVersion);

    logGameFacadeGroup('executeAction dispatch', {
      actionType,
      snapshotVersionBeforeRequest: snapshot.stateVersion,
      expectedStateVersionSent: snapshot.stateVersion,
      optimisticAwaitingVersion: optimisticNewStateVersion
    });

    try {
      const response = await firstValueFrom(this.gameApi.executeAction(gameId, request));
      const accepted = response.success !== false;
      const newStateVersion = readNewStateVersion(response, snapshot.stateVersion);
      if (accepted) {
        this.requestStateSyncOnce(gameId, newStateVersion);
        if (newStateVersion !== optimisticNewStateVersion) {
          this.awaitingSnapshotFromVersionSignal.set(newStateVersion);
        }
      } else {
        this.awaitingSnapshotFromVersionSignal.set(null);
      }

      logGameFacadeGroup('executeAction response', {
        actionType,
        accepted,
        responseNewStateVersion: response.newStateVersion,
        readNewStateVersion: newStateVersion,
        snapshotVersionAfterResponse: this.snapshotSignal()?.stateVersion ?? null
      });

      return {
        accepted,
        newStateVersion
      };
    } catch (error) {
      await this.handleActionError(gameId, error);
      this.awaitingSnapshotFromVersionSignal.set(null);
      return { accepted: false, newStateVersion: null };
    } finally {
      this.isActionPendingSignal.set(false);
    }
  }

  chooseInitialPokemon(
    activeCardInstanceId: string,
    benchCardInstanceIds: string[]
  ): Promise<GameActionExecutionResult> {
    const payload: ChooseInitialPokemonPayload = { activeCardInstanceId, benchCardInstanceIds };
    return this.executeAction(GameActionType.ChooseInitialPokemon, payload);
  }

  ackMulliganNotice(): Promise<GameActionExecutionResult> {
    return this.executeAction(GameActionType.AckMulliganNotice);
  }

  drawCard(): Promise<GameActionExecutionResult> {
    return this.executeAction(GameActionType.DrawCard);
  }

  playBasicPokemon(cardId: string): Promise<GameActionExecutionResult> {
    const payload: PlayBasicPokemonPayload = { cardId };
    return this.executeAction(GameActionType.PlayBasicPokemon, payload);
  }

  attachEnergy(cardId: string, cardInstanceId: string, pokemonInPlayId: string): Promise<GameActionExecutionResult> {
    const payload: AttachEnergyPayload = { cardId, cardInstanceId, pokemonInPlayId };
    return this.executeAction(GameActionType.AttachEnergy, payload);
  }

  declareAttack(
    attackId: string,
    switchTargetPokemonInPlayId?: string,
    useBonusDamage?: boolean,
    selfTargetPokemonInPlayId?: string
  ): Promise<GameActionExecutionResult> {
    const payload: DeclareAttackPayload = {
      attackId,
      useBonusDamage,
      selfTargetPokemonInPlayId,
      switchTargetPokemonInPlayId
    };
    return this.executeAction(GameActionType.DeclareAttack, payload);
  }

  endTurn(): Promise<GameActionExecutionResult> {
    return this.executeAction(GameActionType.EndTurn);
  }

  promoteBenchPokemon(pokemonInPlayId: string): Promise<GameActionExecutionResult> {
    const payload: PromoteBenchPokemonPayload = { pokemonInPlayId };
    return this.executeAction(GameActionType.PromoteBenchPokemon, payload);
  }

  resolveAttackChoice(payload: ResolveAttackChoicePayload): Promise<GameActionExecutionResult> {
    return this.executeAction(GameActionType.ResolveAttackChoice, payload);
  }

  evolvePokemon(cardId: string, pokemonInPlayId: string): Promise<GameActionExecutionResult> {
    const payload: EvolvePokemonPayload = { cardId, pokemonInPlayId };
    return this.executeAction(GameActionType.EvolvePokemon, payload);
  }

  playTrainer(
    cardId: string | undefined,
    targetCardId?: string,
    targetPokemonInPlayId?: string,
    targetCardInstanceId?: string,
    targetEnergyCardInstanceId?: string,
    cardInstanceId?: string,
    selectedEvolutionExternalId?: string,
    selectedCardInstanceId?: string,
    selectedCardIds?: string[]
  ): Promise<GameActionExecutionResult> {
    const payload: PlayTrainerPayload = {
      cardId,
      cardInstanceId,
      targetCardId,
      targetPokemonInPlayId,
      targetCardInstanceId,
      targetEnergyCardInstanceId,
      selectedEvolutionExternalId,
      selectedCardInstanceId,
      selectedCardIds
    };
    return this.executeAction(GameActionType.PlayTrainer, payload);
  }

  retreat(targetPokemonInPlayId: string): Promise<GameActionExecutionResult> {
    const payload: RetreatPayload = { targetPokemonInPlayId };
    return this.executeAction(GameActionType.Retreat, payload);
  }

  useAbility(payload: UseAbilityPayload): Promise<GameActionExecutionResult> {
    return this.executeAction(GameActionType.UseAbility, payload);
  }

  sendChatMessage(content: string): void {
    const gameId = this.gameIdSignal();
    if (!gameId) {
      throw new Error(this.languageService.t('GAME.ERRORS.NO_CHAT_GAME'));
    }

    const request: GameChatMessageRequest = {
      context: GameChatContext.Game,
      content
    };
    this.gameRealtime.sendChatMessage(gameId, request);
  }

  publishCardHoverChanged(target: CardHoverVisualTarget): void {
    const gameId = this.gameIdSignal();
    const playerId = this.currentUserId();
    if (!gameId || !playerId) {
      return;
    }

    if (target.owner !== 'SELF') {
      return;
    }

    const safeTarget = this.sanitizeHoverTarget(target);
    const targetKey = visualCardKey(safeTarget);
    const nextKey = target.hovered ? targetKey : null;
    if (target.hovered && this.lastPublishedHoverKey === nextKey) {
      return;
    }

    if (!target.hovered && (this.lastPublishedHoverKey === null || this.lastPublishedHoverKey !== targetKey)) {
      return;
    }

    if (target.hovered && !this.canSendRemoteHover(safeTarget)) {
      return;
    }

    this.lastPublishedHoverKey = nextKey;
    this.gameRealtime.sendCardHoverChanged(gameId, {
      type: 'CARD_HOVER_CHANGED',
      gameId,
      playerId,
      zone: safeTarget.zone,
      owner: safeTarget.owner,
      visualIndex: safeTarget.visualIndex,
      cardInstanceId: safeTarget.cardInstanceId ?? undefined,
      hovered: target.hovered
    });
  }

  clearLocalCardHover(): void {
    if (this.lastPublishedHoverKey === null) {
      return;
    }

    const gameId = this.gameIdSignal();
    const playerId = this.currentUserId();
    this.lastPublishedHoverKey = null;
    if (!gameId || !playerId) {
      return;
    }

    this.gameRealtime.sendCardHoverChanged(gameId, {
      type: 'CARD_HOVER_CHANGED',
      gameId,
      playerId,
      zone: 'HAND',
      owner: 'SELF',
      hovered: false
    });
  }

  publishSetupPreview(nextPreview: SetupPreviewState): void {
    const safePreview = normalizeSetupPreview(nextPreview);
    if (setupPreviewStatesEqual(this.lastPublishedSetupPreview, safePreview)) {
      return;
    }

    this.publishSetupPreviewChanges(this.lastPublishedSetupPreview, safePreview);
    this.lastPublishedSetupPreview = safePreview;
  }

  hydrateLocalSetupPreview(preview: SetupPreviewState): void {
    this.lastPublishedSetupPreview = normalizeSetupPreview(preview);
  }

  clearLocalSetupPreview(): void {
    if (setupPreviewStatesEqual(this.lastPublishedSetupPreview, EMPTY_SETUP_PREVIEW_STATE)) {
      return;
    }

    this.publishSetupPreviewChanges(this.lastPublishedSetupPreview, EMPTY_SETUP_PREVIEW_STATE);
    this.lastPublishedSetupPreview = EMPTY_SETUP_PREVIEW_STATE;
  }

  private canSendRemoteHover(target: Pick<CardHoverVisualTarget, 'zone'>): boolean {
    const snapshot = this.snapshotSignal();
    if (!snapshot) {
      return false;
    }

    if (snapshot.status === GameStatus.Setup) {
      return target.zone === 'HAND' || target.zone === 'ACTIVE' || target.zone === 'BENCH';
    }

    if (snapshot.status !== GameStatus.Active) {
      return false;
    }

    const activePlayerId = snapshot.turn.activePlayerId?.trim();
    if (!activePlayerId) {
      return false;
    }

    return localPlayerAliases(
      this.detailSignal(),
      this.currentUserId(),
      this.currentParticipantId()
    ).has(activePlayerId);
  }

  private sanitizeHoverTarget(target: CardHoverVisualTarget): CardHoverVisualTarget {
    const snapshot = this.snapshotSignal();
    if (
      snapshot?.status === GameStatus.Setup &&
      (target.zone === 'HAND' || target.zone === 'ACTIVE' || target.zone === 'BENCH')
    ) {
      return {
        ...target,
        cardInstanceId: null
      };
    }

    return target;
  }

  private connectRealtime(gameId: string): void {
    logGameFacadeGroup('realtime connect requested', {
      gameId,
      activeRealtimeGameId: this.activeRealtimeGameId,
      hasSubscription: Boolean(this.realtimeSubscription),
      connectionStatus: this.connectionStatusSignal()
    });

    if (this.activeRealtimeGameId === gameId && this.realtimeSubscription && this.visualRealtimeSubscription) {
      logGameFacadeGroup('realtime connect skipped', {
        gameId,
        reason: 'already subscribed for game'
      });
      return;
    }

    if (this.activeRealtimeGameId && this.activeRealtimeGameId !== gameId) {
      this.gameRealtime.leave(this.activeRealtimeGameId);
    }

    this.realtimeSubscription?.unsubscribe();
    this.visualRealtimeSubscription?.unsubscribe();
    this.connectionStatusSignal.set(
      this.connectionStatusSignal() === 'reconnecting' && this.activeRealtimeGameId === gameId
        ? 'reconnecting'
        : 'connecting'
    );
    this.activeRealtimeGameId = gameId;
    this.realtimeSubscription = this.gameRealtime.connect(gameId).subscribe({
      next: (envelope) => {
        this.connectionStatusSignal.set('connected');
        if (this.uiErrorSignal()?.kind === 'connection-lost') {
          this.uiErrorSignal.set(null);
        }
        this.handleRealtimeEnvelope(gameId, envelope);
      },
      error: () => {
        this.clearRemoteCardHover();
        this.clearRemoteSetupPreview();
        this.connectionStatusSignal.set('reconnecting');
        this.uiErrorSignal.set(
          createUiError(
            'connection-lost',
            this.languageService.t('GAME.CONNECTION_LOST'),
            true
          )
        );
        void this.hydrate(gameId, false);
      }
    });

    this.visualRealtimeSubscription = this.gameRealtime.watchVisualEvents(gameId).subscribe({
      next: (event) => this.handleVisualEvent(event),
      error: () => {
        this.clearRemoteCardHover();
        this.clearRemoteSetupPreview();
      }
    });
  }

  private async hydrate(gameId: string, showLoading: boolean): Promise<void> {
    if (showLoading) {
      this.isLoadingSignal.set(true);
    }

    if (this.uiErrorSignal()?.kind !== 'connection-lost') {
      this.uiErrorSignal.set(null);
    }

    try {
      await this.ensureCurrentUserLoaded();
      const { detail, events, history, chat } = await firstValueFrom(
        forkJoin({
          detail: this.gameApi.getGame(gameId),
          events: this.gameApi.getEvents(gameId),
          history: this.gameApi.getHistory(gameId),
          chat: this.gameApi.getChatHistory(gameId)
        })
      );
      const snapshot = await this.loadReadySnapshot(gameId);

      assertValidSnapshot(snapshot);
      const currentUser = this.storageService.currentUser();
      if (!currentUser) {
        throw new MissingCurrentUserError();
      }

      const localPlayerId = currentPlayerId(detail, snapshot, currentUser.id);
      if (!localPlayerId) {
        throw new MissingLocalParticipantError();
      }

      this.detailSignal.set(detail);
      this.snapshotSignal.set(snapshot);
      this.clearRemoteCardHover();
      this.clearRemoteSetupPreviewIfNeeded(snapshot.status);
      this.lastPublishedHoverKey = null;
      this.clearPendingStateSyncRequests(gameId, snapshot.stateVersion);
      const waitingFromVersionAfterHydrate = this.awaitingSnapshotFromVersionSignal();
      if (
        waitingFromVersionAfterHydrate !== null &&
        snapshot.stateVersion >= waitingFromVersionAfterHydrate
      ) {
        this.awaitingSnapshotFromVersionSignal.set(null);
      }
      this.eventFeedSignal.set(dedupeEvents(events));
      this.historySignal.set(history);
      this.chatMessagesSignal.set(chat);
      this.resetUiState();
      logGameFacadeGroup('hydrate result', {
        gameId,
        showLoading,
        detail,
        snapshot,
        localPlayerId,
        history,
        chat,
        currentUser,
        currentParticipantId: this.currentParticipantId(),
        board: this.board()
      });
    } catch (error) {
        if (showLoading) {
          this.detailSignal.set(null);
          this.snapshotSignal.set(null);
          this.eventFeedSignal.set([]);
          this.historySignal.set([]);
          this.chatMessagesSignal.set([]);
        }

        this.resetUiState();
        this.clearRemoteCardHover();
        this.clearRemoteSetupPreview();

        if (
          error instanceof InvalidGameSnapshotError ||
          error instanceof MissingCurrentUserError ||
          error instanceof MissingLocalParticipantError ||
          error instanceof SnapshotNotReadyError
        ) {
        this.uiErrorSignal.set(
          createUiError('invalid-snapshot', this.languageService.t(error.message), false)
        );
      } else {
        this.uiErrorSignal.set(
          createHttpUiError(
            error,
            this.languageService.t('GAME.ERRORS.INVALID_SNAPSHOT'),
            'load-failed',
            this.languageService.t.bind(this.languageService)
          )
        );
      }
    } finally {
      if (showLoading) {
        this.isLoadingSignal.set(false);
      }
    }
  }

  private handleRealtimeEnvelope(gameId: string, envelope: GameRealtimeEnvelope): void {
    logGameFacadeGroup('realtime envelope', {
      gameId,
      eventType: 'eventType' in envelope ? envelope.eventType : null,
      envelope
    });

    if (isGameStateSync(envelope)) {
      try {
        assertValidSnapshot(envelope.state);
      } catch (error) {
        this.resetUiState();
        this.uiErrorSignal.set(
          createUiError(
            'invalid-snapshot',
            this.languageService.t(
              readErrorMessage(error, this.languageService.t('GAME.ERRORS.INVALID_SNAPSHOT'))
            ),
            false
          )
        );
        return;
      }

      if (!this.isSnapshotReadyForBoardRender(envelope.state)) {
        logGameFacadeGroup('state sync skipped', {
          reason: 'snapshot not ready',
          incomingStateVersion: envelope.state.stateVersion,
          readiness: this.snapshotReadiness(envelope.state)
        });

        const currentSnapshot = this.snapshotSignal();
        if (!currentSnapshot || !this.isSnapshotReadyForBoardRender(currentSnapshot)) {
          this.snapshotSignal.set(envelope.state);
          this.clearRemoteCardHover();
          this.clearRemoteSetupPreviewIfNeeded(envelope.state.status);
        }

        void this.hydrate(gameId, false);
        return;
      }

      this.snapshotSignal.set(envelope.state);
      this.clearRemoteCardHover();
      this.clearRemoteSetupPreviewIfNeeded(envelope.state.status);
      this.lastPublishedHoverKey = null;
      this.clearPendingStateSyncRequests(gameId, envelope.state.stateVersion);
      this.resetUiState();
      const waitingFromVersion = this.awaitingSnapshotFromVersionSignal();
      if (waitingFromVersion !== null && envelope.state.stateVersion >= waitingFromVersion) {
        this.awaitingSnapshotFromVersionSignal.set(null);
        this.isActionPendingSignal.set(false);
      }
      logGameFacadeGroup('state sync ready applied', {
        incomingStateVersion: envelope.state.stateVersion,
        previousSnapshotVersion: null,
        waitingFromVersion,
        awaitingCleared: waitingFromVersion !== null && envelope.state.stateVersion >= waitingFromVersion
      });
      void this.refreshVisibleEvents(gameId);
      void this.refreshDetail(gameId);
      return;
    }

    if (isChatEvent(envelope)) {
      this.chatMessagesSignal.update((messages) => appendUniqueChat(messages, mapChatEvent(envelope)));
      return;
    }

    if (isPresenceEvent(envelope)) {
      this.applyPresenceEvent(gameId, envelope);
      return;
    }

    if (isGameEvent(envelope)) {
      const gameEvent = envelope as GameEvent;
      const currentSnapshot = this.snapshotSignal();
      this.eventFeedSignal.update((events) => appendUniqueEvent(events, gameEvent));

      if (currentSnapshot && gameEvent.stateVersion > currentSnapshot.stateVersion + 1) {
        this.resetUiState();
        void this.hydrate(gameId, false);
        return;
      }

      if (!currentSnapshot || gameEvent.stateVersion > currentSnapshot.stateVersion) {
        this.requestStateSyncOnce(gameId, gameEvent.stateVersion);
      }

      return;
    }

    this.resetUiState();
    this.clearRemoteCardHover();
    this.clearRemoteSetupPreview();
    void this.hydrate(gameId, false);
  }

  private handleVisualEvent(event: GameVisualEvent): void {
    if (event.gameId !== this.gameIdSignal()) {
      return;
    }

    if (event.playerId === this.currentUserId()) {
      if (event.type === 'SETUP_SLOT_CHANGED') {
        const gameId = this.gameIdSignal();
        if (gameId) {
          void this.hydrate(gameId, false);
        }
      }
      return;
    }

    if (event.type === 'SETUP_SLOT_CHANGED') {
      this.applyRemoteSetupPreview(event);
      const gameId = this.gameIdSignal();
      if (gameId) {
        void this.hydrate(gameId, false);
      }
      return;
    }

    if (event.type !== 'CARD_HOVER_CHANGED') {
      return;
    }

    const key = visualCardKey({
      owner: remoteOwnerFor(event.owner),
      zone: event.zone,
      visualIndex: event.visualIndex,
      cardInstanceId: event.cardInstanceId
    });

    if (!event.hovered) {
      if (key === null || this.remoteHoveredCardKeySignal() === key) {
        this.clearRemoteCardHover();
      }
      return;
    }

    this.remoteHoveredCardKeySignal.set(key);
    this.scheduleRemoteHoverClear();
  }

  private scheduleRemoteHoverClear(): void {
    this.clearRemoteHoverTimeout();
    this.remoteHoverTimeoutId = window.setTimeout(() => {
      this.clearRemoteCardHover();
    }, REMOTE_HOVER_CLEAR_MS);
  }

  private clearRemoteCardHover(): void {
    this.remoteHoveredCardKeySignal.set(null);
    this.clearRemoteHoverTimeout();
  }

  private applyRemoteSetupPreview(event: SetupSlotChangedEvent): void {
    if (event.zone !== 'ACTIVE' && event.zone !== 'BENCH') {
      return;
    }

    this.remoteSetupPreviewSignal.update((currentPreview) => {
      const previousOccupied = event.zone === 'ACTIVE'
        ? currentPreview.activeOccupied
        : typeof event.visualIndex === 'number' && event.visualIndex >= 0
          ? currentPreview.benchOccupiedIndexes.includes(event.visualIndex)
          : false;
      const nextBenchIndexes = new Set(currentPreview.benchOccupiedIndexes);
      let activeOccupied = currentPreview.activeOccupied;

      if (event.zone === 'ACTIVE') {
        activeOccupied = event.occupied;
      } else if (typeof event.visualIndex === 'number' && event.visualIndex >= 0) {
        if (event.occupied) {
          nextBenchIndexes.add(event.visualIndex);
        } else {
          nextBenchIndexes.delete(event.visualIndex);
        }
      }

      if (event.occupied && !previousOccupied) {
        this.animateRemoteSetupPlacement(event);
      }

      if (!event.occupied) {
        this.clearRemoteSetupHover(event);
      }

      return normalizeSetupPreview({
        activeOccupied,
        activeCardInstanceId: null,
        benchOccupiedIndexes: Array.from(nextBenchIndexes),
        benchCardInstanceIdsByIndex: {}
      });
    });
  }

  private clearRemoteSetupPreview(): void {
    this.remoteSetupPreviewSignal.set(EMPTY_SETUP_PREVIEW_STATE);
    this.clearRemoteCardHover();
  }

  private clearRemoteSetupPreviewIfNeeded(status: GameStatus): void {
    if (status === GameStatus.Setup) {
      return;
    }

    this.clearRemoteSetupPreview();
  }

  private animateRemoteSetupPlacement(event: SetupSlotChangedEvent): void {
    const toAnchor = event.zone === 'ACTIVE'
      ? 'opponent-active'
      : typeof event.visualIndex === 'number'
        ? `opponent-bench-${event.visualIndex}`
        : null;
    if (!toAnchor) {
      return;
    }

    this.boardAnimationService.enqueue([
      {
        type: 'OPPONENT_MOVE_CARD',
        fromAnchor: 'opponent-hand',
        toAnchor,
        targetPulseAnchor: toAnchor,
        cardLabel: this.languageService.t('GAME.HIDDEN_CARD'),
        durationMs: 640,
        hideLabel: true,
        useCardBack: true,
        travelRotationDeg: event.zone === 'ACTIVE' ? -4 : 4
      }
    ]);
  }

  private clearRemoteSetupHover(event: SetupSlotChangedEvent): void {
    const hoverKey = visualCardKey({
      owner: 'OPPONENT',
      zone: event.zone,
      visualIndex: event.visualIndex,
      cardInstanceId: null
    });
    if (hoverKey !== null && this.remoteHoveredCardKeySignal() === hoverKey) {
      this.clearRemoteCardHover();
    }
  }

  private clearRemoteHoverTimeout(): void {
    if (this.remoteHoverTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.remoteHoverTimeoutId);
    this.remoteHoverTimeoutId = null;
  }

  private publishSetupPreviewChanges(previousPreview: SetupPreviewState, nextPreview: SetupPreviewState): void {
    const gameId = this.gameIdSignal();
    const playerId = this.currentUserId();
    const snapshot = this.snapshotSignal();
    if (!gameId || !playerId || snapshot?.status !== GameStatus.Setup) {
      return;
    }

    if (
      previousPreview.activeOccupied !== nextPreview.activeOccupied
      || previousPreview.activeCardInstanceId !== nextPreview.activeCardInstanceId
    ) {
      this.gameRealtime.sendSetupSlotChanged(gameId, {
        type: 'SETUP_SLOT_CHANGED',
        gameId,
        playerId,
        zone: 'ACTIVE',
        owner: 'SELF',
        visualIndex: 0,
        occupied: nextPreview.activeOccupied,
        cardInstanceId: nextPreview.activeCardInstanceId ?? undefined
      });
    }

    const previousBenchIndexes = new Set(previousPreview.benchOccupiedIndexes);
    const nextBenchIndexes = new Set(nextPreview.benchOccupiedIndexes);
    const changedBenchIndexes = new Set<number>([
      ...previousBenchIndexes,
      ...nextBenchIndexes
    ]);

    for (const visualIndex of changedBenchIndexes) {
      const previousCardInstanceId = previousPreview.benchCardInstanceIdsByIndex[visualIndex];
      const nextCardInstanceId = nextPreview.benchCardInstanceIdsByIndex[visualIndex];
      if (
        previousBenchIndexes.has(visualIndex) === nextBenchIndexes.has(visualIndex)
        && previousCardInstanceId === nextCardInstanceId
      ) {
        continue;
      }

      this.gameRealtime.sendSetupSlotChanged(gameId, {
        type: 'SETUP_SLOT_CHANGED',
        gameId,
        playerId,
        zone: 'BENCH',
        owner: 'SELF',
        visualIndex,
        occupied: nextBenchIndexes.has(visualIndex),
        cardInstanceId: nextCardInstanceId
      });
    }
  }

  private projectRemoteSetupPreview(
    board: BoardGameViewModel,
    status: GameStatus
  ): BoardGameViewModel {
    if (status !== GameStatus.Setup) {
      return board;
    }

    const preview = this.remoteSetupPreviewSignal();
    if (
      !preview.activeOccupied &&
      preview.benchOccupiedIndexes.length === 0
    ) {
      return board;
    }

    return {
      ...board,
      rivalPlayer: {
        ...board.rivalPlayer,
        activePokemon: projectRemoteSetupPreviewSlot(
          board.rivalPlayer.activePokemon,
          preview.activeOccupied,
          `${board.rivalPlayer.id}-remote-setup-active`,
          this.languageService.t.bind(this.languageService)
        ),
        benchSlots: board.rivalPlayer.benchSlots.map((slot, index) =>
          projectRemoteSetupPreviewSlot(
            slot,
            preview.benchOccupiedIndexes.includes(index),
            `${board.rivalPlayer.id}-remote-setup-bench-${index + 1}`,
            this.languageService.t.bind(this.languageService)
          )
        )
      }
    };
  }

  private applyPresenceEvent(gameId: string, event: GameEvent): void {
    const userId = event.payload['userId'];
    const connected = event.payload['connected'];
    const lastSeenAt = event.payload['lastSeenAt'];

    logGameFacadeGroup('presence event', {
      gameId,
      userId,
      connected,
      lastSeenAt,
      payload: event.payload
    });

    if (typeof userId !== 'string' || typeof connected !== 'boolean') {
      void this.hydrate(gameId, false);
      return;
    }

    this.detailSignal.update((detail) => {
      if (!detail) {
        return detail;
      }

      return {
        ...detail,
        participants: detail.participants.map((participant) =>
          participant.userId === userId
            ? {
                ...participant,
                connected,
                lastSeenAt:
                  typeof event.payload['lastSeenAt'] === 'string' ? event.payload['lastSeenAt'] : null
              }
            : participant
        )
      };
    });
  }

  private async refreshDetail(gameId: string): Promise<void> {
    try {
      const detail = await firstValueFrom(this.gameApi.getGame(gameId));
      this.detailSignal.set(detail);
    } catch {
      this.uiErrorSignal.set(
        createUiError('unknown', this.languageService.t('GAME.ERRORS.UPDATE_PRESENCE'), true)
      );
    }
  }

  private async refreshVisibleEvents(gameId: string): Promise<void> {
    try {
      const events = await firstValueFrom(this.gameApi.getEvents(gameId));
      if (this.gameIdSignal() !== gameId) {
        return;
      }

      this.eventFeedSignal.update((currentEvents) => dedupeEvents([...currentEvents, ...events]));
    } catch (error) {
      logGameFacadeGroup('visible events refresh skipped', {
        gameId,
        error
      });
    }
  }

  private async ensureCurrentUserLoaded(): Promise<void> {
    if (this.storageService.currentUser()) {
      return;
    }

    if (!this.storageService.getAccessToken()) {
      throw new MissingCurrentUserError();
    }

    await firstValueFrom(this.authApi.getCurrentUser());
  }

  private async loadReadySnapshot(gameId: string): Promise<GameSnapshot> {
    for (let attempt = 1; attempt <= SNAPSHOT_READY_MAX_ATTEMPTS; attempt += 1) {
      const snapshot = await firstValueFrom(this.gameApi.getSnapshot(gameId));
      assertValidSnapshot(snapshot);

      logGameFacadeGroup('snapshot board readiness', {
        gameId,
        attempt,
        ...this.snapshotReadiness(snapshot)
      });

      if (this.isSnapshotReadyForBoardRender(snapshot)) {
        return snapshot;
      }

      if (attempt < SNAPSHOT_READY_MAX_ATTEMPTS) {
        await this.delay(SNAPSHOT_READY_RETRY_MS);
      }
    }

    throw new SnapshotNotReadyError();
  }

  private isSnapshotReadyForBoardRender(snapshot: GameSnapshot): boolean {
    const players = snapshot.board?.view?.players ?? [];
    if (players.length < 2) {
      return false;
    }

    return players.every((player) =>
      player.hand !== null &&
      player.deck !== null &&
      player.prize !== null &&
      typeof player.deck.count === 'number' &&
      typeof player.prize.count === 'number'
    );
  }

  private snapshotReadiness(snapshot: GameSnapshot): Record<string, unknown> {
    const players = snapshot.board?.view?.players ?? [];
    return {
      hasBoard: Boolean(snapshot.board),
      hasView: Boolean(snapshot.board?.view),
      playersCount: players.length,
      status: snapshot.status,
      stateVersion: snapshot.stateVersion,
      playerSummaries: players.map((player) => ({
        playerId: player.playerId,
        local: player.local,
        handCount: player.hand?.count ?? null,
        deckCount: player.deck?.count ?? null,
        prizeCount: player.prize?.count ?? null
      }))
    };
  }

  private delay(milliseconds: number): Promise<void> {
    return new Promise((resolve) => {
      window.setTimeout(resolve, milliseconds);
    });
  }

  private requestStateSyncOnce(gameId: string, stateVersion: number): void {
    const currentSnapshot = this.snapshotSignal();
    if (currentSnapshot?.gameId === gameId && currentSnapshot.stateVersion >= stateVersion) {
      return;
    }

    const requestKey = this.stateSyncRequestKey(gameId, stateVersion);
    if (this.pendingStateSyncRequests.has(requestKey)) {
      return;
    }

    this.pendingStateSyncRequests.add(requestKey);
    this.gameRealtime.requestStateSync(gameId);
  }

  private clearPendingStateSyncRequests(gameId: string, stateVersion: number): void {
    for (const requestKey of this.pendingStateSyncRequests) {
      const [requestGameId, requestStateVersion] = requestKey.split(':');
      if (requestGameId === gameId && Number(requestStateVersion) <= stateVersion) {
        this.pendingStateSyncRequests.delete(requestKey);
      }
    }
  }

  clearAwaitingSnapshotFallback(): void {
    this.awaitingSnapshotFromVersionSignal.set(null);
  }

  private stateSyncRequestKey(gameId: string, stateVersion: number): string {
    return `${gameId}:${stateVersion}`;
  }

  private async handleActionError(gameId: string, error: unknown): Promise<void> {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      this.resetUiState();
      await this.hydrate(gameId, false);
      this.uiErrorSignal.set(
        createUiError(
          'version-conflict',
          this.languageService.t('GAME.ERRORS.VERSION_SYNCED'),
          true,
          error
        )
      );
      return;
    }

    this.resetUiState();
    this.uiErrorSignal.set(
      createHttpUiError(
        error,
        this.languageService.t('GAME.ERRORS.ACTION_FAILED'),
        'unknown',
        this.languageService.t.bind(this.languageService)
      )
    );
  }
}

function projectRemoteSetupPreviewSlot(
  slot: BoardGameViewModel['rivalPlayer']['activePokemon'],
  occupied: boolean,
  cardId: string,
  t: (key: string, params?: Record<string, string | number>) => string
): BoardGameViewModel['rivalPlayer']['activePokemon'] {
  if (!occupied || slot.occupied) {
    return slot;
  }

  return {
    ...slot,
    occupied: true,
    card: {
      id: cardId,
      label: t('GAME.HIDDEN_CARD'),
      visibility: 'hidden',
      faceDown: true,
      rotation: 0
    },
    pokemon: null
  };
}
