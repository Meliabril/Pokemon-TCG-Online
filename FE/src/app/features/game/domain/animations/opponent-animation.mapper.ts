import { GameEventType } from '../../../../core/models/enums/game/game-event-type.enum';
import { GameEvent } from '../../../../core/models/interfaces/game/game-event.interface';
import {
  MulliganRevealedCard,
  readMulliganExtraCardsGrantedPayload,
  readMulliganHandRevealedPayload,
  readMulliganHandValidatedPayload,
  readMulliganNewHandDrawnPayload
} from '../../../../core/models/interfaces/game/mulligan-event.interface';
import { BoardAnimationCommand, BoardAnimationType } from './board-animation.types';
import {
  BoardCardViewModel,
  BoardGameViewModel,
  BoardPlayerViewModel,
  BoardSlotViewModel
} from '../board/board-game-view-model.interface';
import { UiTranslateFn } from '../../../../core/services/language.service';

const OPPONENT_DECK_ANCHOR = 'opponent-deck';
const OPPONENT_HAND_ANCHOR = 'opponent-hand';
const OPPONENT_MOBILE_HAND_ANCHOR = 'opponent-mobile-hand-button';
const OPPONENT_ACTIVE_ANCHOR = 'opponent-active';
const OPPONENT_DISCARD_ANCHOR = 'opponent-discard';
const OPPONENT_DEFAULT_BENCH_ANCHOR = 'opponent-bench-0';
const LOCAL_HAND_ANCHOR = 'local-hand';
const LOCAL_MOBILE_HAND_ANCHOR = 'local-mobile-hand-button';
const LOCAL_ACTIVE_ANCHOR = 'local-active';
const LOCAL_DISCARD_ANCHOR = 'local-discard';
const LOCAL_DEFAULT_BENCH_ANCHOR = 'local-bench-0';
const MAX_VISIBLE_MULTI_DISCARD_ANIMATIONS = 3;
// Mulligan is intentionally SLOW and legible: a clear beat per step with a banner and a short pause.
const MULLIGAN_REVEAL_HAND_DURATION_MS = 2600;
const MULLIGAN_SUMMARY_MARKER_MS = 120;
const MULLIGAN_BANNER_MARKER_MS = 1300;
const MULLIGAN_STEP_PAUSE_MS = 650;
const MULLIGAN_MOVE_DURATION_MS = 700;
const MULLIGAN_SHUFFLE_DURATION_MS = 1200;
// One clear card-back per step: a single card to the deck and a single card back. More than one reads as
// the animation repeating itself for the same attempt.
const MULLIGAN_RETURN_CARDS = 1;
const MULLIGAN_DRAW_CARDS = 1;
const MULLIGAN_MAX_EXTRA_CARDS = 1;
const MULLIGAN_NEW_HAND_SIZE = 7;
const OPENING_SHUFFLE_DURATION_MS = 420;
const OPENING_DEAL_DURATION_MS = 460;
const OPENING_DEAL_CARDS_PER_PLAYER = 1;
const SELF_DAMAGE_REASONS = new Set([
  'RECOIL_DAMAGE',
  'CONFUSION_SELF_DAMAGE',
  'OPTIONAL_BONUS_RECOIL_DAMAGE',
  'WATER_SHURIKEN'
]);
const RECOIL_DAMAGE_REASONS = new Set(['RECOIL_DAMAGE', 'OPTIONAL_BONUS_RECOIL_DAMAGE']);
const SELF_DAMAGE_DURATION_MS = 520;
const RECOIL_DAMAGE_DURATION_MS = 1000;
const RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE_EFFECT = 'RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE';
const SEARCH_ANY_CARD_FROM_DECK_EFFECT = 'SEARCH_ANY_CARD_FROM_DECK';
const SEARCH_ENERGY_FROM_DECK_TO_BENCH_EFFECT = 'SEARCH_ENERGY_FROM_DECK_TO_BENCH';
const SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND_EFFECT = 'SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND';
const SEARCH_SUPPORTER_FROM_DECK_EFFECT = 'SEARCH_SUPPORTER_FROM_DECK';
const SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW_EFFECT = 'SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW';
const SHUFFLE_OPPONENT_HAND_AND_DRAW_TRAINER_EFFECT = 'SHUFFLE_OPPONENT_HAND_AND_DRAW';
const UPSIDE_DOWN_EVOLUTION_EFFECT = 'UPSIDE_DOWN_EVOLUTION';
const SEARCH_EVOLUTION_FROM_DECK_EFFECT = 'SEARCH_EVOLUTION_FROM_DECK';
const WATER_SHURIKEN_EFFECT = 'WATER_SHURIKEN';
const TRAINER_EVENT_SOURCE = 'TRAINER';

