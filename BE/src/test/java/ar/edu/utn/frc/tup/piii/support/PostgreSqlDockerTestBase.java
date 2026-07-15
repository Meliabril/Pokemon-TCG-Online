package ar.edu.utn.frc.tup.piii.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Transitional compatibility base.
 *
 * The class keeps its original name to avoid touching many tests, but it now
 * forces all inherited tests to use H2 instead of a Dockerized PostgreSQL.
 */
public abstract class PostgreSqlDockerTestBase {

    private static final String JDBC_URL = "jdbc:h2:mem:pokemon_tcg;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> USERNAME);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    }
}
