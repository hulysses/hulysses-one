package br.com.hulysses.hulysses_one.shared.domain;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            EntityNotFoundException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now(),
                                "status", 404,
                                "error", "Not Found",
                                "message", exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateEntityException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now(),
                                "status", 400,
                                "error", "Bad Request",
                                "message", exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(
            IllegalArgumentException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now(),
                                "status", 400,
                                "error", "Bad Request",
                                "message", exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidation(
            MethodArgumentNotValidException exception
    ) {

        Map<String, String> errors =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        FieldError::getField,
                                        error ->
                                                error.getDefaultMessage() == null
                                                        ? "Invalid value"
                                                        : error.getDefaultMessage(),
                                        (first, second) -> first
                                )
                        );

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "timestamp",
                                LocalDateTime.now(),

                                "status",
                                400,

                                "error",
                                "Validation error",

                                "fields",
                                errors
                        )
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>>
    handleDataIntegrity(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now(),
                                "status", 409,
                                "error", "Conflict",
                                "message",
                                "Resource cannot be modified because it is referenced by another resource"
                        )
                );
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(
            DomainException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now(),
                                "status", 400,
                                "error", "Bad Request",
                                "message", exception.getMessage()
                        )
                );
    }
}