package ar.edu.utn.frc.tup.piii.repositories.projections;

public interface DeckValidationSummaryProjection {

    long getTotalCards();

    long getCardsOutsideSet();

    long getBasicPokemonCards();
}
