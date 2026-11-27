package com.crazym8nd.notes.repository.jpa;

import com.crazym8nd.notes.domain.Note;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteEntityMapper {

    Note toDomain(NoteEntity entity);

    NoteEntity toEntity(Note note);

    default List<String> map(Set<String> tags) {
        return tags == null ? List.of() : List.copyOf(tags);
    }

    default Set<String> map(List<String> tags) {
        return tags == null ? Set.of() : Set.copyOf(tags);
    }
}
