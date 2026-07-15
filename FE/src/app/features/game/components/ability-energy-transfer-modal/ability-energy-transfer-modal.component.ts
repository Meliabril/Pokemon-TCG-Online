import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal, untracked } from '@angular/core';
import { UseAbilityPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { BoardPlayerViewModel } from '../../domain/board/board-player-view-model.interface';
import { BoardCardViewModel } from '../../domain/cards/board-card-view-model.interface';
import { BoardPokemonViewModel } from '../../domain/cards/board-pokemon-view-model.interface';

interface TransferPokemonOption {
  pokemonInPlayId: string;
  label: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  fairyEnergyCards: TransferEnergyOption[];
}

interface TransferEnergyOption {
  cardInstanceId: string;
  label: string;
}

@Component({
  selector: 'app-ability-energy-transfer-modal',
  templateUrl: './ability-energy-transfer-modal.component.html',
  styleUrl: './ability-energy-transfer-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AbilityEnergyTransferModalComponent {
  readonly isOpen = input(false);
  readonly actionPending = input(false);
  readonly localPlayer = input.required<BoardPlayerViewModel>();
  readonly abilityOwnerPokemonId = input<string | null>(null);
  readonly abilityCode = input<string | null>(null);

  readonly closed = output<void>();
  readonly confirmed = output<UseAbilityPayload>();

  readonly selectedFromPokemonId = signal<string | null>(null);
  readonly selectedToPokemonId = signal<string | null>(null);

  readonly ownPokemon = computed(() => [
    this.localPlayer().activePokemon.pokemon ?? null,
    ...this.localPlayer().benchSlots.map((slot) => slot.pokemon ?? null)
  ].filter((pokemon): pokemon is BoardPokemonViewModel => pokemon !== null));

  readonly sourceOptions = computed<TransferPokemonOption[]>(() =>
    this.ownPokemon()
      .map((pokemon) => this.toTransferPokemonOption(pokemon))
      .filter((option) => option.fairyEnergyCards.length > 0)
  );

  readonly targetOptions = computed<TransferPokemonOption[]>(() =>
    this.ownPokemon()
      .filter((pokemon) => !isKnockedOut(pokemon))
      .map((pokemon) => this.toTransferPokemonOption(pokemon))
  );

  readonly canConfirm = computed(() => Boolean(
    this.isOpen() &&
    !this.actionPending() &&
    this.abilityOwnerPokemonId() &&
    this.abilityCode() &&
    this.selectedFromPokemonId() &&
    this.selectedToPokemonId() &&
    this.selectedFromPokemonId() !== this.selectedToPokemonId()
  ));

  constructor() {
    effect(() => {
      if (!this.isOpen()) {
        untracked(() => this.clearSelection());
        return;
      }

      const selectedFromPokemonId = this.selectedFromPokemonId();
      const sourceOptions = this.sourceOptions();
      if (selectedFromPokemonId && !sourceOptions.some((option) => option.pokemonInPlayId === selectedFromPokemonId)) {
        untracked(() => this.selectFromPokemon(null));
      }
    });
  }

  selectFromPokemon(pokemonInPlayId: string | null): void {
    this.selectedFromPokemonId.set(pokemonInPlayId);
    if (pokemonInPlayId !== null && this.selectedToPokemonId() === pokemonInPlayId) {
      this.selectedToPokemonId.set(null);
    }
  }

  selectToPokemon(pokemonInPlayId: string): void {
    if (this.selectedFromPokemonId() === pokemonInPlayId) {
      return;
    }
    this.selectedToPokemonId.set(pokemonInPlayId);
  }

  isTargetDisabled(pokemonInPlayId: string): boolean {
    return this.selectedFromPokemonId() === pokemonInPlayId;
  }

  confirm(): void {
    if (!this.canConfirm()) {
      return;
    }

    const payload = {
      sourcePokemonId: this.abilityOwnerPokemonId() ?? '',
      abilityCode: this.abilityCode() ?? '',
      fromPokemonId: this.selectedFromPokemonId() ?? undefined,
      toPokemonId: this.selectedToPokemonId() ?? undefined
    };
    this.confirmed.emit(payload);
  }

  close(): void {
    this.clearSelection();
    this.closed.emit();
  }

  optionImage(option: TransferPokemonOption): string | null {
    return option.imageLargeUrl ?? option.imageSmallUrl;
  }

  private clearSelection(): void {
    this.selectedFromPokemonId.set(null);
    this.selectedToPokemonId.set(null);
  }

  private toTransferPokemonOption(pokemon: BoardPokemonViewModel): TransferPokemonOption {
    return {
      pokemonInPlayId: pokemon.id,
      label: pokemon.activeCard.label,
      imageSmallUrl: pokemon.activeCard.imageSmallUrl ?? null,
      imageLargeUrl: pokemon.activeCard.imageLargeUrl ?? null,
      fairyEnergyCards: pokemon.attachedEnergyCards
        .filter((card) => isFairyEnergy(card))
        .map((card) => ({
          cardInstanceId: card.cardInstanceId ?? card.id,
          label: card.label
        }))
    };
  }
}

function isFairyEnergy(card: BoardCardViewModel): boolean {
  const label = normalize(card.label);
  const type = normalize(card.pokemonType);
  return type === 'fairy' || label.includes('fairy energy') || label.includes('energia hada');
}

function normalize(value: string | null | undefined): string {
  return (value ?? '').trim().toLowerCase();
}

function isKnockedOut(pokemon: BoardPokemonViewModel): boolean {
  const hp = pokemon.activeCard.hp;
  const damageCounters = pokemon.damageCounters ?? 0;
  return typeof hp === 'number' && hp > 0 && damageCounters * 10 >= hp;
}
