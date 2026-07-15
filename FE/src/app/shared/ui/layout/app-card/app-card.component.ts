import { ChangeDetectionStrategy, Component, HostBinding, booleanAttribute, input } from '@angular/core';

export type AppCardTone = 'dark' | 'light';
export type AppCardVariant = 'default' | 'home' | 'compact' | 'feature';

@Component({
  selector: 'app-card',
  templateUrl: './app-card.component.html',
  styleUrl: './app-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppCardComponent {
  readonly variant = input<AppCardVariant>('default');
  readonly tone = input<AppCardTone>('dark');
  readonly fill = input(false, { transform: booleanAttribute });

  @HostBinding('class.app-card')
  readonly appCardClass = true;

  @HostBinding('class.app-card--default')
  get defaultVariantClass(): boolean {
    return this.variant() === 'default';
  }

  @HostBinding('class.app-card--home')
  get homeVariantClass(): boolean {
    return this.variant() === 'home';
  }

  @HostBinding('class.app-card--compact')
  get compactVariantClass(): boolean {
    return this.variant() === 'compact';
  }

  @HostBinding('class.app-card--feature')
  get featureVariantClass(): boolean {
    return this.variant() === 'feature';
  }

  @HostBinding('class.app-card--dark')
  get darkToneClass(): boolean {
    return this.tone() === 'dark';
  }

  @HostBinding('class.app-card--light')
  get lightToneClass(): boolean {
    return this.tone() === 'light';
  }

  @HostBinding('class.app-card--fill')
  get fillClass(): boolean {
    return this.fill();
  }
}
