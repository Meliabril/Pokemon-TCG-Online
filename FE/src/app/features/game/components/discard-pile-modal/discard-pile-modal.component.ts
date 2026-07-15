import { ChangeDetectionStrategy, Component, HostListener, computed, inject, input, output } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { BoardCardViewModel } from '../../domain/board/board-game-view-model.interface';

@Component({
  selector: 'app-discard-pile-modal',
  templateUrl: './discard-pile-modal.component.html',
  styleUrl: './discard-pile-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DiscardPileModalComponent {
  private readonly languageService = inject(LanguageService);
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly isOpen = input(false);
  readonly label = input<string | null>(null);
  readonly cards = input<BoardCardViewModel[]>([]);
  readonly closed = output<void>();

  readonly title = computed(() => this.label() ?? this.t('GAME.DISCARD_MODAL.TITLE'));

  close(): void {
    this.closed.emit();
  }

  cardImage(card: BoardCardViewModel): string | null {
    return card.imageLargeUrl ?? card.imageSmallUrl ?? null;
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    if (this.isOpen()) {
      this.close();
    }
  }
}
