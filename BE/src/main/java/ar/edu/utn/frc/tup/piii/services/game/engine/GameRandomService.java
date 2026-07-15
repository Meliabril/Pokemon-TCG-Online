package ar.edu.utn.frc.tup.piii.services.game.engine;

import java.util.List;

public interface GameRandomService {

    <T> List<T> shuffledCopy(List<T> source);

    <T> T chooseOne(List<T> source);

    boolean flipCoin();
}
