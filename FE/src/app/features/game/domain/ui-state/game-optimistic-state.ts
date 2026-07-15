import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameActionPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { GameSnapshot } from '../../../../core/models/interfaces/game/game-snapshot.interface';
import {
  VisibleCardDto,
  VisiblePlayerBoardDto,
  VisiblePokemonDto
} from '../../../../core/models/interfaces/game/visible-board.interface';

export function applyOptimisticGameAction(
  snapshot: GameSnapshot,
  playerId: string,
  actionType: GameActionType,
  payload: GameActionPayload
): GameSnapshot {
  const boardView = snapshot.board.view;
  if (!boardView || !isOptimisticAction(actionType)) {
    return snapshot;
  }

  const players = boardView.players.map((player) =>
    player.playerId === playerId ? updatePlayer(player, snapshot.stateVersion + 1, actionType, payload) : player
  );

  return {
    ...snapshot,
    stateVersion: snapshot.stateVersion + 1,
    boardPlayers: undefined,
    board: {
      ...snapshot.board,
      view: {
        ...boardView,
        players
      }
    },
    updatedAt: new Date().toISOString()
  };
}

function updatePlayer(
  player: VisiblePlayerBoardDto,
  nextVersion: number,
  actionType: GameActionType,
  payload: GameActionPayload
): VisiblePlayerBoardDto {
  switch (actionType) {
    case GameActionType.DrawCard:
      return drawCard(player, nextVersion);
    case GameActionType.PlayBasicPokemon:
      return playBasicPokemon(player, readString(payload, 'cardId'));
    case GameActionType.AttachEnergy:
      return attachEnergy(
        player,
        readString(payload, 'cardInstanceId'),
        readString(payload, 'cardId'),
        readString(payload, 'pokemonInPlayId')
      );
    case GameActionType.EvolvePokemon:
      return evolvePokemon(player, readString(payload, 'cardId'), readString(payload, 'pokemonInPlayId'));
    case GameActionType.Retreat:
      return swapActiveWithBench(player, readString(payload, 'targetPokemonInPlayId'));
    case GameActionType.PromoteBenchPokemon:
      return swapActiveWithBench(player, readString(payload, 'pokemonInPlayId'));
    default:
      return player;
  }
}

function drawCard(player: VisiblePlayerBoardDto, nextVersion: number): VisiblePlayerBoardDto {
  const pendingCard: VisibleCardDto = {
    cardInstanceId: `optimistic-draw-${nextVersion}`,
    cardId: null,
    name: null,
    externalId: null,
    setCode: null,
    number: null,
    supertype: null,
    category: null,
    subtype: null,
    imageSmallUrl: null,
    imageLargeUrl: null,
    hp: null,
    faceDown: true,
    playable: false,
    suggestedAction: null,
    disabledReason: 'Esperando confirmacion del servidor.',
    validTargetPokemonInPlayIds: []
  };

  return {
    ...player,
    hand: {
      ...player.hand,
      count: player.hand.count + 1,
      cards: [...player.hand.cards, pendingCard]
    },
    deck: {
      ...player.deck,
      count: Math.max(0, player.deck.count - 1)
    }
  };
}

function playBasicPokemon(player: VisiblePlayerBoardDto, cardId: string | null): VisiblePlayerBoardDto {
  const card = handCard(player, null, cardId);
  if (!card) {
    return player;
  }

  const nextSlot = Math.max(0, ...player.benchPokemon.map((pokemon) => pokemon.slotPosition ?? 0)) + 1;
  const pokemon: VisiblePokemonDto = {
    pokemonInPlayId: `optimistic-${card.cardInstanceId ?? card.cardId}`,
    ownerPlayerId: player.playerId,
    slotPosition: nextSlot,
    activeCard: card,
    evolutionStack: [],
    attachedEnergyCards: [],
    attachedTrainerCards: [],
    damageCounters: 0,
    specialConditions: [],
    attacks: [],
    abilities: [],
    canReceiveEnergy: false,
    canReceiveTrainer: false,
    canRetreatTo: false,
    canPromote: false
  };

  return {
    ...withoutHandCard(player, card),
    benchPokemon: [...player.benchPokemon, pokemon]
  };
}

