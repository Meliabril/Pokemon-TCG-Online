package ar.edu.utn.frc.tup.piii.controllers.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.services.card.CardImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cards")
@Tag(name = "Admin Cards", description = "Admin-only card import operations")
@RequiredArgsConstructor
public class AdminCardController {

    private final CardImportService cardImportService;

    @PostMapping("/import/xy1")
    @Operation(summary = "Import the full XY1 set")
    public CardImportResultDto importXy1() {
        return cardImportService.importXy1();
    }
}
