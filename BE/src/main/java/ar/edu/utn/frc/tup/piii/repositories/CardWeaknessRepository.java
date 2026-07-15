package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardWeaknessRepository extends JpaRepository<CardWeakness, UUID> {
}
