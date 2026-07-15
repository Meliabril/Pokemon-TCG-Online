import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';

@Component({
  selector: 'app-mulligan-setup-panel',
  templateUrl: './mulligan-setup-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MulliganSetupPanelComponent {
  private readonly languageService = inject(LanguageService);

  readonly noticePending = input(false);
  readonly ackPending = input(false);
  readonly mulliganFlowActive = input(false);
  /** True when the LOCAL player is one of the players that must take a Mulligan (from the snapshot). */
  readonly localMustMulligan = input(false);
  /** Cumulative Mulligan counts taken from the authoritative snapshot state (never from the DOM). */
  readonly ownMulliganCount = input(0);
  readonly opponentMulliganCount = input(0);
  readonly noticeAcknowledged = output<void>();
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  // Synchronized initial barrier (a single confirmation per player, NOT a per-round ACK):
  //  - the Mulligan player confirms "Iniciar Mulligan" (showOwnerNotice);
  //  - the observer confirms "Entendido. Continuar" (showWatcherNotice);
  //  - once a player confirmed but the flow has not started, they wait for the rival (showWaiting).
  // After the barrier passes the flow runs automatically and no button is shown.
  readonly showOwnerNotice = computed(() => this.noticePending() && this.localMustMulligan());
  readonly showWatcherNotice = computed(() => this.noticePending() && !this.localMustMulligan());
  readonly showWaiting = computed(() => !this.noticePending() && this.mulliganFlowActive());

  // Desktop shows ONLY a light text counter (no revealed cards, no images, no verbose history).
  // The counter appears as soon as there is any Mulligan activity or accumulated counts.
  readonly showCounters = computed(
    () =>
      this.ownMulliganCount() > 0 ||
      this.opponentMulliganCount() > 0 ||
      this.mulliganFlowActive() ||
      this.noticePending()
  );
  readonly hasContent = computed(() => this.showCounters() || this.showWaiting());

  acknowledge(): void {
    if (this.noticePending() && !this.ackPending()) {
      this.noticeAcknowledged.emit();
    }
  }
}
