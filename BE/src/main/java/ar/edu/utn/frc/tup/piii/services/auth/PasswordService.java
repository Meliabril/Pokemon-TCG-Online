package ar.edu.utn.frc.tup.piii.services.auth;

public interface PasswordService {
    String hash(String plainPassword);

    boolean matches(String plainPassword, String passwordHash);
}
