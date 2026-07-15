import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MatchmakingQueueStatus } from '../../../core/models/interfaces/matchmaking/matchmaking-queue-status.interface';
import { MatchFoundMessage } from '../../../core/models/interfaces/matchmaking/matchmaking-realtime.interface';
import { StompRealtimeService } from '../../../core/services/stomp-realtime.service';

export type MatchmakingRealtimeMessage = MatchmakingQueueStatus | MatchFoundMessage;

@Injectable({ providedIn: 'root' })
export class MatchmakingRealtimeService {
  private readonly stompRealtimeService = inject(StompRealtimeService);

  readonly destinations = {
    matchmaking: '/user/queue/matchmaking',
    privateGame: (gameId: string) => `/user/queue/games/${gameId}`,
    stateSync: (gameId: string) => `/app/games/${gameId}/state-sync`
  } as const;

  watchMatchmaking(): Observable<MatchmakingRealtimeMessage> {
    return this.stompRealtimeService.watch<MatchmakingRealtimeMessage>(this.destinations.matchmaking);
  }

  disconnect(): void {
    this.stompRealtimeService.disconnect();
  }
}
