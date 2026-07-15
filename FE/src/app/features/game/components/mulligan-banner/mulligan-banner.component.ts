import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { MulliganVisualService } from '../../services/mulligan-visual.service';

@Component({
  selector: 'app-mulligan-banner',
  templateUrl: './mulligan-banner.component.html',
  styleUrl: './mulligan-banner.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MulliganBannerComponent {
  private readonly languageService = inject(LanguageService);
  private readonly mulliganVisual = inject(MulliganVisualService);

  readonly banner = this.mulliganVisual.banner;
  readonly bannerText = computed(() => {
    const banner = this.banner();
    return banner ? this.languageService.t(banner.key, banner.params) : '';
  });
}
