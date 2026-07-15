package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface MatchmakingUserAccessRepository {

    Optional<User> findById(UUID userId);
}
