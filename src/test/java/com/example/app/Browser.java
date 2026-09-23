package com.example.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Client HTTP qui se comporte comme le front-end Angular : il garde les cookies (session, XSRF-TOKEN) et renvoie le jeton
 * CSRF dans l'en-tête X-XSRF-TOKEN des requêtes qui modifient des données.
 */
public final class Browser {

    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client;
    private final String baseUrl;

    Browser(int port) {
        this.client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .proxy(HttpClient.Builder.NO_PROXY)
                .build();
        this.baseUrl = "http://localhost:" + port;
    }

    public HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    public HttpResponse<String> post(String path, String json) {
        return send(withCsrf(json("POST", path, json)));
    }

    public HttpResponse<String> put(String path, String json) {
        return send(withCsrf(json("PUT", path, json)));
    }

    /** Envoi de fichier (multipart/form-data), comme un FormData Angular. */
    public HttpResponse<String> putFile(
            String path, String field, String filename, String contentType, byte[] content) {
        String boundary = "----first-app-" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + filename + "\"\r\n"
                        + "Content-Type: " + contentType + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.writeBytes(content);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(withCsrf(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))));
    }

    public HttpResponse<byte[]> getBytes(String path) {
        try {
            return client.send(
                    HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public HttpResponse<String> delete(String path) {
        return send(withCsrf(HttpRequest.newBuilder(uri(path)).DELETE()));
    }

    /** Requête sans jeton CSRF, comme celle qu'un site tiers pourrait forger. */
    public HttpResponse<String> postWithoutCsrf(String path, String json) {
        return send(json("POST", path, json));
    }

    public Optional<String> cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst();
    }

    /** Inscrit un nouvel utilisateur ; le navigateur est ensuite connecté. */
    public HttpResponse<String> register(String email, String password, String displayName) {
        // Première visite : l'application Angular appelle /api/auth/me, qui fournit le cookie XSRF-TOKEN
        get("/api/auth/me");
        return post(
                "/api/auth/register",
                """
                {"email": "%s", "password": "%s", "displayName": "%s"}"""
                        .formatted(email, password, displayName));
    }

    private HttpRequest.Builder json(String method, String path, String json) {
        return HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpRequest.Builder withCsrf(HttpRequest.Builder request) {
        cookie("XSRF-TOKEN").ifPresent(token -> request.header("X-XSRF-TOKEN", token));
        return request;
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
