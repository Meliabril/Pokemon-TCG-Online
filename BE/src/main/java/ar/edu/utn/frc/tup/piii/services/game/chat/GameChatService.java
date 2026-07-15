package ar.edu.utn.frc.tup.piii.services.game.chat;

import ar.edu.utn.frc.tup.piii.dtos.game.GameChatMessageDto;
import ar.edu.utn.frc.tup.piii.dtos.websocket.GameChatMessageRequestDto;

import java.util.List;
import java.util.UUID;

public interface GameChatService {

    void sendMessage(UUID gameId, UUID senderUserId, GameChatMessageRequestDto request);

    List<GameChatMessageDto> getVisibleChatHistory(UUID gameId, UUID viewerUserId);
}
