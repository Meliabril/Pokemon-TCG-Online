import { HttpErrorResponse } from '@angular/common/http';

import { computed, inject, Injectable, signal } from '@angular/core';

import { firstValueFrom, forkJoin, Observable } from 'rxjs';

import {

DeckActivationResponse,

DeckSummary,

DeckValidationResponse

} from '../../../core/models/interfaces/deck/deck-summary.interface';

import { MatchmakingQueueStatus } from '../../../core/models/interfaces/matchmaking/matchmaking-queue-status.interface';

import {

PreMatchChecklistState,

QueueRoomState

} from '../../../core/models/interfaces/play/prematch.interface';

import { StorageService } from '../../../core/storage/storage.service';
import { LanguageService } from '../../../core/services/language.service';

import { DeckApiService } from '../../../infrastructure/api/deck/deck-api.service';

import { MatchmakingApiService } from '../../../infrastructure/api/matchmaking/matchmaking-api.service';



const ROOM_HANDOFF_GRACE_POLLS = 1;
const ACTIVE_GAME_STATUSES = new Set(['WAITING', 'SETUP', 'ACTIVE']);
const INACTIVE_GAME_STATUSES = new Set(['FINISHED', 'CANCELLED', 'CANCELED', 'ABANDONED']);

function hasGameId(

queueStatus: MatchmakingQueueStatus | null | undefined

): queueStatus is MatchmakingQueueStatus & { gameId: string } {

return typeof queueStatus?.gameId === 'string' && queueStatus.gameId.trim().length > 0;

}



function hasMatchAssigned(queueStatus: MatchmakingQueueStatus | null | undefined): boolean {

return Boolean(queueStatus?.matchedUserId) || hasGameId(queueStatus);

}



function isQueueActive(queueStatus: MatchmakingQueueStatus | null | undefined): boolean {

return Boolean(queueStatus?.queued) || hasMatchAssigned(queueStatus);

}

function isActiveGameStatus(queueStatus: MatchmakingQueueStatus | null | undefined): boolean {

const normalizedStatus = queueStatus?.status?.trim().toUpperCase() ?? null;

if (normalizedStatus && ACTIVE_GAME_STATUSES.has(normalizedStatus)) {

return true;

}

if (!hasGameId(queueStatus)) {

return false;

}

return !normalizedStatus || !INACTIVE_GAME_STATUSES.has(normalizedStatus);

}



function buildQueueFeedbackMessage(
queueStatus: MatchmakingQueueStatus | null | undefined,
t: (key: string) => string
): string {

if (hasGameId(queueStatus)) {

return '';

}



return queueStatus?.matchedUserId ? t('PLAY.MATCH_FOUND_PENDING') : '';

}



function createChecklistState(isAuthenticated: boolean): PreMatchChecklistState {

return {

status: isAuthenticated ? 'idle' : 'blocked',

availableDecks: [],

activeDeck: null,

validation: null,

queueStatus: null,

errorMessage: '',

feedbackMessage: '',

checks: {

sessionReady: isAuthenticated,

activeDeckFound: false,

deckValid: false

}

};

}



function createRoomState(): QueueRoomState {

return {

status: 'idle',

activeDeck: null,

queueStatus: null,

errorMessage: '',

infoMessage: '',

matchFound: false,

gameId: null,

opponentUserId: null,

matchedAt: null

};

}



@Injectable({ providedIn: 'root' })

