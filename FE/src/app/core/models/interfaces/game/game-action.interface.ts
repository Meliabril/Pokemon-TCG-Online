import { GameActionType } from '../../enums/game/game-action-type.enum';
import {
  AttachEnergyPayload,
  ChooseInitialPokemonPayload,
  ConcedePayload,
  DeclareAttackPayload,
  DrawCardPayload,
  EmptyActionPayload,
  EndTurnPayload,
  EvolvePokemonPayload,
  GameActionPayload,
  PlayBasicPokemonPayload,
  PlayTrainerPayload,
  PromoteBenchPokemonPayload,
  ResolveAttackChoicePayload,
  RetreatPayload,
  SelectTargetPayload,
  TakePrizeCardPayload,
  UseAbilityPayload
} from './game-action-payloads.interface';

export type { GameActionPayload } from './game-action-payloads.interface';

type GameActionRequestBase<TActionType extends GameActionType, TPayload extends GameActionPayload> = {
  gameId: string;
  clientActionId: string;
  actionType: TActionType;
  expectedStateVersion: number;
  payload: TPayload;
};

export type GameActionRequest =
  | GameActionRequestBase<GameActionType.CreateGame, EmptyActionPayload>
  | GameActionRequestBase<GameActionType.JoinGame, EmptyActionPayload>
  | GameActionRequestBase<GameActionType.StartGame, EmptyActionPayload>
  | GameActionRequestBase<GameActionType.AckMulliganNotice, EmptyActionPayload>
  | GameActionRequestBase<GameActionType.ChooseInitialPokemon, ChooseInitialPokemonPayload>
  | GameActionRequestBase<GameActionType.PauseGame, EmptyActionPayload>
  | GameActionRequestBase<GameActionType.ResumeGame, EmptyActionPayload>
  | GameActionRequestBase<GameActionType.DrawCard, DrawCardPayload>
  | GameActionRequestBase<GameActionType.PlayBasicPokemon, PlayBasicPokemonPayload>
  | GameActionRequestBase<GameActionType.EvolvePokemon, EvolvePokemonPayload>
  | GameActionRequestBase<GameActionType.AttachEnergy, AttachEnergyPayload>
  | GameActionRequestBase<GameActionType.PlayTrainer, PlayTrainerPayload>
  | GameActionRequestBase<GameActionType.Retreat, RetreatPayload>
  | GameActionRequestBase<GameActionType.DeclareAttack, DeclareAttackPayload>
  | GameActionRequestBase<GameActionType.SelectTarget, SelectTargetPayload>
  | GameActionRequestBase<GameActionType.EndTurn, EndTurnPayload>
  | GameActionRequestBase<GameActionType.PromoteBenchPokemon, PromoteBenchPokemonPayload>
  | GameActionRequestBase<GameActionType.ResolveAttackChoice, ResolveAttackChoicePayload>
  | GameActionRequestBase<GameActionType.TakePrizeCard, TakePrizeCardPayload>
  | GameActionRequestBase<GameActionType.UseAbility, UseAbilityPayload>
  | GameActionRequestBase<GameActionType.Concede, ConcedePayload>;

export type GameActionResultPayload = Record<string, unknown>;

export interface GameActionResponse<TData = unknown> {
  success: boolean;
  message: string;
  newStateVersion: number;
  data: TData | null;
}

export interface GameActionLogEntry {
  id: string;
  gameId: string;
  actorUserId: string;
  actionType: GameActionType;
  payload: GameActionPayload;
  result: GameActionResultPayload;
  version: number;
  clientActionId: string | null;
  createdAt: string;
}
