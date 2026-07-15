import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { GameChatContext } from '../../../../core/models/enums/game/game-chat-context.enum';
import { GameChatMessage } from '../../../../core/models/interfaces/game/game-chat-message.interface';
import { LanguageService } from '../../../../core/services/language.service';

const MAX_CHAT_MESSAGE_LENGTH = 500;

@Component({
  selector: 'app-game-chat-drawer',
  templateUrl: './game-chat-drawer.component.html',
  styleUrl: './game-chat-drawer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameChatDrawerComponent {
  private readonly languageService = inject(LanguageService);

  readonly messages = input<GameChatMessage[]>([]);
  readonly currentUserId = input<string | null>(null);
  readonly connected = input(false);
  readonly open = input(false);
  readonly openChange = output<boolean>();
  readonly sendMessage = output<string>();
  readonly draft = signal('');
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly gameMessages = computed(() =>
    this.messages().filter((message) => message.context === GameChatContext.Game)
  );
  readonly canSend = computed(() => {
    const content = this.draft().trim();
    return this.connected() && content.length > 0 && content.length <= MAX_CHAT_MESSAGE_LENGTH;
  });

  close(): void {
    this.openChange.emit(false);
  }

  updateDraft(event: Event): void {
    if (!(event.target instanceof HTMLTextAreaElement)) {
      return;
    }

    this.draft.set(event.target.value.slice(0, MAX_CHAT_MESSAGE_LENGTH));
  }

  handleComposerEnter(event: Event): void {
    if (!(event instanceof KeyboardEvent) || event.shiftKey || event.isComposing) {
      return;
    }

    event.preventDefault();
    this.submit();
  }

  submit(): void {
    const content = this.draft().trim();
    if (!this.canSend()) {
      return;
    }

    this.sendMessage.emit(content);
    this.draft.set('');
  }

  messageAuthorLabel(message: GameChatMessage): string {
    if (message.senderUserId === this.currentUserId()) {
      return this.t('GAME.CHAT.YOUR_MESSAGE');
    }

    return message.senderUsername.toUpperCase();
  }

  messageCardClasses(message: GameChatMessage): string {
    return message.senderUserId === this.currentUserId()
      ? 'game-chat-drawer__message--self'
      : 'game-chat-drawer__message--rival';
  }

  formatTime(value: string): string {
    const parsedDate = new Date(value);
    if (Number.isNaN(parsedDate.getTime())) {
      return '--:--';
    }

    return parsedDate.toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
