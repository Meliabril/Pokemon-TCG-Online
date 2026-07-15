import { GameEventType } from '../../../../core/models/enums/game/game-event-type.enum';
import { GameEvent } from '../../../../core/models/interfaces/game/game-event.interface';
import {
  BoardCardViewModel,
  BoardGameViewModel,
  BoardPlayerViewModel,
  BoardSlotViewModel
} from '../board/board-game-view-model.interface';
import { mapEventToBoardAnimations } from './opponent-animation.mapper';

describe('mapEventToBoardAnimations draw destinations', () => {
  it('targets the current user mobile hand button for a local draw', () => {
    const commands = mapEventToBoardAnimations(
      drawEvent('local-player', { cardId: 'card-1' }),
      board(),
      (key) => key
    );

    expect(commands[0]?.type).toBe('PLAYER_DRAW_CARD');
    expect(commands[0]?.fromAnchor).toBe('local-deck');
    expect(commands[0]?.toAnchor).toBe('local-hand');
    expect(commands[0]?.mobileToAnchor).toBe('local-mobile-hand-button');
  });

  it('targets the opponent mobile hand button for an opponent draw', () => {
    const commands = mapEventToBoardAnimations(
      drawEvent('rival-player'),
      board(),
      (key) => key
    );

    expect(commands[0]?.type).toBe('OPPONENT_DRAW_CARD');
    expect(commands[0]?.fromAnchor).toBe('opponent-deck');
    expect(commands[0]?.toAnchor).toBe('opponent-hand');
    expect(commands[0]?.mobileToAnchor).toBe('opponent-mobile-hand-button');
    expect(commands[0]?.useCardBack).toBeTrue();
  });
});

describe('mapEventToBoardAnimations deck search attack effects', () => {
  it('moves the found card from local deck to hand and shuffles for Zorua Nasty Plot', () => {
    const localBoard = board();
    localBoard.localPlayer.handCards = [
      card({
        id: 'found-instance',
        cardInstanceId: 'found-instance',
        cardId: 'xy1-72',
        label: 'Zorua'
      })
    ];

    const commands = mapEventToBoardAnimations(
      attackEffectEvent('local-player', {
        effectType: 'SEARCH_ANY_CARD_FROM_DECK',
        foundCard: true,
        cardInstanceId: 'found-instance',
        pokemonInPlayId: 'local-active'
      }),
      localBoard,
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'PLAYER_MOVE_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-hand',
        cardLabel: 'Zorua',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }
    ]);
  });

  it('does not animate Nasty Plot deck search when no card was found', () => {
    const commands = mapEventToBoardAnimations(
      attackEffectEvent('local-player', {
        effectType: 'SEARCH_ANY_CARD_FROM_DECK',
        foundCard: false,
        pokemonInPlayId: 'local-active'
      }),
      board(),
      (key) => key
    );

    expect(commands).toEqual([]);
  });

  it('moves searched Fairy energies from local deck to bench and shuffles for Xerneas Geomancy', () => {
    const localBoard = board();
    localBoard.localPlayer.benchSlots = [
      benchSlot('local-bench-pokemon-1'),
      benchSlot('local-bench-pokemon-2')
    ];
    localBoard.localPlayer.benchSlots[0].pokemon!.attachedEnergyCards = [
      card({
        id: 'fairy-energy-instance-1',
        cardInstanceId: 'fairy-energy-instance-1',
        label: 'Fairy Energy'
      })
    ];
    localBoard.localPlayer.benchSlots[1].pokemon!.attachedEnergyCards = [
      card({
        id: 'fairy-energy-instance-2',
        cardInstanceId: 'fairy-energy-instance-2',
        label: 'Fairy Energy'
      })
    ];

    const commands = mapEventToBoardAnimations(
      attackEffectEvent('local-player', {
        effectType: 'SEARCH_ENERGY_FROM_DECK_TO_BENCH',
        attachedPokemonInPlayIds: ['local-bench-pokemon-1', 'local-bench-pokemon-2'],
        attachedCardInstanceIds: ['fairy-energy-instance-1', 'fairy-energy-instance-2'],
        pokemonInPlayId: 'local-active'
      }),
      localBoard,
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'PLAYER_MOVE_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-bench-0',
        targetPulseAnchor: 'local-bench-0',
        cardLabel: 'Fairy Energy',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 100 },
      jasmine.objectContaining({
        type: 'PLAYER_MOVE_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-bench-1',
        targetPulseAnchor: 'local-bench-1',
        cardLabel: 'Fairy Energy',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }
    ]);
  });

  it('moves selected basic energies from local deck to hand and shuffles for Delcatty Energy Salon', () => {
    const localBoard = board();
    localBoard.localPlayer.handCards = [
      card({
        id: 'grass-energy-instance',
        cardInstanceId: 'grass-energy-instance',
        label: 'Grass Energy'
      }),
      card({
        id: 'fire-energy-instance',
        cardInstanceId: 'fire-energy-instance',
        label: 'Fire Energy'
      })
    ];

    const commands = mapEventToBoardAnimations(
      attackEffectEvent('local-player', {
        effectType: 'SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND',
        cardInstanceIds: ['grass-energy-instance', 'fire-energy-instance'],
        pokemonInPlayId: 'local-active'
      }),
      localBoard,
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'PLAYER_MOVE_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-hand',
        cardLabel: 'Grass Energy',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 100 },
      jasmine.objectContaining({
        type: 'PLAYER_MOVE_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-hand',
        cardLabel: 'Fire Energy',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }
    ]);
  });
});

