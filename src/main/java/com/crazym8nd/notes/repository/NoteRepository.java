package com.crazym8nd.notes.repository;

import com.crazym8nd.notes.domain.Note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Хранилище заметок — порт в терминах DIP: сервис зависит только от этого
 * контракта, реализация подменяется без правок сервиса.
 *
 * <p>Все методы потокобезопасны. Методы записи возвращают предыдущую версию
 * записи — по ней отличают создание от замены.</p>
 */
public interface NoteRepository {

    /**
     * Сохраняет заметку: добавляет новую или заменяет существующую с тем же {@code id}.
     *
     * @param note заметка с заполненными {@code id} и {@code createdAt}
     * @return предыдущая версия записи с этим id; пустой {@link Optional}, если записи не было
     */
    Optional<Note> put(Note note);

    /**
     * Атомарно заменяет существующую заметку результатом применения
     * {@code transformer}; преобразование применяется не более одного раза.
     *
     * @param id          идентификатор заметки
     * @param transformer функция, строящая новую версию из предыдущей
     * @return обновлённая заметка; пустой {@link Optional}, если записи с таким id нет
     */
    Optional<Note> replace(UUID id, UnaryOperator<Note> transformer);

    /**
     * Находит заметку по идентификатору.
     *
     * @return заметка, если существует
     */
    Optional<Note> find(UUID id);

    /**
     * Возвращает все заметки, отсортированные от новых к старым.
     *
     * @return заметки; пустой список, если хранилище пусто
     */
    List<Note> findAll();

    /**
     * Возвращает заметки с тегом {@code tag}, отсортированные от новых к старым.
     *
     * @param tag тег в нормализованном виде: нижний регистр, без пробелов по краям
     * @return заметки; пустой список, если ни одна заметка тег не содержит
     */
    List<Note> findByTag(String tag);

    /**
     * Удаляет заметку по идентификатору.
     *
     * @param id идентификатор заметки
     * @return {@code true}, если запись существовала и удалена
     */
    boolean delete(UUID id);
}