export function mapEventToBoardAnimations(
  event: GameEvent,
  board: BoardGameViewModel,
  t: UiTranslateFn,
  previousBoard: BoardGameViewModel | null = null
): BoardAnimationCommand[] {
  if (event.eventType === GameEventType.OpeningHandsDealt) {
    return mapOpeningHandsDealtAnimations(board);
  }

  const mulliganCommands = mapMulliganEventToAnimations(event, board, t);
  if (mulliganCommands !== null) {
    // Tag every Mulligan command with its source event so the panel/log timeline releases that event
    // only when its animation starts playing (keeps logs in sync with the visuals, no spoilers).
    return mulliganCommands.map((command) => ({ ...command, sourceEventId: event.eventId }));
  }

  const actor = resolveActor(event, board);
  if (!actor) {
    return [];
  }

  switch (event.eventType) {
    case GameEventType.CardDrawn: {
      if (isGreatBallRevealEvent(event)) {
        return greatBallRevealAnimations(event, actor, t);
      }

      const cardsDrawn = Math.max(
        readNumberPayload(event, 'cardsDrawn') ?? readStringArrayPayload(event, 'cardIds').length,
        1
      );

      if (isRedCardDrawEvent(event)) {
        if (!actor.isOpponent && !event.privateEvent) {
          return [];
        }

        return [
          ...handShuffleToDeckAnimations(event, actor, t),
          { type: 'WAIT', durationMs: 120 },
          ...drawCardAnimations(event, actor, t, cardsDrawn)
        ];
      }

      return drawCardAnimations(event, actor, t, cardsDrawn);
    }


    case GameEventType.CardPlayed:
      return [
        {
          type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' : 'PLAYER_MOVE_CARD',
          fromAnchor: actor.handAnchor,
          toAnchor: resolveBenchAnchor(event, actor) ?? actor.defaultBenchAnchor,
          ...resolveCardVisual(event, actor, t),
          revealBeforeMove: true,
          durationMs: 780,
          travelRotationDeg: actor.isOpponent ? 4 : -4
        }
      ];

    case GameEventType.PokemonEvolved: {
      const effectType = readStringPayload(event, 'effectType');
      if (
        effectType !== UPSIDE_DOWN_EVOLUTION_EFFECT &&
        effectType !== SEARCH_EVOLUTION_FROM_DECK_EFFECT
      ) {
        return [];
      }

      const targetAnchor = resolvePokemonAnchor(readStringPayload(event, 'pokemonInPlayId'), board) ?? actor.activeAnchor;
      return [
        {
          type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' : 'PLAYER_MOVE_CARD',
          fromAnchor: actor.deckAnchor,
          toAnchor: targetAnchor,
          targetPulseAnchor: targetAnchor,
          ...resolveCardVisual(event, actor, t),
          revealBeforeMove: true,
          durationMs: 780,
          travelRotationDeg: actor.isOpponent ? -5 : 5
        },
        { type: 'WAIT', durationMs: 150 },
        { type: 'SHUFFLE_DECK', fromAnchor: actor.deckAnchor }
      ];
    }

    case GameEventType.EnergyAttached: {
      const targetAnchor = resolvePokemonAnchor(readStringPayload(event, 'pokemonInPlayId'), board) ?? actor.activeAnchor;

      if (actor.isOpponent) {
        return [
          {
            type: 'OPPONENT_ATTACH_ENERGY',
            fromAnchor: actor.handAnchor,
            toAnchor: targetAnchor,
            targetPulseAnchor: targetAnchor,
            ...resolveCardVisual(event, actor, t),
            durationMs: 760,
            travelRotationDeg: -4
          }
        ];
      }

      // Local player equipping a Pokemon Tool — animate card flying from hand to the target Pokemon
      if (readStringPayload(event, 'attachedCardType') === 'POKEMON_TOOL') {
        return [
          {
            type: 'PLAYER_MOVE_CARD',
            fromAnchor: actor.handAnchor,
            toAnchor: targetAnchor,
            targetPulseAnchor: targetAnchor,
            ...resolveCardVisual(event, actor, t),
            revealBeforeMove: true,
            durationMs: 760,
            travelRotationDeg: -4
          }
        ];
      }

      return [];
    }

    case GameEventType.TrainerPlayed: {
      const effectType = readStringPayload(event, 'effectType');
      const trainerToDiscard: BoardAnimationCommand = {
        type: actor.isOpponent ? 'OPPONENT_PLAY_TRAINER' : 'DISCARD_CARD',
        fromAnchor: actor.handAnchor,
        toAnchor: actor.discardAnchor,
        ...resolveCardVisual(event, actor, t),
        revealBeforeMove: true,
        durationMs: 820,
        travelRotationDeg: 5
      };

      if (effectType === 'DISCARD_HAND_AND_DRAW') {
        const discardedCount = readNumberPayload(event, 'discardedHandCards') ?? 1;
        const visibleMoves = Math.min(Math.max(discardedCount, 1), MAX_VISIBLE_MULTI_DISCARD_ANIMATIONS);
        const handToDiscard = repeatCardMove(visibleMoves, (index) => ({
          type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' as const : 'PLAYER_MOVE_CARD' as const,
          fromAnchor: actor.handAnchor,
          toAnchor: actor.discardAnchor,
          targetPulseAnchor: actor.discardAnchor,
          cardLabel: t('GAME.ANIMATION.OPPONENT_CARD'),
          hideLabel: true,
          useCardBack: true,
          durationMs: 550,
          travelRotationDeg: index % 2 === 0 ? -5 : 5
        }));
        return [trainerToDiscard, { type: 'WAIT' as const, durationMs: 150 }, ...handToDiscard];
      }

      if (effectType === 'SHUFFLE_HAND_AND_DRAW') {
        const shuffledCount = readNumberPayload(event, 'shuffledHandCards') ?? 1;
        const visibleMoves = Math.min(Math.max(shuffledCount, 1), MAX_VISIBLE_MULTI_DISCARD_ANIMATIONS);
        const handToDeck = repeatCardMove(visibleMoves, (index) => ({
          type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' as const : 'PLAYER_MOVE_CARD' as const,
          fromAnchor: actor.handAnchor,
          toAnchor: actor.deckAnchor,
          targetPulseAnchor: actor.deckAnchor,
          cardLabel: t('GAME.ANIMATION.OPPONENT_CARD'),
          hideLabel: true,
          useCardBack: true,
          durationMs: 550,
          travelRotationDeg: index % 2 === 0 ? -5 : 5
        }));
        return [
          trainerToDiscard,
          { type: 'WAIT' as const, durationMs: 150 },
          ...handToDeck,
          { type: 'WAIT' as const, durationMs: 120 },
          { type: 'SHUFFLE_DECK' as const, fromAnchor: actor.deckAnchor }
        ];
      }

      if (effectType === 'RETURN_BENCHED_POKEMON_TO_HAND') {
        return [
          trainerToDiscard,
          { type: 'WAIT' as const, durationMs: 150 },
          { type: 'SHUFFLE_DECK' as const, fromAnchor: actor.deckAnchor }
        ];
      }

      return [trainerToDiscard];
    }

    case GameEventType.CoinFlipped: {
      const trainerCoinResults = readCoinResultsPayload(event);
      if (!trainerCoinResults) {
        return [];
      }
      return [
        {
          type: 'COIN_FLIP' as const,
          coinResults: trainerCoinResults,
          highlightAnchor: actor.activeAnchor,
          coinFlipLabel: actor.isOpponent
            ? t('GAME.ANIMATION.COIN_FLIP_OPPONENT')
            : t('GAME.ANIMATION.COIN_FLIP_SELF')
        }
      ];
    }

    case GameEventType.RetreatDone:
      return energyDiscardAnimations(event, actor, actor.activeAnchor, t);

    case GameEventType.AttackEffectResolved: {
      const targetAnchor = resolvePokemonAnchor(readStringPayload(event, 'pokemonInPlayId'), board);
      const fallbackAnchor = targetAnchor ?? actor.activeAnchor;
      const coinResults = readCoinResultsPayload(event);
      const effectType = readStringPayload(event, 'effectType');

      if (effectType === RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE_EFFECT) {
        const handOwner = resolveActorByPlayerId(readStringPayload(event, 'opponentPlayerId'), board) ?? opponentOf(actor, board);
        const revealedCard = findCardFromEvent(event, handOwner);

        return [
          {
            type: 'OPPONENT_HAND_REVEAL_SHUFFLE',
            fromAnchor: handOwner.handAnchor,
            toAnchor: handOwner.deckAnchor,
            cardImageUrl: revealedCard?.imageSmallUrl ?? revealedCard?.imageLargeUrl ?? undefined,
            cardLabel: revealedCard?.visibility === 'visible'
              ? revealedCard.label
              : t('GAME.ANIMATION.REVEALED_HAND_CARD'),
            targetPulseAnchor: handOwner.deckAnchor,
            durationMs: 1500,
            travelRotationDeg: handOwner.isOpponent ? -6 : 6
          },
          { type: 'WAIT', durationMs: 100 },
          { type: 'SHUFFLE_DECK', fromAnchor: handOwner.deckAnchor }
        ];
      }

      if (effectType === 'HEAL_DAMAGE') {
        if (!targetAnchor) {
          return [];
        }

        return [
          {
            type: 'SELF_HEAL',
            fromAnchor: targetAnchor,
            toAnchor: targetAnchor,
            targetPokemonInPlayId: readStringPayload(event, 'pokemonInPlayId') ?? undefined,
            durationMs: 1100
          }
        ];
      }

      const commands: BoardAnimationCommand[] = [];
      if (isDeckSearchToHandEffect(effectType, event)) {
        commands.push(...deckSearchToHandAnimations(event, actor, t));
      }

      if (effectType === SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW_EFFECT) {
        commands.push(...opponentHandShuffleDrawAnimations(event, actor, board, t));
      }

      if (effectType === SEARCH_ENERGY_FROM_DECK_TO_BENCH_EFFECT) {
        commands.push(...deckSearchToBenchEnergyAnimations(event, actor, board, t));
      }

      if (effectType === SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND_EFFECT) {
        commands.push(...distinctBasicEnergiesToHandAnimations(event, actor, t));
      }

      if (effectType === WATER_SHURIKEN_EFFECT) {
        commands.push(...waterShurikenAnimations(event, actor, board, t));
      }

      if (coinResults) {
        commands.push({
          type: 'COIN_FLIP',
          coinResults,
          highlightAnchor: fallbackAnchor,
          coinFlipLabel: actor.isOpponent
            ? t('GAME.ANIMATION.COIN_FLIP_OPPONENT')
            : t('GAME.ANIMATION.COIN_FLIP_SELF'),
          durationMs: 1700
        });
      }

      commands.push(...energyDiscardAnimations(event, actor, fallbackAnchor, t));
      return commands;
    }

    case GameEventType.PassiveAbilityTriggered: {
      const targetAnchor = resolvePokemonAnchor(readStringPayload(event, 'targetPokemonId'), board);
      const sourceAnchor = resolvePokemonAnchor(readStringPayload(event, 'sourcePokemonId'), board);
      const fallbackAnchor = sourceAnchor ?? targetAnchor ?? actor.activeAnchor;
      const coinResults = readCoinResultsPayload(event);
      const commands: BoardAnimationCommand[] = [];

      if (coinResults) {
        commands.push({
          type: 'COIN_FLIP',
          coinResults,
          highlightAnchor: fallbackAnchor,
          coinFlipLabel: actor.isOpponent
            ? t('GAME.ANIMATION.COIN_FLIP_OPPONENT')
            : t('GAME.ANIMATION.COIN_FLIP_SELF'),
          durationMs: 1700
        });
      }

      return commands;
    }

    case GameEventType.PokemonKnockedOut: {
      const pokemonInPlayId = readStringPayload(event, 'pokemonInPlayId');
      const knockedOutAnchor = resolvePokemonAnchor(pokemonInPlayId, board);
      const referenceBoard = previousBoard ?? board;
      const knockedOutCard = findPokemonCardById(pokemonInPlayId, referenceBoard);
      const totalDiscardedCards = knockoutDiscardCount(actor.playerId, board, previousBoard);
      return knockoutDiscardAnimations(
        actor,
        knockedOutAnchor ?? actor.activeAnchor,
        knockedOutCard,
        totalDiscardedCards,
        t
      );
    }

    case GameEventType.AttackDeclared: {
      const attackerPokemonInPlayId = readStringPayload(event, 'attackerPokemonInPlayId');
      const defenderPokemonInPlayId = readStringPayload(event, 'defenderPokemonInPlayId');
      const attackId = readStringPayload(event, 'attackId');

      if (!shouldShowAttackLunge(attackId, attackerPokemonInPlayId, board)) {
        return [];
      }

      const attackerAnchor = resolvePokemonAnchor(attackerPokemonInPlayId, board) ?? actor.activeAnchor;
      const defenderAnchor =
        resolvePokemonAnchor(defenderPokemonInPlayId, board) ?? opponentOf(actor, board).activeAnchor;

      return [
        {
          type: 'ATTACK_LUNGE',
          fromAnchor: attackerAnchor,
          toAnchor: defenderAnchor,
          targetPokemonInPlayId: defenderPokemonInPlayId ?? undefined
        }
      ];
    }

    case GameEventType.DamageApplied: {
      const reason = readStringPayload(event, 'reason');
      const pokemonInPlayId = readStringPayload(event, 'defenderPokemonInPlayId');
      const targetAnchor = resolvePokemonAnchor(pokemonInPlayId, board);

      if (!reason || !pokemonInPlayId || !targetAnchor || !SELF_DAMAGE_REASONS.has(reason)) {
        return [];
      }

      return [
        {
          type: 'SELF_DAMAGE',
          fromAnchor: targetAnchor,
          toAnchor: targetAnchor,
          targetPokemonInPlayId: pokemonInPlayId,
          durationMs: RECOIL_DAMAGE_REASONS.has(reason) ? RECOIL_DAMAGE_DURATION_MS : SELF_DAMAGE_DURATION_MS
        }
      ];
    }

    default:
      return [];
  }
}

