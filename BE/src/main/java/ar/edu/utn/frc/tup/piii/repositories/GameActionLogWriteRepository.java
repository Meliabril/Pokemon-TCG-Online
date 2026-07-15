package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameActionLog;

public interface GameActionLogWriteRepository {

    <S extends GameActionLog> S save(S log);
}
