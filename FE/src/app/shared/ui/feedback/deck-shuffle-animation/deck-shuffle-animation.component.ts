import { ChangeDetectionStrategy, Component, input } from '@angular/core';

const SHUFFLE_CARD_COUNT = 7;

@Component({
  selector: 'app-deck-shuffle-animation',
  templateUrl: './deck-shuffle-animation.component.html',
  styleUrl: './deck-shuffle-animation.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckShuffleAnimationComponent {
  readonly label = input<string | null>(null);

  readonly cardIndexes = Array.from({ length: SHUFFLE_CARD_COUNT }, (_, index) => index);
}
