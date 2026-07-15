package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Card;

import java.util.List;
import java.util.UUID;

public interface CardService {

    List<CardResponseDto> getCards(String setCode);

    List<CardResponseDto> searchCards(String setCode, String name);

    CardResponseDto getCard(UUID cardId);

    Card getCardEntityById(UUID cardId);

    List<Card> getCardEntitiesByIds(List<UUID> cardIds);

    List<Card> getCardEntities(String setCode);

    CardImportStatusDto getXy1ImportStatus();
}
