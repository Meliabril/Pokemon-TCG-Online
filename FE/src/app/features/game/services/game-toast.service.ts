import { Injectable, signal } from '@angular/core';

export type GameToastTone = 'info' | 'warning' | 'error';

export interface GameToast {
  id: number;
  message: string;
  tone: GameToastTone;
}

const DEFAULT_DURATION_MS = 3200;

@Injectable({ providedIn: 'root' })
export class GameToastService {
  private readonly toastsSignal = signal<GameToast[]>([]);
  private nextToastId = 1;

  readonly toasts = this.toastsSignal.asReadonly();

  show(message: string, tone: GameToastTone = 'info', durationMs = DEFAULT_DURATION_MS): void {
    const id = this.nextToastId++;
    this.toastsSignal.update((toasts) => [...toasts, { id, message, tone }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  clear(): void {
    this.toastsSignal.set([]);
  }
}