describe('mapEventToBoardAnimations Evosoda', () => {
  it('moves the evolution from deck to its target and shuffles', () => {
    const commands = mapEventToBoardAnimations(
      {
        eventId: 'evosoda-evolution',
        gameId: 'game-1',
        eventType: GameEventType.PokemonEvolved,
        stateVersion: 4,
        privateEvent: false,
        occurredAt: '2026-06-27T22:00:00Z',
        payload: {
          playerId: 'local-player',
          pokemonInPlayId: 'local-active',
          effectType: 'SEARCH_EVOLUTION_FROM_DECK'
        }
      },
      boardWithActivePokemon(),
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'PLAYER_MOVE_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-active',
        targetPulseAnchor: 'local-active',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }
    ]);
  });
});

describe('mapEventToBoardAnimations Great Ball', () => {
  it('reveals the selected Pokemon to the opponent and shuffles', () => {
    const commands = mapEventToBoardAnimations(
      greatBallEvent('rival-player', false, {
        cardsRevealed: 7,
        cardsTaken: 1,
        takenCardId: 'pikachu-card-id',
        deckShuffled: true
      }),
      board(),
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'OPPONENT_REVEAL_CARD',
        fromAnchor: 'opponent-deck',
        toAnchor: 'opponent-hand',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'opponent-deck' }
    ]);
  });

  it('moves the selected Pokemon to the local hand and shuffles', () => {
    const commands = mapEventToBoardAnimations(
      greatBallEvent('local-player', true, {
        revealedCardIds: ['card-1', 'card-2'],
        cardIds: ['pikachu-card-id']
      }),
      board(),
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'PLAYER_DRAW_CARD',
        fromAnchor: 'local-deck',
        toAnchor: 'local-hand',
        revealBeforeMove: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }
    ]);
  });

  it('only shuffles when the top cards contain no Pokemon', () => {
    const commands = mapEventToBoardAnimations(
      greatBallEvent('rival-player', false, {
        cardsRevealed: 7,
        cardsTaken: 0,
        deckShuffled: true
      }),
      board(),
      (key) => key
    );

    expect(commands).toEqual([{ type: 'SHUFFLE_DECK', fromAnchor: 'opponent-deck' }]);
  });
});

describe('mapEventToBoardAnimations Talonflame attack effects', () => {
  it('moves the opponent hand into deck and shuffles for Devastating Wind', () => {
    const commands = mapEventToBoardAnimations(
      attackEffectEvent('local-player', {
        effectType: 'SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW',
        opponentPlayerId: 'rival-player',
        shuffledHandCards: 4,
        cardsDrawn: 4,
        pokemonInPlayId: 'local-active'
      }),
      boardWithActivePokemon(),
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'OPPONENT_MOVE_CARD',
        fromAnchor: 'opponent-hand',
        toAnchor: 'opponent-deck',
        useCardBack: true,
        hideLabel: true
      }),
      { type: 'WAIT', durationMs: 90 },
      jasmine.objectContaining({
        type: 'OPPONENT_MOVE_CARD',
        fromAnchor: 'opponent-hand',
        toAnchor: 'opponent-deck',
        useCardBack: true,
        hideLabel: true
      }),
      { type: 'WAIT', durationMs: 90 },
      jasmine.objectContaining({
        type: 'OPPONENT_MOVE_CARD',
        fromAnchor: 'opponent-hand',
        toAnchor: 'opponent-deck',
        useCardBack: true,
        hideLabel: true
      }),
      { type: 'WAIT', durationMs: 150 },
      { type: 'SHUFFLE_DECK', fromAnchor: 'opponent-deck' }
    ]);
  });

  it('discards Fire energies from the attacker for Flare Blitz', () => {
    const commands = mapEventToBoardAnimations(
      attackEffectEvent('local-player', {
        effectType: 'DISCARD_ENERGY',
        pokemonInPlayId: 'local-active',
        discardedEnergyCount: 2,
        discardedCardIds: ['fire-energy-1', 'fire-energy-2']
      }),
      boardWithActivePokemon(),
      (key) => key
    );

    expect(commands).toEqual([
      jasmine.objectContaining({
        type: 'DISCARD_CARD',
        fromAnchor: 'local-active',
        toAnchor: 'local-discard',
        useCardBack: true,
        hideLabel: true
      }),
      { type: 'WAIT', durationMs: 90 },
      jasmine.objectContaining({
        type: 'DISCARD_CARD',
        fromAnchor: 'local-active',
        toAnchor: 'local-discard',
        useCardBack: true,
        hideLabel: true
      })
    ]);
  });
});

