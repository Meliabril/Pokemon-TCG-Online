package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SupabasePersistenceConfigurationTest {

    @Test
    void shouldConfigureOsivAndJdbcBatchingForSupabase() throws IOException {
        Properties properties = loadSupabaseProperties();

        assertThat(properties)
                .containsEntry("spring.jpa.open-in-view", "false")
                .containsEntry("spring.jpa.properties.hibernate.jdbc.batch_size", "50")
                .containsEntry("spring.jpa.properties.hibernate.order_inserts", "true")
                .containsEntry("spring.jpa.properties.hibernate.order_updates", "true")
                .containsEntry("spring.datasource.hikari.data-source-properties.reWriteBatchedInserts", "true");
    }

    private Properties loadSupabaseProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-supabase.properties")) {
            assertThat(input).isNotNull();
            properties.load(input);
        }
        return properties;
    }
}
