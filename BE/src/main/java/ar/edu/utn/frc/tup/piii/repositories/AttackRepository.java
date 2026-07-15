package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttackRepository extends JpaRepository<Attack, UUID> {
}
