import { CardCategory } from '../../enums/card/card-category.enum';
import { CardSupertype } from '../../enums/card/card-supertype.enum';

export interface MulliganRevealedCard {
  cardId: string;
  name: string | null;
  externalId: string | null;
  setCode: string | null;
  number: string | null;
  supertype: CardSupertype | null;
  category: CardCategory | null;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  hp: number | null;
}

export interface MulliganHandRevealedPayload {
  revealingPlayerId: string;
  mulliganNumber: number;
  mulliganCount: number;
  revealedCardIds: string[];
  revealedCards: MulliganRevealedCard[];
  extraCardsGranted: number;
  pendingExtraCardsForViewer: number;
}

export interface MulliganSequenceCompletedPayload {
  playerId: string;
  mulliganCount: number;
  extraCardsGrantedToOpponent: number;
  opponentPlayerId: string;
  automatic: boolean;
}

export interface MulliganNoticeAcknowledgedPayload {
  playerId: string;
  acknowledged: boolean;
  automatic: boolean;
  checkpoint: string | null;
  roundNumber: number;
}

export interface MulliganRequiredPayload {
  playerId: string;
  mulliganNumber: number;
  mulliganCount: number;
  roundNumber: number;
  automatic: boolean;
}

export interface MulliganStepPayload {
  playerId: string;
  mulliganNumber: number;
  roundNumber: number;
  automatic: boolean;
}

export interface MulliganNewHandDrawnPayload extends MulliganStepPayload {
  cardsDrawn: number;
  /** The owner's freshly drawn hand (private to the owner) so the new hand can be shown after each draw. */
  drawnCards: MulliganRevealedCard[];
}

export interface MulliganHandValidatedPayload extends MulliganStepPayload {
  hasBasic: boolean;
  willRepeat: boolean;
}

export interface MulliganExtraCardsGrantedPayload {
  playerId: string;
  sourceMulliganPlayerId: string;
  cardsGranted: number;
  automatic: boolean;
}

export interface MulliganFlowCompletedPayload {
  roundNumber: number;
  automatic: boolean;
}

export function readMulliganHandRevealedPayload(
  payload: Record<string, unknown>
): MulliganHandRevealedPayload | null {
  const revealingPlayerId = readString(payload['revealingPlayerId']);
  const mulliganNumber = readNumber(payload['mulliganNumber']) ?? 1;

  if (!revealingPlayerId) {
    return null;
  }

  return {
    revealingPlayerId,
    mulliganNumber,
    mulliganCount: readNumber(payload['mulliganCount']) ?? mulliganNumber,
    revealedCardIds: readStringArray(payload['revealedCardIds']),
    revealedCards: readRevealedCards(payload['revealedCards']),
    extraCardsGranted: readNumber(payload['extraCardsGranted']) ?? 1,
    pendingExtraCardsForViewer: readNumber(payload['pendingExtraCardsForViewer']) ?? mulliganNumber
  };
}

export function readMulliganSequenceCompletedPayload(
  payload: Record<string, unknown>
): MulliganSequenceCompletedPayload | null {
  const playerId = readString(payload['playerId']);
  const opponentPlayerId = readString(payload['opponentPlayerId']);
  const mulliganCount = readNumber(payload['mulliganCount']);

  if (!playerId || !opponentPlayerId || mulliganCount === null) {
    return null;
  }

  return {
    playerId,
    mulliganCount,
    extraCardsGrantedToOpponent: readNumber(payload['extraCardsGrantedToOpponent']) ?? mulliganCount,
    opponentPlayerId,
    automatic: readBoolean(payload['automatic']) ?? true
  };
}

export function readMulliganNoticeAcknowledgedPayload(
  payload: Record<string, unknown>
): MulliganNoticeAcknowledgedPayload | null {
  const playerId = readString(payload['playerId']);

  if (!playerId) {
    return null;
  }

  return {
    playerId,
    acknowledged: readBoolean(payload['acknowledged']) ?? true,
    automatic: readBoolean(payload['automatic']) ?? true,
    checkpoint: readString(payload['checkpoint']),
    roundNumber: readNumber(payload['roundNumber']) ?? 0
  };
}

