import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { GameActionPayload } from '../../../../core/models/interfaces/game/game-action.interface';

export interface BoardActionViewModel {
  actionType: GameActionType;
  label: string;
  enabled: boolean;
  disabledReason?: string | null;
  payload?: GameActionPayload;
}
