import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';

export type BoardActivePanel = 'none' | 'card-detail' | 'action-detail' | 'event-log';

export interface BoardEphemeralUiState {
  selectedCardInstanceId: string | null;
  selectedPokemonInPlayId: string | null;
  targetPokemonInPlayId: string | null;
  activePanel: BoardActivePanel;
  pendingAction: GameActionType | null;
}
