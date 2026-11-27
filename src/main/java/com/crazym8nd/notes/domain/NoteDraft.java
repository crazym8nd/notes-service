package com.crazym8nd.notes.domain;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record NoteDraft(@NotBlank String title, String content, List<String> tags) {
}
