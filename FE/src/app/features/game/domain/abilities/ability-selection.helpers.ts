import { UseAbilityPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { BoardPlayerViewModel } from '../board/board-player-view-model.interface';
import { BoardCardViewModel } from '../cards/board-card-view-model.interface';
import { BoardPokemonAbilityViewModel, BoardPokemonViewModel } from '../cards/board-pokemon-view-model.interface';

const ABILITY_CODE = {
  MysticalFire: 'MYSTICAL_FIRE',
  WaterShuriken: 'WATER_SHURIKEN',
  FairyTransfer: 'FAIRY_TRANSFER',
  DriveOff: 'DRIVE_OFF',
  StanceChange: 'STANCE_CHANGE',
  UpsideDownEvolution: 'UPSIDE_DOWN_EVOLUTION'
} as const;

export interface AbilityCardOption {
  cardInstanceId: string;
  label: string;
  externalId?: string | null;
  cardId?: string | null;
  number?: string | null;
  imageSmallUrl?: string | null;
  imageLargeUrl?: string | null;
}

export interface AbilityPokemonOption {
  pokemonInPlayId: string;
  label: string;
}

export interface AbilityAttachedEnergyOption {
  cardInstanceId: string;
  label: string;
}

export interface AbilitySelectionViewModel {
  abilityCode: string;
  requiresHandCard?: boolean;
  handCardPrompt?: string;
  handCardOptions?: AbilityCardOption[];
  requiresTargetPokemon?: boolean;
  targetPokemonPrompt?: string;
  targetPokemonOptions?: AbilityPokemonOption[];
  requiresFromPokemon?: boolean;
  fromPokemonPrompt?: string;
  fromPokemonOptions?: AbilityPokemonOption[];
  requiresToPokemon?: boolean;
  toPokemonPrompt?: string;
  toPokemonOptions?: AbilityPokemonOption[];
  requiresSourceEnergy?: boolean;
  sourceEnergyPrompt?: string;
  attachedEnergyOptionsByPokemonId?: Record<string, AbilityAttachedEnergyOption[]>;
  requiresDeckCard?: boolean;
  deckCardPrompt?: string;
  deckCardOptions?: AbilityCardOption[];
  unsupportedReason?: string | null;
}

export interface AbilitySelectionState {
  selectedHandCardId?: string | null;
  targetPokemonId?: string | null;
  sourceEnergyCardId?: string | null;
  fromPokemonId?: string | null;
  toPokemonId?: string | null;
  selectedDeckCardId?: string | null;
}

export function buildAbilitySelectionViewModel(params: {
  ability: BoardPokemonAbilityViewModel;
  sourcePokemonId?: string | null;
  localPlayer: BoardPlayerViewModel;
  rivalPlayer: BoardPlayerViewModel;
}): AbilitySelectionViewModel {
  const { ability, sourcePokemonId, localPlayer, rivalPlayer } = params;

  switch (ability.id) {
    case ABILITY_CODE.MysticalFire:
      return { abilityCode: ability.id };
    case ABILITY_CODE.WaterShuriken:
      return {
        abilityCode: ability.id,
        requiresHandCard: true,
        handCardPrompt: 'Energias Agua en tu mano',
        handCardOptions: localPlayer.handCards
          .filter((card) => isNamedEnergy(card, 'Water'))
          .map(toCardOption),
        requiresTargetPokemon: true,
        targetPokemonPrompt: 'Pokemon rival objetivo',
        targetPokemonOptions: allPokemonOptions(rivalPlayer)
      };
    case ABILITY_CODE.FairyTransfer: {
      const ownPokemon = ownPokemons(localPlayer);
      const attachedEnergyOptionsByPokemonId = Object.fromEntries(
        ownPokemon
          .map((pokemon) => {
            const fairyEnergyOptions = pokemon.attachedEnergyCards
              .filter((card) => isNamedEnergy(card, 'Fairy'))
              .map(toCardOption);
            return [pokemon.id, fairyEnergyOptions] as const;
          })
          .filter((entry) => entry[1].length > 0)
      );

      return {
        abilityCode: ability.id,
        requiresFromPokemon: true,
        fromPokemonPrompt: 'Elegi desde que Pokemon mover la Energia Hada.',
        fromPokemonOptions: ownPokemon
          .filter((pokemon) => (attachedEnergyOptionsByPokemonId[pokemon.id] ?? []).length > 0)
          .map(toPokemonOption),
        requiresSourceEnergy: true,
        sourceEnergyPrompt: 'Elegi la Energia Hada a mover.',
        attachedEnergyOptionsByPokemonId,
        requiresToPokemon: true,
        toPokemonPrompt: 'Elegi a que Pokemon mover la Energia Hada.',
        toPokemonOptions: ownPokemon.map(toPokemonOption)
      };
    }
    case ABILITY_CODE.DriveOff:
      return {
        abilityCode: ability.id,
        requiresTargetPokemon: true,
        targetPokemonPrompt: 'Elegi el Pokemon rival de banca que va a entrar activo.',
        targetPokemonOptions: benchPokemonOptions(rivalPlayer)
      };
    case ABILITY_CODE.StanceChange:
      const sourcePokemon = sourcePokemonId ? ownPokemons(localPlayer).find((pokemon) => pokemon.id === sourcePokemonId) : null;
      const sourceCardBaseId = cardBaseId(sourcePokemon?.activeCard ?? null);
      return {
        abilityCode: ability.id,
        requiresHandCard: true,
        handCardPrompt: 'Elegi el Aegislash de tu mano para intercambiar.',
        handCardOptions: localPlayer.handCards
          .filter((card) => isNamedCard(card, 'Aegislash'))
          .filter((card) => cardBaseId(card) !== sourceCardBaseId)
          .map(toCardOption)
      };
    case ABILITY_CODE.UpsideDownEvolution:
      return {
        abilityCode: ability.id,
        requiresDeckCard: true,
        deckCardPrompt: 'Elegi una evolucion para Inkay',
        deckCardOptions: uniqueByBaseCard(ability.deckCardOptions ?? [])
      };
    default:
      return {
        abilityCode: ability.id,
        unsupportedReason: 'Esta habilidad todavia no tiene selector configurado en el tablero.'
      };
  }
}

export function buildUseAbilityPayload(params: {
  sourcePokemonId: string;
  abilityCode: string;
  selection: AbilitySelectionState;
}): UseAbilityPayload {
  const { sourcePokemonId, abilityCode, selection } = params;
  return {
    sourcePokemonId,
    abilityCode,
    selectedHandCardId: selection.selectedHandCardId ?? undefined,
    targetPokemonId: selection.targetPokemonId ?? undefined,
    sourceEnergyCardId: selection.sourceEnergyCardId ?? undefined,
    fromPokemonId: selection.fromPokemonId ?? undefined,
    toPokemonId: selection.toPokemonId ?? undefined,
    selectedDeckCardId: selection.selectedDeckCardId ?? undefined
  };
}

function ownPokemons(player: BoardPlayerViewModel): BoardPokemonViewModel[] {
  return [
    player.activePokemon.pokemon ?? null,
    ...player.benchSlots.map((slot) => slot.pokemon ?? null)
  ].filter((pokemon): pokemon is BoardPokemonViewModel => pokemon !== null);
}

function allPokemonOptions(player: BoardPlayerViewModel): AbilityPokemonOption[] {
  return [
    player.activePokemon.pokemon ? toPokemonOption(player.activePokemon.pokemon) : null,
    ...player.benchSlots.map((slot) => (slot.pokemon ? toPokemonOption(slot.pokemon) : null))
  ].filter((option): option is AbilityPokemonOption => option !== null);
}

function benchPokemonOptions(player: BoardPlayerViewModel): AbilityPokemonOption[] {
  return player.benchSlots
    .map((slot) => slot.pokemon)
    .filter((pokemon): pokemon is BoardPokemonViewModel => pokemon !== null)
    .map(toPokemonOption);
}

function toCardOption(card: BoardCardViewModel): AbilityCardOption {
  const labelSuffix = card.number ? ` #${card.number}` : '';
  return {
    cardInstanceId: card.cardInstanceId ?? card.id,
    label: `${card.label}${labelSuffix}`,
    externalId: card.externalId,
    cardId: card.cardId,
    number: card.number,
    imageSmallUrl: card.imageSmallUrl,
    imageLargeUrl: card.imageLargeUrl
  };
}

function uniqueByBaseCard(options: AbilityCardOption[]): AbilityCardOption[] {
  const seenBaseIds = new Set<string>();
  const uniqueOptions: AbilityCardOption[] = [];
  for (const option of options) {
    const baseId = cardOptionBaseId(option);
    if (seenBaseIds.has(baseId)) {
      continue;
    }
    seenBaseIds.add(baseId);
    uniqueOptions.push(option);
  }
  return uniqueOptions;
}

function cardOptionBaseId(option: AbilityCardOption): string {
  return option.externalId ?? option.cardId ?? option.number ?? option.cardInstanceId;
}

function toPokemonOption(pokemon: BoardPokemonViewModel): AbilityPokemonOption {
  return {
    pokemonInPlayId: pokemon.id,
    label: pokemon.activeCard.label
  };
}

function isNamedEnergy(card: BoardCardViewModel, energyName: 'Water' | 'Fairy'): boolean {
  const label = normalize(card.label);
  return label.includes(`${normalize(energyName)} energy`) || label.includes(`energia ${normalizeSpanishEnergy(energyName)}`);
}

function isNamedCard(card: BoardCardViewModel, expectedName: string): boolean {
  return normalize(card.label) === normalize(expectedName);
}

function cardBaseId(card: BoardCardViewModel | null): string | null {
  return card?.externalId ?? card?.cardId ?? card?.number ?? null;
}

function normalize(value: string | null | undefined): string {
  return (value ?? '').trim().toLowerCase();
}

function normalizeSpanishEnergy(energyName: 'Water' | 'Fairy'): string {
  return energyName === 'Water' ? 'agua' : 'hada';
}
