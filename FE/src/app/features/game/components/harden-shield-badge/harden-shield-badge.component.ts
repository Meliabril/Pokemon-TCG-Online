import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';

export type HardenShieldBadgeVariant = 'active' | 'bench';

/**
 * Purely cosmetic damage-prevention shield badge (Harden). Renders nothing when `active` is
 * false. Never computes or infers whether the underlying rule is active — the caller (an
 * `active-pokemon-slot` or `bench-row` cell) decides that from the backend-provided
 * `visualEffects` field on the board snapshot, never locally.
 */
@Component({
  selector: 'app-harden-shield-badge',
  templateUrl: './harden-shield-badge.component.html',
  styleUrl: './harden-shield-badge.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HardenShieldBadgeComponent {
  private readonly languageService = inject(LanguageService);

  readonly active = input(false);
  readonly variant = input<HardenShieldBadgeVariant>('active');
  readonly value = input<number | null>(null);

  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
}
