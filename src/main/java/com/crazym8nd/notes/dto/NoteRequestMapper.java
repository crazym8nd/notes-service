package com.crazym8nd.notes.dto;

import com.crazym8nd.notes.domain.NoteDraft;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteRequestMapper {

    NoteDraft toDraft(NoteCreateRequestDto request);

    NoteDraft toDraft(NoteUpdateRequestDto request);

    default List<String> map(Set<String> tags) {
        return tags == null ? List.of() : List.copyOf(tags);
    }
}
