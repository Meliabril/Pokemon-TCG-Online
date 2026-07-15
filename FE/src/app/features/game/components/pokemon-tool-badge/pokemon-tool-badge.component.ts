import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';

export type PokemonToolBadgeVariant = 'active' | 'bench';
export type PokemonToolBadgeType = 'attack' | 'defense';

@Component({
  selector: 'app-pokemon-tool-badge',
  templateUrl: './pokemon-tool-badge.component.html',
  styleUrl: './pokemon-tool-badge.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PokemonToolBadgeComponent {
  private readonly languageService = inject(LanguageService);

  readonly type = input<PokemonToolBadgeType>('attack');
  readonly value = input(20);
  readonly variant = input<PokemonToolBadgeVariant>('active');

  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
}
