import { ChangeDetectionStrategy, Component, output, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-primary-button',
  imports: [RouterLink],
  template: `
    @if (routerLink()) {
      <a class="primary-button" [routerLink]="routerLink()!">
        {{ label() }}
      </a>
    } @else {
      <button class="primary-button" type="button" [disabled]="disabled()" (click)="pressed.emit()">
        {{ label() }}
      </button>
    }
  `,
  styles: [`
    .primary-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      min-height: 2.875rem;
      padding: 0.75rem 1.1rem;
      border: 0;
      border-radius: 999px;
      background: linear-gradient(135deg, var(--app-primary), var(--app-primary-strong));
      color: #fff;
      font: inherit;
      font-weight: 700;
      text-decoration: none;
      cursor: pointer;
      box-shadow: 0 0.9rem 2rem rgba(16, 24, 40, 0.16);
      transition:
        transform 160ms ease,
        box-shadow 160ms ease,
        opacity 160ms ease;
    }

    .primary-button:hover {
      transform: translateY(-1px);
      box-shadow: 0 1.1rem 2.4rem rgba(16, 24, 40, 0.2);
    }

    .primary-button:disabled {
      cursor: not-allowed;
      opacity: 0.65;
      transform: none;
      box-shadow: none;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PrimaryButtonComponent {
  readonly label = input.required<string>();
  readonly routerLink = input<string | readonly string[] | null>(null);
  readonly disabled = input(false);
  readonly pressed = output<void>();
}