function isGreatBallRevealEvent(event: GameEvent): boolean {
  return (
    readStringPayload(event, 'source') === TRAINER_EVENT_SOURCE &&
    (readNumberPayload(event, 'cardsRevealed') !== null ||
      readStringArrayPayload(event, 'revealedCardIds').length > 0)
  );
}

function isRedCardDrawEvent(event: GameEvent): boolean {
  return readStringPayload(event, 'effectType') === SHUFFLE_OPPONENT_HAND_AND_DRAW_TRAINER_EFFECT;
}

function greatBallRevealAnimations(
  event: GameEvent,
  actor: AnimationActor,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const selectedCardId = readStringPayload(event, 'takenCardId') ?? readDrawnCardId(event);
  if (!selectedCardId) {
    return event.privateEvent
      ? []
      : [{ type: 'SHUFFLE_DECK', fromAnchor: actor.deckAnchor }];
  }

  if (!actor.isOpponent && !event.privateEvent) {
    return [];
  }

  return [
    {
      type: actor.isOpponent ? 'OPPONENT_REVEAL_CARD' : 'PLAYER_DRAW_CARD',
      fromAnchor: actor.deckAnchor,
      toAnchor: actor.handAnchor,
      mobileToAnchor: actor.isOpponent ? OPPONENT_MOBILE_HAND_ANCHOR : LOCAL_MOBILE_HAND_ANCHOR,
      ...resolveCardVisual(event, actor, t),
      cardLabel: readStringPayload(event, 'cardName') ?? t('GAME.ANIMATION.REVEALED_HAND_CARD'),
      revealBeforeMove: true,
      durationMs: 900,
      travelRotationDeg: actor.isOpponent ? -5 : 5
    },
    { type: 'WAIT', durationMs: 150 },
    { type: 'SHUFFLE_DECK', fromAnchor: actor.deckAnchor }
  ];
}

