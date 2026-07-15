import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { AppLanguage, LanguageService } from '../../../../core/services/language.service';
import {
  GameAttackOptionViewModel,
  GameInteractionViewModel
} from '../../domain/interaction/game-interaction-view-model.interface';

@Component({
  selector: 'app-game-command-panel',
  templateUrl: './game-command-panel.component.html',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameCommandPanelComponent {
  private readonly languageService = inject(LanguageService);
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly interaction = input.required<GameInteractionViewModel>();
  readonly actionPending = input(false);
  readonly actionSelected = output<GameActionType>();
  readonly confirmRequested = output<void>();
  readonly cancelRequested = output<void>();
  readonly attackSelected = output<string>();
  readonly setupBenchToggleRequested = output<void>();
  readonly visibleActionButtons = computed(() =>
    this.interaction().actionButtons.filter((action) => !HIDDEN_PRIMARY_ACTIONS.has(action.actionType))
  );

  readonly GameActionType = GameActionType;

  attackDisplayName(attack: GameAttackOptionViewModel, language: AppLanguage): string {
    return this.languageService.attackName({
      name: attack.name ?? attack.label,
      displayName: attack.displayName
    }, language);
  }

  disabledReason(reason: string | null | undefined): string {
    return this.languageService.gameDisabledReason(reason);
  }

  selectAttack(attackId: string): void {
    if (this.actionPending() || this.interaction().awaitingSnapshot) {
      return;
    }

    const selectedAttack = this.interaction().attackOptions.find((attack) => attack.id === attackId);
    if (!selectedAttack || !selectedAttack.enabled) {
      return;
    }

    this.attackSelected.emit(selectedAttack.id);
  }
}

const HIDDEN_PRIMARY_ACTIONS = new Set<GameActionType>([
  GameActionType.PlayBasicPokemon,
  GameActionType.AttachEnergy,
  GameActionType.EvolvePokemon,
  GameActionType.PlayTrainer,
  GameActionType.Retreat,
  GameActionType.PromoteBenchPokemon,
  GameActionType.EndTurn,
  GameActionType.Concede
]);
