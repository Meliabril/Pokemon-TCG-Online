import { inject, Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { APP_CONFIG } from '../../core/constants/app/app.constants';
import { StorageService } from '../../core/storage/storage.service';
import { LanguageService } from '../../core/services/language.service';

type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'error';

interface RegisteredSubscription<T> {
  destination: string;
  handler: (payload: T) => void;
  activeSubscription: StompSubscription | null;
}

@Injectable({ providedIn: 'root' })
export class GameWebSocketService {
  private readonly storageService = inject(StorageService);
  private readonly languageService = inject(LanguageService);

  private client: Client | null = null;
  private connectionAttempt: Promise<void> | null = null;
  private readonly subscriptions = new Map<string, RegisteredSubscription<unknown>>();
  private subscriptionSequence = 0;

  readonly connectionStatus = signal<ConnectionStatus>('idle');
  readonly lastError = signal('');

  async ensureConnected(): Promise<void> {
    if (this.client?.connected) {
      return;
    }

    if (this.connectionAttempt) {
      return this.connectionAttempt;
    }

    const accessToken = this.storageService.getAccessToken();
    if (!accessToken) {
      throw new Error(this.languageService.t('WEBSOCKET.NO_TOKEN'));
    }

    this.connectionStatus.set('connecting');
    this.lastError.set('');

    const client = this.ensureClient();
    this.connectionAttempt = new Promise<void>((resolve, reject) => {
      let settled = false;

      client.beforeConnect = async () => {
        const refreshedToken = this.storageService.getAccessToken();
        if (!refreshedToken) {
          throw new Error(this.languageService.t('WEBSOCKET.SESSION_TOKEN_MISSING'));
        }

        client.connectHeaders = {
          Authorization: `Bearer ${refreshedToken}`
        };
      };

      client.onConnect = () => {
        this.connectionStatus.set('connected');
        this.lastError.set('');
        this.connectionAttempt = null;
        this.resubscribeAll();
        if (!settled) {
          settled = true;
          resolve();
        }
      };

      client.onWebSocketClose = () => {
        this.connectionStatus.set('disconnected');
        this.clearActiveSubscriptions();
        if (!settled) {
          settled = true;
          this.connectionAttempt = null;
          reject(new Error(this.languageService.t('WEBSOCKET.CLOSED_BEFORE_CONNECT')));
        }
      };

      client.onWebSocketError = () => {
        this.connectionStatus.set('error');
        this.lastError.set(this.languageService.t('WEBSOCKET.CHANNEL_FAILED'));
        if (!settled) {
          settled = true;
          this.connectionAttempt = null;
          reject(new Error(this.lastError()));
        }
      };

      client.onStompError = (frame) => {
        this.connectionStatus.set('error');
        this.lastError.set(frame.headers['message'] ?? this.languageService.t('WEBSOCKET.BROKER_ERROR'));
        if (!settled) {
          settled = true;
          this.connectionAttempt = null;
          reject(new Error(this.lastError()));
        }
      };
    });

    client.activate();
    return this.connectionAttempt;
  }

  subscribe<T>(destination: string, handler: (payload: T) => void): () => void {
    const key = `${destination}#${this.subscriptionSequence++}`;
    this.subscriptions.set(key, {
      destination,
      handler: handler as (payload: unknown) => void,
      activeSubscription: null
    });

    if (this.client?.connected) {
      this.activateSubscription(key);
    }

    return () => {
      const registered = this.subscriptions.get(key);
      registered?.activeSubscription?.unsubscribe();
      this.subscriptions.delete(key);
    };
  }

  async publish(destination: string, payload: Record<string, unknown> = {}): Promise<void> {
    await this.ensureConnected();
    this.client?.publish({
      destination,
      body: JSON.stringify(payload)
    });
  }

  reset(): void {
    this.clearActiveSubscriptions();
    this.subscriptions.clear();
    this.connectionAttempt = null;
    this.lastError.set('');

    if (this.client?.active) {
      this.client.deactivate();
    }

    this.client = null;
    this.connectionStatus.set('idle');
  }

  private ensureClient(): Client {
    if (this.client) {
      return this.client;
    }

    this.client = new Client({
      brokerURL: APP_CONFIG.wsBaseUrl,
      reconnectDelay: 2_500
    });

    return this.client;
  }

  private resubscribeAll(): void {
    for (const key of this.subscriptions.keys()) {
      this.activateSubscription(key);
    }
  }

  private clearActiveSubscriptions(): void {
    for (const registered of this.subscriptions.values()) {
      registered.activeSubscription = null;
    }
  }

  private activateSubscription(key: string): void {
    const registered = this.subscriptions.get(key);
    if (!registered || !this.client?.connected) {
      return;
    }

    registered.activeSubscription?.unsubscribe();
    registered.activeSubscription = this.client.subscribe(registered.destination, (message) => {
      registered.handler(this.parseMessage(message));
    });
  }

  private parseMessage(message: IMessage): unknown {
    if (!message.body) {
      return {};
    }

    try {
      return JSON.parse(message.body) as unknown;
    } catch {
      this.lastError.set(this.languageService.t('WEBSOCKET.INVALID_MESSAGE'));
      return {};
    }
  }
}