function repeatCardMove(
  amount: number,
  commandForIndex: (index: number) => BoardAnimationCommand
): BoardAnimationCommand[] {
  const commands: BoardAnimationCommand[] = [];
  for (let index = 0; index < amount; index++) {
    if (index > 0) {
      commands.push({ type: 'WAIT', durationMs: 90 });
    }
    commands.push(commandForIndex(index));
  }
  return commands;
}

function drawCardAnimations(
  event: GameEvent,
  actor: AnimationActor,
  t: UiTranslateFn,
  cardsDrawn: number
): BoardAnimationCommand[] {
  if (actor.isOpponent) {
    return repeatCardMove(cardsDrawn, (index) => ({
        type: 'OPPONENT_DRAW_CARD',
        fromAnchor: actor.deckAnchor,
        toAnchor: actor.handAnchor,
        mobileToAnchor: OPPONENT_MOBILE_HAND_ANCHOR,
        cardLabel: cardsDrawn > 1
          ? t('GAME.ANIMATION.DRAWN_CARD_NUMBER', { number: index + 1 })
          : t('GAME.ANIMATION.DRAWN_CARD'),
        durationMs: 760,
        hideLabel: true,
        useCardBack: true,
        travelRotationDeg: -5
      }));
  }

  // Only the private CARD_DRAWN event carrying card identity triggers local draw animation.
  if (!readDrawnCardId(event)) {
    return [];
  }

  return repeatCardMove(cardsDrawn, (index) => ({
      type: 'PLAYER_DRAW_CARD',
      fromAnchor: actor.deckAnchor,
      toAnchor: actor.handAnchor,
      mobileToAnchor: LOCAL_MOBILE_HAND_ANCHOR,
      ...resolveCardVisual(event, actor, t),
      cardLabel: cardsDrawn > 1
        ? t('GAME.ANIMATION.DRAWN_CARD_NUMBER', { number: index + 1 })
        : resolveCardVisual(event, actor, t).cardLabel,
      durationMs: 760,
      revealBeforeMove: index === 0,
      travelRotationDeg: 5
    }));
}

interface AnimationActor {
  playerId: string;
  isOpponent: boolean;
  deckAnchor: string;
  handAnchor: string;
  /** Mobile-only hand target: the visible "hand" button (bottom for local, top for rival). On mobile the
   *  desktop hand zone (handAnchor) is collapsed/mispositioned, so deck->hand moves must target this. */
  mobileHandAnchor: string;
  activeAnchor: string;
  discardAnchor: string;
  defaultBenchAnchor: string;
  benchAnchorPrefix: string;
  cards: BoardCardViewModel[];
}

function resolveActor(event: GameEvent, board: BoardGameViewModel): AnimationActor | null {
  const actorPlayerId =
    readStringPayload(event, 'actorPlayerId') ?? readStringPayload(event, 'revealingPlayerId');
  if (actorPlayerId === board.rivalPlayer.id) {
    return rivalActor(board);
  }

  if (actorPlayerId === board.localPlayer.id) {
    return localActor(board);
  }

  const playerId = readStringPayload(event, 'playerId');
  if (playerId === board.rivalPlayer.id) {
    return rivalActor(board);
  }

  if (playerId === board.localPlayer.id) {
    return localActor(board);
  }

  const opponentPlayerId = readStringPayload(event, 'opponentPlayerId');
  if (opponentPlayerId === board.rivalPlayer.id) {
    return rivalActor(board);
  }

  if (opponentPlayerId === board.localPlayer.id) {
    return localActor(board);
  }

  const pokemonInPlayId =
    readStringPayload(event, 'pokemonInPlayId') ?? readStringPayload(event, 'defenderPokemonInPlayId');
  if (pokemonInPlayId) {
    if (
      board.rivalPlayer.activePokemon.pokemon?.id === pokemonInPlayId ||
      board.rivalPlayer.benchSlots.some((slot) => slot.pokemon?.id === pokemonInPlayId)
    ) {
      return rivalActor(board);
    }

    if (
      board.localPlayer.activePokemon.pokemon?.id === pokemonInPlayId ||
      board.localPlayer.benchSlots.some((slot) => slot.pokemon?.id === pokemonInPlayId)
    ) {
      return localActor(board);
    }
  }

  return null;
}

/**
 * Builds the opening shuffle + deal animation for BOTH players (public event). Card backs only,
 * so no identity is exposed. Kept short/staggered to stay fluid at game start.
 */
function mapOpeningHandsDealtAnimations(board: BoardGameViewModel): BoardAnimationCommand[] {
  const actors = [localActor(board), rivalActor(board)];
  const commands: BoardAnimationCommand[] = [];

  for (const actor of actors) {
    commands.push({
      type: 'OPENING_DECK_SHUFFLE',
      fromAnchor: actor.deckAnchor,
      toAnchor: actor.deckAnchor,
      targetPulseAnchor: actor.deckAnchor,
      useCardBack: true,
      hideLabel: true,
      durationMs: OPENING_SHUFFLE_DURATION_MS,
      travelRotationDeg: 6,
      affectedPlayerId: actor.playerId
    });
  }

  for (let index = 0; index < OPENING_DEAL_CARDS_PER_PLAYER; index++) {
    for (const actor of actors) {
      commands.push({ type: 'WAIT', durationMs: 60 });
      commands.push({
        type: 'OPENING_DEAL_CARD',
        fromAnchor: actor.deckAnchor,
        toAnchor: actor.handAnchor,
        mobileToAnchor: actor.mobileHandAnchor,
        targetPulseAnchor: actor.handAnchor,
        useCardBack: true,
        hideLabel: true,
        durationMs: OPENING_DEAL_DURATION_MS,
        travelRotationDeg: index % 2 === 0 ? -5 : 5,
        affectedPlayerId: actor.playerId
      });
    }
  }

  return commands;
}

/**
 * Maps Mulligan setup events to short, sequential board animations. Returns `null` for
 * non-Mulligan events so the regular animation pipeline takes over. These ride the same
 * event-driven dedup pipeline (game-page.component.ts), so they never replay on refresh.
 */
