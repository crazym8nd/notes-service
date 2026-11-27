package com.crazym8nd.notes.rest;

import com.crazym8nd.notes.config.SecurityConfig;
import com.crazym8nd.notes.domain.Note;
import com.crazym8nd.notes.domain.NoteDraft;
import com.crazym8nd.notes.dto.NoteRequestMapperImpl;
import com.crazym8nd.notes.dto.NoteResponseMapperImpl;
import com.crazym8nd.notes.exception.NoteNotFoundException;
import com.crazym8nd.notes.service.NoteService;
import com.crazym8nd.notes.support.FixtureLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotesController.class)
@Import({SecurityConfig.class, NoteResponseMapperImpl.class, NoteRequestMapperImpl.class})
class NotesControllerWebTest {

    private static final String READER = "notes-reader";
    private static final String WRITER = "notes-writer";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoteService noteService;

    @BeforeEach
    void resetNoteServiceStub() {
        reset(noteService);
    }

    @TestConfiguration
    static class StubConfig {
        @Primary
        @Bean
        NoteService noteService() {
            return mock(NoteService.class);
        }
    }

    @Test
    @DisplayName("GET /api/v1/notes с ролью reader -> 200 и список заметок")
    void shouldReturnNotesForReader() throws Exception {
        when(noteService.list()).thenReturn(List.of(
                new Note(UUID.randomUUID(), "Заметка", "текст",
                        Instant.parse("2026-01-01T10:00:00Z"), List.of("work"))));

        mockMvc.perform(get("/api/v1/notes").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Заметка"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-01T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /api/v1/notes без токена -> 401")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/notes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/notes с ролью writer -> 200: писатель может читать")
    void shouldReturnNotesForWriter() throws Exception {
        when(noteService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notes").with(writer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/notes?tag=work передаёт фильтр в сервис")
    void shouldFilterByTag() throws Exception {
        when(noteService.listByTag("work")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notes").queryParam("tag", "work").with(reader()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /api/v1/notes с тегом длиннее 50 символов -> 400 Problem")
    void shouldReturn400ForTooLongTag() throws Exception {
        mockMvc.perform(get("/api/v1/notes").queryParam("tag", "x".repeat(51)).with(reader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/notes с пустым tag -> список без фильтра")
    void shouldListAllForBlankTag() throws Exception {
        when(noteService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notes").queryParam("tag", "   ").with(reader()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("POST /api/v1/notes с ролью writer -> 201, Location и телом заметки")
    void shouldCreateNoteForWriter() throws Exception {
        UUID id = UUID.randomUUID();
        when(noteService.create(any(NoteDraft.class))).thenReturn(
                new Note(id, "Встреча", "обсудить релиз", Instant.now(), List.of("work", "дом")));

        mockMvc.perform(post("/api/v1/notes")
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FixtureLoader.json("note-create.json")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/notes/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/notes с ролью только reader -> 403")
    void shouldReturn403ForReaderOnCreate() throws Exception {
        mockMvc.perform(post("/api/v1/notes")
                        .with(reader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FixtureLoader.json("note-create.json")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/notes с пустым title -> 400 Problem")
    void shouldReturn400ForBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/notes")
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FixtureLoader.json("note-blank-title.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/notes/{id} существующей -> 200")
    void shouldReturnNoteById() throws Exception {
        UUID id = UUID.randomUUID();
        when(noteService.get(id)).thenReturn(new Note(id, "Заголовок", "текст",
                Instant.parse("2026-01-01T10:00:00Z"), List.of("work")));

        mockMvc.perform(get("/api/v1/notes/{id}", id).with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/notes/{id} отсутствующей -> 404 Problem")
    void shouldReturn404ForUnknownNote() throws Exception {
        when(noteService.get(any())).thenThrow(new NoteNotFoundException(UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/notes/{id}", UUID.randomUUID()).with(reader()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{id} с ролью writer -> 200 и обновлённым телом")
    void shouldUpdateNoteForWriter() throws Exception {
        UUID id = UUID.randomUUID();
        when(noteService.update(eq(id), any(NoteDraft.class))).thenReturn(
                new Note(id, "Финал", "итог встречи", Instant.parse("2026-01-01T10:00:00Z"), List.of("final")));

        mockMvc.perform(put("/api/v1/notes/{id}", id)
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FixtureLoader.json("note-update.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Финал"));
    }

    @Test
    @DisplayName("DELETE /api/v1/notes/{id} с ролью writer -> 204")
    void shouldDeleteNoteForWriter() throws Exception {
        mockMvc.perform(delete("/api/v1/notes/{id}", UUID.randomUUID()).with(writer()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/notes/{id} отсутствующей -> 404 Problem")
    void shouldReturn404ForUnknownNoteOnDelete() throws Exception {
        doThrow(new NoteNotFoundException(UUID.randomUUID())).when(noteService).delete(any());

        mockMvc.perform(delete("/api/v1/notes/{id}", UUID.randomUUID()).with(writer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private static JwtRequestPostProcessor reader() {
        return jwt().authorities(new SimpleGrantedAuthority(READER));
    }

    private static JwtRequestPostProcessor writer() {
        return jwt().authorities(new SimpleGrantedAuthority(WRITER));
    }
}
