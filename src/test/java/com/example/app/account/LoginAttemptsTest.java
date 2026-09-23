package com.example.app.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.app.web.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptsTest {

    private Instant now = Instant.parse("2026-09-23T10:00:00Z");
    private final LoginAttempts attempts = new LoginAttempts(new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    });

    private void failTimes(int n) {
        for (int i = 0; i < n; i++) {
            attempts.failed("a@example.com");
        }
    }

    @Test
    void blocksAfterTooManyFailures() {
        failTimes(LoginAttempts.MAX_FAILURES - 1);
        assertThatCode(() -> attempts.checkAllowed("a@example.com")).doesNotThrowAnyException();

        failTimes(1);
        assertThatThrownBy(() -> attempts.checkAllowed("a@example.com")).isInstanceOf(ApiException.class);
        assertThatCode(() -> attempts.checkAllowed("b@example.com")).doesNotThrowAnyException();
    }

    @Test
    void unblocksAfterTheWindow() {
        failTimes(LoginAttempts.MAX_FAILURES);
        now = now.plus(Duration.ofMinutes(16));
        assertThatCode(() -> attempts.checkAllowed("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheCounter() {
        failTimes(LoginAttempts.MAX_FAILURES - 1);
        attempts.succeeded("a@example.com");
        failTimes(LoginAttempts.MAX_FAILURES - 1);
        assertThatCode(() -> attempts.checkAllowed("a@example.com")).doesNotThrowAnyException();
    }
}