function mapMulliganEventToAnimations(
  event: GameEvent,
  board: BoardGameViewModel,
  t: UiTranslateFn
): BoardAnimationCommand[] | null {
  switch (event.eventType) {
    case GameEventType.MulliganHandRevealed: {
      const payload = readMulliganHandRevealedPayload(event.payload);
      const actor = payload ? resolveActorByPlayerId(payload.revealingPlayerId, board) : null;
      if (!payload || !actor) {
        return [];
      }

      // Reveal the WHOLE invalid hand (allowed exception): all 7 cards surface face-up in the revealing
      // player's hand zone (own zone for the owner, rival zone for the opponent) headlined by ¡MULLIGAN!,
      // held long enough to read, then a short pause before it retracts.
      return [
        {
          type: 'MULLIGAN_REVEAL_HAND',
          targetPulseAnchor: actor.handAnchor,
          revealedHandCards: payload.revealedCards,
          durationMs: MULLIGAN_REVEAL_HAND_DURATION_MS,
          affectedPlayerId: actor.playerId,
          banner: { key: 'GAME.MULLIGAN.BANNER.MULLIGAN', params: { n: payload.mulliganNumber } }
        },
        { type: 'WAIT', durationMs: MULLIGAN_STEP_PAUSE_MS, affectedPlayerId: actor.playerId }
      ];
    }

    case GameEventType.MulliganHandReturned: {
      const actor = resolveActor(event, board);
      if (!actor) {
        return [];
      }

      return withBanner(
        mulliganCardBackMoves(
          'MULLIGAN_RETURN_CARD', actor.handAnchor, actor.deckAnchor, MULLIGAN_RETURN_CARDS, actor.playerId),
        { key: 'GAME.MULLIGAN.BANNER.RETURNING' });
    }

    case GameEventType.MulliganDeckShuffled: {
      const actor = resolveActor(event, board);
      if (!actor) {
        return [];
      }

      return [
        {
          type: 'MULLIGAN_SHUFFLE',
          fromAnchor: actor.deckAnchor,
          toAnchor: actor.deckAnchor,
          targetPulseAnchor: actor.deckAnchor,
          useCardBack: true,
          hideLabel: true,
          durationMs: MULLIGAN_SHUFFLE_DURATION_MS,
          travelRotationDeg: 6,
          affectedPlayerId: actor.playerId,
          banner: { key: 'GAME.MULLIGAN.BANNER.SHUFFLING' }
        }
      ];
    }

    case GameEventType.MulliganNewHandDrawn: {
      // Private to the owner: animate the owner's own draw of the new hand (card backs), then SHOW the
      // real drawn hand when the last card lands — for EVERY attempt, including still-invalid ones.
      const payload = readMulliganNewHandDrawnPayload(event.payload);
      const actor = payload ? resolveActorByPlayerId(payload.playerId, board) : null;
      if (!payload || !actor) {
        return [];
      }

      const draws = withBanner(
        mulliganCardBackMoves(
          'MULLIGAN_DRAW_CARD', actor.deckAnchor, actor.handAnchor, MULLIGAN_DRAW_CARDS, actor.playerId,
          actor.mobileHandAnchor),
        { key: 'GAME.MULLIGAN.BANNER.NEW_HAND' });
      return withTrailingPause(
        settleHandOnLastMove(draws, { playerId: actor.playerId, cards: payload.drawnCards }),
        actor.playerId);
    }

    case GameEventType.MulliganHandValidated: {
      const payload = readMulliganHandValidatedPayload(event.payload);
      if (!payload) {
        return [];
      }

      // The owner already drew (and showed) its new hand off the private MULLIGAN_NEW_HAND_DRAWN; here it
      // only gets the "valid hand" headline when applicable. The OBSERVER never receives the private draw,
      // so it animates the rival's new draw (card backs) off this public event and SHOWS the rival's hand
      // as face-down cards when the draw lands — for every attempt, not only the final one.
      if (payload.playerId === board.localPlayer.id) {
        return payload.hasBasic ? [mulliganBannerMarker('GAME.MULLIGAN.BANNER.VALID_HAND')] : [];
      }

      const actor = resolveActorByPlayerId(payload.playerId, board);
      if (!actor) {
        return [];
      }

      const draws = withBanner(
        mulliganCardBackMoves(
          'MULLIGAN_DRAW_CARD', actor.deckAnchor, actor.handAnchor, MULLIGAN_DRAW_CARDS, actor.playerId,
          actor.mobileHandAnchor),
        { key: 'GAME.MULLIGAN.BANNER.NEW_HAND' });
      const validatedCommands = withTrailingPause(
        settleHandOnLastMove(draws, { playerId: actor.playerId, faceDownCount: MULLIGAN_NEW_HAND_SIZE }),
        actor.playerId);
      return payload.hasBasic
        ? [...validatedCommands, mulliganBannerMarker('GAME.MULLIGAN.BANNER.VALID_HAND')]
        : validatedCommands;
    }

    case GameEventType.MulliganExtraCardsGranted: {
      const payload = readMulliganExtraCardsGrantedPayload(event.payload);
      const actor = payload ? resolveActorByPlayerId(payload.playerId, board) : null;
      if (!payload || !actor) {
        return [];
      }

      // Extra cards are dealt at the very END of the whole flow. The receiver's snapshot hand already
      // includes them, but they stay visually hidden (sliced) until THIS animation lands — at which point
      // the last command clears the pending slice so the +N appear together with the animation.
      const cardCount = Math.max(1, Math.min(MULLIGAN_MAX_EXTRA_CARDS, payload.cardsGranted));
      const extraCommands = withBanner(
        mulliganCardBackMoves(
          'MULLIGAN_EXTRA_CARD', actor.deckAnchor, actor.handAnchor, cardCount, undefined,
          actor.mobileHandAnchor),
        { key: 'GAME.MULLIGAN.BANNER.EXTRA_CARDS', params: { count: payload.cardsGranted } });
      return extraCommands.map((command, index) =>
        index === extraCommands.length - 1
          ? { ...command, clearsPendingExtraForPlayerId: actor.playerId }
          : command);
    }

    case GameEventType.MulliganSequenceCompleted:
      // No board movement; emit a short marker so the panel's own-summary entry is released in order
      // (right after this player's draw animations), keeping the log in sync with the visuals.
      return [{ type: 'WAIT', durationMs: MULLIGAN_SUMMARY_MARKER_MS }];

    case GameEventType.MulliganFlowCompleted:
      // The whole flow is done (this is the last event). Reset the per-player hand staging so the real
      // snapshot hands take over — required for initial Pokémon selection, which uses the real hand cards.
      return [{ type: 'WAIT', durationMs: MULLIGAN_SUMMARY_MARKER_MS, clearsMulliganStaging: true }];

    default:
      return null;
  }
}

/** Attaches a floating banner to the first command of a group so it shows when the group starts. */
function withBanner(
  commands: BoardAnimationCommand[],
  banner: { key: string; params?: Record<string, string | number> }
): BoardAnimationCommand[] {
  if (commands.length === 0) {
    return commands;
  }

  return commands.map((command, index) => (index === 0 ? { ...command, banner } : command));
}

/** Appends a short pause that keeps the affected player "animating" (hand gated) between steps. */
function withTrailingPause(commands: BoardAnimationCommand[], affectedPlayerId: string): BoardAnimationCommand[] {
  if (commands.length === 0) {
    return commands;
  }

  return [...commands, { type: 'WAIT', durationMs: MULLIGAN_STEP_PAUSE_MS, affectedPlayerId }];
}