export class PrematchFacadeService {

private readonly storageService = inject(StorageService);

private readonly deckApi = inject(DeckApiService);

private readonly matchmakingApi = inject(MatchmakingApiService);
private readonly languageService = inject(LanguageService);



private readonly checklistStateSignal = signal<PreMatchChecklistState>(

createChecklistState(this.storageService.isAuthenticated())

);

private readonly roomStateSignal = signal<QueueRoomState>(createRoomState());

private roomMissingGracePolls = 0;



readonly isAuthenticated = this.storageService.isAuthenticated;

readonly checklistState = this.checklistStateSignal.asReadonly();

readonly hasActiveGame = computed(() => isActiveGameStatus(this.checklistStateSignal().queueStatus));

readonly roomState = this.roomStateSignal.asReadonly();

readonly activeDeck = computed(

() => this.roomStateSignal().activeDeck ?? this.checklistStateSignal().activeDeck

);



async refreshChecklist(feedbackMessage = ''): Promise<void> {

if (!this.storageService.isAuthenticated()) {

this.checklistStateSignal.set({

...createChecklistState(false),

feedbackMessage

});

return;

}



this.checklistStateSignal.update((state) => ({

...state,

status: 'checking',

errorMessage: '',

feedbackMessage

}));



try {

const { decks, queueStatus } = await firstValueFrom(

forkJoin({

decks: this.deckApi.getDecks(),

queueStatus: this.matchmakingApi.getMyQueueStatus()

})

);



const activeDeck = decks.find((deck) => deck.active) ?? null;

const validation = activeDeck

? await firstValueFrom(this.deckApi.validateDeck(activeDeck.id))

: null;



this.checklistStateSignal.set(

this.buildChecklistState(decks, activeDeck, validation, queueStatus, feedbackMessage)

);

} catch (error) {

this.checklistStateSignal.set({

...createChecklistState(true),

status: 'error',

errorMessage: this.readErrorMessage(error, 'checklist'),

feedbackMessage

});

}

}



async activateDeck(deckId: string): Promise<boolean> {

const previousState = this.checklistStateSignal();

if (this.hasActiveGame()) {

this.checklistStateSignal.set({

...previousState,

errorMessage: this.languageService.t('HOME.ACTIVE_GAME_DECK_LOCK'),

feedbackMessage: this.languageService.t('HOME.ACTIVE_GAME_DECK_LOCK_DETAIL')

});

return false;

}

const selectedDeck = previousState.availableDecks.find((deck) => deck.id === deckId);

if (selectedDeck) {

this.checklistStateSignal.set(

this.buildActivatedChecklistState(

previousState,

{

id: deckId,

active: true,

valid: selectedDeck.valid,

validationErrors: selectedDeck.validationErrors

},

''

)

);

}



try {

const activation = await firstValueFrom(this.deckApi.activateDeck(deckId));

this.checklistStateSignal.set(

this.buildActivatedChecklistState(

this.checklistStateSignal(),

activation,

''

)

);

return true;

} catch (error) {

this.checklistStateSignal.set({

...previousState,

errorMessage: this.readErrorMessage(error, 'checklist')

});

return false;

}

}



async randomizeDeck(deckId: string): Promise<boolean> {

if (this.hasActiveGame()) {

this.checklistStateSignal.update((state) => ({

...state,

errorMessage: this.languageService.t('HOME.ACTIVE_GAME_DECK_LOCK'),

feedbackMessage: this.languageService.t('HOME.ACTIVE_GAME_DECK_LOCK_DETAIL')

}));

return false;

}

this.checklistStateSignal.update((state) => ({

...state,

errorMessage: '',

feedbackMessage: ''

}));



try {

const deck = await firstValueFrom(this.deckApi.randomizeDeck(deckId));

await this.refreshChecklist(

`Randomizamos ${deck.name}. Revisa su validacion antes de buscar partida.`

);

return true;

} catch (error) {

this.checklistStateSignal.update((state) => ({

...state,

errorMessage: this.readErrorMessage(error, 'checklist')

}));

return false;

}

}



  async joinQueue(): Promise<MatchmakingQueueStatus | null> {
    return this.internalJoinQueue(this.matchmakingApi.joinQueue());
  }

  async createCustomQueue(): Promise<MatchmakingQueueStatus | null> {
    return this.internalJoinQueue(this.matchmakingApi.createCustomQueue());
  }

  async joinCustomQueue(code: string): Promise<MatchmakingQueueStatus | null> {
    return this.internalJoinQueue(this.matchmakingApi.joinCustomQueue(code));
  }

