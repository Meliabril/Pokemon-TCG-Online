import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { GameChatMessage } from '../../../../core/models/interfaces/game/game-chat-message.interface';

@Component({
  selector: 'app-live-chat-panel',
  imports: [ReactiveFormsModule],
  template: `
    <article class="rounded-[24px] border border-[#8a4b20]/70 bg-[#3b0905]/80 p-5 shadow-[0_20px_48px_rgba(0,0,0,0.28)] backdrop-blur-md sm:p-6">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="font-pixel text-sm tracking-[0.18em] text-[#fde68a]">{{ title() }}</p>
          <p class="mt-2 text-sm leading-6 text-[#d8b982]">{{ subtitle() }}</p>
        </div>

        <span class="rounded-full border px-3 py-1 text-[0.68rem] {{ statusBadgeClasses() }}">
          {{ connected() ? 'Realtime activo' : 'Reconectando' }}
        </span>
      </div>

      <div class="mt-4 grid gap-3">
        <div class="max-h-[320px] overflow-y-auto rounded-[22px] border border-[#8a4b20]/60 bg-[#2a0805]/72 p-3">
          @if (!messages().length) {
            <p class="text-sm leading-7 text-[#d8b982]" aria-live="polite">
              El chat quedara disponible apenas llegue el primer mensaje entre ambos jugadores.
            </p>
          } @else {
            <div class="grid gap-3">
              @for (message of messages(); track message.messageId) {
                <article
                  class="max-w-[92%] rounded-2xl border px-3 py-2 {{ messageCardClasses(message.senderUserId) }}"
                >
                  <div class="flex items-center justify-between gap-3 text-[0.68rem] text-[#d8b982]">
                    <span class="font-pixel tracking-[0.12em]">
                      {{ message.senderUserId === currentUserId() ? 'TU MENSAJE' : message.senderUsername.toUpperCase() }}
                    </span>
                    <span>{{ formatTime(message.sentAt) }}</span>
                  </div>
                  <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-[#fff7d6]">{{ message.content }}</p>
                </article>
              }
            </div>
          }
        </div>

        <label class="grid gap-2 text-sm text-[#d8b982]">
          <span class="font-pixel text-[0.68rem] tracking-[0.14em] text-[#fde68a]">MENSAJE</span>
          <textarea
            class="min-h-28 rounded-2xl border border-[#8a4b20] bg-[#2a0d06]/80 px-4 py-3 text-sm text-[#fff7d6] outline-none transition placeholder:text-[#d8b982]/60 focus:border-[#facc15] focus:ring-2 focus:ring-[#facc15]/30"
            [formControl]="draftControl"
            [placeholder]="placeholder()"
            [disabled]="sending()"
          ></textarea>
        </label>

        @if (errorMessage()) {
          <p class="rounded-2xl border border-[#dc2626]/45 bg-[#450a0a]/75 px-4 py-3 text-sm text-[#fecaca]">
            {{ errorMessage() }}
          </p>
        }

        <div class="flex justify-end">
          <button
            type="button"
            class="font-pixel inline-flex min-h-12 items-center justify-center rounded-2xl border border-[#facc15]/30 bg-[#8b070c] px-5 py-3 text-[0.72rem] tracking-[0.14em] text-[#fff7d6] shadow-[0_18px_34px_rgba(101,4,8,0.34)] transition hover:bg-[#650408] disabled:cursor-not-allowed disabled:opacity-65"
            [disabled]="sendDisabled()"
            (click)="submit()"
          >
            {{ sending() ? 'Enviando...' : 'Enviar mensaje' }}
          </button>
        </div>
      </div>
    </article>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LiveChatPanelComponent {
  readonly title = input('CHAT EN VIVO');
  readonly subtitle = input('Conversación en vivo entre ambos jugadores.');
  readonly placeholder = input('Escribe tu mensaje para el rival...');
  readonly currentUserId = input<string | null>(null);
  readonly messages = input<GameChatMessage[]>([]);
  readonly connected = input(false);
  readonly sending = input(false);
  readonly errorMessage = input('');
  readonly sendMessage = output<string>();

  readonly draftControl = new FormControl('', { nonNullable: true });
  readonly sendDisabled = computed(
    () => this.sending() || !this.draftControl.value.trim() || !this.connected()
  );

  submit(): void {
    const content = this.draftControl.value.trim();
    if (!content || this.sendDisabled()) {
      return;
    }

    this.sendMessage.emit(content);
    this.draftControl.setValue('');
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

  statusBadgeClasses(): string {
    return this.connected()
      ? 'border-[#22c55e]/35 bg-[#14532d]/35 text-[#bbf7d0]'
      : 'border-[#f59e0b]/35 bg-[#451a03]/72 text-[#fde68a]';
  }

  messageCardClasses(senderUserId: string): string {
    const isCurrentUser = senderUserId === this.currentUserId();
    return isCurrentUser
      ? 'ml-auto border-[#facc15]/30 bg-[#5d0b0f]/70'
      : 'mr-auto border-[#8a4b20]/60 bg-[#24110d]/82';
  }
}
