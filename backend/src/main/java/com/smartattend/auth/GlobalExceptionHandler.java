package com.smartattend.auth;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        List<AuthController.FieldException> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(this::fieldError).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException exception) {
        List<AuthController.FieldException> errors = exception.getConstraintViolations().stream()
            .map(error -> new AuthController.FieldException(error.getPropertyPath().toString(), error.getMessage())).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> status(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        List<AuthController.FieldException> errors = List.of(new AuthController.FieldException(null, message));
        return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponse(errors));
    }

    @ExceptionHandler(AuthController.RegistrationFieldException.class)
    ResponseEntity<ErrorResponse> registrationField(AuthController.RegistrationFieldException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(List.of(new AuthController.FieldException(exception.field(), exception.getMessage()))));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(List.of(new AuthController.FieldException(null, "An unexpected server error occurred"))));
    }

    private AuthController.FieldException fieldError(FieldError error) {
        return new AuthController.FieldException(error.getField(), error.getDefaultMessage());
    }

    public record ErrorResponse(List<AuthController.FieldException> errors) { }
}