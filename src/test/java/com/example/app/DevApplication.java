package com.example.app;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Lance l'application en développement avec un PostgreSQL embarqué, sans Docker ni installation.
 *
 * <p>Les données sont conservées entre deux lancements dans {@code .dev-db/} (supprimer ce dossier pour repartir de
 * zéro). Usage : {@code scripts/dev.sh} ou {@code ./mvnw spring-boot:test-run
 * -Dspring-boot.run.main-class=com.example.app.DevApplication}.
 */
public final class DevApplication {

    private static final int POSTGRES_PORT = 54329;

    private DevApplication() {}

    public static void main(String[] args) throws IOException {
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setDataDirectory(Path.of(".dev-db"))
                .setCleanDataDirectory(false)
                .setPort(POSTGRES_PORT)
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (IOException e) {
                // arrêt de la JVM : rien à faire de plus
            }
        }));

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        System.setProperty("server.servlet.session.cookie.secure", "false"); // http://localhost
        Application.main(args);
    }
}
