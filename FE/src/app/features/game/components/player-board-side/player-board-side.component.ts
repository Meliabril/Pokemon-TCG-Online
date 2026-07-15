import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { ActivePokemonSlotComponent } from '../active-pokemon-slot/active-pokemon-slot.component';
import { BenchRowComponent } from '../bench-row/bench-row.component';
import { CardZoneStackComponent } from '../card-zone-stack/card-zone-stack.component';
import { HandFanComponent } from '../hand-fan/hand-fan.component';
import { BoardPlayerViewModel } from '../../domain/board/board-game-view-model.interface';

@Component({
  selector: 'app-player-board-side',
  templateUrl: './player-board-side.component.html',
  styleUrl: './player-board-side.component.css',
  imports: [ActivePokemonSlotComponent, BenchRowComponent, CardZoneStackComponent, HandFanComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlayerBoardSideComponent {
  private readonly languageService = inject(LanguageService);
  readonly player = input.required<BoardPlayerViewModel>();
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
}
