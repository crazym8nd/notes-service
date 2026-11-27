package com.crazym8nd.notes.exception;

import com.crazym8nd.notes.dto.ProblemDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String PROBLEM_TYPE_PREFIX = "urn:problem:";

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ProblemDto> noteNotFound(NoteNotFoundException exception, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "note-not-found", "Заметка не найдена",
                exception.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDto> constraintViolation(ConstraintViolationException exception,
                                                       HttpServletRequest request) {
        String detail = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, "validation-failed", "Запрос не прошёл валидацию",
                detail, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDto> invalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, "validation-failed", "Запрос не прошёл валидацию",
                detail, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDto> unsupportedMediaType(HttpMediaTypeNotSupportedException exception,
                                                           HttpServletRequest request) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type",
                "Неподдерживаемый тип содержимого", exception.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDto> unreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "malformed-body", "Некорректное тело запроса",
                "Тело запроса не читается: ожидается корректный JSON", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDto> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Необработанная ошибка при обработке запроса {} {}",
                request.getMethod(), request.getRequestURI(), exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Внутренняя ошибка сервиса",
                "Детали — в логах сервиса", request);
    }

    private ResponseEntity<ProblemDto> respond(HttpStatus status, String slug, String title,
                                            String detail, HttpServletRequest request) {
        if (status.is4xxClientError()) {
            log.warn("Клиентская ошибка {} {}: {}", request.getMethod(), request.getRequestURI(), detail);
        }
        ProblemDto problem = new ProblemDto();
        problem.setType(URI.create(PROBLEM_TYPE_PREFIX + slug));
        problem.setTitle(title);
        problem.setStatus(status.value());
        problem.setDetail(detail);
        problem.setInstance(request.getRequestURI());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
