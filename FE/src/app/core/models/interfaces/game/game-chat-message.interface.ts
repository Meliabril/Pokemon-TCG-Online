import { GameChatContext } from '../../enums/game/game-chat-context.enum';

export interface GameChatMessage {
  messageId: string;
  gameId: string;
  senderUserId: string;
  senderUsername: string;
  context: GameChatContext;
  content: string;
  sentAt: string;
}

export interface GameChatMessageRequest {
  context: GameChatContext;
  content: string;
}
