package ar.edu.utn.frc.tup.piii.services.user;

import ar.edu.utn.frc.tup.piii.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountService {

    Optional<User> findUserEntityByEmail(String email);

    User getUserEntityByEmail(String email);

    User getUserEntityById(UUID id);

    User markEmailAsVerified(User user);

    User updatePasswordHash(User user, String passwordHash);
}
