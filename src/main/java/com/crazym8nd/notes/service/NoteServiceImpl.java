package com.crazym8nd.notes.service;

import com.crazym8nd.notes.domain.Note;
import com.crazym8nd.notes.domain.NoteDraft;
import com.crazym8nd.notes.exception.NoteNotFoundException;
import com.crazym8nd.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class NoteServiceImpl implements NoteService {

    private final NoteRepository repository;

    @Override
    public Note create(NoteDraft draft) {
        Note note = new Note(UUID.randomUUID(), NoteNormalizer.normalizeTitle(draft.title()),
                NoteNormalizer.normalizeContent(draft.content()),
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                NoteNormalizer.normalizeTags(draft.tags()));
        repository.put(note);
        log.info("Заметка создана: id={}", note.id());
        return note;
    }

    @Override
    public Note get(UUID id) {
        return repository.find(id).orElseThrow(() -> new NoteNotFoundException(id));
    }

    @Override
    public List<Note> list() {
        return repository.findAll();
    }

    @Override
    public List<Note> listByTag(String tag) {
        return repository.findByTag(NoteNormalizer.normalizeTag(tag));
    }

    @Override
    public Note update(UUID id, NoteDraft draft) {
        Note updated = repository.replace(id, previous ->
                        new Note(id, NoteNormalizer.normalizeTitle(draft.title()),
                                NoteNormalizer.normalizeContent(draft.content()),
                                previous.createdAt(), NoteNormalizer.normalizeTags(draft.tags())))
                .orElseThrow(() -> new NoteNotFoundException(id));
        log.info("Заметка обновлена: id={}", id);
        return updated;
    }

    @Override
    public void delete(UUID id) {
        if (!repository.delete(id)) {
            throw new NoteNotFoundException(id);
        }
        log.info("Заметка удалена: id={}", id);
    }
}
