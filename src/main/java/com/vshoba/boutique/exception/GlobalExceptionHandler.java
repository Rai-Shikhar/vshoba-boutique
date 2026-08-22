package com.vshoba.boutique.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * The single place where EVERY error thrown by a controller/service
 * becomes an HTTP response with a clean JSON body.
 *
 * Without this class, an exception would produce a big ugly stack trace
 * in the response. With it, the API always answers:
 *   { "status": <code>, "message": "...", "timestamp": "..." }
 *
 * @RestControllerAdvice = "watch every controller, globally".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBusinessRule(BusinessRuleException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    /**
     * Wrong email/password at login -> 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthentication(AuthenticationException ex) {
        return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid email or password");
    }

    /**
     * Logged in, but not allowed (e.g. customer calling an admin route).
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponse(HttpStatus.FORBIDDEN.value(), "You are not allowed to do this");
    }

    /**
     * Triggered by @Valid when the JSON body fails validation
     * (e.g. missing name, negative price, bad email format).
     * Joins every failed field into one readable message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * Triggered when a required query parameter is missing
     * (e.g. GET /api/products/search without ?q=).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    /**
     * Triggered when a path/query parameter has the wrong type
     * (e.g. GET /api/products/abc where "abc" is not a Long).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'");
    }
}
