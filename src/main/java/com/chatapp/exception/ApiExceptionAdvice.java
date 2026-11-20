package com.chatapp.exception;

import com.chatapp.dto.StatusResponse;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionAdvice.class);

    private ResponseEntity<StatusResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StatusResponse(message, null, "error"));
    }

    @ExceptionHandler(RoomNotFoundException.class)
    ResponseEntity<StatusResponse> missing(RoomNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RoomAlreadyExistsException.class)
    ResponseEntity<StatusResponse> duplicate(RoomAlreadyExistsException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ParticipantNotInRoomException.class)
    ResponseEntity<StatusResponse> forbidden(ParticipantNotInRoomException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<StatusResponse> invalid(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<StatusResponse> unexpected(Exception e) {
        log.error("Request failed", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }
}
