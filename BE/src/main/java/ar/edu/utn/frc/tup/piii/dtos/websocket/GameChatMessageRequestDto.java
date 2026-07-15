package ar.edu.utn.frc.tup.piii.dtos.websocket;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameChatContext;

public record GameChatMessageRequestDto(
        GameChatContext context,
        String content) {
}
