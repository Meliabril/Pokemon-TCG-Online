package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttackCostRepository extends JpaRepository<AttackCost, UUID> {
}
