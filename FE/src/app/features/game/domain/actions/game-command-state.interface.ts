import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';

export interface GameCommandState {
  pendingAction: GameActionType | null;
  selectedCardInstanceId: string | null;
  selectedPokemonInPlayId: string | null;
  selectedAttackId: string | null;
  selectedAttackUseBonusDamage: boolean;
  setupActiveCardInstanceId: string | null;
  setupBenchCardInstanceIds: string[];
}

export const EMPTY_GAME_COMMAND_STATE: GameCommandState = {
  pendingAction: null,
  selectedCardInstanceId: null,
  selectedPokemonInPlayId: null,
  selectedAttackId: null,
  selectedAttackUseBonusDamage: false,
  setupActiveCardInstanceId: null,
  setupBenchCardInstanceIds: []
};