/** A banner-only beat (no board movement) so a headline like "Valid hand!" can be read on its own. */
function mulliganBannerMarker(
  key: string,
  params?: Record<string, string | number>
): BoardAnimationCommand {
  return { type: 'WAIT', durationMs: MULLIGAN_BANNER_MARKER_MS, banner: { key, params } };
}

/** Tags the last move of a draw so that, when it lands, the player's just-drawn hand is shown. */
function settleHandOnLastMove(
  commands: BoardAnimationCommand[],
  settleHand: { playerId: string; cards?: MulliganRevealedCard[]; faceDownCount?: number }
): BoardAnimationCommand[] {
  if (commands.length === 0) {
    return commands;
  }

  return commands.map((command, index) =>
    index === commands.length - 1 ? { ...command, settleHand } : command);
}

function mulliganCardBackMoves(
  type: BoardAnimationType,
  fromAnchor: string,
  toAnchor: string,
  count: number,
  affectedPlayerId?: string,
  mobileToAnchor?: string
): BoardAnimationCommand[] {
  const commands: BoardAnimationCommand[] = [];
  for (let index = 0; index < count; index++) {
    if (index > 0) {
      commands.push({ type: 'WAIT', durationMs: 70 });
    }

    commands.push({
      type,
      fromAnchor,
      toAnchor,
      mobileToAnchor,
      targetPulseAnchor: toAnchor,
      useCardBack: true,
      hideLabel: true,
      durationMs: MULLIGAN_MOVE_DURATION_MS,
      travelRotationDeg: index % 2 === 0 ? -5 : 5,
      affectedPlayerId
    });
  }

  return commands;
}

function resolveActorByPlayerId(playerId: string | null, board: BoardGameViewModel): AnimationActor | null {
  if (!playerId) {
    return null;
  }

  if (playerId === board.rivalPlayer.id) {
    return rivalActor(board);
  }

  if (playerId === board.localPlayer.id) {
    return localActor(board);
  }

  return null;
}

function opponentOf(actor: AnimationActor, board: BoardGameViewModel): AnimationActor {
  return actor.playerId === board.rivalPlayer.id ? localActor(board) : rivalActor(board);
}

function rivalActor(board: BoardGameViewModel): AnimationActor {
  return {
    playerId: board.rivalPlayer.id,
    isOpponent: true,
    deckAnchor: OPPONENT_DECK_ANCHOR,
    handAnchor: OPPONENT_HAND_ANCHOR,
    mobileHandAnchor: OPPONENT_MOBILE_HAND_ANCHOR,
    activeAnchor: OPPONENT_ACTIVE_ANCHOR,
    discardAnchor: OPPONENT_DISCARD_ANCHOR,
    defaultBenchAnchor: OPPONENT_DEFAULT_BENCH_ANCHOR,
    benchAnchorPrefix: 'opponent-bench',
    cards: playerCards(board.rivalPlayer)
  };
}

function localActor(board: BoardGameViewModel): AnimationActor {
  return {
    playerId: board.localPlayer.id,
    isOpponent: false,
    deckAnchor: 'local-deck',
    handAnchor: LOCAL_HAND_ANCHOR,
    mobileHandAnchor: LOCAL_MOBILE_HAND_ANCHOR,
    activeAnchor: LOCAL_ACTIVE_ANCHOR,
    discardAnchor: LOCAL_DISCARD_ANCHOR,
    defaultBenchAnchor: LOCAL_DEFAULT_BENCH_ANCHOR,
    benchAnchorPrefix: 'local-bench',
    cards: playerCards(board.localPlayer)
  };
}

function resolveBenchAnchor(event: GameEvent, actor: AnimationActor): string | null {
  const slotPosition = readNumberPayload(event, 'slotPosition');
  if (typeof slotPosition !== 'number') {
    return null;
  }

  const anchorIndex = Math.max(0, Math.min(4, slotPosition - 1));
  return `${actor.benchAnchorPrefix}-${anchorIndex}`;
}

function resolveCardVisual(
  event: GameEvent,
  actor: AnimationActor,
  t: UiTranslateFn,
  preferredCard: BoardCardViewModel | null = null
): Pick<BoardAnimationCommand, 'cardImageUrl' | 'cardLabel'> {
  const matchingCard = preferredCard ?? findCardFromEvent(event, actor);
  return {
    cardImageUrl: matchingCard?.imageSmallUrl ?? matchingCard?.imageLargeUrl ?? undefined,
    cardLabel:
      readStringPayload(event, 'cardName') ??
      readStringPayload(event, 'name') ??
      matchingCard?.label ??
      (actor.isOpponent ? t('GAME.ANIMATION.OPPONENT_CARD') : t('GAME.CARD_FALLBACK'))
  };
}

function findCardFromEvent(
  event: GameEvent,
  actor: AnimationActor
): BoardCardViewModel | null {
  const cardInstanceId = readStringPayload(event, 'cardInstanceId') ?? readStringPayload(event, 'revealedCardInstanceId');
  const cardId = readDrawnCardId(event) ?? readStringPayload(event, 'revealedCardId');

  return (
    actor.cards.find((card) => card.cardInstanceId === cardInstanceId) ??
    actor.cards.find((card) => card.cardId === cardId) ??
    null
  );
}

function findCardByInstanceId(
  cardInstanceId: string | null,
  actor: AnimationActor
): BoardCardViewModel | null {
  if (!cardInstanceId) {
    return null;
  }

  return actor.cards.find((card) => card.cardInstanceId === cardInstanceId) ?? null;
}

function playerCards(player: BoardPlayerViewModel): BoardCardViewModel[] {
  return [
    ...player.handCards,
    ...player.discardCards,
    ...player.deckCards,
    ...cardsFromSlot(player.activePokemon),
    ...player.benchSlots.flatMap(cardsFromSlot)
  ];
}

function cardsFromSlot(slot: BoardSlotViewModel): BoardCardViewModel[] {
  return [
    slot.card ?? null,
    ...(slot.pokemon?.attachedEnergyCards ?? []),
    ...(slot.pokemon?.attachedTrainerCards ?? [])
  ].filter((card): card is BoardCardViewModel => card !== null);
}

function resolvePokemonAnchor(
  pokemonInPlayId: string | null,
  board: BoardGameViewModel
): string | undefined {
  if (!pokemonInPlayId) {
    return undefined;
  }

  if (board.rivalPlayer.activePokemon.pokemon?.id === pokemonInPlayId) {
    return OPPONENT_ACTIVE_ANCHOR;
  }

  const rivalBenchIndex = board.rivalPlayer.benchSlots.findIndex((slot) => slot.pokemon?.id === pokemonInPlayId);
  if (rivalBenchIndex >= 0) {
    return `opponent-bench-${rivalBenchIndex}`;
  }

  if (board.localPlayer.activePokemon.pokemon?.id === pokemonInPlayId) {
    return 'local-active';
  }

  const localBenchIndex = board.localPlayer.benchSlots.findIndex((slot) => slot.pokemon?.id === pokemonInPlayId);
  return localBenchIndex >= 0 ? `local-bench-${localBenchIndex}` : undefined;
}

