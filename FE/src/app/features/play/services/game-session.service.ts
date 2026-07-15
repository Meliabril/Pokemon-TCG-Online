import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { GameChatContext } from '../../../core/models/enums/game/game-chat-context.enum';
import { GameEventType } from '../../../core/models/enums/game/game-event-type.enum';
import {
  GameChatMessage,
  GameChatMessageRequest
} from '../../../core/models/interfaces/game/game-chat-message.interface';
import { GameEvent } from '../../../core/models/interfaces/game/game-event.interface';
import { GameDetail, GameState, GameStateSync } from '../../../core/models/interfaces/game/game-state.interface';
import { MatchFoundMessage } from '../../../core/models/interfaces/matchmaking/matchmaking-realtime.interface';
import { StorageService } from '../../../core/storage/storage.service';
import { LanguageService } from '../../../core/services/language.service';
import { GameApiService } from '../../../infrastructure/api/game/game-api.service';
import { GameWebSocketService } from '../../../infrastructure/websocket/game-websocket.service';

interface GameRealtimeSnapshot {
  gameId: string | null;
  detail: GameDetail | null;
  state: GameState | null;
  publicEvents: GameEvent[];
  chatMessages: GameChatMessage[];
  loading: boolean;
  errorMessage: string;
}

function createRealtimeSnapshot(): GameRealtimeSnapshot {
  return {
    gameId: null,
    detail: null,
    state: null,
    publicEvents: [],
    chatMessages: [],
    loading: false,
    errorMessage: ''
  };
}

function isMatchFoundMessage(value: unknown): value is MatchFoundMessage {
  if (!value || typeof value !== "object") {
    return false;
  }

  const payload = value as Record<string, unknown>;
  return (
    typeof payload['opponentUserId'] === 'string' &&
    typeof payload['gameId'] === 'string' &&
    typeof payload['matchedAt'] === 'string'
  );
}

function isGameEvent(value: unknown): value is GameEvent {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const payload = value as Record<string, unknown>;
  return (
    typeof payload['eventId'] === 'string' &&
    typeof payload['gameId'] === 'string' &&
    typeof payload['eventType'] === 'string' &&
    typeof payload['stateVersion'] === 'number' &&
    typeof payload['privateEvent'] === 'boolean' &&
    typeof payload['occurredAt'] === 'string' &&
    typeof payload['payload'] === 'object'
  );
}

function isGameStateSync(value: unknown): value is GameStateSync {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const payload = value as Record<string, unknown>;
  return (
    payload['eventType'] === GameEventType.StateSync &&
    typeof payload['gameId'] === 'string' &&
    typeof payload['stateVersion'] === 'number' &&
    typeof payload['state'] === 'object'
  );
}

@Injectable({ providedIn: 'root' })
export class GameSessionService {
  private readonly gameApi = inject(GameApiService);
  private readonly gameWebSocket = inject(GameWebSocketService);
  private readonly storageService = inject(StorageService);
  private readonly languageService = inject(LanguageService);

  private readonly snapshotSignal = signal<GameRealtimeSnapshot>(createRealtimeSnapshot());
  private readonly latestMatchSignal = signal<MatchFoundMessage | null>(null);

  private matchmakingUnsubscribe: (() => void) | null = null;
  private publicGameUnsubscribe: (() => void) | null = null;
  private privateGameUnsubscribe: (() => void) | null = null;
  private subscribedGameId: string | null = null;

  readonly connectionStatus = this.gameWebSocket.connectionStatus.asReadonly();
  readonly connectionError = this.gameWebSocket.lastError.asReadonly();
  readonly currentUser = this.storageService.currentUser;
  readonly latestMatch = this.latestMatchSignal.asReadonly();
  readonly snapshot = this.snapshotSignal.asReadonly();
  readonly gameId = computed(() => this.snapshotSignal().gameId);
  readonly detail = computed(() => this.snapshotSignal().detail);
  readonly state = computed(() => this.snapshotSignal().state);
  readonly chatMessages = computed(() => this.snapshotSignal().chatMessages);
  readonly publicEvents = computed(() => this.snapshotSignal().publicEvents);
  readonly loading = computed(() => this.snapshotSignal().loading);
  readonly errorMessage = computed(() => this.snapshotSignal().errorMessage);

  async ensureMatchmakingStream(): Promise<void> {
    if (this.matchmakingUnsubscribe) {
      return;
    }

    await this.gameWebSocket.ensureConnected();
    this.matchmakingUnsubscribe = this.gameWebSocket.subscribe<unknown>(
      '/user/queue/matchmaking',
      (message) => {
        if (isMatchFoundMessage(message)) {
          this.latestMatchSignal.set(message);
        }
      }
    );
  }

