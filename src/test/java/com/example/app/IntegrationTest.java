package com.example.app;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Démarre l'application complète sur un port libre, avec un vrai PostgreSQL embarqué (partagé par tous les tests
 * d'intégration ; chaque test crée ses propres comptes, sans nettoyage).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @Value("${local.server.port}")
    protected int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        // Tests en HTTP simple : un cookie Secure ne serait pas renvoyé
        registry.add("server.servlet.session.cookie.secure", () -> "false");
    }

    protected Browser browser() {
        return new Browser(port);
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException e) {
                    // arrêt de la JVM : rien à faire de plus
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
