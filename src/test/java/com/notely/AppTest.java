package com.notely;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke test — verifies the project compiles and JUnit 5 runs.
 */
class AppTest {

    @Test
    void applicationClassCanBeInstantiated() {
        assertDoesNotThrow(() -> new App(),
                "App should be instantiable without throwing");
    }
}