  private async internalJoinQueue(
    joinObservable: Observable<MatchmakingQueueStatus>
  ): Promise<MatchmakingQueueStatus | null> {
    this.checklistStateSignal.update((state) => ({
      ...state,
      status: 'joining',
      errorMessage: ''
    }));

    try {
      const queueStatus = await firstValueFrom(joinObservable);
      const currentState = this.checklistStateSignal();

      this.checklistStateSignal.set({
        ...currentState,
        queueStatus,
        status: isQueueActive(queueStatus) ? 'queued' : 'ready',
        feedbackMessage: buildQueueFeedbackMessage(queueStatus, this.languageService.t.bind(this.languageService))
      });

      return queueStatus;
    } catch (error) {
      this.checklistStateSignal.update((state) => ({
        ...state,
        status: state.checks.activeDeckFound && state.checks.deckValid ? 'ready' : 'error',
        errorMessage: this.readErrorMessage(error, 'matchmaking')
      }));
      return null;
    }
  }



async refreshRoomState(): Promise<boolean> {

this.roomStateSignal.update((state) => ({

...state,

status: 'checking',

errorMessage: '',

infoMessage: ''

}));



try {

const { activeDeck, queueStatus } = await firstValueFrom(

forkJoin({

activeDeck: this.deckApi.getActiveDeck(),

queueStatus: this.matchmakingApi.getMyQueueStatus()

})

);



if (hasMatchAssigned(queueStatus)) {

this.roomMissingGracePolls = 0;

this.roomStateSignal.set(this.buildMatchedRoomState(activeDeck, queueStatus));

return true;

}



if (!queueStatus.queued) {

if (this.shouldPreserveRoomDuringHandoff()) {

this.roomMissingGracePolls += 1;

this.roomStateSignal.update((state) => ({

...state,

status: 'queued',

activeDeck,

errorMessage: '',

infoMessage: this.languageService.t('PLAY.ROOM_HANDOFF_GRACE'),

matchFound: state.matchFound || hasMatchAssigned(state.queueStatus)

}));

return true;

}



this.roomMissingGracePolls = 0;

this.roomStateSignal.set({

...createRoomState(),

activeDeck

});

return false;

}



this.roomMissingGracePolls = 0;

this.roomStateSignal.set({

status: 'queued',

activeDeck,

queueStatus,

errorMessage: '',

infoMessage: this.languageService.t('PLAY.AUTOMATIC_OPEN_PENDING'),

matchFound: false,

gameId: null,

opponentUserId: null,

matchedAt: null

});



return true;

} catch (error) {

this.roomMissingGracePolls = 0;

this.roomStateSignal.set({

...createRoomState(),

status: 'error',

errorMessage: this.readErrorMessage(error, 'matchmaking')

});

return true;

}

}



async showMatchedRoom(matchedUserId: string | null, gameId: string | null = null): Promise<void> {

this.roomMissingGracePolls = 0;

this.roomStateSignal.update((state) => ({

...state,

status: 'checking',

errorMessage: '',

infoMessage: ''

}));



try {

const activeDeck = await firstValueFrom(this.deckApi.getActiveDeck());



this.roomStateSignal.set(

this.buildMatchedRoomState(activeDeck, {

queued: false,

queuedAt: null,

queueSize: null,

matchedUserId,

gameId

})

);

} catch (error) {

this.roomStateSignal.set({

...createRoomState(),

status: 'error',

errorMessage: this.readErrorMessage(error, 'matchmaking')

});

}

}



async leaveQueue(): Promise<boolean> {

const activeDeck = this.roomStateSignal().activeDeck;



this.roomStateSignal.update((state) => ({

...state,

status: 'leaving',

errorMessage: ''

}));



try {

await firstValueFrom(this.matchmakingApi.leaveQueue());

this.roomMissingGracePolls = 0;

this.roomStateSignal.set({

...createRoomState(),

activeDeck

});

return true;

} catch (error) {

this.roomStateSignal.update((state) => ({

...state,

status: state.matchFound ? 'queued' : 'error',

errorMessage: this.readErrorMessage(error, 'matchmaking')

}));

return false;

}

}



private buildChecklistState(

decks: DeckSummary[],

activeDeck: DeckSummary | null,

validation: DeckValidationResponse | null,

queueStatus: MatchmakingQueueStatus | null,

feedbackMessage: string

): PreMatchChecklistState {

const activeDeckFound = Boolean(activeDeck);

const deckValid = Boolean(activeDeck && activeDeck.valid && validation?.valid !== false);



return {

status: isQueueActive(queueStatus) ? 'queued' : activeDeckFound && deckValid ? 'ready' : 'blocked',

availableDecks: [...decks],

activeDeck,

validation,

queueStatus,

errorMessage: '',

feedbackMessage,

checks: {

sessionReady: true,

activeDeckFound,

deckValid

}

};

}



private buildActivatedChecklistState(

state: PreMatchChecklistState,

activation: DeckActivationResponse,

feedbackMessage: string

): PreMatchChecklistState {

const keepsCurrentActiveDeck = state.activeDeck?.id === activation.id;

const availableDecks = state.availableDecks.map((deck) => ({

...deck,

active: deck.id === activation.id,

...(deck.id === activation.id

? {

valid: activation.valid,

validationErrors: activation.validationErrors

}

: {})

}));

const activeDeck = availableDecks.find((deck) => deck.id === activation.id) ?? null;

const validation = activeDeck

? {

deckId: activeDeck.id,

valid: activation.valid,

errors: activation.validationErrors

}

: null;



return this.buildChecklistState(

availableDecks,

activeDeck,

validation,

keepsCurrentActiveDeck ? state.queueStatus : null,

feedbackMessage

);

}



private buildMatchedRoomState(

activeDeck: DeckSummary | null,

queueStatus: MatchmakingQueueStatus

): QueueRoomState {

return {

status: 'queued',

activeDeck,

queueStatus,

errorMessage: '',

infoMessage: buildQueueFeedbackMessage(queueStatus, this.languageService.t.bind(this.languageService)),

matchFound: true,

gameId: queueStatus.gameId ?? null,

opponentUserId: queueStatus.matchedUserId ?? null,

matchedAt: null

};

}



private shouldPreserveRoomDuringHandoff(): boolean {

const roomState = this.roomStateSignal();

const roomWasActive =

roomState.status === 'queued' || roomState.matchFound || hasMatchAssigned(roomState.queueStatus);



return roomWasActive && this.roomMissingGracePolls < ROOM_HANDOFF_GRACE_POLLS;

}



private readErrorMessage(error: unknown, context: 'checklist' | 'matchmaking'): string {

const fallback = this.languageService.t(
context === 'checklist' ? 'PLAY.CHECKLIST_VALIDATION_GAP' : 'PLAY.MATCHMAKING_GAP'
);



if (!(error instanceof HttpErrorResponse)) {

return fallback;

}



if (error.status === 0) {

return this.languageService.t('AUTH.ERRORS.CONNECTION');

}



const payload = error.error;

if (payload && typeof payload === 'object') {

const message = payload['message'];

if (typeof message === 'string' && message.trim()) {
return fallback;
}



const validationErrors = payload['validationErrors'];

if (Array.isArray(validationErrors) && validationErrors.length > 0) {
return fallback;

}

}



return fallback;

}

}

