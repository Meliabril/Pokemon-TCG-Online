import { Injectable, inject } from '@angular/core';
import { ignoreElements, merge, Observable, tap } from 'rxjs';
import { GameChatMessageRequest } from '../../../core/models/interfaces/game/game-chat-message.interface';
import {
  CardHoverChangedEvent,
  GameVisualEvent,
  SetupSlotChangedEvent
} from '../../../core/models/interfaces/game/game-visual-event.interface';
import { GameRealtimeEnvelope } from '../../../core/models/interfaces/matchmaking/matchmaking-realtime.interface';
import { StompRealtimeService } from '../../../core/services/stomp-realtime.service';

@Injectable({ providedIn: 'root' })
export class GameRealtimeService {
  private readonly stompRealtimeService = inject(StompRealtimeService);

  connect(gameId: string): Observable<GameRealtimeEnvelope> {
    const privateMessages$ = this.stompRealtimeService.watch<GameRealtimeEnvelope>(
      `/user/queue/games/${gameId}`
    );
    const publicMessages$ = this.stompRealtimeService.watch<GameRealtimeEnvelope>(
      `/topic/games/${gameId}`
    );
    const stateSyncRequests$ = this.stompRealtimeService.connected$.pipe(
      tap(() => {
        this.stompRealtimeService.publish(`/app/games/${gameId}/state-sync`);
      }),
      ignoreElements()
    );

    return merge(privateMessages$, publicMessages$, stateSyncRequests$);
  }

  requestStateSync(gameId: string): void {
    this.stompRealtimeService.publish(`/app/games/${gameId}/state-sync`);
  }

  watchVisualEvents(gameId: string): Observable<GameVisualEvent> {
    return this.stompRealtimeService.watch<GameVisualEvent>(
      `/topic/games/${gameId}/visual-events`
    );
  }

  sendCardHoverChanged(gameId: string, event: CardHoverChangedEvent): void {
    this.stompRealtimeService.publish(`/app/games/${gameId}/visual-events`, event);
  }

  sendSetupSlotChanged(gameId: string, event: SetupSlotChangedEvent): void {
    this.stompRealtimeService.publish(`/app/games/${gameId}/visual-events`, event);
  }

  sendChatMessage(gameId: string, request: GameChatMessageRequest): void {
    this.stompRealtimeService.publish(`/app/games/${gameId}/chat`, request);
  }

  leave(gameId: string): void {
    if (!this.stompRealtimeService.isConnected()) {
      return;
    }

    this.stompRealtimeService.publish(`/app/games/${gameId}/leave`);
  }
}
