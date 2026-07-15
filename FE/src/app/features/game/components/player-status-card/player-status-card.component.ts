import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { LanguageService, TranslationParams } from '../../../../core/services/language.service';
import { BoardPlayerViewModel } from '../../domain/board/board-game-view-model.interface';

export type PlayerStatusCardSide = 'current' | 'opponent';

@Component({
  selector: 'app-player-status-card',
  templateUrl: './player-status-card.component.html',
  styleUrl: './player-status-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlayerStatusCardComponent {
  private readonly languageService = inject(LanguageService);

  readonly player = input.required<BoardPlayerViewModel>();
  readonly fieldLabel = input.required<string>();
  readonly showChat = input(false);
  readonly showTurnBadge = input(true);
  readonly side = input<PlayerStatusCardSide>('current');
  readonly openChat = output<void>();
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);
}
