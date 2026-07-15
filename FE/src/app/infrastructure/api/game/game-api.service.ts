import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { GameActionType } from '../../../core/models/enums/game/game-action-type.enum';
import {
  GameActionLogEntry,
  GameActionRequest,
  GameActionResponse
} from '../../../core/models/interfaces/game/game-action.interface';
import { GameChatMessage } from '../../../core/models/interfaces/game/game-chat-message.interface';
import { GameEvent } from '../../../core/models/interfaces/game/game-event.interface';
import { GameSnapshot } from '../../../core/models/interfaces/game/game-snapshot.interface';
import { GameDetail, GameState, PauseGameRequest } from '../../../core/models/interfaces/game/game-state.interface';

type DeclareAttackRequest = Extract<GameActionRequest, { actionType: GameActionType.DeclareAttack }>;

@Injectable({ providedIn: 'root' })
export class GameApiService {
  private readonly http = inject(HttpClient);

  getGame(gameId: string): Observable<GameDetail> {
    return this.http.get<GameDetail>(this.buildUrl(API_ENDPOINTS.games.detail(gameId)));
  }

  executeAction<TData = unknown>(
    gameId: string,
    request: GameActionRequest
  ): Observable<GameActionResponse<TData>> {
    return this.http.post<GameActionResponse<TData>>(
      this.buildUrl(API_ENDPOINTS.games.actions(gameId)),
      request
    );
  }

  declareAttack<TData = unknown>(
    gameId: string,
    request: DeclareAttackRequest
  ): Observable<GameActionResponse<TData>> {
    return this.executeAction<TData>(gameId, request);
  }

  pauseGame(gameId: string, request: PauseGameRequest = {}): Observable<GameState> {
    return this.http.post<GameState>(this.buildUrl(API_ENDPOINTS.games.pause(gameId)), request);
  }

  resumeGame(gameId: string): Observable<GameState> {
    return this.http.post<GameState>(this.buildUrl(API_ENDPOINTS.games.resume(gameId)), {});
  }

  getHistory(gameId: string): Observable<GameActionLogEntry[]> {
    return this.http.get<GameActionLogEntry[]>(this.buildUrl(API_ENDPOINTS.games.history(gameId)));
  }

  getEvents(gameId: string): Observable<GameEvent[]> {
    return this.http.get<GameEvent[]>(this.buildUrl(API_ENDPOINTS.games.events(gameId)));
  }

  getSnapshot(gameId: string): Observable<GameSnapshot> {
    return this.http.get<GameSnapshot>(this.buildUrl(API_ENDPOINTS.games.latestSnapshot(gameId)));
  }

  getLatestSnapshot(gameId: string): Observable<GameState> {
    return this.http.get<GameState>(this.buildUrl(API_ENDPOINTS.games.latestSnapshot(gameId)));
  }

  getChatHistory(gameId: string): Observable<GameChatMessage[]> {
    return this.http.get<GameChatMessage[]>(this.buildUrl(API_ENDPOINTS.games.chat(gameId)));
  }

  previewTrainer(gameId: string, payload: Record<string, unknown> = {}): Observable<Record<string, unknown>> {
    return this.http.post<Record<string, unknown>>(
      this.buildUrl(API_ENDPOINTS.games.trainerPreview(gameId)),
      payload,
      { withCredentials: true }
    );
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
