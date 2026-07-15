package ar.edu.utn.frc.tup.piii.controllers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GameControllerLombokStructureTest {

    @Test
    void gameControllerShouldUseRequiredArgsConstructorWithoutManualConstructor() throws IOException {
        assertUsesRequiredArgsConstructor("GameController.java", "public GameController(");
    }

    @Test
    void gameHistoryControllerShouldUseRequiredArgsConstructorWithoutManualConstructor() throws IOException {
        assertUsesRequiredArgsConstructor("GameHistoryController.java", "public GameHistoryController(");
    }

    private void assertUsesRequiredArgsConstructor(String fileName, String removedConstructorSignature) throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "ar", "edu", "utn", "frc", "tup", "piii", "controllers", fileName));

        assertThat(source).contains("@RequiredArgsConstructor");
        assertThat(source).doesNotContain(removedConstructorSignature);
    }
}
