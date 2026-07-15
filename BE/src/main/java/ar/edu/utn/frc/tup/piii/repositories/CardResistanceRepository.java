package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardResistanceRepository extends JpaRepository<CardResistance, UUID> {
}
