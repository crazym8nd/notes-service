package com.crazym8nd.notes.exception;

import java.util.UUID;

public class NoteNotFoundException extends RuntimeException {

    private final UUID id;

    public NoteNotFoundException(UUID id) {
        super("Заметка %s не найдена".formatted(id));
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
