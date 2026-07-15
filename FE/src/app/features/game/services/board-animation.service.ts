import { Injectable, signal } from '@angular/core';
import { BoardAnimation, BoardAnimationCommand } from '../domain/animations/board-animation.types';

@Injectable({ providedIn: 'root' })
export class BoardAnimationService {
  private readonly queue: BoardAnimation[] = [];
  private readonly activeAnimationSignal = signal<BoardAnimation | null>(null);
  private readonly isAnimatingSignal = signal(false);
  private readonly animatingPlayerIdsSignal = signal<ReadonlySet<string>>(new Set());
  private readonly heldDamagePokemonIdsSignal = signal<ReadonlySet<string>>(new Set());
  private nextAnimationId = 1;

  readonly activeAnimation = this.activeAnimationSignal.asReadonly();
  /** True while an animation is playing or the queue still has pending animations. */
  readonly isAnimating = this.isAnimatingSignal.asReadonly();
  /** Players that currently have an active or queued animation tagged with their `affectedPlayerId`. */
  readonly animatingPlayerIds = this.animatingPlayerIdsSignal.asReadonly();
  readonly heldDamagePokemonIds = this.heldDamagePokemonIdsSignal.asReadonly();

  enqueue(commands: BoardAnimationCommand[]): void {
    if (commands.length === 0) {
      return;
    }

    this.queue.push(
      ...commands.map((command) => ({
        ...command,
        id: this.nextAnimationId++
      }))
    );
    this.playNextIfIdle();
    this.syncAnimatingState();
  }

  complete(animationId: number): void {
    if (this.activeAnimationSignal()?.id !== animationId) {
      return;
    }

    this.activeAnimationSignal.set(null);
    this.playNextIfIdle();
    this.syncAnimatingState();
  }

  clear(): void {
    this.queue.length = 0;
    this.activeAnimationSignal.set(null);
    this.heldDamagePokemonIdsSignal.set(new Set());
    this.syncAnimatingState();
  }

  holdDamage(pokemonInPlayId: string | undefined): void {
    if (!pokemonInPlayId) {
      return;
    }

    this.heldDamagePokemonIdsSignal.update((held) => new Set(held).add(pokemonInPlayId));
  }

  releaseDamage(pokemonInPlayId: string | undefined): void {
    if (!pokemonInPlayId || !this.heldDamagePokemonIdsSignal().has(pokemonInPlayId)) {
      return;
    }

    this.heldDamagePokemonIdsSignal.update((held) => {
      const next = new Set(held);
      next.delete(pokemonInPlayId);
      return next;
    });
  }

  private playNextIfIdle(): void {
    if (this.activeAnimationSignal() || this.queue.length === 0) {
      return;
    }

    this.activeAnimationSignal.set(this.queue.shift() ?? null);
  }

  private syncAnimatingState(): void {
    this.isAnimatingSignal.set(this.activeAnimationSignal() !== null || this.queue.length > 0);

    const animatingPlayerIds = new Set<string>();
    const active = this.activeAnimationSignal();
    if (active?.affectedPlayerId) {
      animatingPlayerIds.add(active.affectedPlayerId);
    }
    for (const animation of this.queue) {
      if (animation.affectedPlayerId) {
        animatingPlayerIds.add(animation.affectedPlayerId);
      }
    }
    this.animatingPlayerIdsSignal.set(animatingPlayerIds);
  }
}
