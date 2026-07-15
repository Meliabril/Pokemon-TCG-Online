import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input
} from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { SpecialConditionType } from '../../../../core/models/enums/game/special-condition-type.enum';

export type StatusOverlayVariant = 'active' | 'bench';

export interface StatusOverlayBadge {
  condition: SpecialConditionType;
  row: number;
  label: string;
  emphasized: boolean;
}

const STATUS_ROWS: Record<SpecialConditionType, number> = {
  [SpecialConditionType.Burned]: 0,
  [SpecialConditionType.Poisoned]: 1,
  [SpecialConditionType.Asleep]: 2,
  [SpecialConditionType.Confused]: 3,
  [SpecialConditionType.Paralyzed]: 4
};

const CONDITION_I18N_KEYS: Record<SpecialConditionType, string> = {
  [SpecialConditionType.Asleep]: 'GAME.STATUS_EFFECT.ASLEEP',
  [SpecialConditionType.Burned]: 'GAME.STATUS_EFFECT.BURNED',
  [SpecialConditionType.Confused]: 'GAME.STATUS_EFFECT.CONFUSED',
  [SpecialConditionType.Paralyzed]: 'GAME.STATUS_EFFECT.PARALYZED',
  [SpecialConditionType.Poisoned]: 'GAME.STATUS_EFFECT.POISONED'
};

const CONDITION_ORDER: SpecialConditionType[] = [
  SpecialConditionType.Burned,
  SpecialConditionType.Poisoned,
  SpecialConditionType.Asleep,
  SpecialConditionType.Confused,
  SpecialConditionType.Paralyzed
];

export const STATUS_OVERLAY_ROW_MAPPING = STATUS_ROWS;

@Component({
  selector: 'app-status-overlay',
  templateUrl: './status-overlay.component.html',
  styleUrl: './status-overlay.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusOverlayComponent {
  private readonly languageService = inject(LanguageService);

  readonly pokemonId = input<string | null>(null);
  readonly conditions = input<SpecialConditionType[]>([]);
  readonly variant = input<StatusOverlayVariant>('active');

  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  /**
   * Tracks the conditions that were rendered on the previous emission of `badges`.
   * Because the diff is computed synchronously inside the same `computed` block that
   * reads and writes these class fields, rerenders that keep the same condition set
   * never replay the first-apply emphasis.
   */
  private previousConditions: Set<SpecialConditionType> = new Set();
  private hasInitialSnapshot = false;
  private snapshotPokemonId: string | null | undefined = undefined;

  readonly badges = computed<StatusOverlayBadge[]>(() => {
    const current = new Set<SpecialConditionType>();
    for (const condition of this.conditions()) {
      current.add(condition);
    }

    const pokemon = this.pokemonId();
    const emphasized = this.computeEmphasizedSet(pokemon, current);

    this.previousConditions = new Set(current);
    this.hasInitialSnapshot = true;
    this.snapshotPokemonId = pokemon;

    const result: StatusOverlayBadge[] = [];
    for (const condition of CONDITION_ORDER) {
      if (current.has(condition)) {
        result.push({
          condition,
          row: STATUS_ROWS[condition],
          label: this.t(CONDITION_I18N_KEYS[condition]),
          emphasized: emphasized.has(condition)
        });
      }
    }

    return result;
  });

  readonly hasConditions = computed(() => this.badges().length > 0);

  readonly isCompact = computed(() => this.badges().length >= 2);

  readonly ariaSummary = computed(() => {
    const labels = this.badges().map((badge) => badge.label);
    if (labels.length === 0) {
      return '';
    }
    if (labels.length === 1) {
      return this.t('GAME.STATUS_EFFECT.SUMMARY_SINGLE', { name: labels[0] });
    }
    return this.t('GAME.STATUS_EFFECT.SUMMARY_MULTIPLE', { list: labels.join(', ') });
  });

  trackByCondition(_index: number, badge: StatusOverlayBadge): SpecialConditionType {
    return badge.condition;
  }

  private computeEmphasizedSet(
    pokemon: string | null,
    current: Set<SpecialConditionType>
  ): Set<SpecialConditionType> {
    if (pokemon !== this.snapshotPokemonId) {
      return new Set();
    }

    if (!this.hasInitialSnapshot) {
      return new Set();
    }

    const emphasized = new Set<SpecialConditionType>();
    for (const condition of current) {
      if (!this.previousConditions.has(condition)) {
        emphasized.add(condition);
      }
    }
    return emphasized;
  }
}