  async enterGame(gameId: string): Promise<void> {
    if (this.subscribedGameId !== gameId) {
      this.publicGameUnsubscribe?.();
      this.privateGameUnsubscribe?.();
      this.subscribedGameId = null;
    }

    this.snapshotSignal.update((snapshot) => ({
      ...snapshot,
      gameId,
      loading: true,
      errorMessage: ''
    }));

    try {
      const [detail, chatMessages, state] = await Promise.all([
        firstValueFrom(this.gameApi.getGame(gameId)),
        firstValueFrom(this.gameApi.getChatHistory(gameId)),
        this.loadLatestSnapshot(gameId)
      ]);

      this.snapshotSignal.set({
        gameId,
        detail,
        state,
        publicEvents: [],
        chatMessages,
        loading: false,
        errorMessage: ''
      });

      await this.gameWebSocket.ensureConnected();
      this.subscribeToGameChannels(gameId);
      await this.requestStateSync(gameId);
    } catch (error) {
      this.snapshotSignal.update((snapshot) => ({
        ...snapshot,
        loading: false,
        errorMessage: this.readBootstrapError(error)
      }));
      throw error;
    }
  }

  async requestStateSync(gameId = this.snapshotSignal().gameId): Promise<void> {
    if (!gameId) {
      return;
    }

    await this.gameWebSocket.publish(`/app/games/${gameId}/state-sync`);
  }

  async sendChatMessage(request: GameChatMessageRequest): Promise<void> {
    const gameId = this.snapshotSignal().gameId;
    if (!gameId) {
      throw new Error(this.languageService.t('GAME.ERRORS.NO_CHAT_GAME'));
    }

    await this.gameWebSocket.publish(`/app/games/${gameId}/chat`, {
      context: request.context,
      content: request.content
    });
  }

  clearLatestMatch(): void {
    this.latestMatchSignal.set(null);
  }

  clearGameSession(): void {
    this.publicGameUnsubscribe?.();
    this.privateGameUnsubscribe?.();
    this.publicGameUnsubscribe = null;
    this.privateGameUnsubscribe = null;
    this.subscribedGameId = null;
    this.snapshotSignal.set(createRealtimeSnapshot());
  }

  private subscribeToGameChannels(gameId: string): void {
    if (this.subscribedGameId === gameId) {
      return;
    }

    this.publicGameUnsubscribe = this.gameWebSocket.subscribe<unknown>(
      `/topic/games/${gameId}`,
      (message) => {
        if (!isGameEvent(message)) {
          return;
        }

        if (message.eventType === GameEventType.ChatMessage) {
          this.appendChatMessage(this.mapChatEvent(message));
          return;
        }

        this.snapshotSignal.update((snapshot) => ({
          ...snapshot,
          publicEvents: appendUniqueEvent(snapshot.publicEvents, message)
        }));
      }
    );

    this.privateGameUnsubscribe = this.gameWebSocket.subscribe<unknown>(
      `/user/queue/games/${gameId}`,
      (message) => {
        if (isGameStateSync(message)) {
          this.snapshotSignal.update((snapshot) => ({
            ...snapshot,
            state: message.state
          }));
          return;
        }

        if (!isGameEvent(message)) {
          return;
        }

        if (message.eventType === GameEventType.ChatMessage) {
          this.appendChatMessage(this.mapChatEvent(message));
        }
      }
    );

    this.subscribedGameId = gameId;
  }

  private appendChatMessage(message: GameChatMessage): void {
    this.snapshotSignal.update((snapshot) => ({
      ...snapshot,
      chatMessages: appendUniqueChat(snapshot.chatMessages, message)
    }));
  }

  private mapChatEvent(event: GameEvent): GameChatMessage {
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

  private async loadLatestSnapshot(gameId: string): Promise<GameState | null> {
    try {
      return await firstValueFrom(this.gameApi.getLatestSnapshot(gameId));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        return null;
      }

      this.snapshotSignal.update((snapshot) => ({
        ...snapshot,
        errorMessage: this.languageService.t('GAME.ERRORS.LATEST_SNAPSHOT')
      }));
      return null;
    }
  }

  private readBootstrapError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 403) {
        return this.languageService.t('GAME.ERRORS.GAME_FORBIDDEN');
      }

      if (error.status === 404) {
        return this.languageService.t('GAME.ERRORS.GAME_NOT_FOUND');
      }
    }

    return this.languageService.t('GAME.ERRORS.BOOTSTRAP_FAILED');
  }
}

function appendUniqueEvent(events: GameEvent[], nextEvent: GameEvent): GameEvent[] {
  if (events.some((event) => event.eventId === nextEvent.eventId)) {
    return events;
  }

  return [...events, nextEvent];
}

function appendUniqueChat(messages: GameChatMessage[], nextMessage: GameChatMessage): GameChatMessage[] {
  if (messages.some((message) => message.messageId === nextMessage.messageId)) {
    return messages;
  }

  return [...messages, nextMessage].sort((left, right) => left.sentAt.localeCompare(right.sentAt));
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
