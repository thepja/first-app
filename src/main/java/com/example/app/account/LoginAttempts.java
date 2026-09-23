package com.example.app.account;

import com.example.app.web.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Protection contre le « brute force » : au-delà de {@value #MAX_FAILURES} échecs en 15 minutes sur un compte, les
 * tentatives sont refusées jusqu'à la fin de la fenêtre.
 *
 * <p>Compteurs en mémoire, donc par instance : suffisant pour une instance unique (Render), à déplacer dans un
 * stockage partagé si l'application passe à plusieurs réplicas.
 */
@Component
class LoginAttempts {

    static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_ACCOUNTS = 10_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginAttempts() {
        this(Clock.systemUTC());
    }

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    void checkAllowed(String email) {
        Deque<Instant> recent = failures.get(email);
        if (recent == null) {
            return;
        }
        synchronized (recent) {
            prune(recent);
            if (recent.size() >= MAX_FAILURES) {
                throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives de connexion. Réessayez dans 15 minutes.");
            }
        }
    }

    void failed(String email) {
        if (failures.size() >= MAX_TRACKED_ACCOUNTS) {
            failures.values().removeIf(recent -> {
                synchronized (recent) {
                    prune(recent);
                    return recent.isEmpty();
                }
            });
        }
        Deque<Instant> recent = failures.computeIfAbsent(email, k -> new ArrayDeque<>());
        synchronized (recent) {
            recent.addLast(clock.instant());
        }
    }

    void succeeded(String email) {
        failures.remove(email);
    }

    private void prune(Deque<Instant> recent) {
        Instant limit = clock.instant().minus(WINDOW);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(limit)) {
            recent.removeFirst();
        }
    }
}
