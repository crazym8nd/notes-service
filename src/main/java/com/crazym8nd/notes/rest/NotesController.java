package com.crazym8nd.notes.rest;

import com.crazym8nd.notes.api.NotesApi;
import com.crazym8nd.notes.domain.Note;
import com.crazym8nd.notes.dto.NoteCreateRequestDto;
import com.crazym8nd.notes.dto.NoteRequestMapper;
import com.crazym8nd.notes.dto.NoteResponseDto;
import com.crazym8nd.notes.dto.NoteResponseMapper;
import com.crazym8nd.notes.dto.NoteUpdateRequestDto;
import com.crazym8nd.notes.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
public class NotesController implements NotesApi {

    private final NoteService noteService;
    private final NoteResponseMapper responseMapper;
    private final NoteRequestMapper requestMapper;

    @Override
    public ResponseEntity<List<NoteResponseDto>> listNotes(String tag) {
        var notes = tag == null || tag.isBlank()
                ? noteService.list()
                : noteService.listByTag(tag);
        return ResponseEntity.ok(notes.stream()
                .map(responseMapper::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<NoteResponseDto> createNote(NoteCreateRequestDto noteCreateRequestDto) {
        Note created = noteService.create(requestMapper.toDraft(noteCreateRequestDto));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(responseMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<NoteResponseDto> getNote(UUID id) {
        return ResponseEntity.ok(responseMapper.toResponse(noteService.get(id)));
    }

    @Override
    public ResponseEntity<NoteResponseDto> updateNote(UUID id, NoteUpdateRequestDto noteUpdateRequestDto) {
        Note updated = noteService.update(id, requestMapper.toDraft(noteUpdateRequestDto));
        return ResponseEntity.ok(responseMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteNote(UUID id) {
        noteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
