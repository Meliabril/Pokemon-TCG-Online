package ar.edu.utn.frc.tup.piii.controllers.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Playable card catalog query API")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    @Operation(summary = "List playable cards")
    public List<CardResponseDto> getCards(
            @RequestParam(required = false) String setCode) {
        return cardService.getCards(setCode);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a card by id")
    public CardResponseDto getCard(@PathVariable UUID id) {
        return cardService.getCard(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search playable cards by name")
    public List<CardResponseDto> searchCards(
            @RequestParam(required = false) String setCode,
            @RequestParam @NotBlank(message = "Name is required") String name) {
        return cardService.searchCards(setCode, name);
    }

    @GetMapping("/import-status/xy1")
    @Operation(summary = "Get XY1 import status")
    public CardImportStatusDto getXy1ImportStatus() {
        return cardService.getXy1ImportStatus();
    }
}
