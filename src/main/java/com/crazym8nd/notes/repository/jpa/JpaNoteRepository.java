package com.crazym8nd.notes.repository.jpa;

import com.crazym8nd.notes.domain.Note;
import com.crazym8nd.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Repository
@RequiredArgsConstructor
public class JpaNoteRepository implements NoteRepository {

    private final NoteJpaRepository jpa;
    private final NoteEntityMapper mapper;

    @Override
    @Transactional
    public Optional<Note> put(Note note) {
        Optional<Note> previous = jpa.findById(note.id()).map(mapper::toDomain);
        jpa.save(mapper.toEntity(note));
        return previous;
    }

    @Override
    @Transactional
    public Optional<Note> replace(UUID id, UnaryOperator<Note> transformer) {
        return jpa.findById(id).map(entity -> {
            Note updated = transformer.apply(mapper.toDomain(entity));
            jpa.save(mapper.toEntity(updated));
            return updated;
        });
    }

    @Override
    public Optional<Note> find(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Note> findAll() {
        return jpa.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Note> findByTag(String tag) {
        return jpa.findByTagsOrderByCreatedAtDesc(tag).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }
}
