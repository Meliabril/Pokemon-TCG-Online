package ar.edu.utn.frc.tup.piii.dtos.card;

public record CardImportStatusDto(
        String setCode,
        long importedCards,
        long expectedCards,
        boolean complete) {
}
