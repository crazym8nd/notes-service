package com.crazym8nd.notes.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixtureLoaderTest {

    @Test
    @DisplayName("json читает содержимое фикстуры из classpath")
    void shouldReadFixtureFromClasspath() {
        assertThat(FixtureLoader.json("note-create.json"))
                .contains("\"title\": \"Встреча\"")
                .contains("\"tags\": [\"Work\", \"дом\"]");
    }

    @Test
    @DisplayName("отсутствующая фикстура -> IllegalStateException с именем файла")
    void shouldFailOnMissingFixture() {
        assertThatThrownBy(() -> FixtureLoader.json("no-such-fixture.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no-such-fixture.json");
    }
}
