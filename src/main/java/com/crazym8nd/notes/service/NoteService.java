package com.crazym8nd.notes.service;

import com.crazym8nd.notes.domain.Note;
import com.crazym8nd.notes.domain.NoteDraft;
import com.crazym8nd.notes.repository.NoteRepository;
import com.crazym8nd.notes.exception.NoteNotFoundException;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

/**
 * Правила работы с заметками: нормализация, инварианты, доступ к хранилищу
 * через {@link NoteRepository}. Транспортный формат — задача REST-слоя.
 *
 * <p>Данные нормализуются на входе: заголовок обрезается по краям; теги уходят
 * в нижний регистр, пустые и дубликаты отбрасываются. Выборки отсортированы
 * от новых к старым.</p>
 */
public interface NoteService {

    /**
     * Создаёт заметку: генерирует id и момент создания, нормализует поля и сохраняет.
     *
     * @param draft данные заметки; заголовок обязателен (не пустой после обрезки)
     * @return созданная заметка
     */
    Note create(@Valid NoteDraft draft);

    /**
     * @throws NoteNotFoundException если заметки с таким id нет
     */
    Note get(UUID id);

    /**
     * Возвращает все заметки, отсортированные от новых к старым.
     *
     * @return заметки; пустой список, если их нет
     */
    List<Note> list();

    /**
     * Возвращает заметки с тегом {@code tag}, отсортированные от новых к старым.
     *
     * @param tag тег в нормализованном виде: нижний регистр, без пробелов по краям
     * @return заметки; пустой список, если ни одна заметка тег не содержит
     */
    List<Note> listByTag(String tag);

    /**
     * Полностью заменяет содержимое заметки; createdAt сохраняется от предыдущей версии.
     *
     * @param draft новые данные заметки; заголовок обязателен и не может быть пустым
     *              после обрезки пробелов
     * @throws NoteNotFoundException если заметки с таким id нет
     */
    Note update(UUID id, @Valid NoteDraft draft);

    /**
     * @throws NoteNotFoundException если заметки с таким id нет
     */
    void delete(UUID id);
}
