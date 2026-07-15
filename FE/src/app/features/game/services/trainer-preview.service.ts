import { inject, Injectable } from '@angular/core';
import { firstValueFrom, forkJoin, map, of, switchMap } from 'rxjs';
import { CardsApiService } from '../../../infrastructure/api/card/cards-api.service';
import { GameApiService } from '../../../infrastructure/api/game/game-api.service';
import {
  EvosodaEvolutionOption,
  EvosodaPreview,
  GreatBallPokemonOption,
  GreatBallPreview,
  ProfessorLetterEnergyOption,
  ProfessorLetterPreview
} from '../domain/trainers/trainer-preview.interface';

interface EvosodaPreviewOptionResponse {
  cardId: string;
  externalId: string;
  name: string;
  evolvesFrom: string;
  count: number;
  validTargetPokemonInPlayIds: string[];
}

interface GreatBallOptionResponse {
  cardInstanceId: string;
  cardId: string;
  name: string;
}

interface ProfessorLetterOptionResponse {
  cardId: string;
  name: string;
  availableInDeck: number;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
}

@Injectable({ providedIn: 'root' })
export class TrainerPreviewService {
  private readonly gameApi = inject(GameApiService);
  private readonly cardsApi = inject(CardsApiService);

  previewEvosoda(gameId: string, trainerCardInstanceId: string): Promise<EvosodaPreview> {
    return firstValueFrom(
      this.gameApi.previewTrainer(gameId, { cardInstanceId: trainerCardInstanceId }).pipe(
        map((response) => readEvosodaOptions(response['options'])),
        switchMap((options) => {
          if (!options.length) {
            return of({ options: [] });
          }

          return forkJoin(
            options.map((option) =>
              this.cardsApi.getCardById(option.cardId).pipe(
                map((card): EvosodaEvolutionOption => ({
                  ...option,
                  imageSmallUrl: card.imageSmallUrl,
                  imageLargeUrl: card.imageLargeUrl
                }))
              )
            )
          ).pipe(map((enrichedOptions) => ({ options: enrichedOptions })));
        })
      )
    );
  }

  previewGreatBall(gameId: string, trainerCardInstanceId: string): Promise<GreatBallPreview> {
    return firstValueFrom(
      this.gameApi.previewTrainer(gameId, { cardInstanceId: trainerCardInstanceId }).pipe(
        switchMap((response) => {
          const cardsLookedAt = readNonNegativeInteger(response['cardsLookedAt']);
          const pokemonOptions = readGreatBallOptions(response['pokemonOptions']);
          if (!pokemonOptions.length) {
            return of({ cardsLookedAt, pokemonOptions: [] });
          }

          return forkJoin(
            pokemonOptions.map((option) =>
              this.cardsApi.getCardById(option.cardId).pipe(
                map((card): GreatBallPokemonOption => ({
                  ...option,
                  imageSmallUrl: card.imageSmallUrl,
                  imageLargeUrl: card.imageLargeUrl
                }))
              )
            )
          ).pipe(map((enrichedOptions) => ({ cardsLookedAt, pokemonOptions: enrichedOptions })));
        })
      )
    );
  }

  previewProfessorLetter(gameId: string, trainerCardInstanceId: string): Promise<ProfessorLetterPreview> {
    return firstValueFrom(
      this.gameApi.previewTrainer(gameId, { cardInstanceId: trainerCardInstanceId }).pipe(
        map((response) => {
          const maxSelectable = readPositiveInteger(response['maxSelectable'], 2);
          const options = readProfessorLetterOptions(response['options']);
          return { maxSelectable, options };
        })
      )
    );
  }
}

function readEvosodaOptions(rawOptions: unknown): EvosodaPreviewOptionResponse[] {
  if (!Array.isArray(rawOptions)) {
    return [];
  }

  return rawOptions.flatMap((rawOption) => {
    if (!rawOption || typeof rawOption !== 'object') {
      return [];
    }

    const option = rawOption as Record<string, unknown>;
    const cardId = option['cardId'];
    const externalId = option['externalId'];
    const name = option['name'];
    const evolvesFrom = option['evolvesFrom'];
    const count = option['count'];
    const targetIds = option['validTargetPokemonInPlayIds'];

    if (
      typeof cardId !== 'string' ||
      typeof externalId !== 'string' ||
      typeof name !== 'string' ||
      typeof evolvesFrom !== 'string' ||
      typeof count !== 'number' ||
      !Array.isArray(targetIds) ||
      !targetIds.every((targetId) => typeof targetId === 'string')
    ) {
      return [];
    }

    return [{
      cardId,
      externalId,
      name,
      evolvesFrom,
      count,
      validTargetPokemonInPlayIds: targetIds
    }];
  });
}

function readGreatBallOptions(rawOptions: unknown): GreatBallOptionResponse[] {
  if (!Array.isArray(rawOptions)) {
    return [];
  }

  return rawOptions.flatMap((rawOption) => {
    if (!rawOption || typeof rawOption !== 'object') {
      return [];
    }

    const option = rawOption as Record<string, unknown>;
    const cardInstanceId = option['cardInstanceId'];
    const cardId = option['cardId'];
    const name = option['name'];
    if (typeof cardInstanceId !== 'string' || typeof cardId !== 'string' || typeof name !== 'string') {
      return [];
    }

    return [{ cardInstanceId, cardId, name }];
  });
}

function readProfessorLetterOptions(rawOptions: unknown): ProfessorLetterOptionResponse[] {
  if (!Array.isArray(rawOptions)) {
    return [];
  }

  return rawOptions.flatMap((rawOption) => {
    if (!rawOption || typeof rawOption !== 'object') {
      return [];
    }

    const option = rawOption as Record<string, unknown>;
    const cardId = option['cardId'];
    const name = option['name'];
    const availableInDeck = option['availableInDeck'];
    const imageSmallUrl = option['imageSmallUrl'];
    const imageLargeUrl = option['imageLargeUrl'];
    if (
      typeof cardId !== 'string' ||
      typeof name !== 'string' ||
      typeof availableInDeck !== 'number' ||
      !Number.isInteger(availableInDeck) ||
      availableInDeck <= 0 ||
      !isOptionalString(imageSmallUrl) ||
      !isOptionalString(imageLargeUrl)
    ) {
      return [];
    }

    return [{
      cardId,
      name,
      availableInDeck,
      imageSmallUrl: imageSmallUrl ?? null,
      imageLargeUrl: imageLargeUrl ?? null
    }];
  });
}

function isOptionalString(value: unknown): value is string | null | undefined {
  return value === undefined || value === null || typeof value === 'string';
}

function readNonNegativeInteger(rawValue: unknown): number {
  return typeof rawValue === 'number' && Number.isInteger(rawValue) && rawValue >= 0
    ? rawValue
    : 0;
}

function readPositiveInteger(rawValue: unknown, fallback: number): number {
  return typeof rawValue === 'number' && Number.isInteger(rawValue) && rawValue > 0
    ? rawValue
    : fallback;
}
