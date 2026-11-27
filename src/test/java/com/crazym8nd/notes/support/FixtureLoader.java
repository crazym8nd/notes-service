package com.crazym8nd.notes.support;

import java.nio.charset.StandardCharsets;

public final class FixtureLoader {

    private FixtureLoader() {
    }

    public static String json(String name) {
        try (var stream = FixtureLoader.class.getResourceAsStream("/fixtures/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("Фикстура fixtures/" + name + " не найдена");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Фикстура fixtures/" + name + " не читается", e);
        }
    }
}
