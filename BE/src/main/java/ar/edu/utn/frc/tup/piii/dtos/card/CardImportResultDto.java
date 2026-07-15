package ar.edu.utn.frc.tup.piii.dtos.card;

public record CardImportResultDto(
        String setCode,
        long importedCards,
        boolean complete,
        String message) {
}
