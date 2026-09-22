package com.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests d'intégration : démarre le vrai serveur HTTP sur un port libre. */
class AppIT {

    private App app;
    private final HttpClient client =
            HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @BeforeEach
    void setUp() throws Exception {
        app = new App(0);
        app.start();
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void helloEndpointGreetsByName() throws Exception {
        HttpResponse<String> response = get("/hello?name=Jean%20Paul");
        assertEquals(200, response.statusCode());
        assertEquals("Hello, Jean Paul!", response.body());
    }

    @Test
    void helloEndpointDefaultsToWorld() throws Exception {
        assertEquals("Hello, World!", get("/hello").body());
    }

    @Test
    void rejectsNonGetMethods() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + "/hello"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("Allow").orElseThrow());
    }
}
