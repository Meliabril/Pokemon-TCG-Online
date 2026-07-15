import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  inject,
  output,
  signal
} from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';

export type AvatarOption = {
  id: string;
  name: string;
  imageUrl: string;
};

export const DEFAULT_AVATAR_ID = 'avatar-pikachu-01';

export const AVATAR_OPTIONS: AvatarOption[] = [
  {
    id: DEFAULT_AVATAR_ID,
    name: 'Pikachu',
    imageUrl: 'https://media1.tenor.com/m/hlUcalxy_rAAAAAC/pikachu.gif'
  },
  {
    id: 'avatar-snorlax-01',
    name: 'Snorlax',
    imageUrl: 'https://media.tenor.com/pBM7dzGyfokAAAAi/snorlax-pixel.gif'
  },
  {
    id: 'avatar-squirtle-01',
    name: 'Squirtle',
    imageUrl: 'https://media1.tenor.com/m/zQ1QPaNFv9gAAAAC/shiny-squirtle-squirtle.gif'
  },
  {
    id: 'avatar-cute-pokemon-01',
    name: 'Charizard',
    imageUrl: 'https://giffiles.alphacoders.com/139/13940.gif'
  }
];

export function avatarImageUrlFor(avatarId: string | null | undefined): string {
  return AVATAR_OPTIONS.find((avatar) => avatar.id === avatarId)?.imageUrl ?? AVATAR_OPTIONS[0].imageUrl;
}

export type AvatarPickerVariant = 'form' | 'profile';

@Component({
  selector: 'app-avatar-picker',
  host: {
    '(document:keydown.escape)': 'closeModalWithKeyboard()'
  },
  templateUrl: './avatar-picker.component.html',
  styleUrl: './avatar-picker.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AvatarPickerComponent {
  private readonly languageService = inject(LanguageService);

  readonly selectedAvatarId = input(DEFAULT_AVATAR_ID);
  readonly darkMode = input(false);
  readonly variant = input<AvatarPickerVariant>('form');
  readonly buttonLabel = input('');
  readonly selectedAvatarIdChange = output<string>();
  readonly isModalOpen = signal(false);
  readonly avatarOptions = AVATAR_OPTIONS;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly selectedAvatar = computed(
    () =>
      this.avatarOptions.find((avatar) => avatar.id === this.selectedAvatarId()) ??
      this.avatarOptions[0]
  );

  openModal(): void {
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  selectAvatar(avatarId: string): void {
    this.selectedAvatarIdChange.emit(avatarId);
    this.closeModal();
  }

  labelClasses(): string {
    return this.darkMode()
      ? 'text-sm font-semibold text-white'
      : 'text-sm font-semibold text-slate-900';
  }

  requiredMarkClasses(): string {
    return this.darkMode() ? 'text-red-300' : 'text-[#8b070c]';
  }

  triggerClasses(): string {
    if (this.variant() === 'profile') {
      return 'group relative grid h-32 w-32 place-items-center overflow-hidden rounded-full border-2 border-[#facc15]/50 bg-[#facc15]/14 p-1 shadow-[0_18px_34px_rgba(0,0,0,0.28)] transition hover:border-[#fde68a] hover:shadow-[0_0_28px_rgba(250,204,21,0.24)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#facc15]/80';
    }

    return 'grid h-16 w-16 place-items-center rounded-full border-2 border-[#8b070c] bg-white/90 p-1 shadow-sm transition hover:bg-red-50/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c] focus-visible:ring-offset-2 focus-visible:ring-offset-white/70';
  }

  closeModalWithKeyboard(): void {
    if (this.isModalOpen()) {
      this.closeModal();
    }
  }
}
