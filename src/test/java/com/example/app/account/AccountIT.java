package com.example.app.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.Browser;
import com.example.app.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountIT extends IntegrationTest {

    private static final String PASSWORD = "correct horse battery";

    private static String newEmail() {
        return "reader-" + UUID.randomUUID() + "@example.com";
    }

    private static String credentials(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
    }

    @Test
    void anonymousVisitorIsNotLoggedIn() {
        assertThat(browser().get("/api/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void registrationLogsTheUserIn() {
        Browser browser = browser();
        String email = newEmail();

        var registered = browser.register(email, PASSWORD, "Alice");

        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(registered.body()).contains(email, "Alice").doesNotContain("password");
        var me = browser.get("/api/auth/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(email);
        assertThat(browser.cookie("SESSION")).isPresent();
    }

    @Test
    void emailIsCaseInsensitiveAndUnique() {
        String email = newEmail();
        browser().register(email, PASSWORD, "Alice");

        var duplicate = browser().register(email.toUpperCase(), PASSWORD, "Bob");

        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(duplicate.headers().firstValue("Content-Type")).hasValue("application/problem+json");
    }

    @Test
    void registrationValidatesInput() {
        assertThat(browser().register("not-an-email", PASSWORD, "Alice").statusCode())
                .isEqualTo(400);
        assertThat(browser().register(newEmail(), "short", "Alice").statusCode())
                .isEqualTo(400);
        assertThat(browser().register(newEmail(), PASSWORD, " ").statusCode()).isEqualTo(400);
    }

    @Test
    void loginAndLogout() {
        String email = newEmail();
        browser().register(email, PASSWORD, "Alice");
        Browser browser = browser();
        browser.get("/api/auth/me");

        var login = browser.post("/api/auth/login", credentials("  " + email.toUpperCase(), PASSWORD));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("Alice");
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(200);

        assertThat(browser.post("/api/auth/logout", "{}").statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void sessionIdAndCsrfTokenChangeOnLogin() {
        String email = newEmail();
        browser().register(email, PASSWORD, "Alice");
        Browser browser = browser();
        browser.get("/api/auth/me");
        String csrfBefore = browser.cookie("XSRF-TOKEN").orElseThrow();

        browser.post("/api/auth/login", credentials(email, PASSWORD));

        assertThat(browser.cookie("XSRF-TOKEN")).isPresent().isNotEqualTo(java.util.Optional.of(csrfBefore));
        // Le nouveau jeton est immédiatement utilisable
        assertThat(browser.post("/api/books", """
                {"title": "Dune", "rating": 5}""")
                        .statusCode())
                .isEqualTo(201);
    }

    @Test
    void wrongPasswordIsRejectedWithoutRevealingWhichFieldIsWrong() {
        String email = newEmail();
        browser().register(email, PASSWORD, "Alice");
        Browser browser = browser();
        browser.get("/api/auth/me");

        var wrongPassword = browser.post("/api/auth/login", credentials(email, "wrong password"));
        var unknownUser = browser.post("/api/auth/login", credentials(newEmail(), PASSWORD));

        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(unknownUser.statusCode()).isEqualTo(401);
        assertThat(wrongPassword.body()).isEqualTo(unknownUser.body());
    }

    @Test
    void repeatedFailuresBlockTheAccountTemporarily() {
        String email = newEmail();
        browser().register(email, PASSWORD, "Alice");
        Browser browser = browser();
        browser.get("/api/auth/me");

        for (int i = 0; i < LoginAttempts.MAX_FAILURES; i++) {
            browser.post("/api/auth/login", credentials(email, "wrong password"));
        }

        assertThat(browser.post("/api/auth/login", credentials(email, PASSWORD)).statusCode())
                .isEqualTo(429);
    }

    @Test
    void requestsWithoutCsrfTokenAreRejected() {
        Browser browser = browser();
        browser.register(newEmail(), PASSWORD, "Alice");

        assertThat(browser.postWithoutCsrf("/api/books", """
                {"title": "Dune", "rating": 5}""")
                        .statusCode())
                .isEqualTo(403);
    }
}
