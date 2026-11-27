package com.crazym8nd.notes.dto;

import com.crazym8nd.notes.domain.Note;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NoteResponseMapper {

    NoteResponseDto toResponse(Note note);

    default OffsetDateTime map(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    default Set<String> map(List<String> tags) {
        return tags == null ? null : new LinkedHashSet<>(tags);
    }
}
