import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { BoardEventFeedItemViewModel } from '../../domain/board/board-game-view-model.interface';

const TECHNICAL_HISTORY_PATTERNS = [
  'phase changed',
  'phase change',
  'cambio de fase',
  'fase cambiada',
  'sincronizado',
  'sync',
  'version',
  'websocket',
  'state updated',
  'state sync',
  'state_sync',
  'evento visible',
  'visible event'
];

@Component({
  selector: 'app-game-log-feed',
  templateUrl: './game-log-feed.component.html',
  styleUrl: './game-log-feed.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameLogFeedComponent {
  readonly eventFeed = input.required<BoardEventFeedItemViewModel[]>();
  readonly historySummary = input.required<string>();
  readonly latestActionLabel = input.required<string>();
  readonly emptyLabel = input.required<string>();
  readonly visibleEventFeed = computed(() =>
    this.eventFeed().filter((event) => !hasTechnicalHistoryText(`${event.eventType} ${event.label}`))
  );
}

function hasTechnicalHistoryText(text: string): boolean {
  const normalizedText = text.toLowerCase();
  return TECHNICAL_HISTORY_PATTERNS.some((pattern) => normalizedText.includes(pattern));
}
