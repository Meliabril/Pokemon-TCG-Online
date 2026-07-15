import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { GameToastService } from '../../services/game-toast.service';

@Component({
  selector: 'app-game-toast',
  templateUrl: './game-toast.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameToastComponent {
  private readonly toastService = inject(GameToastService);

  readonly toasts = this.toastService.toasts;

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}
