import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { GameActionType } from '../../../../core/models/enums/game/game-action-type.enum';
import { ResolveAttackChoicePayload, UseAbilityPayload } from '../../../../core/models/interfaces/game/game-action-payloads.interface';
import { GameChatMessage } from '../../../../core/models/interfaces/game/game-chat-message.interface';
import { BoardGameViewModel } from '../../domain/board/board-game-view-model.interface';
import { CardHoverVisualTarget } from '../../domain/cards/card-hover-visual-target.interface';
import {
  GameBlockingOverlayViewModel,
  GameInteractionViewModel
} from '../../domain/interaction/game-interaction-view-model.interface';
import { GameBoardComponent } from '../game-board/game-board.component';

@Component({
  selector: 'app-game-shell',
  templateUrl: './game-shell.component.html',
  styleUrl: './game-shell.component.css',
  imports: [GameBoardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameShellComponent {
  readonly GameActionType = GameActionType;
  readonly board = input.required<BoardGameViewModel>();
  readonly actionPending = input(false);
  readonly interaction = input<GameInteractionViewModel | null>(null);
  readonly blockingOverlay = input<GameBlockingOverlayViewModel | null>(null);
  readonly remoteHoveredCardKey = input<string | null>(null);
  readonly localHandHidden = input(false);
  readonly opponentHandHidden = input(false);
  readonly chatMessages = input<GameChatMessage[]>([]);
  readonly chatCurrentUserId = input<string | null>(null);
  readonly chatConnected = input(false);
  readonly leaveTable = output<void>();
  readonly endTurnRequested = output<void>();
  readonly setupSubmitted = output<void>();
  readonly setupCancelled = output<void>();
  readonly mulliganNoticeAcknowledgedRequested = output<void>();
  readonly playBasicPokemon = output<string>();
  readonly setupActiveCardDropped = output<string>();
  readonly setupBenchCardDropped = output<string>();
  readonly setupBenchCardReturned = output<string>();
  readonly benchPokemonPromoted = output<string>();
  readonly attackChoiceResolved = output<ResolveAttackChoicePayload>();
  readonly activePokemonRetreated = output<string>();
  readonly drawCardRequested = output<void>();
  readonly energyDropped = output<{
    energyCardInstanceId: string;
    targetPokemonInPlayId: string;
  }>();
  readonly evolutionDropped = output<{
    evolutionCardInstanceId: string;
    targetPokemonInPlayId: string;
  }>();
  readonly actionSelected = output<GameActionType>();
  readonly confirmAction = output<void>();
  readonly cancelAction = output<void>();
  readonly handCardSelected = output<string>();
  readonly pokemonSelected = output<string>();
  readonly attackSelected = output<string>();
  readonly attackDeclared = output<{
    attackId: string;
    useBonusDamage?: boolean;
    selfTargetPokemonInPlayId?: string;
  }>();
  readonly abilityUsed = output<UseAbilityPayload>();
  readonly trainerPlayed = output<{
    cardId?: string;
    cardInstanceId?: string;
    targetPokemonInPlayId?: string;
    targetCardInstanceId?: string;
    targetEnergyCardInstanceId?: string;
    selectedEvolutionExternalId?: string;
    selectedCardInstanceId?: string;
    selectedCardIds?: string[];
  }>();
  readonly chatMessageSubmitted = output<string>();
  readonly setupBenchToggleRequested = output<void>();
  readonly cardHoverChanged = output<CardHoverVisualTarget>();
}