function shouldShowAttackLunge(
  attackId: string | null,
  attackerPokemonInPlayId: string | null,
  board: BoardGameViewModel
): boolean {
  if (!attackId || !attackerPokemonInPlayId) {
    return true;
  }

  const slots = [
    board.rivalPlayer.activePokemon,
    ...board.rivalPlayer.benchSlots,
    board.localPlayer.activePokemon,
    ...board.localPlayer.benchSlots
  ];
  const attack = slots
    .find((slot) => slot.pokemon?.id === attackerPokemonInPlayId)
    ?.pokemon?.attacks.find((candidate) => candidate.id === attackId);

  if (!attack) {
    return true;
  }

  const hasBaseDamage = typeof attack.baseDamage === 'number' && attack.baseDamage > 0;
  const hasDamageText = Boolean(attack.damageText && attack.damageText.trim().length > 0);
  return hasBaseDamage || hasDamageText || hasCoinFlipText(attack.effectText, attack.displayText);
}

function hasCoinFlipText(...texts: (string | null | undefined)[]): boolean {
  return texts.some((text) => {
    const normalizedText = text?.toLowerCase() ?? '';
    return normalizedText.includes('coin') || normalizedText.includes('moneda');
  });
}

function findPokemonCardById(pokemonInPlayId: string | null, board: BoardGameViewModel): BoardCardViewModel | null {
  if (!pokemonInPlayId) {
    return null;
  }

  const slots = [
    board.rivalPlayer.activePokemon,
    ...board.rivalPlayer.benchSlots,
    board.localPlayer.activePokemon,
    ...board.localPlayer.benchSlots
  ];
  return slots.find((slot) => slot.pokemon?.id === pokemonInPlayId)?.card ?? null;
}

function energyDiscardAnimations(
  event: GameEvent,
  actor: AnimationActor,
  fromAnchor: string,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const discardCount = readNumberPayload(event, 'discardedEnergyCount') ?? readStringArrayPayload(event, 'discardedCardIds').length;
  if (discardCount <= 0) {
    return [];
  }

  const visibleAnimations = Math.min(discardCount, 2);
  const commands: BoardAnimationCommand[] = [];
  for (let index = 0; index < visibleAnimations; index++) {
    if (index > 0) {
      commands.push({ type: 'WAIT', durationMs: 90 });
    }

    commands.push({
      type: 'DISCARD_CARD',
      fromAnchor,
      toAnchor: actor.discardAnchor,
      targetPulseAnchor: actor.discardAnchor,
      cardLabel: discardCount > 1
        ? t('GAME.ANIMATION.DISCARDED_ENERGY_NUMBER', { number: index + 1 })
        : t('GAME.ANIMATION.DISCARDED_ENERGY'),
      hideLabel: true,
      useCardBack: true,
      durationMs: 700,
      travelRotationDeg: index % 2 === 0 ? -5 : 5
    });
  }

  return commands;
}

function opponentHandShuffleDrawAnimations(
  event: GameEvent,
  actor: AnimationActor,
  board: BoardGameViewModel,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const handOwner = resolveActorByPlayerId(readStringPayload(event, 'opponentPlayerId'), board) ?? opponentOf(actor, board);
  return handShuffleToDeckAnimations(event, handOwner, t);
}

function handShuffleToDeckAnimations(
  event: GameEvent,
  handOwner: AnimationActor,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const shuffledHandCards = readNumberPayload(event, 'shuffledHandCards') ?? 1;
  const visibleAnimations = Math.min(Math.max(shuffledHandCards, 1), MAX_VISIBLE_MULTI_DISCARD_ANIMATIONS);
  const commands = repeatCardMove(visibleAnimations, (index) => ({
    type: handOwner.isOpponent ? 'OPPONENT_MOVE_CARD' : 'PLAYER_MOVE_CARD',
    fromAnchor: handOwner.handAnchor,
    toAnchor: handOwner.deckAnchor,
    targetPulseAnchor: handOwner.deckAnchor,
    cardLabel: shuffledHandCards > 1
      ? t('GAME.ANIMATION.DRAWN_CARD_NUMBER', { number: index + 1 })
      : t('GAME.ANIMATION.OPPONENT_CARD'),
    hideLabel: true,
    useCardBack: true,
    durationMs: 700,
    travelRotationDeg: index % 2 === 0 ? -5 : 5
  }));

  commands.push({ type: 'WAIT', durationMs: 150 });
  commands.push({ type: 'SHUFFLE_DECK', fromAnchor: handOwner.deckAnchor });
  return commands;
}

function waterShurikenAnimations(
  event: GameEvent,
  actor: AnimationActor,
  board: BoardGameViewModel,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const discardedCard = findCardByInstanceId(readStringPayload(event, 'discardedCardInstanceId'), actor);
  const targetAnchor = resolvePokemonAnchor(readStringPayload(event, 'targetPokemonInPlayId'), board);
  const commands: BoardAnimationCommand[] = [
    {
      type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' : 'DISCARD_CARD',
      fromAnchor: actor.handAnchor,
      toAnchor: actor.discardAnchor,
      targetPulseAnchor: actor.discardAnchor,
      ...resolveCardVisual(event, actor, t, discardedCard),
      durationMs: 760,
      revealBeforeMove: Boolean(discardedCard),
      travelRotationDeg: actor.isOpponent ? -5 : 5
    },
    { type: 'WAIT', durationMs: 120 }
  ];

  if (targetAnchor) {
    commands.push({
      type: 'SELF_DAMAGE',
      fromAnchor: targetAnchor,
      toAnchor: targetAnchor,
      targetPokemonInPlayId: readStringPayload(event, 'targetPokemonInPlayId') ?? undefined,
      durationMs: 650
    });
  }

  return commands;
}

function isDeckSearchToHandEffect(effectType: string | null, event: GameEvent): boolean {
  return (
    (effectType === SEARCH_SUPPORTER_FROM_DECK_EFFECT && readBooleanPayload(event, 'foundSupporter')) ||
    (effectType === SEARCH_ANY_CARD_FROM_DECK_EFFECT && readBooleanPayload(event, 'foundCard'))
  );
}

function deckSearchToHandAnimations(
  event: GameEvent,
  actor: AnimationActor,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  return [
    {
      type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' : 'PLAYER_MOVE_CARD',
      fromAnchor: actor.deckAnchor,
      toAnchor: actor.handAnchor,
      ...resolveCardVisual(event, actor, t),
      revealBeforeMove: true,
      durationMs: 780,
      travelRotationDeg: actor.isOpponent ? -5 : 5
    },
    { type: 'WAIT', durationMs: 150 },
    {
      type: 'SHUFFLE_DECK',
      fromAnchor: actor.deckAnchor
    }
  ];
}

