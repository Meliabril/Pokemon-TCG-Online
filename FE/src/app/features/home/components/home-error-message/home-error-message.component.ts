import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-home-error-message',
  templateUrl: './home-error-message.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeErrorMessageComponent {
  readonly message = input('');
  readonly isLightTheme = input(false);

  messageClasses(): string {
    return this.isLightTheme()
      ? 'rounded-2xl border border-[#dc2626]/35 bg-red-50/82 px-4 py-3 text-sm text-red-800'
      : 'rounded-2xl border border-[#dc2626]/45 bg-[#450a0a]/75 px-4 py-3 text-sm text-[#fecaca]';
  }
}
