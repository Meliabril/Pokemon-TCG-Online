import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { BoardAnimationLayerComponent } from '../../components/board-animation-layer/board-animation-layer.component';
import { BoardAnimationCommand } from '../../domain/animations/board-animation.types';
import { BoardAnimationService } from '../../services/board-animation.service';
import { DeckShuffleAnimationComponent } from '../../../../shared/ui/feedback/deck-shuffle-animation/deck-shuffle-animation.component';

interface AnimationCase {
  id: string;
  label: string;
  commands: BoardAnimationCommand[];
}

const PLACEHOLDER_CARD_LABEL = 'Test Card';

@Component({
  selector: 'app-animation-playground-page',
  templateUrl: './animation-playground-page.component.html',
  styleUrl: './animation-playground-page.component.css',
  imports: [BoardAnimationLayerComponent, DeckShuffleAnimationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AnimationPlaygroundPageComponent {
  private readonly animationService = inject(BoardAnimationService);

  readonly showStandaloneShuffle = signal(false);

  readonly cases: AnimationCase[] = [
    {
      id: 'opponent-draw',
      label: 'Robar carta (rival)',
      commands: [
        {
          type: 'OPPONENT_DRAW_CARD',
          fromAnchor: 'opponent-deck',
          toAnchor: 'opponent-hand',
          useCardBack: true,
          hideLabel: true
        }
      ]
    },
    {
      id: 'player-draw',
      label: 'Robar carta (propia)',
      commands: [
        {
          type: 'PLAYER_DRAW_CARD',
          fromAnchor: 'local-deck',
          toAnchor: 'local-hand',
          cardLabel: PLACEHOLDER_CARD_LABEL,
          revealBeforeMove: true
        }
      ]
    },
    {
      id: 'player-move',
      label: 'Jugar carta al banco (propia)',
      commands: [
        {
          type: 'PLAYER_MOVE_CARD',
          fromAnchor: 'local-hand',
          toAnchor: 'local-bench-0',
          cardLabel: PLACEHOLDER_CARD_LABEL,
          revealBeforeMove: true
        }
      ]
    },
    {
      id: 'opponent-move',
      label: 'Jugar carta al banco (rival)',
      commands: [
        {
          type: 'OPPONENT_MOVE_CARD',
          fromAnchor: 'opponent-hand',
          toAnchor: 'opponent-bench-0',
          useCardBack: true,
          hideLabel: true
        }
      ]
    },
    {
      id: 'attach-energy',
      label: 'Adjuntar energia (rival)',
      commands: [
        {
          type: 'OPPONENT_ATTACH_ENERGY',
          fromAnchor: 'opponent-hand',
          toAnchor: 'opponent-active',
          targetPulseAnchor: 'opponent-active',
          useCardBack: true,
          hideLabel: true
        }
      ]
    },
    {
      id: 'play-trainer',
      label: 'Jugar entrenador (rival)',
      commands: [
        {
          type: 'OPPONENT_PLAY_TRAINER',
          fromAnchor: 'opponent-hand',
          toAnchor: 'opponent-discard',
          useCardBack: true,
          hideLabel: true
        }
      ]
    },
    {
      id: 'discard',
      label: 'Descartar carta (propia)',
      commands: [
        {
          type: 'DISCARD_CARD',
          fromAnchor: 'local-hand',
          toAnchor: 'local-discard',
          targetPulseAnchor: 'local-discard',
          cardLabel: PLACEHOLDER_CARD_LABEL,
          hideLabel: true,
          useCardBack: true
        }
      ]
    },
    {
      id: 'attack-lunge',
      label: 'Ataque (lunge)',
      commands: [
        {
          type: 'ATTACK_LUNGE',
          fromAnchor: 'local-active',
          toAnchor: 'opponent-active'
        }
      ]
    },
    {
      id: 'self-damage',
      label: 'Dano propio (confusion)',
      commands: [
        {
          type: 'SELF_DAMAGE',
          fromAnchor: 'local-active',
          toAnchor: 'local-active',
          durationMs: 520
        }
      ]
    },
    {
      id: 'self-damage-recoil',
      label: 'Dano propio (recoil, lento)',
      commands: [
        {
          type: 'SELF_DAMAGE',
          fromAnchor: 'local-active',
          toAnchor: 'local-active',
          durationMs: 1000
        }
      ]
    },
    {
      id: 'self-heal',
      label: 'Curacion',
      commands: [
        {
          type: 'SELF_HEAL',
          fromAnchor: 'local-active',
          toAnchor: 'local-active'
        }
      ]
    },
    {
      id: 'coin-flip-1',
      label: 'Tirar moneda (1)',
      commands: [
        {
          type: 'COIN_FLIP',
          coinResults: [randomCoin()],
          highlightAnchor: 'local-active',
          coinFlipLabel: 'Coin flip'
        }
      ]
    },
    {
      id: 'coin-flip-4',
      label: 'Tirar moneda (4)',
      commands: [
        {
          type: 'COIN_FLIP',
          coinResults: [randomCoin(), randomCoin(), randomCoin(), randomCoin()],
          highlightAnchor: 'opponent-active',
          coinFlipLabel: 'Coin flip x4'
        }
      ]
    },
    {
      id: 'shuffle-local',
      label: 'Barajar mazo (propio)',
      commands: [{ type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }]
    },
    {
      id: 'shuffle-opponent',
      label: 'Barajar mazo (rival)',
      commands: [{ type: 'SHUFFLE_DECK', fromAnchor: 'opponent-deck' }]
    },
    {
      id: 'astonish-reveal-shuffle',
      label: 'Astonish: revelar carta y barajar',
      commands: [
        {
          type: 'OPPONENT_HAND_REVEAL_SHUFFLE',
          fromAnchor: 'opponent-hand',
          toAnchor: 'opponent-deck',
          targetPulseAnchor: 'opponent-deck',
          cardLabel: 'Carta revelada',
          travelRotationDeg: -6
        },
        { type: 'WAIT', durationMs: 100 },
        { type: 'SHUFFLE_DECK', fromAnchor: 'opponent-deck' }
      ]
    },
    {
      id: 'search-supporter',
      label: 'Buscar partidario (Guiar)',
      commands: [
        {
          type: 'PLAYER_MOVE_CARD',
          fromAnchor: 'local-deck',
          toAnchor: 'local-hand',
          cardLabel: PLACEHOLDER_CARD_LABEL,
          revealBeforeMove: true,
          durationMs: 780,
          travelRotationDeg: 5
        },
        { type: 'WAIT', durationMs: 150 },
        { type: 'SHUFFLE_DECK', fromAnchor: 'local-deck' }
      ]
    }
  ];

  run(commands: BoardAnimationCommand[]): void {
    this.animationService.enqueue(commands);
  }

  toggleStandaloneShuffle(): void {
    this.showStandaloneShuffle.update((value) => !value);
  }
}

function randomCoin(): 'HEADS' | 'TAILS' {
  return Math.random() < 0.5 ? 'HEADS' : 'TAILS';
}
