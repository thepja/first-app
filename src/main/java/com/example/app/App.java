package com.example.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serveur HTTP minimal exposant {@code /}, {@code /hello}, {@code /health} et {@code /version}. */
public final class App {

    private static final Logger LOG = System.getLogger(App.class.getName());
    private static final int SHUTDOWN_GRACE_SECONDS = 5;
    private static final String INDEX =
            """
            first-app

            GET /hello?name=Alice  -> Hello, Alice!
            GET /health            -> OK
            GET /version           -> version déployée
            """;

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Greeter greeter = new Greeter();

    public App(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);
        route("/", ex -> respond(ex, 200, INDEX));
        route("/health", ex -> respond(ex, 200, "OK"));
        route("/version", ex -> respond(ex, 200, version()));
        route("/hello", ex -> respond(ex, 200, greeter.greet(queryParam(ex, "name"))));
    }

    public void start() {
        server.start();
        LOG.log(Level.INFO, "Server started on port " + port());
    }

    /** Arrête d'accepter les connexions puis laisse les requêtes en cours se terminer. */
    public void stop() {
        server.stop(SHUTDOWN_GRACE_SECONDS);
        executor.close();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    /** Version du JAR (MANIFEST.MF), « dev » quand l'application ne tourne pas depuis le JAR. */
    static String version() {
        String version = App.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }

    /** Enregistre une route GET sur un chemin exact (HttpServer associe sinon tous les chemins préfixés). */
    private void route(String path, HttpHandler handler) {
        server.createContext(path, getOnly(path, handler));
    }

    private static HttpHandler getOnly(String path, HttpHandler handler) {
        return ex -> {
            try {
                if (!path.equals(ex.getRequestURI().getPath())) {
                    respond(ex, 404, "Not Found");
                    return;
                }
                if (!"GET".equals(ex.getRequestMethod())) {
                    ex.getResponseHeaders().set("Allow", "GET");
                    respond(ex, 405, "Method Not Allowed");
                    return;
                }
                handler.handle(ex);
            } catch (RuntimeException e) {
                LOG.log(Level.ERROR, "Unhandled error on " + ex.getRequestURI(), e);
                respond(ex, 500, "Internal Server Error");
            } finally {
                ex.close();
            }
        };
    }

    static String queryParam(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(key) && kv.length == 2) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        App app = new App(port);
        // SIGTERM (arrêt du pod) : arrêt propre. Pas de log ici, JUL se réinitialise dans son propre hook.
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop, "shutdown"));
        app.start();
    }
}
