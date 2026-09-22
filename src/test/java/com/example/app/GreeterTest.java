package com.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreeterTest {

    private final Greeter greeter = new Greeter();

    @Test
    void greetsByName() {
        assertEquals("Hello, Alice!", greeter.greet("Alice"));
    }

    @Test
    void trimsName() {
        assertEquals("Hello, Bob!", greeter.greet("  Bob "));
    }

    @Test
    void defaultsToWorld() {
        assertEquals("Hello, World!", greeter.greet(null));
        assertEquals("Hello, World!", greeter.greet("   "));
    }
}