function deckSearchToBenchEnergyAnimations(
  event: GameEvent,
  actor: AnimationActor,
  board: BoardGameViewModel,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const targetPokemonIds = readStringArrayPayload(event, 'attachedPokemonInPlayIds');
  const attachedCardInstanceIds = readStringArrayPayload(event, 'attachedCardInstanceIds');
  if (targetPokemonIds.length === 0) {
    return [];
  }

  const commands: BoardAnimationCommand[] = [];
  targetPokemonIds.forEach((pokemonInPlayId, index) => {
    const targetAnchor = resolvePokemonAnchor(pokemonInPlayId, board);
    if (!targetAnchor) {
      return;
    }

    const attachedCard = findCardByInstanceId(attachedCardInstanceIds[index] ?? null, actor);
    if (commands.length > 0) {
      commands.push({ type: 'WAIT', durationMs: 100 });
    }

    commands.push({
      type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' : 'PLAYER_MOVE_CARD',
      fromAnchor: actor.deckAnchor,
      toAnchor: targetAnchor,
      targetPulseAnchor: targetAnchor,
      cardImageUrl: attachedCard?.imageSmallUrl ?? attachedCard?.imageLargeUrl ?? undefined,
      cardLabel: attachedCard?.label ?? t('GAME.ATTACHED_ENERGY_FALLBACK'),
      revealBeforeMove: true,
      durationMs: 780,
      travelRotationDeg: actor.isOpponent ? -5 : 5
    });
  });

  if (commands.length > 0) {
    commands.push({ type: 'WAIT', durationMs: 150 });
    commands.push({ type: 'SHUFFLE_DECK', fromAnchor: actor.deckAnchor });
  }

  return commands;
}

function distinctBasicEnergiesToHandAnimations(
  event: GameEvent,
  actor: AnimationActor,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const cardInstanceIds = readStringArrayPayload(event, 'cardInstanceIds');
  if (cardInstanceIds.length === 0) {
    return [];
  }

  const commands: BoardAnimationCommand[] = [];
  cardInstanceIds.forEach((cardInstanceId, index) => {
    const selectedCard = findCardByInstanceId(cardInstanceId, actor);
    if (index > 0) {
      commands.push({ type: 'WAIT', durationMs: 100 });
    }

    commands.push({
      type: actor.isOpponent ? 'OPPONENT_MOVE_CARD' : 'PLAYER_MOVE_CARD',
      fromAnchor: actor.deckAnchor,
      toAnchor: actor.handAnchor,
      ...resolveCardVisual(event, actor, t, selectedCard),
      revealBeforeMove: true,
      durationMs: 780,
      travelRotationDeg: actor.isOpponent ? -5 : 5
    });
  });

  commands.push({ type: 'WAIT', durationMs: 150 });
  commands.push({ type: 'SHUFFLE_DECK', fromAnchor: actor.deckAnchor });
  return commands;
}

function knockoutDiscardAnimations(
  actor: AnimationActor,
  fromAnchor: string,
  knockedOutCard: BoardCardViewModel | null,
  totalDiscardedCards: number,
  t: UiTranslateFn
): BoardAnimationCommand[] {
  const visibleDiscardAnimations = Math.min(Math.max(totalDiscardedCards, 1), MAX_VISIBLE_MULTI_DISCARD_ANIMATIONS);
  const commands: BoardAnimationCommand[] = [
    {
      type: 'DISCARD_CARD',
      fromAnchor,
      toAnchor: actor.discardAnchor,
      targetPulseAnchor: actor.discardAnchor,
      cardImageUrl: knockedOutCard?.imageSmallUrl ?? knockedOutCard?.imageLargeUrl ?? undefined,
      cardLabel: actor.isOpponent
        ? t('GAME.ANIMATION.DISCARDED_OPPONENT_POKEMON')
        : t('GAME.ANIMATION.DISCARDED_POKEMON'),
      useCardBack: !knockedOutCard,
      durationMs: 860,
      travelRotationDeg: actor.isOpponent ? 6 : -6
    }
  ];

  for (let index = 1; index < visibleDiscardAnimations; index++) {
    commands.push({ type: 'WAIT', durationMs: 80 });
    commands.push({
      type: 'DISCARD_CARD',
      fromAnchor,
      toAnchor: actor.discardAnchor,
      targetPulseAnchor: actor.discardAnchor,
      cardLabel: t('GAME.DISCARD'),
      hideLabel: true,
      useCardBack: true,
      durationMs: 680,
      travelRotationDeg: index % 2 === 0 ? -5 : 5
    });
  }

  return commands;
}

function knockoutDiscardCount(
  playerId: string,
  board: BoardGameViewModel,
  previousBoard: BoardGameViewModel | null
): number {
  if (!previousBoard) {
    return 1;
  }

  const previousPlayer = playerId === previousBoard.localPlayer.id
    ? previousBoard.localPlayer
    : playerId === previousBoard.rivalPlayer.id
      ? previousBoard.rivalPlayer
      : null;
  const currentPlayer = playerId === board.localPlayer.id
    ? board.localPlayer
    : playerId === board.rivalPlayer.id
      ? board.rivalPlayer
      : null;

  if (!previousPlayer || !currentPlayer) {
    return 1;
  }

  return Math.max(currentPlayer.discardCount - previousPlayer.discardCount, 1);
}

function readStringPayload(event: GameEvent, key: string): string | null {
  const value = event.payload[key];
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function readDrawnCardId(event: GameEvent): string | null {
  const cardId = readStringPayload(event, 'cardId');
  if (cardId) {
    return cardId;
  }

  const cardIds = event.payload['cardIds'];
  if (!Array.isArray(cardIds)) {
    return null;
  }

  const firstCardId = cardIds.find((candidate): candidate is string =>
    typeof candidate === 'string' && candidate.trim().length > 0
  );
  return firstCardId ?? null;
}

function readBooleanPayload(event: GameEvent, key: string): boolean {
  return event.payload[key] === true;
}

function readNumberPayload(event: GameEvent, key: string): number | null {
  const value = event.payload[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function readStringArrayPayload(event: GameEvent, key: string): string[] {
  const value = event.payload[key];
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((entry): entry is string => typeof entry === 'string' && entry.trim().length > 0);
}

function readCoinResultsPayload(event: GameEvent): ('HEADS' | 'TAILS')[] | null {
  const payloadCoinResults = event.payload['coinResults'];
  if (!Array.isArray(payloadCoinResults) || payloadCoinResults.length === 0) {
    return null;
  }

  const coinResults = payloadCoinResults.filter(
    (coinResult): coinResult is 'HEADS' | 'TAILS' => coinResult === 'HEADS' || coinResult === 'TAILS'
  );
  return coinResults.length > 0 ? coinResults : null;
}
