import { ChangeDetectionStrategy, Component, HostListener, computed, inject, input, output, signal } from '@angular/core';
import { CardSupertype } from '../../../core/models/enums/card/card-supertype.enum';
import { LanguageService } from '../../../core/services/language.service';
import {
  SharedAbilityViewModel,
  SharedAttackViewModel,
  SharedCardContext,
  SharedCardModalViewModel
} from '../../models/card-modal.interface';

const ENERGY_COLORS: Record<string, string> = {
  Grass: '#5bbd6b',
  Fire: '#f0772b',
  Water: '#4aa3df',
  Lightning: '#f2c33a',
  Psychic: '#b772d6',
  Fighting: '#cf7236',
  Darkness: '#5a6b7b',
  Metal: '#9aa6b0',
  Fairy: '#e87fb5',
  Dragon: '#c9a227',
  Colorless: '#d8c9a8'
};

@Component({
  selector: 'app-card-detail-modal',
  templateUrl: './card-detail-modal.component.html',
  styleUrl: './card-detail-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardDetailModalComponent {
  private readonly languageService = inject(LanguageService);

  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly card = input<SharedCardModalViewModel | null>(null);
  readonly context = input<SharedCardContext | null>(null);
  readonly isOpen = input(false);
  readonly closed = output<void>();
  readonly attackSelected = output<SharedAttackViewModel>();
  readonly abilitySelected = output<SharedAbilityViewModel>();

  readonly cardRotateX = signal(0);
  readonly cardRotateY = signal(0);
  readonly cardGlareX = signal(50);
  readonly cardGlareY = signal(50);

  readonly primaryType = computed(() => this.card()?.types?.[0] ?? null);
  readonly typeColorStyle = computed(() => ({
    '--type-color': this.getEnergyColor(this.primaryType())
  }));
  readonly cardTiltStyle = computed(() => ({
    transform: `perspective(1000px) rotateX(${this.cardRotateX()}deg) rotateY(${this.cardRotateY()}deg)`,
    '--glare-x': `${this.cardGlareX()}%`,
    '--glare-y': `${this.cardGlareY()}%`
  }));
  readonly canSelectAttack = computed(() => Boolean(this.context()?.canAttack && !this.context()?.readonly));
  readonly canSelectAbility = computed(() => Boolean(this.context()?.canUseAbility && !this.context()?.readonly));
  readonly isPokemonCard = computed(() => this.card()?.supertypeCode === CardSupertype.Pokemon);
  readonly isTrainerCard = computed(() => this.card()?.supertypeCode === CardSupertype.Trainer);
  readonly displayRules = computed(() => this.card()?.displayRules ?? []);

  close(): void {
    this.closed.emit();
  }

  selectAttack(attack: SharedAttackViewModel): void {
    if (!this.canSelectAttack()) {
      return;
    }

    this.attackSelected.emit(attack);
  }

  selectAbility(ability: SharedAbilityViewModel): void {
    if (!this.canSelectAbility()) {
      return;
    }

    this.abilitySelected.emit(ability);
  }

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    if (this.isOpen()) {
      this.close();
    }
  }

  onCardMouseMove(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();

    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const xPct = (x / rect.width) * 100;
    const yPct = (y / rect.height) * 100;

    const rotateY = (x / rect.width - 0.5) * 60;
    const rotateX = (y / rect.height - 0.5) * -60;

    this.cardRotateX.set(rotateX);
    this.cardRotateY.set(rotateY);
    this.cardGlareX.set(xPct);
    this.cardGlareY.set(yPct);
  }

  onCardMouseLeave(): void {
    this.cardRotateX.set(0);
    this.cardRotateY.set(0);
    this.cardGlareX.set(50);
    this.cardGlareY.set(50);
  }

  getEnergyColor(type: string | null | undefined): string {
    return ENERGY_COLORS[type ?? ''] ?? '#d8c9a8';
  }

  relationSummary(relations: unknown[] | null | undefined): string {
    if (!relations?.length) {
      return 'Sin datos';
    }

    return relations.map((relation) => this.relationLabel(relation)).join(', ');
  }

  attackDamage(attack: SharedAttackViewModel): string | number {
    return attack.damage ?? '-';
  }

  private relationLabel(relation: unknown): string {
    if (!relation || typeof relation !== 'object') {
      return String(relation);
    }

    const value = relation as Record<string, unknown>;
    const energyType = value['displayEnergyType'] ?? value['energyType'];
    const amount = value['displayValue'] ?? value['value'];

    return [energyType, amount].filter((part) => typeof part === 'string' && part.length > 0).join(' ');
  }
}
