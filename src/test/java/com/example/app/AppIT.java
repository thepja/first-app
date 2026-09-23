package com.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void versionEndpointReturnsVersion() throws Exception {
        HttpResponse<String> response = get("/version");
        assertEquals(200, response.statusCode());
        assertEquals(App.version(), response.body());
    }

    @Test
    void rootServesFrontend() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<app-root>"));
        assertEquals(
                "text/html; charset=utf-8",
                response.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("no-cache", response.headers().firstValue("Cache-Control").orElseThrow());
    }

    @Test
    void servesHashedAssetsWithLongCache() throws Exception {
        HttpResponse<String> response = get("/main-TEST.js");
        assertEquals(200, response.statusCode());
        assertEquals(
                "text/javascript; charset=utf-8",
                response.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(response.headers().firstValue("Cache-Control").orElseThrow().contains("immutable"));
    }

    @Test
    void refusesPathTraversal() throws Exception {
        assertEquals(404, get("/../com/example/app/App.class").statusCode());
        assertEquals(404, get("/%2e%2e/com/example/app/App.class").statusCode());
    }

    @Test
    void unknownPathsReturn404() throws Exception {
        assertEquals(404, get("/nope").statusCode());
        assertEquals(404, get("/hellofoo").statusCode());
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
