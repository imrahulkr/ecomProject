package com.ecommerce.project.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalAuthExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return errorResponse(HttpStatus.CONFLICT, "Email_ALREADY_REGISTERED", ex.getMessage());
    }

    @ExceptionHandler(PasswordSignupBlockedException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordSignupBlocked(PasswordSignupBlockedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "PASSWORD_SIGNUP_BLOCKED",
                "message", ex.getMessage(),
                "existingProvider", ex.getExistingProvider(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(AccountLinkingRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLinkingRequired(AccountLinkingRequiredException ex) {
        return errorResponse(HttpStatus.CONFLICT, "ACCOUNT_LINKING_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return errorResponse(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return errorResponse(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String code, String message){
        return ResponseEntity.status(status).body(Map.of(
                "error", code,
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }
}