function drawEvent(playerId: string, extraPayload: Record<string, unknown> = {}): GameEvent {
  return {
    eventId: `draw-${playerId}`,
    gameId: 'game-1',
    eventType: GameEventType.CardDrawn,
    stateVersion: 2,
    privateEvent: playerId === 'local-player',
    occurredAt: '2026-06-22T22:00:00Z',
    payload: { playerId, ...extraPayload }
  };
}

function attackEffectEvent(playerId: string, extraPayload: Record<string, unknown> = {}): GameEvent {
  return {
    eventId: `attack-effect-${playerId}`,
    gameId: 'game-1',
    eventType: GameEventType.AttackEffectResolved,
    stateVersion: 3,
    privateEvent: playerId === 'local-player',
    occurredAt: '2026-06-22T22:00:01Z',
    payload: { actorPlayerId: playerId, ...extraPayload }
  };
}

function greatBallEvent(
  playerId: string,
  privateEvent: boolean,
  extraPayload: Record<string, unknown>
): GameEvent {
  return {
    eventId: `great-ball-${playerId}-${privateEvent}`,
    gameId: 'game-1',
    eventType: GameEventType.CardDrawn,
    stateVersion: 4,
    privateEvent,
    occurredAt: '2026-06-27T22:00:02Z',
    payload: { playerId, source: 'TRAINER', ...extraPayload }
  };
}

function card(overrides: Partial<BoardCardViewModel>): BoardCardViewModel {
  return {
    id: 'card-instance',
    cardId: 'card-1',
    cardInstanceId: 'card-instance',
    label: 'Card',
    visibility: 'visible',
    faceDown: false,
    rotation: 0,
    ...overrides
  };
}

function board(): BoardGameViewModel {
  return {
    localPlayer: player('local-player', false),
    rivalPlayer: player('rival-player', true)
  } as BoardGameViewModel;
}

function boardWithActivePokemon(): BoardGameViewModel {
  const testBoard = board();
  testBoard.localPlayer.activePokemon = activeSlot('local-active');
  testBoard.rivalPlayer.activePokemon = activeSlot('rival-active');
  return testBoard;
}

function player(id: string, isOpponent: boolean): BoardPlayerViewModel {
  const emptySlot: BoardSlotViewModel = {
    id: `${id}-active`,
    label: 'Active',
    occupied: false,
    card: null,
    pokemon: null
  };
  return {
    id,
    label: id,
    avatarUrl: '',
    connected: true,
    role: isOpponent ? 'opponent' : 'current',
    isOpponent,
    isLocal: !isOpponent,
    isActiveTurn: false,
    setupSelectionSubmitted: false,
    deckCount: 10,
    discardCount: 0,
    prizeCount: 6,
    handCards: [],
    prizeCards: [],
    discardCards: [],
    deckCards: [],
    activePokemon: emptySlot,
    benchSlots: []
  };
}

function benchSlot(pokemonId: string): BoardSlotViewModel {
  const activeCard = card({
    id: `${pokemonId}-card`,
    cardInstanceId: `${pokemonId}-card`,
    label: 'Bench Pokemon'
  });

  return {
    id: pokemonId,
    label: 'Bench',
    occupied: true,
    card: activeCard,
    pokemon: {
      id: pokemonId,
      activeCard,
      evolutionStack: [activeCard],
      attachedEnergyCards: [],
      attachedTrainerCards: [],
      damageCounters: 0,
      specialConditions: [],
      attacks: [],
      abilities: [],
      visualEffects: [],
      actions: [],
      canReceiveEnergy: true,
      canReceiveTrainer: true,
      canRetreatTo: false,
      canPromote: false
    }
  };
}

function activeSlot(pokemonId: string): BoardSlotViewModel {
  const activeCard = card({
    id: `${pokemonId}-card`,
    cardInstanceId: `${pokemonId}-card`,
    label: 'Active Pokemon'
  });

  return {
    id: pokemonId,
    label: 'Active',
    occupied: true,
    card: activeCard,
    pokemon: {
      id: pokemonId,
      activeCard,
      evolutionStack: [activeCard],
      attachedEnergyCards: [],
      attachedTrainerCards: [],
      damageCounters: 0,
      specialConditions: [],
      attacks: [],
      abilities: [],
      visualEffects: [],
      actions: [],
      canReceiveEnergy: true,
      canReceiveTrainer: true,
      canRetreatTo: false,
      canPromote: false
    }
  };
}
