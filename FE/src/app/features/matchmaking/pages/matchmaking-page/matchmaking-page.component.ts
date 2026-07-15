import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { EmptyStateComponent } from '../../../../shared/ui/feedback/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/ui/layout/page-header/page-header.component';
import { LanguageService } from '../../../../core/services/language.service';

@Component({
  selector: 'app-matchmaking-page',
  imports: [PageHeaderComponent, EmptyStateComponent],
  templateUrl: './matchmaking-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatchmakingPageComponent {
  private readonly languageService = inject(LanguageService);
  readonly t = (key: string) => this.languageService.t(key);
}
