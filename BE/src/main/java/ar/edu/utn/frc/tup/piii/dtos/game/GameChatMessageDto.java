package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameChatContext;

import java.time.Instant;
import java.util.UUID;

public record GameChatMessageDto(
        UUID messageId,
        UUID gameId,
        UUID senderUserId,
        String senderUsername,
        GameChatContext context,
        String content,
        Instant sentAt) {
}