function attachEnergy(
  player: VisiblePlayerBoardDto,
  cardInstanceId: string | null,
  cardId: string | null,
  pokemonInPlayId: string | null
): VisiblePlayerBoardDto {
  const card = handCard(player, cardInstanceId, cardId);
  if (!card || !pokemonInPlayId) {
    return player;
  }

  return mapPokemon(withoutHandCard(player, card), pokemonInPlayId, (pokemon) => ({
    ...pokemon,
    attachedEnergyCards: [...pokemon.attachedEnergyCards, card]
  }));
}

function evolvePokemon(
  player: VisiblePlayerBoardDto,
  cardId: string | null,
  pokemonInPlayId: string | null
): VisiblePlayerBoardDto {
  const card = handCard(player, null, cardId);
  if (!card || !pokemonInPlayId) {
    return player;
  }

  return mapPokemon(withoutHandCard(player, card), pokemonInPlayId, (pokemon) => ({
    ...pokemon,
    activeCard: card,
    evolutionStack: pokemon.activeCard
      ? [...pokemon.evolutionStack, pokemon.activeCard]
      : pokemon.evolutionStack
  }));
}

function swapActiveWithBench(
  player: VisiblePlayerBoardDto,
  pokemonInPlayId: string | null
): VisiblePlayerBoardDto {
  if (!player.activePokemon || !pokemonInPlayId) {
    return player;
  }

  const target = player.benchPokemon.find((pokemon) => pokemon.pokemonInPlayId === pokemonInPlayId);
  if (!target) {
    return player;
  }

  return {
    ...player,
    activePokemon: { ...target, slotPosition: 0 },
    benchPokemon: player.benchPokemon.map((pokemon) =>
      pokemon.pokemonInPlayId === pokemonInPlayId
        ? { ...player.activePokemon!, slotPosition: target.slotPosition }
        : pokemon
    )
  };
}

function mapPokemon(
  player: VisiblePlayerBoardDto,
  pokemonInPlayId: string,
  transform: (pokemon: VisiblePokemonDto) => VisiblePokemonDto
): VisiblePlayerBoardDto {
  return {
    ...player,
    activePokemon:
      player.activePokemon?.pokemonInPlayId === pokemonInPlayId
        ? transform(player.activePokemon)
        : player.activePokemon,
    benchPokemon: player.benchPokemon.map((pokemon) =>
      pokemon.pokemonInPlayId === pokemonInPlayId ? transform(pokemon) : pokemon
    )
  };
}

function handCard(
  player: VisiblePlayerBoardDto,
  cardInstanceId: string | null,
  cardId: string | null
): VisibleCardDto | null {
  if (cardInstanceId) {
    return player.hand.cards.find((card) => card.cardInstanceId === cardInstanceId) ?? null;
  }

  return player.hand.cards.find((card) => card.cardId === cardId) ?? null;
}

function withoutHandCard(player: VisiblePlayerBoardDto, card: VisibleCardDto): VisiblePlayerBoardDto {
  return {
    ...player,
    hand: {
      ...player.hand,
      count: Math.max(0, player.hand.count - 1),
      cards: player.hand.cards.filter((candidate) => candidate !== card)
    }
  };
}

function readString(payload: GameActionPayload, key: string): string | null {
  const value = (payload as Record<string, unknown>)[key];
  return typeof value === 'string' ? value : null;
}

function isOptimisticAction(actionType: GameActionType): boolean {
  return (
    actionType === GameActionType.DrawCard ||
    actionType === GameActionType.PlayBasicPokemon ||
    actionType === GameActionType.AttachEnergy ||
    actionType === GameActionType.EvolvePokemon ||
    actionType === GameActionType.Retreat ||
    actionType === GameActionType.PromoteBenchPokemon
  );
}
