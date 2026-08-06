package com.ecommerce.project.exceptions;

import com.ecommerce.project.checkout.IdempotencyConflictException;
import com.ecommerce.project.payload.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Every handler here returns the same APIResponse envelope (timestamp/status/error/message/
// path[/errors]) - see APIResponse's javadoc. AuthEntryPointJwt (401) and RestAccessDeniedHandler
// (403) build the identical shape outside this advice, since Spring Security's filter chain runs
// before @RestControllerAdvice ever gets a chance to handle those.
@RestControllerAdvice
public class MyGlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyGlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse> myMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(fieldName, message);
        });
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse> myResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse> myIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request, null);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<APIResponse> myIdempotencyConflictException(IdempotencyConflictException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", e.getMessage(), request, null);
    }

    @ExceptionHandler(APIException.class)
    public ResponseEntity<APIResponse> myAPIException(APIException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request, null);
    }

    // Thrown by Spring's multipart parsing itself (before any controller/ImageUploadValidator
    // code runs) once a request exceeds spring.servlet.multipart.max-file-size/max-request-size
    // - without this it falls through to the generic 500 handler below, which is both the wrong
    // status code and a confusing message for what's actually a client error.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<APIResponse> myMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Uploaded file is too large", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> myGenericException(Exception e, HttpServletRequest request) {
        logger.error("Unhandled exception", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", request, null);
    }

    private ResponseEntity<APIResponse> build(HttpStatus status, String error, String message,
                                               HttpServletRequest request, Map<String, String> fieldErrors) {
        APIResponse body = APIResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();
        return new ResponseEntity<>(body, status);
    }
}
