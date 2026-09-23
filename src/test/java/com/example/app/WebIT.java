package com.example.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebIT extends IntegrationTest {

    @Test
    void healthAndProbesAreUp() {
        assertThat(browser().get("/health").statusCode()).isEqualTo(200);
        assertThat(browser().get("/health/liveness").statusCode()).isEqualTo(200);
        assertThat(browser().get("/health/readiness").statusCode()).isEqualTo(200);
    }

    @Test
    void helloAndVersionAnswerInPlainText() {
        assertThat(browser().get("/hello?name=CI").body()).isEqualTo("Hello, CI!");
        assertThat(browser().get("/hello").body()).isEqualTo("Hello, World!");
        assertThat(browser().get("/version").body()).isNotBlank();
    }

    @Test
    void servesTheFrontEndWithoutCachingIndex() {
        var response = browser().get("/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<app-root>");
        assertThat(response.headers().firstValue("Cache-Control"))
                .hasValueSatisfying(v -> assertThat(v).contains("no-cache"));
        assertThat(response.headers().firstValue("Content-Security-Policy"))
                .hasValueSatisfying(v -> assertThat(v).contains("default-src 'self'"));
    }

    @Test
    void angularRoutesServeIndexOnReload() {
        var response = browser().get("/books");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<app-root>");
    }

    @Test
    void hashedBundlesAreCachedForAYear() {
        var response = browser().get("/main-TEST.js");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control"))
                .hasValueSatisfying(v -> assertThat(v).contains("max-age=31536000", "immutable"));
    }

    @Test
    void unknownPathsReturn404() {
        assertThat(browser().get("/nope").statusCode()).isEqualTo(404);
    }
}