export function readMulliganRequiredPayload(
  payload: Record<string, unknown>
): MulliganRequiredPayload | null {
  const playerId = readString(payload['playerId']);
  const mulliganNumber = readNumber(payload['mulliganNumber']) ?? 1;

  if (!playerId) {
    return null;
  }

  return {
    playerId,
    mulliganNumber,
    mulliganCount: readNumber(payload['mulliganCount']) ?? mulliganNumber,
    roundNumber: readNumber(payload['roundNumber']) ?? mulliganNumber,
    automatic: readBoolean(payload['automatic']) ?? true
  };
}

export function readMulliganStepPayload(
  payload: Record<string, unknown>
): MulliganStepPayload | null {
  const playerId = readString(payload['playerId']);

  if (!playerId) {
    return null;
  }

  return {
    playerId,
    mulliganNumber: readNumber(payload['mulliganNumber']) ?? 1,
    roundNumber: readNumber(payload['roundNumber']) ?? 1,
    automatic: readBoolean(payload['automatic']) ?? true
  };
}

export function readMulliganNewHandDrawnPayload(
  payload: Record<string, unknown>
): MulliganNewHandDrawnPayload | null {
  const step = readMulliganStepPayload(payload);

  if (!step) {
    return null;
  }

  return {
    ...step,
    cardsDrawn: readNumber(payload['cardsDrawn']) ?? 7,
    drawnCards: readRevealedCards(payload['drawnCards'])
  };
}

export function readMulliganHandValidatedPayload(
  payload: Record<string, unknown>
): MulliganHandValidatedPayload | null {
  const step = readMulliganStepPayload(payload);

  if (!step) {
    return null;
  }

  return {
    ...step,
    hasBasic: readBoolean(payload['hasBasic']) ?? false,
    willRepeat: readBoolean(payload['willRepeat']) ?? false
  };
}

export function readMulliganExtraCardsGrantedPayload(
  payload: Record<string, unknown>
): MulliganExtraCardsGrantedPayload | null {
  const playerId = readString(payload['playerId']);
  const sourceMulliganPlayerId = readString(payload['sourceMulliganPlayerId']);

  if (!playerId || !sourceMulliganPlayerId) {
    return null;
  }

  return {
    playerId,
    sourceMulliganPlayerId,
    cardsGranted: readNumber(payload['cardsGranted']) ?? 0,
    automatic: readBoolean(payload['automatic']) ?? true
  };
}

export function readMulliganFlowCompletedPayload(
  payload: Record<string, unknown>
): MulliganFlowCompletedPayload {
  return {
    roundNumber: readNumber(payload['roundNumber']) ?? 0,
    automatic: readBoolean(payload['automatic']) ?? true
  };
}

function readRevealedCards(value: unknown): MulliganRevealedCard[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((candidate) => readRevealedCard(candidate))
    .filter((card): card is MulliganRevealedCard => card !== null);
}

function readRevealedCard(value: unknown): MulliganRevealedCard | null {
  if (!isRecord(value)) {
    return null;
  }

  const cardId = readString(value['cardId']);
  if (!cardId) {
    return null;
  }

  return {
    cardId,
    name: readString(value['name']),
    externalId: readString(value['externalId']),
    setCode: readString(value['setCode']),
    number: readString(value['number']),
    supertype: readCardSupertype(value['supertype']),
    category: readCardCategory(value['category']),
    imageSmallUrl: readString(value['imageSmallUrl']),
    imageLargeUrl: readString(value['imageLargeUrl']),
    hp: readNumber(value['hp'])
  };
}

function readString(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function readNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function readBoolean(value: unknown): boolean | null {
  return typeof value === 'boolean' ? value : null;
}

function readStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((candidate): candidate is string =>
    typeof candidate === 'string' && candidate.trim().length > 0
  );
}

function readCardSupertype(value: unknown): CardSupertype | null {
  return typeof value === 'string' && Object.values(CardSupertype).includes(value as CardSupertype)
    ? (value as CardSupertype)
    : null;
}

function readCardCategory(value: unknown): CardCategory | null {
  return typeof value === 'string' && Object.values(CardCategory).includes(value as CardCategory)
    ? (value as CardCategory)
    : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
