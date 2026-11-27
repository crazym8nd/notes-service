package com.crazym8nd.notes.service;

import com.crazym8nd.notes.domain.Note;
import com.crazym8nd.notes.domain.NoteDraft;
import com.crazym8nd.notes.exception.NoteNotFoundException;
import com.crazym8nd.notes.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository repository;

    private NoteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NoteServiceImpl(repository);
    }

    @Nested
    @DisplayName("Создание заметки")
    class Create {

        @Test
        @DisplayName("генерирует id и createdAt, нормализует заголовок и теги")
        void createsNormalizedNote() {
            when(repository.put(any())).thenReturn(Optional.empty());

            Note created = service.create(new NoteDraft("  Покупки  ", "молоко", List.of(" Дом ", "", "дом", " Список ")));

            assertThat(created.id()).isNotNull();
            assertThat(created.createdAt()).isNotNull();
            assertThat(created.title()).isEqualTo("Покупки");
            assertThat(created.tags()).containsExactly("дом", "список");
        }

        @Test
        @DisplayName("null-контент и null-теги сохраняются как пустые значения")
        void defaultsMissingFields() {
            when(repository.put(any())).thenReturn(Optional.empty());

            Note created = service.create(new NoteDraft("Заголовок", null, null));

            assertThat(created.content()).isEmpty();
            assertThat(created.tags()).isEmpty();
        }

        @Test
        @DisplayName("сохраняет в репозиторий именно нормализованную заметку")
        void storesNormalizedNote() {
            when(repository.put(any())).thenReturn(Optional.empty());

            service.create(new NoteDraft("Заголовок", "текст", List.of("WORK")));

            ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
            verify(repository).put(captor.capture());
            assertThat(captor.getValue().tags()).containsExactly("work");
            assertThat(captor.getValue().title()).isEqualTo("Заголовок");
        }
    }

    @Nested
    @DisplayName("Получение заметки")
    class Get {

        @Test
        @DisplayName("существующая заметка возвращается")
        void returnsExisting() {
            Note existing = new Note(UUID.randomUUID(), "Заголовок", "текст",
                    Instant.parse("2026-01-01T10:00:00Z"), List.of("work"));
            when(repository.find(existing.id())).thenReturn(Optional.of(existing));

            assertThat(service.get(existing.id())).isEqualTo(existing);
        }

        @Test
        @DisplayName("отсутствующая заметка -> NoteNotFoundException с id в сообщении")
        void shouldThrowNotFoundForMissingNote() {
            UUID id = UUID.randomUUID();
            when(repository.find(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(id))
                    .isInstanceOf(NoteNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("Списки заметок")
    class Lists {

        @Test
        @DisplayName("list() отдаёт все заметки из хранилища")
        void listsAll() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.list()).isEmpty();

            verify(repository).findAll();
        }

        @Test
        @DisplayName("listByTag нормализует тег к нижнему регистру и передаёт в репозиторий")
        void listsByNormalizedTag() {
            when(repository.findByTag("work")).thenReturn(List.of());

            assertThat(service.listByTag("WORK")).isEmpty();

            verify(repository).findByTag("work");
        }
    }

    @Nested
    @DisplayName("Обновление заметки")
    class Update {

        @Test
        @DisplayName("заменяет содержимое и сохраняет createdAt исходной версии")
        void preservesCreatedAt() {
            UUID id = UUID.randomUUID();
            Instant original = Instant.parse("2026-01-01T10:00:00Z");
            Note existing = new Note(id, "Старый", "старый текст", original, List.of("old"));
            when(repository.replace(eq(id), any())).thenAnswer(invocation -> {
                UnaryOperator<Note> transformer = invocation.getArgument(1);
                return Optional.of(transformer.apply(existing));
            });

            Note updated = service.update(id, new NoteDraft("Новый", "новый текст", List.of("NEW")));

            assertThat(updated.title()).isEqualTo("Новый");
            assertThat(updated.content()).isEqualTo("новый текст");
            assertThat(updated.tags()).containsExactly("new");
            assertThat(updated.createdAt()).isEqualTo(original);
        }

        @Test
        @DisplayName("отсутствующая заметка -> NoteNotFoundException")
        void shouldThrowNotFoundWhenUpdatingMissingNote() {
            when(repository.replace(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(UUID.randomUUID(), new NoteDraft("Новый", "", List.of())))
                    .isInstanceOf(NoteNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Удаление заметки")
    class Delete {

        @Test
        @DisplayName("существующая заметка удаляется")
        void deletesExisting() {
            UUID id = UUID.randomUUID();
            when(repository.delete(id)).thenReturn(true);

            service.delete(id);

            verify(repository).delete(id);
        }

        @Test
        @DisplayName("отсутствующая заметка -> NoteNotFoundException")
        void shouldThrowNotFoundWhenDeletingMissingNote() {
            when(repository.delete(any())).thenReturn(false);

            assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                    .isInstanceOf(NoteNotFoundException.class);
        }
    }
}
