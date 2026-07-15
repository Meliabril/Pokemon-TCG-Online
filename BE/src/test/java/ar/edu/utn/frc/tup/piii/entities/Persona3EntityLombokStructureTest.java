package ar.edu.utn.frc.tup.piii.entities;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Persona3EntityLombokStructureTest {

    @Test
    void persona3JpaEntitiesShouldNotUseForbiddenLombokAnnotations() throws IOException {
        for (String entityFileName : persona3EntityFiles()) {
            String source = Files.readString(entitySourcePath(entityFileName));

            assertThat(source)
                    .as(entityFileName)
                    .contains("@Entity")
                    .doesNotContain("@Data")
                    .doesNotContain("@Builder")
                    .doesNotContain("@ToString")
                    .doesNotContain("@EqualsAndHashCode");
        }
    }

    private List<String> persona3EntityFiles() {
        return List.of(
                "Game.java",
                "GameParticipant.java",
                "GameStateSnapshot.java",
                "GameActionLog.java",
                "GameEvent.java",
                "GameCardInstance.java"
        );
    }

    private Path entitySourcePath(String entityFileName) {
        return Path.of("src", "main", "java", "ar", "edu", "utn", "frc", "tup", "piii", "entities", entityFileName);
    }
}
