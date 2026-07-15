package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameRandomServiceImpl implements GameRandomService {

    @Override
    public <T> List<T> shuffledCopy(List<T> source) {
        List<T> shuffled = new ArrayList<>(source);
        java.util.Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled;
    }

    @Override
    public <T> T chooseOne(List<T> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("Cannot choose a random element from an empty source");
        }
        return source.get(ThreadLocalRandom.current().nextInt(source.size()));
    }

    @Override
    public boolean flipCoin() {
        return ThreadLocalRandom.current().nextBoolean();
    }
}
