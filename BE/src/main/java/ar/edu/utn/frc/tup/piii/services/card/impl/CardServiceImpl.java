package ar.edu.utn.frc.tup.piii.services.card.impl;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.card.CardImportService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ar.edu.utn.frc.tup.piii.services.card.CardCatalogModifiedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CardImportService cardImportService;
    private final CardMapper cardMapper;

    private final Map<UUID, Card> cardCache = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void preloadCardCache() {
        log.info("[CARD_CACHE] Iniciando precarga de cartas estáticas...");
        long start = System.currentTimeMillis();
        try {
            List<Card> allCards = cardRepository.findBySetCodeInOrderBySetCodeAscNumberAsc(Card.PLAYABLE_SET_CODES);
            for (Card card : allCards) {
                cardCache.put(card.getId(), card);
            }
            log.info("[CARD_CACHE] Precarga completada exitosamente. {} cartas cargadas en {} ms.", cardCache.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[CARD_CACHE] Error al precargar el catálogo de cartas", e);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCards(String setCode) {
        List<String> setCodes = resolvePlayableSetCodes(setCode);
        List<Card> cards = setCodes.size() == 1
                ? cardRepository.findBySetCodeOrderByNumberAsc(setCodes.getFirst())
                : cardRepository.findBySetCodeInOrderBySetCodeAscNumberAsc(setCodes);
        return cards.stream()
                .map(cardMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> searchCards(String setCode, String name) {
        List<String> setCodes = resolvePlayableSetCodes(setCode);
        List<Card> cards = setCodes.size() == 1
                ? cardRepository.findBySetCodeAndNameContainingIgnoreCaseOrderByNameAsc(setCodes.getFirst(), name)
                : cardRepository.findBySetCodeInAndNameContainingIgnoreCaseOrderByNameAsc(setCodes, name);
        return cards.stream()
                .map(cardMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponseDto getCard(UUID cardId) {
        return cardMapper.toDto(getCardEntityById(cardId));
    }

    @Override
    @Transactional(readOnly = true)
    public Card getCardEntityById(UUID cardId) {
        return cardCache.computeIfAbsent(cardId, id -> {
            log.debug("[CARD_CACHE] Cache miss para carta id: {}. Consultando base de datos...", id);
            return cardRepository.findWithDetailsById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> getCardEntitiesByIds(List<UUID> cardIds) {
        List<Card> cards = cardRepository.findAllById(cardIds);
        Map<UUID, Card> cardsById = cards.stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        for (UUID cardId : cardIds) {
            if (!cardsById.containsKey(cardId)) {
                throw new ResourceNotFoundException("Card not found with id: " + cardId);
            }
        }

        return cardIds.stream()
                .map(cardsById::get)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> getCardEntities(String setCode) {
        List<String> setCodes = resolvePlayableSetCodes(setCode);
        return setCodes.size() == 1
                ? cardRepository.findDeckBuildingCardsBySetCodeOrderByNumberAsc(setCodes.getFirst())
                : cardRepository.findDeckBuildingCardsBySetCodeInOrderBySetCodeAscNumberAsc(setCodes);
    }

    @Override
    @Transactional(readOnly = true)
    public CardImportStatusDto getXy1ImportStatus() {
        return cardImportService.getXy1Status();
    }

    private List<String> resolvePlayableSetCodes(String setCode) {
        if (setCode == null || setCode.isBlank()) {
            return Card.PLAYABLE_SET_CODES;
        }

        String normalizedSetCode = Card.normalizePlayableSetCode(setCode);
        if (normalizedSetCode == null) {
            throw new ResourceNotFoundException("Only playable setCodes xy1 and custom-professors are available");
        }
        return List.of(normalizedSetCode);
    }

    @EventListener
    public void handleCardCatalogModified(CardCatalogModifiedEvent event) {
        log.info("[CARD_CACHE] Catálogo de cartas modificado en BD. Limpiando caché de cartas...");
        cardCache.clear();
    }
}
