import { Injectable, inject } from '@angular/core';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { APP_CONFIG } from '../constants/app/app.constants';
import { StorageService } from '../storage/storage.service';

interface DestinationSubscription<T> {
  readonly subject: Subject<T>;
  activeSubscriptions: number;
  stompSubscription: StompSubscription | null;
}

@Injectable({ providedIn: 'root' })
export class StompRealtimeService {
  private readonly storageService = inject(StorageService);
  private readonly connectionCycleSignal = new ReplaySubject<number>(1);
  private readonly subscriptions = new Map<string, DestinationSubscription<unknown>>();
  private readonly client = this.createClient();
  private connectionCycle = 0;

  readonly connected$ = this.connectionCycleSignal.asObservable();

  watch<T>(destination: string): Observable<T> {
    return new Observable<T>((observer) => {
      const subscription = this.getOrCreateSubscription<T>(destination);
      subscription.activeSubscriptions += 1;
      this.ensureActivated();
      this.ensureDestinationSubscription(destination, subscription);

      const innerSubscription = subscription.subject.subscribe(observer);

      return () => {
        innerSubscription.unsubscribe();
        subscription.activeSubscriptions -= 1;
        if (subscription.activeSubscriptions === 0) {
          subscription.stompSubscription?.unsubscribe();
          subscription.subject.complete();
          this.subscriptions.delete(destination);
        }
      };
    });
  }

  publish(destination: string, body?: unknown): void {
    if (!this.client.connected) {
      this.ensureActivated();
      return;
    }

    this.client.publish({
      destination,
      body: body === undefined ? '{}' : JSON.stringify(body)
    });
  }

  isConnected(): boolean {
    return this.client.connected;
  }

  disconnect(): void {
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  private createClient(): Client {
    return new Client({
      brokerURL: APP_CONFIG.wsBaseUrl,
      reconnectDelay: 2000,
      connectHeaders: this.buildConnectHeaders(),
      beforeConnect: async () => {
        this.client.connectHeaders = this.buildConnectHeaders();
      },
      onConnect: () => {
        this.connectionCycle += 1;

        for (const [destination, subscription] of this.subscriptions.entries()) {
          this.ensureDestinationSubscription(destination, subscription, true);
        }

        this.connectionCycleSignal.next(this.connectionCycle);
      }
    });
  }

  private buildConnectHeaders(): Record<string, string> {
    const accessToken = this.storageService.getAccessToken();
    return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
  }

  private ensureActivated(): void {
    if (!this.client.active) {
      this.client.activate();
    }
  }

  private getOrCreateSubscription<T>(destination: string): DestinationSubscription<T> {
    const existing = this.subscriptions.get(destination) as DestinationSubscription<T> | undefined;
    if (existing) {
      return existing;
    }

    const created: DestinationSubscription<T> = {
      subject: new Subject<T>(),
      activeSubscriptions: 0,
      stompSubscription: null
    };
    this.subscriptions.set(destination, created as DestinationSubscription<unknown>);
    return created;
  }

  private ensureDestinationSubscription<T>(
    destination: string,
    subscription: DestinationSubscription<T>,
    forceResubscribe = false
  ): void {
    if (!this.client.connected) {
      return;
    }

    if (forceResubscribe) {
      subscription.stompSubscription?.unsubscribe();
      subscription.stompSubscription = null;
    }

    if (subscription.stompSubscription) {
      return;
    }

    subscription.stompSubscription = this.client.subscribe(destination, (message) => {
      subscription.subject.next(this.parseMessage<T>(message));
    });
  }

  private parseMessage<T>(message: IMessage): T {
    return JSON.parse(message.body) as T;
  }
}
