import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { UiTranslateFn } from '../../../../core/services/language.service';
import { BoardActionViewModel, BoardGameViewModel } from '../board/board-game-view-model.interface';
import { GameScreenState } from '../ui-state/game-screen-state.type';

export interface GameAttackOptionViewModel {
  id: string;
  label: string;
  name?: string | null;
  displayName?: string | null;
  enabled: boolean;
  selected: boolean;
  requiresTarget: boolean;
  validTargetPokemonInPlayIds: string[];
  disabledReason: string | null;
}

export interface GameInteractionViewModel {
  pendingAction: GameActionType | null;
  prompt: string;
  confirmLabel: string;
  canConfirm: boolean;
  awaitingSnapshot: boolean;
  actionButtons: BoardActionViewModel[];
  attackOptions: GameAttackOptionViewModel[];
  selectableCardInstanceIds: string[];
  selectablePokemonInPlayIds: string[];
  selectedCardInstanceId: string | null;
  selectedPokemonInPlayId: string | null;
  selectedAttackId: string | null;
  optimisticEnergyAttachment: {
    energyCardInstanceId: string;
    targetPokemonInPlayId: string;
  } | null;
  setupActiveCardInstanceId: string | null;
  setupBenchCardInstanceIds: string[];
  helperBadges: string[];
}

export interface GameBlockingOverlayViewModel {
  title: string;
  message: string;
  tone: 'amber' | 'red' | 'green';
  variant: 'notice' | 'result';
  eyebrow: string;
  showLeaveAction: boolean;
  leaveActionLabel: string;
}

export interface GameInteractionContext {
  board: BoardGameViewModel;
  screenState: GameScreenState;
  commandState: {
    pendingAction: GameActionType | null;
    selectedCardInstanceId: string | null;
    selectedPokemonInPlayId: string | null;
    selectedAttackId: string | null;
    optimisticEnergyAttachment: {
      energyCardInstanceId: string;
      targetPokemonInPlayId: string;
    } | null;
    setupActiveCardInstanceId: string | null;
    setupBenchCardInstanceIds: string[];
  };
  actionPending: boolean;
  awaitingSnapshot: boolean;
  t?: UiTranslateFn;
}
