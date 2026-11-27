package com.crazym8nd.notes.it;

import com.crazym8nd.notes.support.AbstractNotesApiIT;
import com.crazym8nd.notes.support.FixtureLoader;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql("/fixtures/db/seed-notes.sql")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotesApiIT extends AbstractNotesApiIT {

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/notes: валидный запрос -> 201 Created, Location и тело")
    void shouldCreateNoteFromFixture() {
        var response = post("/api/v1/notes", FixtureLoader.json("note-create.json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        JsonNode body = read(response);
        assertThat(body.path("title").asText()).isEqualTo("Встреча");
        assertThat(body.path("tags").get(0).asText()).isEqualTo("work");
        assertThat(body.hasNonNull("createdAt")).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/notes: без токена -> 401")
    void shouldReturn401WithoutToken() {
        var response = postWithoutToken("/api/v1/notes", FixtureLoader.json("note-create.json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v1/notes: пустой title -> 400 Problem")
    void shouldReturn400ForBlankTitle() {
        var response = post("/api/v1/notes", FixtureLoader.json("note-blank-title.json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var contentType = response.getHeaders().getContentType();
        assertThat(contentType).isNotNull();
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        assertThat(read(response).path("status").asInt()).isEqualTo(400);
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/notes: список содержит заметку из SQL-фикстуры")
    void shouldReturnSeededNoteInList() {
        var response = get("/api/v1/notes");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsOf(read(response))).contains(SEEDED_NOTE_ID);
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v1/notes?tag=: фильтр возвращает только заметку из фикстуры")
    void shouldFilterBySeededTag() {
        var response = getWithQuery("/api/v1/notes?tag={tag}", "tag", "дом");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsOf(read(response))).containsExactly(SEEDED_NOTE_ID);
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/notes/{id}: заметка из SQL-фикстуры -> 200")
    void shouldReturnSeededNoteById() {
        var response = get("/api/v1/notes/" + SEEDED_NOTE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(response).path("title").asText()).isEqualTo("Заметка из фикстуры");
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/v1/notes/{id}: несуществующая -> 404 Problem")
    void shouldReturn404ForUnknownNote() {
        var response = get("/api/v1/notes/" + UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(read(response).path("status").asInt()).isEqualTo(404);
    }

    @Test
    @Order(8)
    @DisplayName("PUT /api/v1/notes/{id}: обновляет содержимое и сохраняет createdAt")
    void shouldUpdateNotePreservingCreatedAt() {
        var response = put("/api/v1/notes/" + SEEDED_NOTE_ID, FixtureLoader.json("note-update.json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = read(response);
        assertThat(body.path("title").asText()).isEqualTo("Финал");
        assertThat(body.path("createdAt").asText()).isEqualTo(SEEDED_CREATED_AT);
    }

    @Test
    @Order(9)
    @DisplayName("PUT /api/v1/notes/{id}: несуществующая -> 404")
    void shouldReturn404WhenUpdatingUnknownNote() {
        var response = put("/api/v1/notes/" + UUID.randomUUID(), FixtureLoader.json("note-update.json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/v1/notes/{id}: удаляет -> 204, повторный GET -> 404")
    void shouldDeleteNoteAndReturn404OnSecondGet() {
        var deletion = delete("/api/v1/notes/" + SEEDED_NOTE_ID);
        var subsequent = get("/api/v1/notes/" + SEEDED_NOTE_ID);

        assertThat(deletion.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(subsequent.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/v1/notes/{id}: несуществующая -> 404")
    void shouldReturn404WhenDeletingUnknownNote() {
        var response = delete("/api/v1/notes/" + UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
