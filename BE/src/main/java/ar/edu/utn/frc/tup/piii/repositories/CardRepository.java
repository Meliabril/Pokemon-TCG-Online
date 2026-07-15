package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Card;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    boolean existsBySetCode(String setCode);

    long countBySetCode(String setCode);

    Optional<Card> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"attacks", "attacks.costs", "weaknesses", "resistances"})
    List<Card> findBySetCodeOrderByNumberAsc(String setCode);

    @EntityGraph(attributePaths = {"attacks", "attacks.costs", "weaknesses", "resistances"})
    List<Card> findBySetCodeInOrderBySetCodeAscNumberAsc(Collection<String> setCodes);

    @Query("select c from Card c where c.setCode = :setCode order by c.number asc")
    List<Card> findDeckBuildingCardsBySetCodeOrderByNumberAsc(@Param("setCode") String setCode);

    @Query("select c from Card c where c.setCode in :setCodes order by c.setCode asc, c.number asc")
    List<Card> findDeckBuildingCardsBySetCodeInOrderBySetCodeAscNumberAsc(
            @Param("setCodes") Collection<String> setCodes);

    @EntityGraph(attributePaths = {"attacks", "attacks.costs", "weaknesses", "resistances"})
    List<Card> findBySetCodeAndNameContainingIgnoreCaseOrderByNameAsc(String setCode, String name);

    @EntityGraph(attributePaths = {"attacks", "attacks.costs", "weaknesses", "resistances"})
    List<Card> findBySetCodeInAndNameContainingIgnoreCaseOrderByNameAsc(Collection<String> setCodes, String name);

    @EntityGraph(attributePaths = {"attacks", "attacks.costs", "weaknesses", "resistances"})
    Optional<Card> findWithDetailsById(UUID id);
}
