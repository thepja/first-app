package com.example.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Sert le front-end (build Angular) embarqué dans le JAR sous {@code static/}.
 *
 * <p>{@code index.html} n'est jamais mis en cache (il référence les bundles de la version courante) ; les bundles,
 * dont le nom contient une empreinte, sont mis en cache un an.
 */
final class StaticFiles implements HttpHandler {

    private static final String ROOT = "static";
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "json", "application/json",
            "svg", "image/svg+xml",
            "png", "image/png",
            "ico", "image/x-icon",
            "woff2", "font/woff2",
            "txt", "text/plain; charset=utf-8");

    private final ClassLoader classLoader = StaticFiles.class.getClassLoader();

    /** Indique si un front-end est embarqué (absent quand l'application tourne sans build Angular). */
    boolean available() {
        return classLoader.getResource(ROOT + "/index.html") != null;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String file = path.equals("/") ? "index.html" : path.substring(1);
        if (file.isEmpty() || file.contains("..") || file.contains("\\") || file.endsWith("/")) {
            App.respond(ex, 404, "Not Found");
            return;
        }
        try (InputStream in = classLoader.getResourceAsStream(ROOT + "/" + file)) {
            if (in == null) {
                App.respond(ex, 404, "Not Found");
                return;
            }
            byte[] body = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", contentType(file));
            ex.getResponseHeaders()
                    .set(
                            "Cache-Control",
                            file.equals("index.html") ? "no-cache" : "public, max-age=31536000, immutable");
            ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static String contentType(String file) {
        int dot = file.lastIndexOf('.');
        String extension = dot < 0 ? "" : file.substring(dot + 1);
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
