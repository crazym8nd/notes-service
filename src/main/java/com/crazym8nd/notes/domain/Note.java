package com.crazym8nd.notes.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Note(UUID id, String title, String content, Instant createdAt, List<String> tags) {
}
