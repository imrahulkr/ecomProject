package com.ecommerce.project.exceptions;

import com.ecommerce.project.checkout.IdempotencyConflictException;
import com.ecommerce.project.payload.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Every handler here returns the same APIResponse envelope (timestamp/status/error/message/
// path[/errors]) - see APIResponse's javadoc. AuthEntryPointJwt (401) and RestAccessDeniedHandler
// (403) build the identical shape outside this advice, since Spring Security's filter chain runs
// before @RestControllerAdvice ever gets a chance to handle those.
// @Order(LOWEST_PRECEDENCE) so its Exception.class catch-all is always checked after every other
// @RestControllerAdvice (e.g. GlobalAuthExceptionHandler) -- otherwise unordered advice beans tie
// and Spring may pick this one first per-exception, swallowing more specific 4xx mappings as 500s.
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
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

    // Thrown by AccountLinkController.deleteLinkedAccount when unlinking would leave the user
    // with no way to log in (no password, no other linked provider) - a state conflict, not a
    // malformed request.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<APIResponse> myIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONFLICT", e.getMessage(), request, null);
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

    // A missing static file (e.g. /images/<deleted>.jpg) - without this the catch-all below turns it into a 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIResponse> myNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", request, null);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<APIResponse> myTooManyRequestsException(TooManyRequestsException e, HttpServletRequest request) {
        ResponseEntity<APIResponse> response = build(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", e.getMessage(), request, null);
        return ResponseEntity.status(response.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(response.getBody());
    }

    // ---- Client mistakes that used to fall through to the 500 catch-all ----

    // Missing or unparseable JSON body.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse> myHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed", request, null);
    }

    // e.g. ?pageNumber=abc or /orders/xyz
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse> myMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid value for parameter '" + e.getName() + "'", request, null);
    }

    // e.g. ?sortBy=doesNotExist - Spring Data rejects the property, sometimes wrapped by the
    // repository exception translator.
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<APIResponse> myPropertyReferenceException(RuntimeException e, HttpServletRequest request) {
        if (e instanceof PropertyReferenceException || e.getCause() instanceof PropertyReferenceException) {
            PropertyReferenceException pre = e instanceof PropertyReferenceException p ? p : (PropertyReferenceException) e.getCause();
            return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Unknown sort field '" + pre.getPropertyName() + "'", request, null);
        }
        return myGenericException(e, request);
    }

    // @Positive/@Min etc. on @PathVariable/@RequestParam of @Validated controllers.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIResponse> myConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        Map<String, String> violations = new HashMap<>();
        e.getConstraintViolations().forEach(v -> violations.put(v.getPropertyPath().toString(), v.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, violations);
    }

    // Unique/foreign-key violations (e.g. deleting something still referenced). The constraint
    // details are logged, never returned - they leak schema.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse> myDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        logger.warn("Data integrity violation on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data", request, null);
    }

    // Thrown from inside controllers/services (the filter chain's own 401/403 are handled by
    // AuthEntryPointJwt/RestAccessDeniedHandler and never reach here).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse> myAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse> myAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> myGenericException(Exception e, HttpServletRequest request) {
        // Spring MVC's own exceptions (405 method not allowed, 415 media type, missing
        // parameter/header, method-validation failures, ...) carry their proper status - keep it
        // instead of turning a client error into a 500.
        if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            String detail = errorResponse.getBody().getDetail();
            return build(status, status.name(), detail != null ? detail : status.getReasonPhrase(), request, null);
        }
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
