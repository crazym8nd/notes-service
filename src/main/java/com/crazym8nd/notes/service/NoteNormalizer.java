package com.crazym8nd.notes.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class NoteNormalizer {

    private NoteNormalizer() {
    }

    static String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    static String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    static List<String> normalizeTags(Collection<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(NoteNormalizer::normalizeTag)
                .distinct()
                .toList();
    }

    static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }
}
